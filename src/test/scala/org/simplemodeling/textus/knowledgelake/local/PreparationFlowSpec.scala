package org.simplemodeling.textus.knowledgelake.local

import io.circe.Json
import io.circe.parser.parse
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.knowledgelake.model.*

/** Executable preparation, strict wire boundaries and repository-fixture flow.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
class PreparationFlowSpec extends AnyWordSpec with GivenWhenThen with Matchers:
  "Exact preparation input" should {
    "freeze only the selected Context and preserve selection and first-resource order in one CAS" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("two Context versions, retained/reference-only Evidence, baseline and Drive binding")
      val fixture = LocalFlowSpecSupport.fixture()
      val original = fixture.initial.contexts.head
      val context = original.copy(ref = original.ref.copy(revision = 2L), evidenceRefs = original.evidenceRefs.reverse)
      val initial = fixture.initial.copy(contexts = fixture.initial.contexts :+ context)
      val seeded = LocalFlowSpecSupport.seed(directory, initial)
      val delegate = new FilePreparationStore(directory)
      var calls = 0
      val store = new PreparationStore:
        def read(): Either[LocalError, LocalState] = delegate.read()
        def compareAndSet(expectedRevision: Long, draft: LocalDraft): Either[LocalError, LocalState] =
          calls += 1
          delegate.compareAndSet(expectedRevision, draft)
      val command = fixture.preparation
      When("preparation freezes the exact new Context and appends Run and material")
      val result = PreparationFlow.prepare(store, seeded.revision, context.ref, command.inputId, command.runId, command.result).toOption.get
      Then("one CAS captures exact baseline, binding, provenance and ordered source selections")
      calls shouldBe 1
      result.revision shouldBe 2L
      result.bundle.runs.head.input.context shouldBe context
      result.bundle.runs.head.input.evidence.map(_.ref) shouldBe context.evidenceRefs
      result.bundle.runs.head.input.resources.map(_.ref) shouldBe initial.evidence.reverse.map(_.resourceRef)
      result.bundle.runs.head.input.evidence.last.snapshot.content shouldBe Some("日本語の資料 🌱\n第二行\n")
      result.bundle.runs.head.input.evidence.head.snapshot.retention shouldBe Retention.ReferenceOnly
      result.bundle.runs.head.input.evidence.head.snapshot.provenance.actor shouldBe None
      result.bundle.runs.head.input.context.existingInformation shouldBe original.existingInformation
      result.bundle.runs.head.input.context.bindings shouldBe original.bindings
      result.bundle.materials.head.producingRunId shouldBe command.runId
    }

    "keep historical preparation and offline Markdown exact after current revisions change" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("one prepared Run/material and new current Resource, Evidence, Context, binding and baseline versions")
      val prepared = _prepared(directory)
      val bundle = prepared.bundle
      val oldmarkdown = PreparedMaterialMarkdown.render(bundle, bundle.materials.head.ref).toOption.get
      val resource = bundle.resources.head.copy(ref = bundle.resources.head.ref.copy(revision = 3L), title = "Current resource")
      val evidence = bundle.evidence.head.copy(ref = bundle.evidence.head.ref.copy(revision = 3L), resourceRef = resource.ref,
        snapshot = bundle.evidence.head.snapshot.copy(id = "snapshot/current-3", content = Some("Current source")))
      val context = bundle.contexts.head.copy(ref = bundle.contexts.head.ref.copy(revision = 2L), evidenceRefs = Vector(evidence.ref),
        bindings = Vector(bundle.contexts.head.bindings.head.copy(externalId = "current-project")),
        existingInformation = Vector(bundle.contexts.head.existingInformation.head.copy(ref = VersionedRef("information:baseline", 4L),
          snapshot = bundle.contexts.head.existingInformation.head.snapshot.copy(id = "snapshot/baseline-4", content = Some("Current baseline")))))
      val changed = bundle.copy(resources = bundle.resources :+ resource, evidence = bundle.evidence :+ evidence, contexts = bundle.contexts :+ context)
      When("current versions are appended and the historical material is rendered offline")
      val committed = new FilePreparationStore(directory).compareAndSet(prepared.revision, LocalDraft(changed, prepared.deliveries)).toOption.get
      val markdown = PreparedMaterialMarkdown.render(committed.bundle, bundle.materials.head.ref)
      val independent = PreparedMaterialMarkdown.render(committed.bundle.copy(resources = Vector.empty, evidence = Vector.empty, contexts = Vector.empty), bundle.materials.head.ref)
      Then("historical input/material and both projections remain identical")
      committed.bundle.runs shouldBe bundle.runs
      committed.bundle.materials shouldBe bundle.materials
      markdown shouldBe Right(oldmarkdown)
      independent shouldBe Right(oldmarkdown)
      markdown.toOption.get should include("reference-only")
      markdown.toOption.get should include("unknown actor")
    }

    "reject blank input IDs and missing exact Context references without source inference" in {
      Given("valid repository source fixtures and explicit invalid selections")
      val initial = LocalFlowSpecSupport.fixture().initial
      When("freezeInput receives ECMAScript whitespace or an unknown Context revision")
      val blank = PreparationFlow.freezeInput(initial, initial.contexts.head.ref, "\ufeff\u2003")
      val missing = PreparationFlow.freezeInput(initial, initial.contexts.head.ref.copy(revision = 99L), "explicit-input")
      Then("both requests return structured errors rather than generated identities")
      blank.swap.toOption.get.map(_.code) should contain("blank")
      missing.swap.toOption.get.map(_.code) should contain("unresolved")
    }
  }

  "Observable preparation results" should {
    "persist failed results without material and partial results with explicit failures" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("a seeded source bundle and manually supplied failed/partial descriptors")
      val fixture = LocalFlowSpecSupport.fixture()
      val seeded = LocalFlowSpecSupport.seed(directory, fixture.initial)
      val command = fixture.preparation
      val failure = ProcessingFailure("fixture-input", "Unavailable fixture input", Some(fixture.initial.evidence.last.ref))
      val failed = command.result.copy(status = RunStatus.Failed, failures = Vector(failure), material = None)
      val store = new FilePreparationStore(directory)
      When("a failed result is stored and a failed result carrying material is attempted")
      val failedstate = PreparationFlow.prepare(store, seeded.revision, command.contextRef, "failed-input", "failed-run", failed).toOption.get
      val forbidden = PreparationFlow.prepare(store, failedstate.revision, command.contextRef, "forbidden-input", "forbidden-run", failed.copy(material = command.result.material))
      Then("the failure is observable and prohibited material cannot be published")
      failedstate.bundle.runs.head.status shouldBe RunStatus.Failed
      failedstate.bundle.runs.head.failures shouldBe Vector(failure)
      failedstate.bundle.materials shouldBe empty
      forbidden.swap.toOption.get.code shouldBe "invalid"
      store.read() shouldBe Right(failedstate)
      When("a separate partial Run with explicit failures and material is prepared")
      val partial = command.result.copy(status = RunStatus.Partial, failures = Vector(failure))
      val partialstate = PreparationFlow.prepare(store, failedstate.revision, command.contextRef, "partial-input", "partial-run", partial).toOption.get
      Then("the partial descriptor and its exact material share one producing Run")
      partialstate.bundle.runs.last.status shouldBe RunStatus.Partial
      partialstate.bundle.runs.last.failures shouldBe Vector(failure)
      partialstate.bundle.materials.head.producingRunId shouldBe "partial-run"
    }

    "return preparation CAS conflict without inventing success or retry" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("a source seed and an adapter whose CAS observes a concurrent publication")
      val fixture = LocalFlowSpecSupport.fixture()
      val seeded = LocalFlowSpecSupport.seed(directory, fixture.initial)
      val delegate = new FilePreparationStore(directory)
      var calls = 0
      val store = new PreparationStore:
        def read(): Either[LocalError, LocalState] = delegate.read()
        def compareAndSet(expectedRevision: Long, draft: LocalDraft): Either[LocalError, LocalState] =
          calls += 1
          delegate.compareAndSet(expectedRevision, LocalDraft(seeded.bundle, Vector.empty))
          delegate.compareAndSet(expectedRevision, draft)
      val command = fixture.preparation
      When("the single preparation CAS competes with the explicit concurrent write")
      val result = PreparationFlow.prepare(store, seeded.revision, command.contextRef, command.inputId, command.runId, command.result)
      Then("the conflict reports actual revision and leaves both Run and material absent")
      calls shouldBe 1
      result.swap.toOption.get.code shouldBe "revision-conflict"
      result.swap.toOption.get.observedRevision shouldBe Some(2L)
      delegate.read().toOption.get.bundle.runs shouldBe empty
      delegate.read().toOption.get.bundle.materials shouldBe empty
    }

    Vector(StoreWriteStage.BeforePublish, StoreWriteStage.AfterPublish).foreach { stage =>
      s"expose only a whole old or new preparation after $stage fault" in LocalFlowSpecSupport.withDirectory { directory =>
        Given("a source seed and one deterministic publication fault")
        val fixture = LocalFlowSpecSupport.fixture()
        val seed = LocalFlowSpecSupport.seed(directory, fixture.initial)
        val command = fixture.preparation
        val store = new FilePreparationStore(directory, LocalFlowSpecSupport.fault(stage))
        When("preparation appends its Run/material together")
        val result = PreparationFlow.prepare(store, seed.revision, command.contextRef, command.inputId, command.runId, command.result)
        val reopened = new FilePreparationStore(directory).read().toOption.get
        Then("no success is fabricated and replay read exposes no split Run/material")
        result.isLeft shouldBe true
        val expectedcount = if stage == StoreWriteStage.AfterPublish then 1 else 0
        reopened.bundle.runs.size shouldBe expectedcount
        reopened.bundle.materials.size shouldBe expectedcount
        result.swap.toOption.get.writeOutcome shouldBe (if expectedcount == 1 then LocalWriteOutcome.Unknown else LocalWriteOutcome.NotApplied)
      }
    }
  }

  "Producer-scoped proposals" should {
    "derive exact lineage, preserve identical keys and allow independent producer namespaces" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("one prepared material with two ordered Evidence citations")
      val prepared = _prepared(directory)
      val material = prepared.bundle.materials.head
      val store = new FilePreparationStore(directory)
      When("a raw proposal is derived from its exact stored material")
      val first = PreparationFlow.propose(store, prepared.revision, "producer-A", "same-local-id", material.ref).toOption.get
      val bytes = Files.readAllBytes(directory.resolve("state.json")).toVector
      val identical = PreparationFlow.propose(store, first.revision, "producer-A", "same-local-id", material.ref)
      val stale = PreparationFlow.propose(store, prepared.revision, "producer-A", "same-local-id", material.ref)
      Then("proposal title, summary, Run and first-occurrence citations are exact and identical replay performs no write")
      first.bundle.proposals.head shouldBe RawCandidateProposal("producer-A", "same-local-id", material.producingRunId, material.ref, material.title, material.summary, material.sections.flatMap(_.evidenceRefs).distinct)
      identical shouldBe Right(first)
      Files.readAllBytes(directory.resolve("state.json")).toVector shouldBe bytes
      stale.swap.toOption.get.code shouldBe "revision-conflict"
      Given("a new material revision appended without changing the original proposal")
      val newmaterial = material.copy(ref = material.ref.copy(revision = 2L), summary = "Different material")
      val appended = store.compareAndSet(first.revision, LocalDraft(first.bundle.copy(materials = first.bundle.materials :+ newmaterial), first.deliveries)).toOption.get
      When("the existing key is proposed with different material and another producer reuses the local ID")
      val conflict = PreparationFlow.propose(store, appended.revision, "producer-A", "same-local-id", newmaterial.ref)
      val independent = PreparationFlow.propose(store, appended.revision, "producer-B", "same-local-id", material.ref).toOption.get
      Then("the old key stays immutable and the independent producer key is valid")
      conflict.swap.toOption.get.code shouldBe "immutable-conflict"
      independent.bundle.proposals.map(_.producerId) shouldBe Vector("producer-A", "producer-B")
      independent.bundle.proposals.head shouldBe first.bundle.proposals.head
    }
  }

  "Strict local JSON boundaries" should {
    "roundtrip Unicode, opaque IDs and explicit pending/Unknown nulls" in {
      Given("a canonical bundle, a pending request and a matching Unknown observation")
      val bundle = LocalFlowSpecSupport.bundle()
      val request = ProposalRequest("\u2003correlation/日本語 🌱\u00a0", DeliveryOperation.Submit, bundle.proposals.head)
      val unknown = ProposalDelivery._unknown(request, "Simulated unknown 🌱\n保持")
      val pending = LocalState(1L, bundle, Vector(DeliveryAttempt(request, None)))
      val observed = pending.copy(revision = 2L, deliveries = Vector(DeliveryAttempt(request, Some(unknown))))
      When("the public state/request/observation codecs encode and decode the values")
      val pendingresult = LocalFlowJson.encodeState(pending).flatMap(LocalFlowJson.decodeState)
      val observedresult = LocalFlowJson.encodeState(observed).flatMap(LocalFlowJson.decodeState)
      val requestresult = LocalFlowJson.encodeRequest(request).flatMap(LocalFlowJson.decodeRequest)
      val observationresult = LocalFlowJson.encodeObservation(unknown).flatMap(LocalFlowJson.decodeObservation)
      Then("all semantic values and absent receipt/Candidate fields remain exact")
      pendingresult shouldBe Right(pending)
      observedresult shouldBe Right(observed)
      requestresult shouldBe Right(request)
      observationresult shouldBe Right(unknown)
      observationresult.toOption.get.receiptId shouldBe None
      observationresult.toOption.get.candidateRef shouldBe None
      pendingresult.toOption.get.deliveries.head.observation shouldBe None
    }

    "reject missing/extra keys, schemas, enums, malformed syntax and invalid Long values" in {
      Given("valid state and request wire values plus explicit invalid variants")
      val state = LocalState(1L, LocalFlowSpecSupport.bundle(), Vector.empty)
      val statejson = parse(LocalFlowJson.encodeState(state).toOption.get).toOption.get
      val request = LocalFlowSpecSupport.request(state)
      val requestjson = parse(LocalFlowJson.encodeRequest(request).toOption.get).toOption.get
      val badstates = Vector(
        "{",
        statejson.mapObject(_.remove("deliveries")).noSpaces,
        statejson.mapObject(_.add("extra", Json.True)).noSpaces,
        statejson.mapObject(_.add("schema", Json.fromString("tkl.local-state.v2"))).noSpaces,
        statejson.mapObject(_.add("revision", Json.fromLong(0L))).noSpaces,
        statejson.mapObject(_.add("revision", Json.fromBigInt(BigInt(Long.MaxValue) + 1))).noSpaces,
        statejson.mapObject(_.add("revision", Json.fromBigDecimal(BigDecimal("1.5")))).noSpaces,
        statejson.mapObject(_.add("revision", Json.fromString("1"))).noSpaces,
        statejson.mapObject(_.add("revision", Json.Null)).noSpaces,
        statejson.mapObject(_.add("revision", Json.True)).noSpaces,
        statejson.mapObject(_.add("revision", Json.arr())).noSpaces,
        statejson.mapObject(_.add("revision", Json.obj())).noSpaces,
        statejson.mapObject(_.add("revision", Json.fromBigInt(BigInt(Long.MinValue) - 1))).noSpaces
      )
      val badrequests = Vector(
        requestjson.mapObject(_.remove("correlationId")).noSpaces,
        requestjson.mapObject(_.add("extra", Json.Null)).noSpaces,
        requestjson.mapObject(_.add("operation", Json.fromString("retry"))).noSpaces,
        requestjson.mapObject(_.add("schema", Json.fromString("tkl.proposal-request.v2"))).noSpaces,
        requestjson.mapObject(_.add("correlationId", Json.fromString("\ufeff\u00a0"))).noSpaces,
        requestjson.mapObject(_.add("proposal", requestjson.hcursor.downField("proposal").focus.get.mapObject(_.add("extra", Json.True)))).noSpaces
      )
      When("all invalid variants cross the public decoders")
      val states = badstates.map(LocalFlowJson.decodeState)
      val requests = badrequests.map(LocalFlowJson.decodeRequest)
      Then("every invalid payload returns structured errors without successful fallback")
      all(states.map(_.isLeft)) shouldBe true
      states.drop(5).foreach(result => result.swap.toOption.get.map(_.code) should contain("json-decode"))
      all(requests.map(_.isLeft)) shouldBe true
      LocalFlowJson.encodeState(LocalPreparationStore.EMPTY).isLeft shouldBe true
    }

    "preserve positive Long state boundaries and integral numeric spelling exactly" in {
      Given("valid states at revision one and Long.MaxValue, plus revision one spelled 1.0 and 1e0")
      val state = LocalState(1L, LocalFlowSpecSupport.bundle(), Vector.empty)
      val boundaries = Vector(state, state.copy(revision = Long.MaxValue))
      val statejson = parse(LocalFlowJson.encodeState(state).toOption.get).toOption.get
      val variants = Vector("1.0", "1e0").map(spelling =>
        statejson.mapObject(_.add("revision", parse(spelling).toOption.get)))
      When("the numeric boundaries roundtrip and the integral raw variants cross decodeState")
      val roundtrips = boundaries.map(value => LocalFlowJson.encodeState(value).flatMap(LocalFlowJson.decodeState))
      val results = variants.map(json => LocalFlowJson.decodeState(json.noSpaces))
      Then("positive boundary revisions and integral spellings preserve exact state without coercion or overflow")
      roundtrips shouldBe boundaries.map(Right(_))
      all(results) shouldBe Right(state)
      roundtrips.last.toOption.get.revision shouldBe Long.MaxValue
    }

    "reject matching numeric revision strings at local and nested canonical boundaries" in {
      Given("valid state, request, confirmed observation and repository fixture JSON with matching revision string mutations")
      val state = LocalState(1L, LocalFlowSpecSupport.bundle(), Vector.empty)
      val request = LocalFlowSpecSupport.request(state)
      val confirmed = LocalFlowSpecSupport.confirmed(request)
      val statejson = parse(LocalFlowJson.encodeState(state).toOption.get).toOption.get
      val requestjson = parse(LocalFlowJson.encodeRequest(request).toOption.get).toOption.get
      val observationjson = parse(LocalFlowJson.encodeObservation(confirmed).toOption.get).toOption.get
      val fixturejson = parse(Files.readString(Path.of("fixtures/tkl/local-flow-v1.json"))).toOption.get
      val cases: Vector[(String, Json, Vector[String], String => Either[Vector[ModelError], Unit])] = Vector(
        ("state nested Resource", statejson, Vector("bundle", "resources", "0", "ref"), text => LocalFlowJson.decodeState(text).map(_ => ())),
        ("request material", requestjson, Vector("proposal", "materialRef"), text => LocalFlowJson.decodeRequest(text).map(_ => ())),
        ("observation Candidate", observationjson, Vector("candidateRef"), text => LocalFlowJson.decodeObservation(text).map(_ => ())),
        ("fixture initial Resource", fixturejson, Vector("initial", "resources", "0", "ref"), text => LocalFlowJson.decodeFixture(text).map(_ => ())),
        ("fixture selected Context", fixturejson, Vector("preparation", "contextRef"), text => LocalFlowJson.decodeFixture(text).map(_ => ())),
        ("fixture material result", fixturejson, Vector("preparation", "result", "material", "ref"), text => LocalFlowJson.decodeFixture(text).map(_ => ())),
        ("fixture proposal material", fixturejson, Vector("proposal", "materialRef"), text => LocalFlowJson.decodeFixture(text).map(_ => ()))
      )
      cases.foreach { (label, json, path, decode) =>
        withClue(s"Unmodified $label: ") { decode(json.noSpaces).isRight shouldBe true }
      }
      val variants = cases.map { (label, json, path, decode) =>
        val invalid = _edit(json, path :+ "revision", value => Json.fromString(value.asNumber.get.toLong.get.toString))
        (label, invalid, decode)
      }
      When("each matching-value mutation crosses its public local decoder")
      val results = variants.map { (label, json, decode) => (label, decode(json.noSpaces)) }
      Then("every mutation returns json-decode before lineage semantics while all unmodified counterparts remain valid")
      results.foreach { (label, result) =>
        withClue(s"Mutated $label: ") {
          result.isLeft shouldBe true
          result.swap.toOption.get.map(_.code) should contain("json-decode")
        }
      }
    }

    "reject malformed confirmed/null fields and failed/material fixture payloads" in {
      Given("a valid confirmed assertion and a valid repository preparation fixture")
      val state = LocalState(1L, LocalFlowSpecSupport.bundle(), Vector.empty)
      val confirmed = LocalFlowSpecSupport.confirmed(LocalFlowSpecSupport.request(state))
      val observation = parse(LocalFlowJson.encodeObservation(confirmed).toOption.get).toOption.get
      val badobservations = Vector(
        observation.mapObject(_.remove("receiptId")),
        observation.mapObject(_.add("candidateRef", Json.Null)),
        observation.mapObject(_.add("receiptId", Json.fromString("\u2003\ufeff"))),
        observation.mapObject(_.add("outcome", Json.fromString("unknown"))),
        observation.mapObject(_.add("outcome", Json.fromString("accepted"))),
        observation.mapObject(_.add("schema", Json.fromString("foreign.v1"))),
        observation.mapObject(_.add("candidateRef", Json.obj("id" -> Json.fromString("candidate"), "revision" -> Json.fromBigInt(BigInt(Long.MaxValue) + 1))))
      )
      val fixturejson = parse(Files.readString(Path.of("fixtures/tkl/local-flow-v1.json"))).toOption.get
      val preparation = fixturejson.hcursor.downField("preparation").focus.get
      val result = preparation.hcursor.downField("result").focus.get
      val failedresult = result.mapObject(_.add("status", Json.fromString("failed")).add("failures", Json.arr(Json.obj("code" -> Json.fromString("fixture"), "message" -> Json.fromString("failed"), "evidenceRef" -> Json.Null))))
      val failedfixture = fixturejson.mapObject(_.add("preparation", preparation.mapObject(_.add("result", failedresult))))
      When("public decoders receive invalid observations and a failed result carrying material")
      val observations = badobservations.map(json => LocalFlowJson.decodeObservation(json.noSpaces))
      val fixture = LocalFlowJson.decodeFixture(failedfixture.noSpaces)
      Then("all confirmed/nonconfirmed and failed material constraints are enforced")
      all(observations.map(_.isLeft)) shouldBe true
      fixture.swap.toOption.get.map(_.code) should contain("failed-run")
      LocalFlowJson.encodeObservation(confirmed.copy(receiptId = None)).isLeft shouldBe true
      LocalFlowJson.encodeObservation(confirmed.copy(outcome = ProposalOutcome.Rejected)).isLeft shouldBe true
    }
  }

  "Repository local-flow execution" should {
    "persist revision seven and export canonical lineage plus offline Markdown" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("the actual repository-owned UTF-8 fixture and a fresh explicit output directory")
      val fixturepath = Path.of("fixtures/tkl/local-flow-v1.json")
      val fixture = LocalFlowSpecSupport.fixture()
      When("LocalFlowMain.execute runs preparation, proposal and ordered fixture exchanges")
      val report = LocalFlowMain.execute(fixturepath, directory).toOption.get
      val persisted = new FilePreparationStore(directory).read().toOption.get
      val exported = PreparationJson.decode(Files.readString(directory.resolve("preparation-bundle.json"))).toOption.get
      val request = LocalFlowJson.decodeRequest(Files.readString(directory.resolve("proposal-request.json"))).toOption.get
      val markdown = Files.readString(directory.resolve("prepared-material.md"))
      Then("seven actual revisions retain one fixed lineage and Unknown followed by Confirmed assertions")
      report.storeRevision shouldBe 7L
      persisted.revision shouldBe 7L
      report.outcomes shouldBe Vector(ProposalOutcome.Unknown, ProposalOutcome.Confirmed)
      report.assurance shouldBe LocalFlowJson.ASSURANCE
      report.bundlePath shouldBe directory.resolve("preparation-bundle.json").toString
      report.markdownPath shouldBe directory.resolve("prepared-material.md").toString
      report.requestPath shouldBe directory.resolve("proposal-request.json").toString
      exported shouldBe persisted.bundle
      persisted.bundle.runs.size shouldBe 1
      persisted.bundle.materials.size shouldBe 1
      persisted.bundle.proposals.size shouldBe 1
      persisted.deliveries.size shouldBe 2
      request shouldBe persisted.deliveries.head.request
      request.proposal shouldBe persisted.bundle.proposals.head
      request.proposal.materialRef shouldBe fixture.proposal.materialRef
      request.proposal.producingRunId shouldBe fixture.preparation.runId
      markdown shouldBe PreparedMaterialMarkdown.render(exported, fixture.proposal.materialRef).toOption.get
      markdown should include("日本語の資料 🌱")
      markdown should include("reference-only")
      persisted.bundle.runs.head.input.evidence.last.snapshot.content shouldBe None
      persisted.bundle.runs.head.input.evidence.last.snapshot.provenance.actor shouldBe None
      parse(LocalFlowJson.encodeReport(report).toOption.get).isRight shouldBe true
    }

    "refuse initialized stores without overwriting primary state or exports" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("a successfully initialized output directory")
      val fixturepath = Path.of("fixtures/tkl/local-flow-v1.json")
      val first = LocalFlowMain.execute(fixturepath, directory).toOption.get
      val paths = Vector("state.json", "preparation-bundle.json", "prepared-material.md", "proposal-request.json")
      val bytes = paths.map(name => Files.readAllBytes(directory.resolve(name)).toVector)
      When("the same fixture is explicitly executed again")
      val result = LocalFlowMain.execute(fixturepath, directory)
      Then("already-initialized identifies the existing revision and every file is unchanged")
      result.swap.toOption.get.code shouldBe "already-initialized"
      result.swap.toOption.get.observedRevision shouldBe Some(first.storeRevision)
      paths.map(name => Files.readAllBytes(directory.resolve(name)).toVector) shouldBe bytes
    }

    "report Applied projection failure without undoing committed preparation" in LocalFlowSpecSupport.withDirectory { directory =>
      Given("an empty primary store with an unwriteable projection target represented by a directory")
      Files.createDirectory(directory.resolve("prepared-material.md"))
      When("the repository fixture commits primary state before exporting Markdown")
      val result = LocalFlowMain.execute(Path.of("fixtures/tkl/local-flow-v1.json"), directory)
      val persisted = new FilePreparationStore(directory).read().toOption.get
      Then("projection-write reports Applied and the known final state remains intact")
      result.swap.toOption.get.code shouldBe "projection-write"
      result.swap.toOption.get.writeOutcome shouldBe LocalWriteOutcome.Applied
      result.swap.toOption.get.observedRevision shouldBe Some(7L)
      persisted.revision shouldBe 7L
      persisted.bundle.runs.size shouldBe 1
      persisted.bundle.materials.size shouldBe 1
      persisted.bundle.proposals.size shouldBe 1
      persisted.deliveries.last.observation.get.outcome shouldBe ProposalOutcome.Confirmed
    }
  }

  private def _prepared(directory: Path): LocalState =
    val fixture = LocalFlowSpecSupport.fixture()
    val seeded = LocalFlowSpecSupport.seed(directory, fixture.initial)
    val command = fixture.preparation
    PreparationFlow.prepare(new FilePreparationStore(directory), seeded.revision, command.contextRef, command.inputId, command.runId, command.result).toOption.get

  private def _edit(json: Json, path: Vector[String], change: Json => Json): Json =
    if path.isEmpty then change(json)
    else json.asObject match
      case Some(obj) => json.mapObject(_.add(path.head, _edit(obj(path.head).get, path.tail, change)))
      case None => json.mapArray(values => values.updated(path.head.toInt, _edit(values(path.head.toInt), path.tail, change)))
