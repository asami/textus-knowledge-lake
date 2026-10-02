package org.simplemodeling.textus.knowledgelake.model

import io.circe.{Decoder, DecodingFailure, Encoder, Json, Printer}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.parse

/** Strict canonical v1 representation. Every optional value is an explicit null.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
object PreparationJson:
  val SCHEMA: String = "tkl.preparation-bundle.v1"

  private final case class BundleDocument(schema: String, resources: Vector[Resource], evidence: Vector[Evidence], contexts: Vector[KnowledgeProcessingContext], runs: Vector[ProcessingRun], materials: Vector[PreparedMaterial], proposals: Vector[RawCandidateProposal])

  private def _strict[A](decoder: Decoder[A], fields: String*): Decoder[A] = Decoder.instance { cursor =>
    cursor.value.asObject match
      case None => Left(DecodingFailure("Expected object", cursor.history))
      case Some(obj) =>
        val expected = fields.toSet
        val actual = obj.keys.toSet
        val missing = expected.diff(actual).toVector.sorted
        val extra = actual.diff(expected).toVector.sorted
        if missing.nonEmpty || extra.nonEmpty then
          Left(DecodingFailure(s"Missing fields: ${missing.mkString(",")}; unknown fields: ${extra.mkString(",")}", cursor.history))
        else decoder(cursor)
  }

  private given _provenance_status_encoder: Encoder[ProvenanceStatus] = Encoder.encodeString.contramap {
    case ProvenanceStatus.Known => "known"
    case ProvenanceStatus.Unknown => "unknown"
  }
  private given _provenance_status_decoder: Decoder[ProvenanceStatus] = Decoder.decodeString.emap {
    case "known" => Right(ProvenanceStatus.Known)
    case "unknown" => Right(ProvenanceStatus.Unknown)
    case tag => Left(s"Unknown provenance tag: $tag")
  }
  private given _retention_encoder: Encoder[Retention] = Encoder.encodeString.contramap {
    case Retention.Retained => "retained"
    case Retention.ReferenceOnly => "reference-only"
  }
  private given _retention_decoder: Decoder[Retention] = Decoder.decodeString.emap {
    case "retained" => Right(Retention.Retained)
    case "reference-only" => Right(Retention.ReferenceOnly)
    case tag => Left(s"Unknown retention tag: $tag")
  }
  private given _run_status_encoder: Encoder[RunStatus] = Encoder.encodeString.contramap {
    case RunStatus.Completed => "completed"
    case RunStatus.Partial => "partial"
    case RunStatus.Failed => "failed"
  }
  private given _run_status_decoder: Decoder[RunStatus] = Decoder.decodeString.emap {
    case "completed" => Right(RunStatus.Completed)
    case "partial" => Right(RunStatus.Partial)
    case "failed" => Right(RunStatus.Failed)
    case tag => Left(s"Unknown Run status tag: $tag")
  }
  private given _versioned_ref_encoder: Encoder[VersionedRef] = deriveEncoder
  private given _versioned_ref_decoder: Decoder[VersionedRef] = _strict(deriveDecoder, "id", "revision")
  private given _location_encoder: Encoder[ProviderLocation] = deriveEncoder
  private given _location_decoder: Decoder[ProviderLocation] = _strict(deriveDecoder, "provider", "externalId", "uri")
  private given _provenance_encoder: Encoder[Provenance] = deriveEncoder
  private given _provenance_decoder: Decoder[Provenance] = _strict(deriveDecoder, "status", "actor", "description")
  private given _snapshot_encoder: Encoder[InputSnapshot] = deriveEncoder
  private given _snapshot_decoder: Decoder[InputSnapshot] = _strict(deriveDecoder, "id", "retention", "content", "mediaType", "provenance")
  private given _resource_encoder: Encoder[Resource] = deriveEncoder
  private given _resource_decoder: Decoder[Resource] = _strict(deriveDecoder, "ref", "kind", "title", "location")
  private given _evidence_encoder: Encoder[Evidence] = deriveEncoder
  private given _evidence_decoder: Decoder[Evidence] = _strict(deriveDecoder, "ref", "resourceRef", "providerRevision", "snapshot")
  private given _binding_encoder: Encoder[ExternalContextBinding] = deriveEncoder
  private given _binding_decoder: Decoder[ExternalContextBinding] = _strict(deriveDecoder, "id", "kind", "provider", "externalId")
  private given _information_encoder: Encoder[ExistingInformation] = deriveEncoder
  private given _information_decoder: Decoder[ExistingInformation] = _strict(deriveDecoder, "ref", "source", "title", "snapshot")
  private given _context_encoder: Encoder[KnowledgeProcessingContext] = deriveEncoder
  private given _context_decoder: Decoder[KnowledgeProcessingContext] = _strict(deriveDecoder, "ref", "title", "evidenceRefs", "bindings", "existingInformation")
  private given _input_encoder: Encoder[RunInput] = deriveEncoder
  private given _input_decoder: Decoder[RunInput] = _strict(deriveDecoder, "id", "context", "resources", "evidence")
  private given _failure_encoder: Encoder[ProcessingFailure] = deriveEncoder
  private given _failure_decoder: Decoder[ProcessingFailure] = _strict(deriveDecoder, "code", "message", "evidenceRef")
  private given _run_encoder: Encoder[ProcessingRun] = deriveEncoder
  private given _run_decoder: Decoder[ProcessingRun] = _strict(deriveDecoder, "id", "input", "processor", "status", "failures")
  private given _section_encoder: Encoder[MaterialSection] = deriveEncoder
  private given _section_decoder: Decoder[MaterialSection] = _strict(deriveDecoder, "title", "text", "evidenceRefs")
  private given _material_encoder: Encoder[PreparedMaterial] = deriveEncoder
  private given _material_decoder: Decoder[PreparedMaterial] = _strict(deriveDecoder, "ref", "producingRunId", "title", "summary", "sections")
  private given _proposal_encoder: Encoder[RawCandidateProposal] = deriveEncoder
  private given _proposal_decoder: Decoder[RawCandidateProposal] = _strict(deriveDecoder, "producerId", "proposalId", "producingRunId", "materialRef", "title", "summary", "evidenceRefs")
  private given _document_encoder: Encoder[BundleDocument] = deriveEncoder
  private given _document_decoder: Decoder[BundleDocument] = _strict(deriveDecoder, "schema", "resources", "evidence", "contexts", "runs", "materials", "proposals")
  private val _printer = Printer.spaces2.copy(dropNullValues = false, sortKeys = false)

  def encode(bundle: PreparationBundle): Either[Vector[ModelError], String] =
    PreparationModel.validate(bundle).map { valid =>
      val document = BundleDocument(SCHEMA, valid.resources, valid.evidence, valid.contexts, valid.runs, valid.materials, valid.proposals)
      _printer.print(_document_encoder(document))
    }

  def decode(text: String): Either[Vector[ModelError], PreparationBundle] =
    parse(text).left.map(error => Vector(ModelError("$", "json-syntax", error.message))).flatMap { json =>
      json.hcursor.get[String]("schema").left.map(error => _decoding_errors(error)).flatMap { schema =>
        if schema != SCHEMA then Left(Vector(ModelError("$.schema", "schema", s"Unsupported schema: $schema")))
        else _document_decoder.decodeJson(json).left.map(error => _decoding_errors(error)).flatMap { document =>
          PreparationModel.validate(PreparationBundle(document.resources, document.evidence, document.contexts, document.runs, document.materials, document.proposals))
        }
      }
    }

  private def _decoding_errors(error: DecodingFailure): Vector[ModelError] =
    Vector(ModelError("$", "json-decode", s"${error.message}; cursor: ${error.history.mkString("/")}"))
