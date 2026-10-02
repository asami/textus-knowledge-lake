package org.simplemodeling.textus.knowledgelake.local

import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.knowledgelake.model.*

/** Executable pending-first, producer/correlation and explicit recovery contract.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
class ProposalDeliverySpec extends AnyWordSpec with GivenWhenThen with Matchers:
  "Pending-first delivery" should {
    "publish pending before the port sees state and persist exact confirmed correspondence" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("a stored immutable proposal and a port that reads the published state")
      val initial = LocalFlowSpecSupport.seed(directory)
      val store = new FilePreparationStore(directory)
      val request = LocalFlowSpecSupport.request(initial)
      val observation = LocalFlowSpecSupport.confirmed(request)
      var seen: Option[LocalState] = None
      var calls = 0
      val port = new ProposalTransport:
        def exchange(value: ProposalRequest): ProposalObservation =
          calls += 1
          seen = new FilePreparationStore(directory).read().toOption
          observation
      When("the initial Submit reserves and invokes the port once")
      val completed = ProposalDelivery.execute(store, initial.revision, request, port).toOption.get
      Then("the port saw durable pending and the confirmed receipt/reference remain exact")
      calls shouldBe 1
      seen.get.revision shouldBe 2L
      seen.get.deliveries shouldBe Vector(DeliveryAttempt(request, None))
      completed.revision shouldBe 3L
      completed.deliveries shouldBe Vector(DeliveryAttempt(request, Some(observation)))
      completed.bundle shouldBe initial.bundle
      completed.deliveries.head.observation.get.receiptId shouldBe Some("receipt/確認")
      completed.deliveries.head.observation.get.candidateRef shouldBe Some(VersionedRef("candidate/日本語", 1L))
      new FilePreparationStore(directory).read() shouldBe Right(completed)
    }

    Vector(false, true).foreach { throws =>
      Vector(DeliveryOperation.Query, DeliveryOperation.Submit).foreach { operation =>
        s"recover ${if throws then "port exception" else "Unknown"} with explicit $operation and the same payload" in LocalFlowSpecSupport.withDirectory { directory =>
          Given("a stored proposal and a first port that has an uncertain fixture result")
          val initial = LocalFlowSpecSupport.seed(directory)
          val store = new FilePreparationStore(directory)
          val first = LocalFlowSpecSupport.request(initial, "first-correlation")
          var calls = 0
          var invoked = Vector.empty[ProposalRequest]
          val firstport = new ProposalTransport:
            def exchange(value: ProposalRequest): ProposalObservation =
              calls += 1
              invoked = invoked :+ value
              if throws then throw new java.io.IOException("simulated timeout")
              else ProposalDelivery._unknown(value, "Simulated timeout; outcome unknown")
          When("the initial Submit observes uncertainty and an explicit new-correlation recovery is issued")
          val unresolved = ProposalDelivery.execute(store, initial.revision, first, firstport).toOption.get
          val recovery = ProposalRequest("recovery-correlation", operation, first.proposal)
          val observation = LocalFlowSpecSupport.confirmed(recovery)
          val fixtureport = new FixtureProposalTransport(Vector(observation))
          val secondport = new ProposalTransport:
            def exchange(value: ProposalRequest): ProposalObservation =
              calls += 1
              invoked = invoked :+ value
              fixtureport.exchange(value)
          val recovered = ProposalDelivery.execute(store, unresolved.revision, recovery, secondport).toOption.get
          Then("Unknown was retained and the new fixture assertion maps to the unchanged producer key and payload")
          calls shouldBe 2
          invoked shouldBe Vector(first, recovery)
          all(invoked.map(_.proposal)) shouldBe first.proposal
          unresolved.deliveries.head.observation.get.outcome shouldBe ProposalOutcome.Unknown
          unresolved.deliveries.head.observation.get.receiptId shouldBe None
          unresolved.deliveries.head.observation.get.candidateRef shouldBe None
          recovered.deliveries.map(_.observation.get.outcome) shouldBe Vector(ProposalOutcome.Unknown, ProposalOutcome.Confirmed)
          recovered.deliveries.last.observation shouldBe Some(observation)
          recovered.bundle.proposals shouldBe initial.bundle.proposals
        }
      }
    }
  }

  "Observation validation" should {
    val mismatches: Vector[(String, ProposalObservation => ProposalObservation)] = Vector(
      "correlation" -> (observation => observation.copy(correlationId = "foreign-correlation")),
      "producer" -> (observation => observation.copy(producerId = "foreign-producer")),
      "proposal" -> (observation => observation.copy(proposalId = "foreign-proposal"))
    )
    mismatches.foreach { (field, change) =>
      s"convert wrong $field to matching Unknown without foreign receipt/reference" in LocalFlowSpecSupport.withDirectory { directory =>
        Given(s"a request and a confirmed assertion with a mismatched $field")
        val initial = LocalFlowSpecSupport.seed(directory)
        val request = LocalFlowSpecSupport.request(initial)
        val foreign = change(LocalFlowSpecSupport.confirmed(request))
        val port = new ProposalTransport:
          def exchange(value: ProposalRequest): ProposalObservation = foreign
        When("the returned assertion is checked against all request identity dimensions")
        val completed = ProposalDelivery.execute(new FilePreparationStore(directory), initial.revision, request, port).toOption.get
        val observation = completed.deliveries.head.observation.get
        Then("a matching Unknown replaces the foreign assertion without changing the request")
        completed.deliveries.head.request shouldBe request
        observation.correlationId shouldBe request.correlationId
        observation.producerId shouldBe request.proposal.producerId
        observation.proposalId shouldBe request.proposal.proposalId
        observation.outcome shouldBe ProposalOutcome.Unknown
        observation.receiptId shouldBe None
        observation.candidateRef shouldBe None
        observation.message should include("mismatch")
      }
    }

    val malformed: Vector[(String, ProposalObservation => ProposalObservation)] = Vector(
      "missing receipt" -> (observation => observation.copy(receiptId = None)),
      "blank ECMAScript receipt" -> (observation => observation.copy(receiptId = Some("\ufeff\u2003"))),
      "missing Candidate" -> (observation => observation.copy(candidateRef = None)),
      "nonpositive Candidate" -> (observation => observation.copy(candidateRef = Some(VersionedRef("candidate", 0L)))),
      "blank Candidate" -> (observation => observation.copy(candidateRef = Some(VersionedRef("\u00a0", 1L)))),
      "nonconfirmed receipt and Candidate" -> (observation => observation.copy(outcome = ProposalOutcome.Unknown)),
      "rejected receipt" -> (observation => observation.copy(outcome = ProposalOutcome.Rejected, candidateRef = None)),
      "conflict Candidate" -> (observation => observation.copy(outcome = ProposalOutcome.Conflict, receiptId = None)),
      "blank message" -> (observation => observation.copy(message = "\u3000"))
    )
    malformed.foreach { (label, change) =>
      s"retain original request and record Unknown for $label" in LocalFlowSpecSupport.withDirectory { directory =>
        Given(s"an immutable request and a port that returns $label")
        val initial = LocalFlowSpecSupport.seed(directory)
        val request = LocalFlowSpecSupport.request(initial)
        val bad = change(LocalFlowSpecSupport.confirmed(request))
        val port = new ProposalTransport:
          def exchange(value: ProposalRequest): ProposalObservation = bad
        When("delivery validates the returned observation before result recording")
        val completed = ProposalDelivery.execute(new FilePreparationStore(directory), initial.revision, request, port).toOption.get
        val observation = completed.deliveries.head.observation.get
        Then("the malformed fields cannot survive as a confirmed or nonconfirmed receipt")
        completed.deliveries.head.request shouldBe request
        observation.outcome shouldBe ProposalOutcome.Unknown
        observation.receiptId shouldBe None
        observation.candidateRef shouldBe None
        observation.message should include("Malformed observation")
      }
    }
  }

  "Reservation failures" should {
    Vector("stale", "cas-conflict", "payload", "blank", "duplicate", "query-without-prior", "reservation-unknown").foreach { defect =>
      s"never invoke the port after $defect" in LocalFlowSpecSupport.withDirectory { directory =>
        Given(s"a stored proposal and a reservation request with $defect")
        val initial = LocalFlowSpecSupport.seed(directory)
        val delegate = new FilePreparationStore(directory)
        val original = LocalFlowSpecSupport.request(initial)
        val current = if defect == "duplicate" then
          delegate.compareAndSet(initial.revision, LocalDraft(initial.bundle, Vector(DeliveryAttempt(original, None)))).toOption.get
        else initial
        val request = defect match
          case "payload" => original.copy(proposal = original.proposal.copy(title = "Different payload"))
          case "blank" => original.copy(correlationId = "\ufeff\u2003")
          case "query-without-prior" => original.copy(operation = DeliveryOperation.Query)
          case _ => original
        val store: PreparationStore = if defect == "cas-conflict" then
          new PreparationStore:
            def read(): Either[LocalError, LocalState] = delegate.read()
            def compareAndSet(expectedRevision: Long, draft: LocalDraft): Either[LocalError, LocalState] =
              delegate.compareAndSet(expectedRevision, LocalDraft(current.bundle, current.deliveries))
              delegate.compareAndSet(expectedRevision, draft)
        else if defect == "reservation-unknown" then new FilePreparationStore(directory, LocalFlowSpecSupport.fault(StoreWriteStage.AfterPublish))
        else delegate
        var calls = 0
        val port = new ProposalTransport:
          def exchange(value: ProposalRequest): ProposalObservation =
            calls += 1
            LocalFlowSpecSupport.confirmed(value)
        When("the invalid, conflicting or uncertain reservation is attempted")
        val result = ProposalDelivery.execute(store, if defect == "stale" then 0L else current.revision, request, port)
        Then("an error is returned with zero exchange calls")
        result.isLeft shouldBe true
        calls shouldBe 0
        if defect == "reservation-unknown" then
          result.swap.toOption.get.writeOutcome shouldBe LocalWriteOutcome.Unknown
          delegate.read().toOption.get.deliveries shouldBe Vector(DeliveryAttempt(original, None))
      }
    }

    "never invoke the port when the stable reservation lock is busy" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("an existing proposal and a held stable OS file lock")
      val initial = LocalFlowSpecSupport.seed(directory)
      val channel = FileChannel.open(directory.resolve("state.lock"), StandardOpenOption.WRITE)
      val lock = channel.lock()
      var calls = 0
      val port = new ProposalTransport:
        def exchange(value: ProposalRequest): ProposalObservation =
          calls += 1
          LocalFlowSpecSupport.confirmed(value)
      try
        When("Submit cannot acquire the reservation lock")
        val result = ProposalDelivery.execute(new FilePreparationStore(directory), initial.revision, LocalFlowSpecSupport.request(initial), port)
        Then("busy is NotApplied and the port was never called")
        result.swap.toOption.get.code shouldBe "busy"
        result.swap.toOption.get.writeOutcome shouldBe LocalWriteOutcome.NotApplied
        calls shouldBe 0
      finally
        lock.release()
        channel.close()
      new FilePreparationStore(directory).read() shouldBe Right(initial)
    }
  }

  "Result recording uncertainty" should {
    "retain pending after final conflict and recover by explicit new-correlation Query" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("a port that publishes a concurrent local update during its single exchange")
      val initial = LocalFlowSpecSupport.seed(directory)
      val store = new FilePreparationStore(directory)
      val request = LocalFlowSpecSupport.request(initial, "first-correlation")
      var calls = 0
      val port = new ProposalTransport:
        def exchange(value: ProposalRequest): ProposalObservation =
          calls += 1
          val pending = store.read().toOption.get
          store.compareAndSet(pending.revision, LocalDraft(pending.bundle, pending.deliveries))
          LocalFlowSpecSupport.confirmed(value)
      When("reservation succeeds but completion CAS observes the concurrent revision")
      val result = ProposalDelivery.execute(store, initial.revision, request, port)
      val pending = store.read().toOption.get
      Then("result-not-recorded keeps the cause and pending reservation without retry")
      calls shouldBe 1
      result.swap.toOption.get.code shouldBe "result-not-recorded"
      result.swap.toOption.get.writeOutcome shouldBe LocalWriteOutcome.Unknown
      result.swap.toOption.get.observedRevision shouldBe Some(3L)
      result.swap.toOption.get.message should include("revision-conflict")
      pending.deliveries shouldBe Vector(DeliveryAttempt(request, None))
      Given("the explicit pending state and a new Query correlation for the exact same payload")
      val query = ProposalRequest("query-correlation", DeliveryOperation.Query, request.proposal)
      val observation = LocalFlowSpecSupport.confirmed(query)
      val queryport = new ProposalTransport:
        def exchange(value: ProposalRequest): ProposalObservation =
          calls += 1
          new FixtureProposalTransport(Vector(observation)).exchange(value)
      When("the caller explicitly queries the unresolved producer key")
      val recovered = ProposalDelivery.execute(store, pending.revision, query, queryport).toOption.get
      Then("the old pending request stays exact and the new matching confirmation is recorded")
      calls shouldBe 2
      recovered.deliveries.head shouldBe DeliveryAttempt(request, None)
      recovered.deliveries.last shouldBe DeliveryAttempt(query, Some(observation))
      recovered.bundle.proposals shouldBe initial.bundle.proposals
    }

    "resolve a potentially applied final CAS through read without reverting the observation" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("an observer that faults after publication of the final observation")
      val initial = LocalFlowSpecSupport.seed(directory)
      var publications = 0
      val observer = new StoreWriteObserver:
        def observe(stage: StoreWriteStage): Unit =
          if stage == StoreWriteStage.AfterPublish then
            publications += 1
            if publications == 2 then throw new java.io.IOException("final publication observer fault")
      val store = new FilePreparationStore(directory, observer)
      val request = LocalFlowSpecSupport.request(initial)
      val observation = LocalFlowSpecSupport.confirmed(request)
      var calls = 0
      val port = new ProposalTransport:
        def exchange(value: ProposalRequest): ProposalObservation =
          calls += 1
          observation
      When("completion may have applied before the reported failure")
      val result = ProposalDelivery.execute(store, initial.revision, request, port)
      val actual = new FilePreparationStore(directory).read().toOption.get
      Then("result-not-recorded remains uncertain while explicit read exposes the actual complete observation")
      result.swap.toOption.get.code shouldBe "result-not-recorded"
      result.swap.toOption.get.writeOutcome shouldBe LocalWriteOutcome.Unknown
      result.swap.toOption.get.message should include("final publication observer fault")
      calls shouldBe 1
      publications shouldBe 2
      actual.revision shouldBe 3L
      actual.deliveries shouldBe Vector(DeliveryAttempt(request, Some(observation)))
    }
  }

  "Terminal and producer-scoped keys" should {
    Vector(ProposalOutcome.Confirmed, ProposalOutcome.Rejected, ProposalOutcome.Conflict).foreach { outcome =>
      s"retain observed $outcome and reject later Submit/Query without invocation" in LocalFlowSpecSupport.withDirectory { directory =>
        Given(s"an explicit $outcome fixture for one producer-scoped proposal")
        val initial = LocalFlowSpecSupport.seed(directory)
        val store = new FilePreparationStore(directory)
        val first = LocalFlowSpecSupport.request(initial, "first")
        val observation = if outcome == ProposalOutcome.Confirmed then LocalFlowSpecSupport.confirmed(first)
        else ProposalObservation(first.correlationId, first.proposal.producerId, first.proposal.proposalId, outcome, None, None, s"Fixture-only $outcome")
        val completed = ProposalDelivery.execute(store, initial.revision, first, new FixtureProposalTransport(Vector(observation))).toOption.get
        var calls = 0
        val port = new ProposalTransport:
          def exchange(value: ProposalRequest): ProposalObservation =
            calls += 1
            LocalFlowSpecSupport.confirmed(value)
        When("a terminal key is submitted or queried with new correlations")
        val results = Vector(DeliveryOperation.Submit, DeliveryOperation.Query).map(operation =>
          ProposalDelivery.execute(store, completed.revision, ProposalRequest(s"later-$operation", operation, first.proposal), port))
        Then("both attempts fail locally and the distinct observed outcome stays immutable")
        all(results.map(_.swap.toOption.get.code)) shouldBe "terminal"
        calls shouldBe 0
        store.read() shouldBe Right(completed)
        completed.deliveries.head.observation.get.outcome shouldBe outcome
      }
    }

    "correlate identical local proposal IDs independently under different producers" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("two producers using the same local proposal ID and a stateless exact fixture transport")
      val original = LocalFlowSpecSupport.bundle()
      val firstproposal = original.proposals.head
      val secondproposal = firstproposal.copy(producerId = "independent-producer")
      val initial = LocalFlowSpecSupport.seed(directory, original.copy(proposals = original.proposals :+ secondproposal))
      val store = new FilePreparationStore(directory)
      val first = ProposalRequest("producer-A-correlation", DeliveryOperation.Submit, firstproposal)
      val second = ProposalRequest("producer-B-correlation", DeliveryOperation.Submit, secondproposal)
      val firstobservation = LocalFlowSpecSupport.confirmed(first)
      val secondobservation = LocalFlowSpecSupport.confirmed(second).copy(receiptId = Some("producer-B-receipt"), candidateRef = Some(VersionedRef("producer-B-candidate", 2L)))
      val transport = new FixtureProposalTransport(Vector(secondobservation, firstobservation))
      When("both producer keys are submitted in order despite sharing their local proposal ID")
      val firststate = ProposalDelivery.execute(store, initial.revision, first, transport).toOption.get
      val secondstate = ProposalDelivery.execute(store, firststate.revision, second, transport).toOption.get
      Then("each exact correlation maps to its own receipt/Candidate and unchanged proposal")
      secondstate.deliveries.map(_.request) shouldBe Vector(first, second)
      secondstate.deliveries.map(_.observation.get) shouldBe Vector(firstobservation, secondobservation)
      secondstate.bundle.proposals shouldBe Vector(firstproposal, secondproposal)
      first.proposal.proposalId shouldBe second.proposal.proposalId
    }
  }

  "Stateless fixture transport" should {
    "return matching Unknown for unsupported lookup without inventing a receipt" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("a stored proposal and only foreign correlation/key fixture assertions")
      val initial = LocalFlowSpecSupport.seed(directory)
      val request = LocalFlowSpecSupport.request(initial)
      val foreign = LocalFlowSpecSupport.confirmed(request).copy(producerId = "foreign-producer")
      val transport = new FixtureProposalTransport(Vector(foreign))
      When("the unsupported exact request is delivered against the fixture transport")
      val result = ProposalDelivery.execute(new FilePreparationStore(directory), initial.revision, request, transport).toOption.get
      val observation = result.deliveries.head.observation.get
      Then("Unknown is explicit, matching and carries no implied downstream persistence")
      observation.correlationId shouldBe request.correlationId
      observation.producerId shouldBe request.proposal.producerId
      observation.proposalId shouldBe request.proposal.proposalId
      observation.outcome shouldBe ProposalOutcome.Unknown
      observation.receiptId shouldBe None
      observation.candidateRef shouldBe None
      observation.message should include("Missing fixture")
      result.bundle.proposals shouldBe initial.bundle.proposals
    }
  }
