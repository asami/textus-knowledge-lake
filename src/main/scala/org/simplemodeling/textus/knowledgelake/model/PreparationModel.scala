package org.simplemodeling.textus.knowledgelake.model

/** Immutable preparation data and its local lineage contract.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
final case class VersionedRef(id: String, revision: Long)
final case class ProviderLocation(provider: String, externalId: String, uri: Option[String])
enum ProvenanceStatus:
  case Known, Unknown
final case class Provenance(status: ProvenanceStatus, actor: Option[String], description: String)
enum Retention:
  case Retained, ReferenceOnly
final case class InputSnapshot(id: String, retention: Retention, content: Option[String], mediaType: String, provenance: Provenance)
final case class Resource(ref: VersionedRef, kind: String, title: String, location: ProviderLocation)
final case class Evidence(ref: VersionedRef, resourceRef: VersionedRef, providerRevision: Option[String], snapshot: InputSnapshot)
final case class ExternalContextBinding(id: String, kind: String, provider: String, externalId: String)
final case class ExistingInformation(ref: VersionedRef, source: String, title: String, snapshot: InputSnapshot)
final case class KnowledgeProcessingContext(ref: VersionedRef, title: String, evidenceRefs: Vector[VersionedRef], bindings: Vector[ExternalContextBinding], existingInformation: Vector[ExistingInformation])
final case class RunInput(id: String, context: KnowledgeProcessingContext, resources: Vector[Resource], evidence: Vector[Evidence])
final case class ProcessingFailure(code: String, message: String, evidenceRef: Option[VersionedRef])
enum RunStatus:
  case Completed, Partial, Failed
final case class ProcessingRun(id: String, input: RunInput, processor: Provenance, status: RunStatus, failures: Vector[ProcessingFailure])
final case class MaterialSection(title: String, text: String, evidenceRefs: Vector[VersionedRef])
final case class PreparedMaterial(ref: VersionedRef, producingRunId: String, title: String, summary: String, sections: Vector[MaterialSection])
final case class RawCandidateProposal(producerId: String, proposalId: String, producingRunId: String, materialRef: VersionedRef, title: String, summary: String, evidenceRefs: Vector[VersionedRef])
final case class PreparationBundle(resources: Vector[Resource], evidence: Vector[Evidence], contexts: Vector[KnowledgeProcessingContext], runs: Vector[ProcessingRun], materials: Vector[PreparedMaterial], proposals: Vector[RawCandidateProposal])
final case class ModelError(path: String, code: String, message: String)

/** Pure validation; historical Run inputs resolve only within their own snapshot.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
object PreparationModel:
  def validate(bundle: PreparationBundle): Either[Vector[ModelError], PreparationBundle] =
    val errors = Vector.newBuilder[ModelError]
    def _error_(path: String, code: String, message: String): Unit =
      errors += ModelError(path, code, message)
    def _nonblank_(value: String, path: String): Unit =
      // Match the schema's ECMAScript whitespace without rewriting opaque values.
      if !value.exists(character =>
        Character.getType(character) != Character.SPACE_SEPARATOR &&
          character != '\t' && character != '\n' && character != '\u000b' &&
          character != '\f' && character != '\r' && character != '\u2028' &&
          character != '\u2029' && character != '\ufeff'
      ) then _error_(path, "blank", "A nonblank value is required")
    def _ref_(value: VersionedRef, path: String): Unit =
      _nonblank_(value.id, s"$path.id")
      if value.revision <= 0 then _error_(s"$path.revision", "revision", "Entity revision must be positive")
    def _unique_[A](values: Vector[A], path: String): Unit =
      if values.distinct.size != values.size then _error_(path, "duplicate", "Duplicate identity or selection")
    def _resolve_(ref: VersionedRef, available: Set[VersionedRef], path: String): Unit =
      if !available.contains(ref) then _error_(path, "unresolved", s"Unresolved reference ${ref.id}@${ref.revision}")
    def _provenance_(value: Provenance, path: String): Unit =
      _nonblank_(value.description, s"$path.description")
      value.status match
        case ProvenanceStatus.Known =>
          value.actor match
            case Some(actor) => _nonblank_(actor, s"$path.actor")
            case None => _error_(s"$path.actor", "provenance", "Known provenance requires an actor")
        case ProvenanceStatus.Unknown =>
          if value.actor.nonEmpty then _error_(s"$path.actor", "provenance", "Unknown provenance must have no actor")
    def _snapshot_(value: InputSnapshot, path: String): Unit =
      _nonblank_(value.id, s"$path.id")
      _nonblank_(value.mediaType, s"$path.mediaType")
      _provenance_(value.provenance, s"$path.provenance")
      value.retention match
        case Retention.Retained =>
          if value.content.isEmpty then _error_(s"$path.content", "retention", "Retained input requires content, including empty content")
        case Retention.ReferenceOnly =>
          if value.content.nonEmpty then _error_(s"$path.content", "retention", "Reference-only input must not contain content")
    def _resource_(value: Resource, path: String): Unit =
      _ref_(value.ref, s"$path.ref")
      _nonblank_(value.kind, s"$path.kind")
      _nonblank_(value.title, s"$path.title")
      _nonblank_(value.location.provider, s"$path.location.provider")
      _nonblank_(value.location.externalId, s"$path.location.externalId")
      value.location.uri.foreach(uri => _nonblank_(uri, s"$path.location.uri"))
    def _evidence_(value: Evidence, path: String): Unit =
      _ref_(value.ref, s"$path.ref")
      _ref_(value.resourceRef, s"$path.resourceRef")
      value.providerRevision.foreach(revision => _nonblank_(revision, s"$path.providerRevision"))
      _snapshot_(value.snapshot, s"$path.snapshot")
    def _citations_(values: Vector[VersionedRef], path: String): Unit =
      _unique_(values, path)
      values.zipWithIndex.foreach((ref, index) => _ref_(ref, s"$path[$index]"))
    def _context_(value: KnowledgeProcessingContext, path: String): Unit =
      _ref_(value.ref, s"$path.ref")
      _nonblank_(value.title, s"$path.title")
      _citations_(value.evidenceRefs, s"$path.evidenceRefs")
      _unique_(value.bindings.map(_.id), s"$path.bindings")
      value.bindings.zipWithIndex.foreach { (binding, index) =>
        val base = s"$path.bindings[$index]"
        _nonblank_(binding.id, s"$base.id")
        _nonblank_(binding.kind, s"$base.kind")
        _nonblank_(binding.provider, s"$base.provider")
        _nonblank_(binding.externalId, s"$base.externalId")
      }
      _unique_(value.existingInformation.map(_.ref), s"$path.existingInformation")
      value.existingInformation.zipWithIndex.foreach { (information, index) =>
        val base = s"$path.existingInformation[$index]"
        _ref_(information.ref, s"$base.ref")
        _nonblank_(information.source, s"$base.source")
        _nonblank_(information.title, s"$base.title")
        _snapshot_(information.snapshot, s"$base.snapshot")
      }
    def _input_(value: RunInput, path: String): Unit =
      _nonblank_(value.id, s"$path.id")
      _context_(value.context, s"$path.context")
      _unique_(value.resources.map(_.ref), s"$path.resources")
      _unique_(value.evidence.map(_.ref), s"$path.evidence")
      _unique_(value.evidence.map(_.snapshot.id) ++ value.context.existingInformation.map(_.snapshot.id), s"$path.snapshots")
      value.resources.zipWithIndex.foreach((resource, index) => _resource_(resource, s"$path.resources[$index]"))
      value.evidence.zipWithIndex.foreach { (evidence, index) =>
        _evidence_(evidence, s"$path.evidence[$index]")
        _resolve_(evidence.resourceRef, value.resources.map(_.ref).toSet, s"$path.evidence[$index].resourceRef")
      }
      if value.context.evidenceRefs.toSet != value.evidence.map(_.ref).toSet then
        _error_(s"$path.evidence", "selection", "Run evidence must exactly match frozen Context selections")
      if value.resources.map(_.ref).toSet != value.evidence.map(_.resourceRef).toSet then
        _error_(s"$path.resources", "selection", "Run resources must exactly match selected Evidence resources")

    _unique_(bundle.resources.map(_.ref), "$.resources")
    _unique_(bundle.evidence.map(_.ref), "$.evidence")
    _unique_(bundle.contexts.map(_.ref), "$.contexts")
    _unique_(bundle.runs.map(_.id), "$.runs")
    _unique_(bundle.materials.map(_.ref), "$.materials")
    _unique_(bundle.proposals.map(proposal => (proposal.producerId, proposal.proposalId)), "$.proposals")
    bundle.resources.zipWithIndex.foreach((resource, index) => _resource_(resource, s"$$.resources[$index]"))
    bundle.evidence.zipWithIndex.foreach { (evidence, index) =>
      val path = s"$$.evidence[$index]"
      _evidence_(evidence, path)
      _resolve_(evidence.resourceRef, bundle.resources.map(_.ref).toSet, s"$path.resourceRef")
    }
    bundle.contexts.zipWithIndex.foreach { (context, index) =>
      val path = s"$$.contexts[$index]"
      _context_(context, path)
      context.evidenceRefs.zipWithIndex.foreach((ref, index) => _resolve_(ref, bundle.evidence.map(_.ref).toSet, s"$path.evidenceRefs[$index]"))
    }
    bundle.runs.zipWithIndex.foreach { (run, index) =>
      val path = s"$$.runs[$index]"
      _nonblank_(run.id, s"$path.id")
      _input_(run.input, s"$path.input")
      _provenance_(run.processor, s"$path.processor")
      run.status match
        case RunStatus.Completed =>
          if run.failures.nonEmpty then _error_(s"$path.failures", "status", "Completed Run must have no failures")
        case RunStatus.Partial | RunStatus.Failed =>
          if run.failures.isEmpty then _error_(s"$path.failures", "status", "Partial or Failed Run requires observable failures")
      run.failures.zipWithIndex.foreach { (failure, index) =>
        val base = s"$path.failures[$index]"
        _nonblank_(failure.code, s"$base.code")
        _nonblank_(failure.message, s"$base.message")
        failure.evidenceRef.foreach { ref =>
          _ref_(ref, s"$base.evidenceRef")
          _resolve_(ref, run.input.evidence.map(_.ref).toSet, s"$base.evidenceRef")
        }
      }
    }
    bundle.materials.zipWithIndex.foreach { (material, index) =>
      val path = s"$$.materials[$index]"
      _ref_(material.ref, s"$path.ref")
      _nonblank_(material.producingRunId, s"$path.producingRunId")
      _nonblank_(material.title, s"$path.title")
      _nonblank_(material.summary, s"$path.summary")
      val run = bundle.runs.find(_.id == material.producingRunId)
      if run.isEmpty then _error_(s"$path.producingRunId", "unresolved", "Producing Run does not exist")
      if run.exists(_.status == RunStatus.Failed) then _error_(s"$path.producingRunId", "failed-run", "Failed Run cannot produce material")
      if material.sections.isEmpty then _error_(s"$path.sections", "empty", "Material requires at least one section")
      material.sections.zipWithIndex.foreach { (section, index) =>
        val base = s"$path.sections[$index]"
        _nonblank_(section.title, s"$base.title")
        _citations_(section.evidenceRefs, s"$base.evidenceRefs")
        if section.evidenceRefs.isEmpty then _error_(s"$base.evidenceRefs", "empty", "Section requires Evidence citations")
        run.foreach(run => section.evidenceRefs.zipWithIndex.foreach((ref, index) => _resolve_(ref, run.input.evidence.map(_.ref).toSet, s"$base.evidenceRefs[$index]")))
      }
    }
    bundle.proposals.zipWithIndex.foreach { (proposal, index) =>
      val path = s"$$.proposals[$index]"
      _nonblank_(proposal.producerId, s"$path.producerId")
      _nonblank_(proposal.proposalId, s"$path.proposalId")
      _nonblank_(proposal.producingRunId, s"$path.producingRunId")
      _nonblank_(proposal.title, s"$path.title")
      _nonblank_(proposal.summary, s"$path.summary")
      _ref_(proposal.materialRef, s"$path.materialRef")
      _citations_(proposal.evidenceRefs, s"$path.evidenceRefs")
      if proposal.evidenceRefs.isEmpty then _error_(s"$path.evidenceRefs", "empty", "Proposal requires Evidence citations")
      val material = bundle.materials.find(_.ref == proposal.materialRef)
      if material.isEmpty then _error_(s"$path.materialRef", "unresolved", "Material revision does not exist")
      material.foreach { material =>
        if material.producingRunId != proposal.producingRunId then _error_(s"$path.producingRunId", "lineage", "Proposal and material must share producing Run")
        if !material.sections.flatMap(_.evidenceRefs).toSet.subsetOf(proposal.evidenceRefs.toSet) then
          _error_(s"$path.evidenceRefs", "coverage", "Proposal must cover every material citation")
      }
      bundle.runs.find(_.id == proposal.producingRunId) match
        case None => _error_(s"$path.producingRunId", "unresolved", "Proposal Run does not exist")
        case Some(run) => proposal.evidenceRefs.zipWithIndex.foreach((ref, index) => _resolve_(ref, run.input.evidence.map(_.ref).toSet, s"$path.evidenceRefs[$index]"))
    }
    val result = errors.result()
    if result.isEmpty then Right(bundle) else Left(result)
