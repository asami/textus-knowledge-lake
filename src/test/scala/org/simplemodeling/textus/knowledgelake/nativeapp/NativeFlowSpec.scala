package org.simplemodeling.textus.knowledgelake.nativeapp

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import org.goldenport.{Conclusion, Consequence}
import org.goldenport.observation.Taxonomy
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.knowledgelake.KnowledgeLakeComponent
import org.simplemodeling.textus.knowledgelake.local.*
import org.simplemodeling.textus.knowledgelake.model.*
import scala.jdk.CollectionConverters.*

/** Native dispatch acceptance against persisted local data and structured failures.
 * @since Oct. 3, 2026
 * @version Oct. 3, 2026
 * @author ASAMI, Tomoharu
 */
class NativeFlowSpec extends AnyWordSpec with GivenWhenThen with Matchers:
  private val _fixture_path = Path.of("fixtures/tkl/local-flow-v1.json").toAbsolutePath

  "Native local preparation" should {
    "persist and report the repository fixture with exact lineage and export bytes" in _with_directory { directory =>
      Given("the decoded repository fixture and an absent explicit local store")
      val fixture = LocalFlowJson.decodeFixture(_read(_fixture_path)).toOption.get
      val storedirectory = directory.resolve("store")
      val command = fixture.preparation
      val input = PreparationFlow.freezeInput(fixture.initial, command.contextRef, command.inputId).toOption.get
      val expectedrun = ProcessingRun(command.runId, input, command.result.processor, command.result.status, command.result.failures)
      val draft = command.result.material.get
      val expectedmaterial = PreparedMaterial(draft.ref, expectedrun.id, draft.title, draft.summary, draft.sections)
      val expectedproposal = RawCandidateProposal(fixture.proposal.producerId, fixture.proposal.proposalId,
        expectedrun.id, expectedmaterial.ref, expectedmaterial.title, expectedmaterial.summary,
        expectedmaterial.sections.flatMap(_.evidenceRefs).distinct)
      val expectedbundle = fixture.initial.copy(runs = Vector(expectedrun), materials = Vector(expectedmaterial), proposals = Vector(expectedproposal))
      When("the native entry resolves and executes the generated fixture command")
      val result = NativeFlowMain.execute(_fixture_path, storedirectory)
      Then("the standard record contains exactly the report encoded from persisted state and output paths")
      val reportjson = _report_json(result)
      val state = new FilePreparationStore(storedirectory).read().toOption.get
      state.revision shouldBe 7L
      state.bundle shouldBe expectedbundle
      state.bundle.runs.head.id shouldBe "run-local-001"
      state.bundle.runs.head.input.id shouldBe "run-input-local-001"
      state.bundle.runs.head.input.context shouldBe fixture.initial.contexts.head
      state.bundle.runs.head.input.resources shouldBe fixture.initial.resources
      state.bundle.runs.head.input.evidence shouldBe fixture.initial.evidence
      state.bundle.runs.head.input.evidence.map(_.ref) shouldBe Vector(VersionedRef("evidence:資料/α", 1L), VersionedRef("evidence:資料/α", 2L))
      state.bundle.runs.head.input.evidence.head.snapshot.content shouldBe Some("日本語の資料 🌱\n第二行\n")
      state.bundle.runs.head.input.evidence.last.snapshot.retention shouldBe Retention.ReferenceOnly
      state.bundle.runs.head.input.evidence.last.snapshot.provenance.status shouldBe ProvenanceStatus.Unknown
      state.bundle.runs.head.input.evidence.last.snapshot.content shouldBe None
      state.bundle.materials.head.ref shouldBe VersionedRef("material:準備", 1L)
      state.bundle.materials.head.producingRunId shouldBe "run-local-001"
      state.bundle.proposals.head.producerId shouldBe "textus-knowledge-lake"
      state.bundle.proposals.head.proposalId shouldBe "proposal-local-001"
      state.bundle.proposals.head.materialRef shouldBe state.bundle.materials.head.ref
      state.bundle.proposals.head.evidenceRefs shouldBe input.evidence.map(_.ref)
      state.deliveries shouldBe fixture.deliveries.map(delivery =>
        DeliveryAttempt(ProposalRequest(delivery.correlationId, delivery.operation, expectedproposal), Some(delivery.observation)))
      val outcomes = state.deliveries.flatMap(_.observation.map(_.outcome))
      outcomes shouldBe Vector(ProposalOutcome.Unknown, ProposalOutcome.Confirmed)
      state.deliveries.last.observation.get.receiptId shouldBe Some("fixture-receipt-001")
      state.deliveries.last.observation.get.candidateRef shouldBe Some(VersionedRef("fixture-candidate-001", 1L))
      val report = LocalFlowReport(state.revision, state.bundle.runs.head.id, state.bundle.materials.head.ref,
        ProposalKey(state.bundle.proposals.head.producerId, state.bundle.proposals.head.proposalId), outcomes,
        storedirectory.resolve("preparation-bundle.json").toString, storedirectory.resolve("prepared-material.md").toString,
        storedirectory.resolve("proposal-request.json").toString, "fixture-only; external guarantees unverified")
      report.assurance shouldBe LocalFlowJson.ASSURANCE
      reportjson shouldBe LocalFlowJson.encodeReport(report).toOption.get
      reportjson should include("\n")
      _read(Path.of(report.bundlePath)) shouldBe PreparationJson.encode(state.bundle).toOption.get
      PreparationJson.decode(_read(Path.of(report.bundlePath))) shouldBe Right(state.bundle)
      _read(Path.of(report.markdownPath)) shouldBe PreparedMaterialMarkdown.render(state.bundle, report.materialRef).toOption.get
      val expectedrequest = ProposalRequest(fixture.deliveries.head.correlationId, fixture.deliveries.head.operation, state.bundle.proposals.head)
      _read(Path.of(report.requestPath)) shouldBe LocalFlowJson.encodeRequest(expectedrequest).toOption.get
      LocalFlowJson.decodeRequest(_read(Path.of(report.requestPath))) shouldBe Right(expectedrequest)
    }

    "reject an initialized local store without changing any written bytes" in _with_directory { directory =>
      Given("a store initialized through a successful native fixture request")
      val storedirectory = directory.resolve("store")
      _report_json(NativeFlowMain.execute(_fixture_path, storedirectory))
      val originalbytes = _file_bytes(storedirectory)
      When("the same native request targets the initialized store")
      val result = NativeFlowMain.execute(_fixture_path, storedirectory)
      Then("the structured refusal retains revision 7 and all previously written files remain identical")
      val error = _native_error(result)
      error shouldBe LocalError("already-initialized", "Selected store is already initialized", LocalWriteOutcome.NotApplied, Some(7L))
      _file_bytes(storedirectory) shouldBe originalbytes
      new FilePreparationStore(storedirectory).read().toOption.get.revision shouldBe 7L
    }

    "retain an Applied failure after projection writing fails" in _with_directory { directory =>
      Given("an absent primary state and a directory occupying the Markdown export path")
      val storedirectory = directory.resolve("store")
      Files.createDirectories(storedirectory.resolve("prepared-material.md"))
      val fixture = LocalFlowJson.decodeFixture(_read(_fixture_path)).toOption.get
      When("native dispatch persists the preparation and attempts its projections")
      val result = NativeFlowMain.execute(_fixture_path, storedirectory)
      Then("no success report is returned and the structured projection failure records the readable committed revision")
      val error = _native_error(result)
      error.code shouldBe "projection-write"
      error.message should include("Primary state committed")
      error.writeOutcome shouldBe LocalWriteOutcome.Applied
      error.observedRevision shouldBe Some(7L)
      val state = new FilePreparationStore(storedirectory).read().toOption.get
      state.revision shouldBe 7L
      state.bundle.runs.head.id shouldBe fixture.preparation.runId
      state.bundle.materials.head.ref shouldBe fixture.proposal.materialRef
      state.bundle.proposals.head.proposalId shouldBe fixture.proposal.proposalId
      state.deliveries.flatMap(_.observation.map(_.outcome)) shouldBe Vector(ProposalOutcome.Unknown, ProposalOutcome.Confirmed)
      PreparationJson.decode(_read(storedirectory.resolve("preparation-bundle.json"))) shouldBe Right(state.bundle)
      Files.isDirectory(storedirectory.resolve("prepared-material.md")) shouldBe true
      Files.exists(storedirectory.resolve("proposal-request.json")) shouldBe false
    }

    "reject malformed fixture JSON without creating state or outputs" in _with_directory { directory =>
      Given("malformed JSON encoded as UTF-8 and an absent store directory")
      val fixturepath = directory.resolve("malformed.json")
      Files.writeString(fixturepath, "{ malformed 日本語 🌱", StandardCharsets.UTF_8)
      val storedirectory = directory.resolve("store")
      When("the native fixture operation reads and decodes the malformed document")
      val result = NativeFlowMain.execute(fixturepath, storedirectory)
      Then("the structured invalid error is NotApplied without an observed revision or any store writes")
      val error = _native_error(result)
      error.code shouldBe "invalid"
      error.message should include("json-syntax")
      error.writeOutcome shouldBe LocalWriteOutcome.NotApplied
      error.observedRevision shouldBe None
      Files.exists(storedirectory) shouldBe false
    }

    "report a missing fixture without creating state or outputs" in _with_directory { directory =>
      Given("a missing explicit fixture path and an absent store directory")
      val fixturepath = directory.resolve("missing.json")
      val storedirectory = directory.resolve("store")
      When("the native fixture operation attempts to read the missing document")
      val result = NativeFlowMain.execute(fixturepath, storedirectory)
      Then("the structured fixture-read error retains NotApplied and no observed revision or store writes")
      val error = _native_error(result)
      error.code shouldBe "fixture-read"
      error.message should include("Cannot read UTF-8 fixture")
      error.writeOutcome shouldBe LocalWriteOutcome.NotApplied
      error.observedRevision shouldBe None
      Files.exists(storedirectory) shouldBe false
    }
  }

  "Native request boundaries" should {
    "reject each missing required property before local execution" in _with_directory { directory =>
      Given("real fixture requests each omitting one generated required property")
      val storedirectory = directory.resolve("store")
      val properties = List(Property("fixturePath", _fixture_path.toString, None), Property("storeDirectory", storedirectory.toString, None))
      val requests = Vector(
        "fixturePath" -> _request("LocalPreparationService", "executeFixture", List(properties(1))),
        "storeDirectory" -> _request("LocalPreparationService", "executeFixture", List(properties.head)))
      When("the native resolver dispatches each incomplete request")
      val results = requests.map { (missingname, request) => missingname -> NativeFlowMain.executeRequest(request) }
      Then("each absent field is rejected as argumentMissing with BadRequest and its name before any native local error or store writes")
      results.foreach { (missingname, result) =>
        val failure = _framework_failure(result)
        failure.conclusion.observation.taxonomy shouldBe Taxonomy.argumentMissing
        failure.conclusion.status.webCode shouldBe Conclusion.WebCode.BadRequest
        failure.conclusion.display should include(missingname)
        failure.conclusion.cause.getException.exists(_.isInstanceOf[NativeFlowFailure]) shouldBe false
      }
      Files.exists(storedirectory) shouldBe false
    }

    "reject an invalid NUL path without creating state or outputs" in _with_directory { directory =>
      Given("a generated-valid request whose fixture path contains NUL and an absent store")
      val storedirectory = directory.resolve("store")
      val request = _request("LocalPreparationService", "executeFixture", List(
        Property("fixturePath", "invalid\u0000fixture.json", None), Property("storeDirectory", storedirectory.toString, None)))
      When("native dispatch converts the required path strings")
      val result = NativeFlowMain.executeRequest(request)
      Then("the structured invalid-path error is NotApplied with no observed revision or local writes")
      val error = _native_error(result)
      error.code shouldBe "invalid-path"
      error.message should include("Cannot convert explicit local path")
      error.writeOutcome shouldBe LocalWriteOutcome.NotApplied
      error.observedRevision shouldBe None
      Files.exists(storedirectory) shouldBe false
    }

    "reject generated CRUD and future operations outside the local service catalog" in _with_directory { directory =>
      Given("requests for generated aggregate, view and entity operations and a planned future Use Case")
      val storedirectory = directory.resolve("store")
      val targets = Vector(
        "aggregate" -> "createLocalPreparationSession", "aggregate" -> "loadLocalPreparationSession", "aggregate" -> "searchLocalPreparationSession",
        "view" -> "loadLocalPreparationSession", "view" -> "searchLocalPreparationSession",
        "entity" -> "createLocalPreparationSession", "entity" -> "loadLocalPreparationSession", "entity" -> "searchLocalPreparationSession",
        "LocalPreparationService" -> "explore_candidates_by_theme")
      val properties = List(Property("fixturePath", _fixture_path.toString, None), Property("storeDirectory", storedirectory.toString, None))
      When("each operation is submitted to the real native resolver")
      val results = targets.map { (service, operation) => NativeFlowMain.executeRequest(_request(service, operation, properties)) }
      Then("every excluded operation is rejected as operationNotFound with NotFound before any native local error or store writes")
      results.size shouldBe targets.size
      results.foreach { result =>
        val failure = _framework_failure(result)
        failure.conclusion.observation.taxonomy shouldBe Taxonomy.operationNotFound
        failure.conclusion.status.webCode shouldBe Conclusion.WebCode.NotFound
        failure.conclusion.cause.getException.exists(_.isInstanceOf[NativeFlowFailure]) shouldBe false
      }
      Files.exists(storedirectory) shouldBe false
    }
  }

  private def _request(service: String, operation: String, properties: List[Property]): Request =
    Request(Some(KnowledgeLakeComponent.name), Some(service), operation, Nil, Nil, properties, None)

  private def _framework_failure(result: Consequence[OperationResponse]): Consequence.Failure[?] =
    result match
      case failure: Consequence.Failure[?] => failure
      case _ => fail("Expected a failed native framework consequence")

  private def _native_error(result: Consequence[OperationResponse]): LocalError =
    _framework_failure(result).conclusion.cause.getException match
      case Some(failure: NativeFlowFailure) =>
        failure.getMessage should include(failure.localError.code)
        failure.getMessage should include(failure.localError.message)
        failure.getMessage should include(failure.localError.writeOutcome.toString)
        failure.getMessage should include(failure.localError.observedRevision.toString)
        failure.localError
      case _ => fail("Expected the exact structured NativeFlowFailure in the consequence cause")

  private def _report_json(result: Consequence[OperationResponse]): String =
    result.TAKE match
      case response: OperationResponse.RecordResponse =>
        response.record.getAny("reportJson") match
          case Some(reportjson: String) => reportjson
          case _ => fail("Expected String reportJson in the standard native record")
      case _ => fail("Expected a standard native RecordResponse")

  private def _read(path: Path): String = Files.readString(path, StandardCharsets.UTF_8)

  private def _file_bytes(directory: Path): Map[String, Vector[Byte]] =
    val paths = Files.walk(directory)
    try paths.iterator().asScala.filter(path => Files.isRegularFile(path)).map(path =>
      directory.relativize(path).toString -> Files.readAllBytes(path).toVector).toMap
    finally paths.close()

  private def _with_directory(body: Path => Unit): Unit =
    val directory = Files.createTempDirectory("tkl-native-flow-")
    try body(directory)
    finally
      val paths = Files.walk(directory)
      try paths.iterator().asScala.toVector.sortBy(_.getNameCount).reverse.foreach(path => Files.deleteIfExists(path))
      finally paths.close()
