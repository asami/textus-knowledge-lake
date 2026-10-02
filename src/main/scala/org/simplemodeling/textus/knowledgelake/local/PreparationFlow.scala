package org.simplemodeling.textus.knowledgelake.local

import org.simplemodeling.textus.knowledgelake.model.*

/** Explicit preparation result; no source content or identities are inferred.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
final case class MaterialDraft(ref: VersionedRef, title: String, summary: String, sections: Vector[MaterialSection])
final case class PreparationResult(processor: Provenance, status: RunStatus, failures: Vector[ProcessingFailure], material: Option[MaterialDraft])

/** Freeze exact selections and atomically append one Run and optional material.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
object PreparationFlow:
  def freezeInput(bundle: PreparationBundle, contextRef: VersionedRef, inputId: String): Either[Vector[ModelError], RunInput] =
    PreparationModel.validate(bundle).flatMap { valid =>
      if !LocalPreparationStore._nonblank(inputId) then
        Left(Vector(ModelError("$.inputId", "blank", "Explicit nonblank RunInput ID is required")))
      else valid.contexts.find(_.ref == contextRef) match
        case None => Left(Vector(ModelError("$.contextRef", "unresolved", "Exact Context revision does not exist")))
        case Some(context) =>
          val evidence = context.evidenceRefs.map(ref => valid.evidence.find(_.ref == ref).get)
          val resources = evidence.map(_.resourceRef).distinct.map(ref => valid.resources.find(_.ref == ref).get)
          Right(RunInput(inputId, context, resources, evidence))
    }

  def prepare(store: PreparationStore, expectedRevision: Long, contextRef: VersionedRef, inputId: String, runId: String, result: PreparationResult): Either[LocalError, LocalState] =
    store.read().flatMap { current =>
      _expected(current, expectedRevision).flatMap { _ =>
        freezeInput(current.bundle, contextRef, inputId).left.map(errors => LocalPreparationStore._invalid(errors, Some(current.revision))).flatMap { input =>
          if result.status == RunStatus.Failed && result.material.nonEmpty then
            Left(LocalError("invalid", "Failed Run cannot carry material", LocalWriteOutcome.NotApplied, Some(current.revision)))
          else
            val run = ProcessingRun(runId, input, result.processor, result.status, result.failures)
            val materials = result.material.toVector.map(material => PreparedMaterial(material.ref, runId, material.title, material.summary, material.sections))
            val bundle = current.bundle.copy(runs = current.bundle.runs :+ run, materials = current.bundle.materials ++ materials)
            PreparationModel.validate(bundle).left.map(errors => LocalPreparationStore._invalid(errors, Some(current.revision))).flatMap { valid =>
              store.compareAndSet(expectedRevision, LocalDraft(valid, current.deliveries))
            }
        }
      }
    }

  def propose(store: PreparationStore, expectedRevision: Long, producerId: String, proposalId: String, materialRef: VersionedRef): Either[LocalError, LocalState] =
    store.read().flatMap { current =>
      _expected(current, expectedRevision).flatMap { _ =>
        current.bundle.materials.find(_.ref == materialRef) match
          case None => Left(LocalError("invalid", "Exact material revision does not exist", LocalWriteOutcome.NotApplied, Some(current.revision)))
          case Some(material) =>
            val proposal = RawCandidateProposal(producerId, proposalId, material.producingRunId, material.ref, material.title, material.summary, material.sections.flatMap(_.evidenceRefs).distinct)
            current.bundle.proposals.find(existing => existing.producerId == producerId && existing.proposalId == proposalId) match
              case Some(existing) if existing == proposal => Right(current)
              case Some(_) => Left(LocalError("immutable-conflict", "Producer-scoped proposal key already denotes different content", LocalWriteOutcome.NotApplied, Some(current.revision)))
              case None =>
                val bundle = current.bundle.copy(proposals = current.bundle.proposals :+ proposal)
                PreparationModel.validate(bundle).left.map(errors => LocalPreparationStore._invalid(errors, Some(current.revision))).flatMap { valid =>
                  store.compareAndSet(expectedRevision, LocalDraft(valid, current.deliveries))
                }
      }
    }

  private def _expected(current: LocalState, expected: Long): Either[LocalError, Unit] =
    if current.revision == expected then Right(())
    else Left(LocalError("revision-conflict", s"Expected revision $expected but found ${current.revision}", LocalWriteOutcome.NotApplied, Some(current.revision)))
