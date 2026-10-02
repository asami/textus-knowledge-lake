package org.simplemodeling.textus.knowledgelake.local

import io.circe.{Decoder, DecodingFailure, Encoder, Json, Printer}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.parse
import org.simplemodeling.textus.knowledgelake.model.*

/** Explicit fixture commands and derived local execution report.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
final case class LocalFlowFixture(initial: PreparationBundle, preparation: PreparationCommand, proposal: ProposalCommand, deliveries: Vector[FixtureDelivery])
final case class PreparationCommand(contextRef: VersionedRef, inputId: String, runId: String, result: PreparationResult)
final case class ProposalCommand(producerId: String, proposalId: String, materialRef: VersionedRef)
final case class FixtureDelivery(correlationId: String, operation: DeliveryOperation, observation: ProposalObservation)
final case class LocalFlowReport(storeRevision: Long, runId: String, materialRef: VersionedRef, proposal: ProposalKey, outcomes: Vector[ProposalOutcome], bundlePath: String, markdownPath: String, requestPath: String, assurance: String)

/** Strict current v1 codecs; nested bundles reuse the unchanged canonical public API and format.
 * Both private Long decoders require exact numeric Long values, accepting integral numeric
 * spelling while rejecting strings, fractions and overflow; positivity remains semantic validation.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
object LocalFlowJson:
  val STATE_SCHEMA: String = "tkl.local-state.v1"
  val REQUEST_SCHEMA: String = "tkl.proposal-request.v1"
  val OBSERVATION_SCHEMA: String = "tkl.proposal-observation.v1"
  val FIXTURE_SCHEMA: String = "tkl.local-flow-fixture.v1"
  val REPORT_SCHEMA: String = "tkl.local-flow-report.v1"
  val ASSURANCE: String = "fixture-only; external guarantees unverified"

  private final case class StateDocument(schema: String, revision: Long, bundle: PreparationBundle, deliveries: Vector[DeliveryAttempt])
  private final case class RequestDocument(schema: String, correlationId: String, operation: DeliveryOperation, proposal: RawCandidateProposal)
  private final case class ObservationDocument(schema: String, correlationId: String, producerId: String, proposalId: String, outcome: ProposalOutcome, receiptId: Option[String], candidateRef: Option[VersionedRef], message: String)
  private final case class FixtureDocument(schema: String, initial: PreparationBundle, preparation: PreparationCommand, proposal: ProposalCommand, deliveries: Vector[FixtureDelivery])
  private final case class ReportDocument(schema: String, storeRevision: Long, runId: String, materialRef: VersionedRef, proposal: ProposalKey, outcomes: Vector[ProposalOutcome], bundlePath: String, markdownPath: String, requestPath: String, assurance: String)

  private def _strict[A](decoder: Decoder[A], fields: String*): Decoder[A] = Decoder.instance { cursor =>
    cursor.value.asObject match
      case None => Left(DecodingFailure("Expected object", cursor.history))
      case Some(obj) =>
        val expected = fields.toSet
        val actual = obj.keys.toSet
        if expected != actual then
          Left(DecodingFailure(s"Missing fields: ${expected.diff(actual).toVector.sorted.mkString(",")}; unknown fields: ${actual.diff(expected).toVector.sorted.mkString(",")}", cursor.history))
        else decoder(cursor)
  }

  private given _long_decoder: Decoder[Long] = Decoder.instance { cursor =>
    cursor.value.asNumber.flatMap(_.toLong).toRight(DecodingFailure("Expected JSON integer within Long range", cursor.history))
  }

  private given _operation_encoder: Encoder[DeliveryOperation] = Encoder.encodeString.contramap {
    case DeliveryOperation.Submit => "submit"
    case DeliveryOperation.Query => "query"
  }
  private given _operation_decoder: Decoder[DeliveryOperation] = Decoder.decodeString.emap {
    case "submit" => Right(DeliveryOperation.Submit)
    case "query" => Right(DeliveryOperation.Query)
    case tag => Left(s"Unknown operation: $tag")
  }
  private given _outcome_encoder: Encoder[ProposalOutcome] = Encoder.encodeString.contramap {
    case ProposalOutcome.Confirmed => "confirmed"
    case ProposalOutcome.Rejected => "rejected"
    case ProposalOutcome.Conflict => "conflict"
    case ProposalOutcome.Unknown => "unknown"
  }
  private given _outcome_decoder: Decoder[ProposalOutcome] = Decoder.decodeString.emap {
    case "confirmed" => Right(ProposalOutcome.Confirmed)
    case "rejected" => Right(ProposalOutcome.Rejected)
    case "conflict" => Right(ProposalOutcome.Conflict)
    case "unknown" => Right(ProposalOutcome.Unknown)
    case tag => Left(s"Unknown outcome: $tag")
  }
  private given _status_encoder: Encoder[RunStatus] = Encoder.encodeString.contramap {
    case RunStatus.Completed => "completed"
    case RunStatus.Partial => "partial"
    case RunStatus.Failed => "failed"
  }
  private given _status_decoder: Decoder[RunStatus] = Decoder.decodeString.emap {
    case "completed" => Right(RunStatus.Completed)
    case "partial" => Right(RunStatus.Partial)
    case "failed" => Right(RunStatus.Failed)
    case tag => Left(s"Unknown Run status: $tag")
  }
  private given _provenance_status_encoder: Encoder[ProvenanceStatus] = Encoder.encodeString.contramap {
    case ProvenanceStatus.Known => "known"
    case ProvenanceStatus.Unknown => "unknown"
  }
  private given _provenance_status_decoder: Decoder[ProvenanceStatus] = Decoder.decodeString.emap {
    case "known" => Right(ProvenanceStatus.Known)
    case "unknown" => Right(ProvenanceStatus.Unknown)
    case tag => Left(s"Unknown provenance: $tag")
  }
  private given _ref_encoder: Encoder[VersionedRef] = deriveEncoder
  private given _ref_decoder: Decoder[VersionedRef] = _strict(deriveDecoder, "id", "revision")
  private given _provenance_encoder: Encoder[Provenance] = deriveEncoder
  private given _provenance_decoder: Decoder[Provenance] = _strict(deriveDecoder, "status", "actor", "description")
  private given _failure_encoder: Encoder[ProcessingFailure] = deriveEncoder
  private given _failure_decoder: Decoder[ProcessingFailure] = _strict(deriveDecoder, "code", "message", "evidenceRef")
  private given _section_encoder: Encoder[MaterialSection] = deriveEncoder
  private given _section_decoder: Decoder[MaterialSection] = _strict(deriveDecoder, "title", "text", "evidenceRefs")
  private given _proposal_encoder: Encoder[RawCandidateProposal] = deriveEncoder
  private given _proposal_decoder: Decoder[RawCandidateProposal] = _strict(deriveDecoder, "producerId", "proposalId", "producingRunId", "materialRef", "title", "summary", "evidenceRefs")
  private given _bundle_encoder: Encoder[PreparationBundle] = Encoder.instance(bundle => parse(PreparationJson.encode(bundle).toOption.get).toOption.get)
  private given _bundle_decoder: Decoder[PreparationBundle] = Decoder.instance(cursor =>
    PreparationJson.decode(cursor.value.noSpaces).left.map(errors => DecodingFailure(errors.map(error => s"${error.path}: ${error.message}").mkString("; "), cursor.history)))
  private given _request_document_encoder: Encoder[RequestDocument] = deriveEncoder
  private given _request_document_decoder: Decoder[RequestDocument] = _strict(deriveDecoder, "schema", "correlationId", "operation", "proposal")
  private given _request_encoder: Encoder[ProposalRequest] = _request_document_encoder.contramap(request => RequestDocument(REQUEST_SCHEMA, request.correlationId, request.operation, request.proposal))
  private given _request_decoder: Decoder[ProposalRequest] = _request_document_decoder.emap { document =>
    if document.schema != REQUEST_SCHEMA then Left(s"Unsupported request schema: ${document.schema}")
    else Right(ProposalRequest(document.correlationId, document.operation, document.proposal))
  }
  private given _observation_document_encoder: Encoder[ObservationDocument] = deriveEncoder
  private given _observation_document_decoder: Decoder[ObservationDocument] = _strict(deriveDecoder, "schema", "correlationId", "producerId", "proposalId", "outcome", "receiptId", "candidateRef", "message")
  private given _observation_encoder: Encoder[ProposalObservation] = _observation_document_encoder.contramap(observation =>
    ObservationDocument(OBSERVATION_SCHEMA, observation.correlationId, observation.producerId, observation.proposalId, observation.outcome, observation.receiptId, observation.candidateRef, observation.message))
  private given _observation_decoder: Decoder[ProposalObservation] = _observation_document_decoder.emap { document =>
    if document.schema != OBSERVATION_SCHEMA then Left(s"Unsupported observation schema: ${document.schema}")
    else Right(ProposalObservation(document.correlationId, document.producerId, document.proposalId, document.outcome, document.receiptId, document.candidateRef, document.message))
  }
  private given _attempt_encoder: Encoder[DeliveryAttempt] = deriveEncoder
  private given _attempt_decoder: Decoder[DeliveryAttempt] = _strict(deriveDecoder, "request", "observation")
  private given _material_encoder: Encoder[MaterialDraft] = deriveEncoder
  private given _material_decoder: Decoder[MaterialDraft] = _strict(deriveDecoder, "ref", "title", "summary", "sections")
  private given _result_encoder: Encoder[PreparationResult] = deriveEncoder
  private given _result_decoder: Decoder[PreparationResult] = _strict(deriveDecoder, "processor", "status", "failures", "material")
  private given _preparation_encoder: Encoder[PreparationCommand] = deriveEncoder
  private given _preparation_decoder: Decoder[PreparationCommand] = _strict(deriveDecoder, "contextRef", "inputId", "runId", "result")
  private given _proposal_command_encoder: Encoder[ProposalCommand] = deriveEncoder
  private given _proposal_command_decoder: Decoder[ProposalCommand] = _strict(deriveDecoder, "producerId", "proposalId", "materialRef")
  private given _fixture_delivery_encoder: Encoder[FixtureDelivery] = deriveEncoder
  private given _fixture_delivery_decoder: Decoder[FixtureDelivery] = _strict(deriveDecoder, "correlationId", "operation", "observation")
  private given _key_encoder: Encoder[ProposalKey] = deriveEncoder
  private given _key_decoder: Decoder[ProposalKey] = _strict(deriveDecoder, "producerId", "proposalId")
  private given _state_document_encoder: Encoder[StateDocument] = deriveEncoder
  private given _state_document_decoder: Decoder[StateDocument] = _strict(deriveDecoder, "schema", "revision", "bundle", "deliveries")
  private given _fixture_document_decoder: Decoder[FixtureDocument] = _strict(deriveDecoder, "schema", "initial", "preparation", "proposal", "deliveries")
  private given _report_document_encoder: Encoder[ReportDocument] = deriveEncoder
  private val _printer = Printer.spaces2.copy(dropNullValues = false, sortKeys = false)

  def encodeState(state: LocalState): Either[Vector[ModelError], String] =
    _persisted(state).map(valid => _printer.print(_state_document_encoder(StateDocument(STATE_SCHEMA, valid.revision, valid.bundle, valid.deliveries))))

  def decodeState(text: String): Either[Vector[ModelError], LocalState] =
    _decode[StateDocument](text, STATE_SCHEMA).flatMap(document => _persisted(LocalState(document.revision, document.bundle, document.deliveries)))

  def encodeRequest(request: ProposalRequest): Either[Vector[ModelError], String] =
    ProposalDelivery._validate_request(request).map(valid => _printer.print(_request_encoder(valid)))

  def decodeRequest(text: String): Either[Vector[ModelError], ProposalRequest] =
    _decode[ProposalRequest](text, REQUEST_SCHEMA).flatMap(ProposalDelivery._validate_request)

  def encodeObservation(observation: ProposalObservation): Either[Vector[ModelError], String] =
    ProposalDelivery._validate_observation(observation).map(valid => _printer.print(_observation_encoder(valid)))

  def decodeObservation(text: String): Either[Vector[ModelError], ProposalObservation] =
    _decode[ProposalObservation](text, OBSERVATION_SCHEMA).flatMap(ProposalDelivery._validate_observation)

  def decodeFixture(text: String): Either[Vector[ModelError], LocalFlowFixture] =
    _decode[FixtureDocument](text, FIXTURE_SCHEMA).flatMap { document =>
      _validate_fixture(LocalFlowFixture(document.initial, document.preparation, document.proposal, document.deliveries))
    }

  def encodeReport(report: LocalFlowReport): Either[Vector[ModelError], String] =
    val fields = Vector(report.runId, report.materialRef.id, report.proposal.producerId, report.proposal.proposalId, report.bundlePath, report.markdownPath, report.requestPath)
    if report.storeRevision <= 0 || report.materialRef.revision <= 0 || fields.exists(value => !LocalPreparationStore._nonblank(value)) || report.assurance != ASSURANCE then
      Left(Vector(ModelError("$", "report", "Report requires positive revisions, nonblank explicit metadata and fixture-only assurance")))
    else Right(_printer.print(_report_document_encoder(ReportDocument(REPORT_SCHEMA, report.storeRevision, report.runId, report.materialRef, report.proposal, report.outcomes, report.bundlePath, report.markdownPath, report.requestPath, report.assurance))))

  private def _persisted(state: LocalState): Either[Vector[ModelError], LocalState] =
    if state.revision <= 0 then Left(Vector(ModelError("$.revision", "revision", "Persisted local revision must be positive Long")))
    else LocalPreparationStore.validateState(state)

  private def _decode[A: Decoder](text: String, schema: String): Either[Vector[ModelError], A] =
    parse(text).left.map(error => Vector(ModelError("$", "json-syntax", error.message))).flatMap { json =>
      json.hcursor.get[String]("schema").left.map(_decoding_errors).flatMap { actual =>
        if actual != schema then Left(Vector(ModelError("$.schema", "schema", s"Unsupported schema: $actual")))
        else summon[Decoder[A]].decodeJson(json).left.map(_decoding_errors)
      }
    }

  private def _decoding_errors(error: DecodingFailure): Vector[ModelError] =
    Vector(ModelError("$", "json-decode", s"${error.message}; cursor: ${error.history.mkString("/")}"))

  private def _validate_fixture(fixture: LocalFlowFixture): Either[Vector[ModelError], LocalFlowFixture] =
    val command = fixture.preparation
    _freeze_fixture_input(fixture).flatMap { input =>
      val run = ProcessingRun(command.runId, input, command.result.processor, command.result.status, command.result.failures)
      val materials = command.result.material.toVector.map(material => PreparedMaterial(material.ref, run.id, material.title, material.summary, material.sections))
      val prepared = fixture.initial.copy(runs = fixture.initial.runs :+ run, materials = fixture.initial.materials ++ materials)
      PreparationModel.validate(prepared).flatMap { valid =>
        valid.materials.find(_.ref == fixture.proposal.materialRef) match
          case None => Left(Vector(ModelError("$.proposal.materialRef", "unresolved", "Fixture proposal requires its exact prepared material")))
          case Some(material) =>
            val proposal = RawCandidateProposal(fixture.proposal.producerId, fixture.proposal.proposalId, material.producingRunId, material.ref, material.title, material.summary, material.sections.flatMap(_.evidenceRefs).distinct)
            val bundle = valid.copy(proposals = valid.proposals :+ proposal)
            val attempts = fixture.deliveries.map(delivery => DeliveryAttempt(ProposalRequest(delivery.correlationId, delivery.operation, proposal), Some(delivery.observation)))
            LocalPreparationStore.validateState(LocalState(1L, bundle, attempts)).map(_ => fixture)
      }
    }

  private def _freeze_fixture_input(fixture: LocalFlowFixture): Either[Vector[ModelError], RunInput] =
    if fixture.initial.runs.nonEmpty || fixture.initial.materials.nonEmpty || fixture.initial.proposals.nonEmpty then
      Left(Vector(ModelError("$.initial", "initial", "Fixture seed must contain no Runs, materials or proposals")))
    else if fixture.deliveries.isEmpty then
      Left(Vector(ModelError("$.deliveries", "empty", "Fixture requires an explicit delivery for request export")))
    else if fixture.preparation.result.status == RunStatus.Failed && fixture.preparation.result.material.nonEmpty then
      Left(Vector(ModelError("$.preparation.result.material", "failed-run", "Failed result must have null material")))
    else PreparationFlow.freezeInput(fixture.initial, fixture.preparation.contextRef, fixture.preparation.inputId)
