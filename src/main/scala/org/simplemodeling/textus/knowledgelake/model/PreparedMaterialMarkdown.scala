package org.simplemodeling.textus.knowledgelake.model

/** Offline, human-readable projection of an exact material revision.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
object PreparedMaterialMarkdown:
  def render(bundle: PreparationBundle, materialRef: VersionedRef): Either[Vector[ModelError], String] =
    PreparationModel.validate(bundle).flatMap { valid =>
      valid.materials.find(_.ref == materialRef) match
        case None => Left(Vector(ModelError("$.materialRef", "unresolved", "Requested material revision does not exist")))
        case Some(material) =>
          val run = valid.runs.find(_.id == material.producingRunId).get
          val lines = Vector.newBuilder[String]
          lines += s"# ${material.title}"
          lines += ""
          lines += material.summary
          lines += ""
          lines += s"Material: ${_ref(material.ref)}"
          lines += s"Run: ${_metadata(run.id)} (${run.status.toString.toLowerCase})"
          lines += s"Context: ${_ref(run.input.context.ref)}"
          lines += s"Run input snapshot: ${_metadata(run.input.id)}"
          lines += s"Processor provenance: ${_provenance(run.processor)}"
          run.input.context.bindings.foreach { binding =>
            lines += s"External binding: ${_metadata(binding.id)}; ${_metadata(binding.kind)}; ${_metadata(binding.provider)}; ${_metadata(binding.externalId)}"
          }
          run.input.context.existingInformation.foreach { information =>
            lines += s"Existing information: ${_ref(information.ref)}; source ${_metadata(information.source)}; snapshot ${_metadata(information.snapshot.id)}; ${_snapshot(information.snapshot)}"
          }
          run.failures.foreach { failure =>
            val citation = failure.evidenceRef.map(_ref).getOrElse("none")
            lines += s"Failure: ${_metadata(failure.code)}; ${_metadata(failure.message)}; evidence $citation"
          }
          material.sections.foreach { section =>
            lines += ""
            lines += s"## ${section.title}"
            lines += ""
            lines += section.text
            lines += ""
            section.evidenceRefs.foreach { ref =>
              val evidence = run.input.evidence.find(_.ref == ref).get
              val resource = run.input.resources.find(_.ref == evidence.resourceRef).get
              lines += s"Evidence: ${_ref(evidence.ref)}; resource ${_ref(resource.ref)}; provider ${_metadata(resource.location.provider)}; external ID ${_metadata(resource.location.externalId)}; provider revision ${evidence.providerRevision.map(_metadata).getOrElse("unknown")}; input snapshot ${_metadata(evidence.snapshot.id)}; ${_snapshot(evidence.snapshot)}"
            }
          }
          Right(lines.result().mkString("\n") + "\n")
    }

  private def _ref(ref: VersionedRef): String = s"${_metadata(ref.id)} @ revision ${ref.revision}"

  private def _snapshot(snapshot: InputSnapshot): String =
    val retention = snapshot.retention match
      case Retention.Retained => "retained"
      case Retention.ReferenceOnly => "reference-only (content not retained; no claim of content analysis)"
    s"$retention; provenance ${_provenance(snapshot.provenance)}"

  private def _provenance(provenance: Provenance): String =
    val actor = provenance.status match
      case ProvenanceStatus.Known => s"known actor ${_metadata(provenance.actor.get)}"
      case ProvenanceStatus.Unknown => "unknown actor"
    s"$actor; ${_metadata(provenance.description)}"

  private def _metadata(value: String): String =
    value.flatMap {
      case '\n' => "&#10;"
      case '\r' => "&#13;"
      case '&' => "&amp;"
      case '<' => "&lt;"
      case '>' => "&gt;"
      case char if "\\`*_{}[]()#+-.!|".contains(char) => s"\\$char"
      case char => char.toString
    }
