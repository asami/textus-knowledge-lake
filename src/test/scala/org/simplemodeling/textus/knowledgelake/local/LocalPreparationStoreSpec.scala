package org.simplemodeling.textus.knowledgelake.local

import io.circe.Json
import io.circe.parser.parse
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.{AtomicMoveNotSupportedException, Files, Path, StandardOpenOption}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.knowledgelake.model.*
import scala.jdk.CollectionConverters.*

/** Disposable local fixtures shared only by the authored executable specifications.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
private[local] object LocalFlowSpecSupport:
  def fixture(): LocalFlowFixture = LocalFlowJson.decodeFixture(Files.readString(Path.of("fixtures/tkl/local-flow-v1.json"), StandardCharsets.UTF_8)).toOption.get
  def bundle(): PreparationBundle = PreparationJson.decode(Files.readString(Path.of("src/test/resources/tkl/preparation-bundle-v1.json"), StandardCharsets.UTF_8)).toOption.get
  def withDirectory[A](body: Path => A): A =
    val directory = Files.createTempDirectory("tkl-local-spec-")
    try body(directory)
    finally
      val stream = Files.walk(directory)
      try stream.iterator().asScala.toVector.sortBy(_.getNameCount).reverse.foreach(target => Files.deleteIfExists(target))
      finally stream.close()
  def seed(directory: Path, initial: PreparationBundle = bundle()): LocalState =
    new FilePreparationStore(directory).compareAndSet(0L, LocalDraft(initial, Vector.empty)).toOption.get
  def fault(stage: StoreWriteStage, error: () => Exception = () => new java.io.IOException("injected fault")): StoreWriteObserver =
    new StoreWriteObserver:
      def observe(current: StoreWriteStage): Unit = if current == stage then throw error()
  def request(state: LocalState, correlation: String = "correlation/日本語", operation: DeliveryOperation = DeliveryOperation.Submit): ProposalRequest =
    ProposalRequest(correlation, operation, state.bundle.proposals.head)
  def confirmed(request: ProposalRequest): ProposalObservation =
    ProposalObservation(request.correlationId, request.proposal.producerId, request.proposal.proposalId,
      ProposalOutcome.Confirmed, Some("receipt/確認"), Some(VersionedRef("candidate/日本語", 1L)), "Fixture-only confirmation")

/** Executable local persistence, immutable history and publication-fault contract.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
class LocalPreparationStoreSpec extends AnyWordSpec with GivenWhenThen with Matchers:
  "Virtual and published snapshots" should {
    "persist lossless canonical values and reopen the same snapshot" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("an absent primary state and opaque slash/Japanese fixture values")
      val store = new FilePreparationStore(directory)
      val initial = LocalFlowSpecSupport.bundle()
      When("the missing state is read and a seed is published through CAS")
      val empty = store.read()
      val seeded = store.compareAndSet(0L, LocalDraft(initial, Vector.empty))
      Then("revision zero was virtual and revision one contains the complete exact bundle")
      empty shouldBe Right(LocalPreparationStore.EMPTY)
      seeded shouldBe Right(LocalState(1L, initial, Vector.empty))
      new FilePreparationStore(directory).read() shouldBe seeded
      Files.readString(directory.resolve("state.json")) shouldBe LocalFlowJson.encodeState(seeded.toOption.get).toOption.get
      val stream = Files.list(directory)
      try stream.iterator().asScala.map(_.getFileName.toString).toSet shouldBe Set("state.json", "state.lock")
      finally stream.close()
    }

    "reject the second reader's stale CAS without changing committed bytes" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("two store instances that read the same published revision")
      LocalFlowSpecSupport.seed(directory)
      val first = new FilePreparationStore(directory)
      val second = new FilePreparationStore(directory)
      val left = first.read().toOption.get
      val right = second.read().toOption.get
      When("the first reader commits and the second uses its stale revision")
      val committed = first.compareAndSet(left.revision, LocalDraft(left.bundle, left.deliveries)).toOption.get
      val bytes = Files.readAllBytes(directory.resolve("state.json")).toVector
      val stale = second.compareAndSet(right.revision, LocalDraft(right.bundle, right.deliveries))
      Then("the second reports conflict, actual revision and no publication")
      stale.swap.toOption.get.code shouldBe "revision-conflict"
      stale.swap.toOption.get.writeOutcome shouldBe LocalWriteOutcome.NotApplied
      stale.swap.toOption.get.observedRevision shouldBe Some(committed.revision)
      Files.readAllBytes(directory.resolve("state.json")).toVector shouldBe bytes
    }

    "return busy under a held stable OS lock and release overlapping JVM resources" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("a stable lock file held through an independent FileChannel")
      val state = LocalFlowSpecSupport.seed(directory)
      val bytes = Files.readAllBytes(directory.resolve("state.json")).toVector
      val channel = FileChannel.open(directory.resolve("state.lock"), StandardOpenOption.WRITE)
      val lock = channel.lock()
      try
        When("two store instances try nonblocking overlapping locks")
        val attempts = Vector(new FilePreparationStore(directory), new FilePreparationStore(directory)).map(_.compareAndSet(state.revision, LocalDraft(state.bundle, state.deliveries)))
        Then("both return busy without modifying the published snapshot")
        all(attempts.map(_.swap.toOption.get.code)) shouldBe "busy"
        all(attempts.map(_.swap.toOption.get.writeOutcome)) shouldBe LocalWriteOutcome.NotApplied
        Files.readAllBytes(directory.resolve("state.json")).toVector shouldBe bytes
      finally
        lock.release()
        channel.close()
      When("the held channel and lock have been released")
      val result = new FilePreparationStore(directory).compareAndSet(state.revision, LocalDraft(state.bundle, state.deliveries))
      Then("the same stable lock can be acquired by a later operation")
      result.toOption.get.revision shouldBe state.revision + 1L
    }
  }

  "Atomic publication failure" should {
    Vector(StoreWriteStage.TemporarySynced, StoreWriteStage.BeforePublish).foreach { stage =>
      s"preserve the old complete snapshot after a fault at $stage" in LocalFlowSpecSupport.withDirectory { directory =>
        Given("a published seed and a draft that appends a complete frozen Run and material")
        val fixture = LocalFlowSpecSupport.fixture()
        val state = LocalFlowSpecSupport.seed(directory, fixture.initial)
        val bytes = Files.readAllBytes(directory.resolve("state.json")).toVector
        val unrelated = directory.resolve(".state-interrupted.tmp")
        Files.writeString(unrelated, "interrupted unrelated file")
        val store = new FilePreparationStore(directory, LocalFlowSpecSupport.fault(stage))
        val command = fixture.preparation
        When("preparation reaches the deterministic prepublication fault")
        val result = PreparationFlow.prepare(store, state.revision, command.contextRef, command.inputId, command.runId, command.result)
        Then("NotApplied preserves the exact old state and ignores unrelated temporary files")
        result.swap.toOption.get.writeOutcome shouldBe LocalWriteOutcome.NotApplied
        new FilePreparationStore(directory).read() shouldBe Right(state)
        Files.readAllBytes(directory.resolve("state.json")).toVector shouldBe bytes
        Files.readString(unrelated) shouldBe "interrupted unrelated file"
        state.bundle.runs shouldBe empty
        state.bundle.materials shouldBe empty
      }
    }

    Vector[() => Exception](
      () => new java.io.IOException("after publish"),
      () => new AtomicMoveNotSupportedException("state", "state", "after publish observer")
    ).zipWithIndex.foreach { (error, index) =>
      s"resolve postpublication uncertainty without rollback or automatic retry $index" in LocalFlowSpecSupport.withDirectory { directory =>
        Given("a seed and an observer that faults only after a successful atomic move")
        val fixture = LocalFlowSpecSupport.fixture()
        val seed = LocalFlowSpecSupport.seed(directory, fixture.initial)
        val command = fixture.preparation
        val store = new FilePreparationStore(directory, LocalFlowSpecSupport.fault(StoreWriteStage.AfterPublish, error))
        When("one complete preparation is published before the observer throws")
        val result = PreparationFlow.prepare(store, seed.revision, command.contextRef, command.inputId, command.runId, command.result)
        val reopened = new FilePreparationStore(directory).read().toOption.get
        Then("Unknown is resolved by an explicit read of one complete new state")
        result.swap.toOption.get.writeOutcome shouldBe LocalWriteOutcome.Unknown
        reopened.revision shouldBe 2L
        reopened.bundle.runs.map(_.id) shouldBe Vector(command.runId)
        reopened.bundle.materials.map(_.producingRunId) shouldBe Vector(command.runId)
      }
    }

    "refuse unsupported atomic publication without falling back" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("a published snapshot and an atomic-move-unavailable failure seam")
      val state = LocalFlowSpecSupport.seed(directory)
      val bytes = Files.readAllBytes(directory.resolve("state.json")).toVector
      val observer = LocalFlowSpecSupport.fault(StoreWriteStage.BeforePublish,
        () => new AtomicMoveNotSupportedException("temporary", "state", "unsupported fixture filesystem"))
      When("the publication operation cannot reach an atomic move")
      val result = new FilePreparationStore(directory, observer).compareAndSet(state.revision, LocalDraft(state.bundle, state.deliveries))
      Then("the failure is NotApplied and the old bytes are still published")
      result.swap.toOption.get.code shouldBe "io"
      result.swap.toOption.get.writeOutcome shouldBe LocalWriteOutcome.NotApplied
      Files.readAllBytes(directory.resolve("state.json")).toVector shouldBe bytes
      new FilePreparationStore(directory).read() shouldBe Right(state)
    }
  }

  "Immutable bundle identity" should {
    val mutations: Vector[(String, PreparationBundle => PreparationBundle)] = Vector(
      "changed same-ref Resource" -> (bundle => bundle.copy(resources = bundle.resources.updated(0, bundle.resources.head.copy(title = "rewritten")))),
      "history deletion" -> (bundle => bundle.copy(contexts = Vector.empty)),
      "history reordering" -> (bundle => bundle.copy(resources = bundle.resources.reverse)),
      "changed material" -> (bundle => bundle.copy(materials = Vector(bundle.materials.head.copy(summary = "rewritten")))),
      "changed proposal" -> (bundle => bundle.copy(proposals = Vector(bundle.proposals.head.copy(title = "rewritten")))),
      "reused RunInput ID with different Context" -> (bundle => bundle.copy(runs = bundle.runs :+ bundle.runs.head.copy(id = "new-run", input = bundle.runs.head.input.copy(context = bundle.runs.head.input.context.copy(title = "new title"))))),
      "reused snapshot ID in new Evidence" -> (bundle => bundle.copy(evidence = bundle.evidence :+ bundle.evidence.head.copy(ref = bundle.evidence.head.ref.copy(revision = 3L), snapshot = bundle.evidence.head.snapshot.copy(content = Some("different"))))),
      "reused baseline snapshot ID" -> (bundle => bundle.copy(contexts = bundle.contexts :+ bundle.contexts.head.copy(ref = bundle.contexts.head.ref.copy(revision = 2L), existingInformation = Vector(bundle.contexts.head.existingInformation.head.copy(snapshot = bundle.contexts.head.existingInformation.head.snapshot.copy(content = Some("different")))))))
    )
    mutations.foreach { (label, mutate) =>
      s"reject $label without publishing" in LocalFlowSpecSupport.withDirectory { directory =>
        Given(s"an existing full snapshot and a draft containing $label")
        val state = LocalFlowSpecSupport.seed(directory)
        val bytes = Files.readAllBytes(directory.resolve("state.json")).toVector
        val draft = LocalDraft(mutate(state.bundle), state.deliveries)
        When("CAS validates the immutable transition")
        val result = new FilePreparationStore(directory).compareAndSet(state.revision, draft)
        Then("the invalid draft cannot replace any published history")
        result.swap.toOption.get.code shouldBe "invalid"
        result.swap.toOption.get.writeOutcome shouldBe LocalWriteOutcome.NotApplied
        Files.readAllBytes(directory.resolve("state.json")).toVector shouldBe bytes
      }
    }

    "append new Context and material revisions while preserving the frozen historical input" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("an existing Run and newly versioned current Context and material")
      val state = LocalFlowSpecSupport.seed(directory)
      val context = state.bundle.contexts.head.copy(ref = state.bundle.contexts.head.ref.copy(revision = 2L), title = "Current context")
      val material = state.bundle.materials.head.copy(ref = state.bundle.materials.head.ref.copy(revision = 2L), summary = "New material revision")
      val draft = LocalDraft(state.bundle.copy(contexts = state.bundle.contexts :+ context, materials = state.bundle.materials :+ material), state.deliveries)
      When("the append is committed and the previous revision is reused")
      val store = new FilePreparationStore(directory)
      val appended = store.compareAndSet(state.revision, draft).toOption.get
      val stale = store.compareAndSet(state.revision, draft)
      Then("history is exact and the stale update observes the new local revision")
      appended.bundle.runs shouldBe state.bundle.runs
      appended.bundle.contexts.last shouldBe context
      appended.bundle.materials.last shouldBe material
      stale.swap.toOption.get.observedRevision shouldBe Some(appended.revision)
    }

    "reject revision exhaustion before writing" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("a valid persisted snapshot at Long.MaxValue")
      val state = LocalState(Long.MaxValue, LocalFlowSpecSupport.bundle(), Vector.empty)
      Files.writeString(directory.resolve("state.json"), LocalFlowJson.encodeState(state).toOption.get)
      val bytes = Files.readAllBytes(directory.resolve("state.json")).toVector
      When("a CAS would need to increment beyond Long.MaxValue")
      val result = new FilePreparationStore(directory).compareAndSet(Long.MaxValue, LocalDraft(state.bundle, state.deliveries))
      Then("revision-exhausted is NotApplied and bytes stay unchanged")
      result.swap.toOption.get.code shouldBe "revision-exhausted"
      result.swap.toOption.get.writeOutcome shouldBe LocalWriteOutcome.NotApplied
      Files.readAllBytes(directory.resolve("state.json")).toVector shouldBe bytes
    }
  }

  "Corrupt and unsafe primary paths" should {
    Vector("{", "{\"schema\":\"tkl.local-state.v99\"}", "lineage").foreach { defect =>
      s"return a structured error and preserve $defect state" in LocalFlowSpecSupport.withDirectory { directory =>
        Given(s"a primary state containing $defect")
        val bundle = LocalFlowSpecSupport.bundle()
        val valid = LocalFlowJson.encodeState(LocalState(1L, bundle, Vector.empty)).toOption.get
        val text = if defect == "lineage" then
          val original = parse(valid).toOption.get
          val changed = original.hcursor.downField("bundle").downField("materials").downArray.downField("producingRunId")
            .withFocus(_ => Json.fromString("missing-run")).top.get
          changed.hcursor.downField("bundle").downField("materials").downArray.get[String]("producingRunId") shouldBe Right("missing-run")
          changed should not be original
          bundle.materials.head.producingRunId should not be "missing-run"
          bundle.runs.map(_.id) should not contain "missing-run"
          changed.noSpaces
        else defect
        Files.writeString(directory.resolve("state.json"), text)
        val store = new FilePreparationStore(directory)
        When("the corrupt primary state is read and an initial CAS is attempted")
        val read = store.read()
        val write = store.compareAndSet(0L, LocalDraft(LocalFlowSpecSupport.bundle(), Vector.empty))
        Then("both boundaries refuse the state and never overwrite it")
        read.swap.toOption.get.code shouldBe "invalid-state"
        write.swap.toOption.get.code shouldBe "invalid-state"
        Files.readString(directory.resolve("state.json")) shouldBe text
      }
    }

    Vector("state.json", "state.lock", "directory").foreach { selector =>
      s"reject a symlink $selector" in LocalFlowSpecSupport.withDirectory { directory =>
        Given("a disposable real directory with a symlink at the selected adapter path")
        val real = Files.createDirectory(directory.resolve("real"))
        val storepath = if selector == "directory" then
          Files.createSymbolicLink(directory.resolve("selected"), real)
        else
          val target = Files.writeString(directory.resolve("target-file"), "untouched")
          Files.createSymbolicLink(real.resolve(selector), target)
          real
        When("read and CAS inspect the selected nonsymlink path contract")
        val store = new FilePreparationStore(storepath)
        val read = store.read()
        val write = store.compareAndSet(0L, LocalDraft(LocalFlowSpecSupport.bundle(), Vector.empty))
        Then("structured NotApplied errors protect the linked target")
        read.swap.toOption.get.code shouldBe "io"
        write.swap.toOption.get.writeOutcome shouldBe LocalWriteOutcome.NotApplied
        if selector != "directory" then Files.readString(directory.resolve("target-file")) shouldBe "untouched"
      }
    }
  }

  "Delivery attempt transitions" should {
    "complete pending once and reject request/result replacement and fabricated observations" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("a stored proposal and one immutable pending request")
      val initial = LocalFlowSpecSupport.seed(directory)
      val request = LocalFlowSpecSupport.request(initial)
      val store = new FilePreparationStore(directory)
      val pending = store.compareAndSet(initial.revision, LocalDraft(initial.bundle, Vector(DeliveryAttempt(request, None)))).toOption.get
      val observation = LocalFlowSpecSupport.confirmed(request)
      When("the pending observation is completed exactly once")
      val completed = store.compareAndSet(pending.revision, LocalDraft(pending.bundle, Vector(DeliveryAttempt(request, Some(observation))))).toOption.get
      Then("the exact request and valid terminal observation are persisted")
      completed.deliveries shouldBe Vector(DeliveryAttempt(request, Some(observation)))
      val invalid = Vector(
        Vector(DeliveryAttempt(request.copy(correlationId = "changed"), Some(observation.copy(correlationId = "changed")))),
        Vector(DeliveryAttempt(request, Some(observation.copy(message = "rewritten")))),
        completed.deliveries :+ DeliveryAttempt(request, None),
        completed.deliveries :+ DeliveryAttempt(request.copy(correlationId = "fabricated"), Some(observation.copy(correlationId = "fabricated")))
      )
      When("drafts rewrite requests/results, duplicate correlations or append already observed attempts")
      val results = invalid.map(attempts => store.compareAndSet(completed.revision, LocalDraft(completed.bundle, attempts)))
      Then("every draft fails and the completed primary state remains exact")
      all(results.map(_.isLeft)) shouldBe true
      store.read() shouldBe Right(completed)
    }
  }
