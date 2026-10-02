package org.simplemodeling.textus.knowledgelake.local

import org.simplemodeling.textus.knowledgelake.model.*
import scala.util.control.NonFatal

/** TKL proposed delivery envelopes; fixture observations are unauthenticated assertions.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
final case class ProposalKey(producerId: String, proposalId: String)
enum DeliveryOperation:
  case Submit, Query
final case class ProposalRequest(correlationId: String, operation: DeliveryOperation, proposal: RawCandidateProposal)
enum ProposalOutcome:
  case Confirmed, Rejected, Conflict, Unknown
final case class ProposalObservation(correlationId: String, producerId: String, proposalId: String, outcome: ProposalOutcome, receiptId: Option[String], candidateRef: Option[VersionedRef], message: String)
final case class DeliveryAttempt(request: ProposalRequest, observation: Option[ProposalObservation])
trait ProposalTransport:
  def exchange(request: ProposalRequest): ProposalObservation

/** Stateless exact fixture lookup; no downstream storage or deduplication service.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
final class FixtureProposalTransport(observations: Vector[ProposalObservation]) extends ProposalTransport:
  private val _observations = observations
  def exchange(request: ProposalRequest): ProposalObservation =
    _observations.find(observation => observation.correlationId == request.correlationId &&
      observation.producerId == request.proposal.producerId && observation.proposalId == request.proposal.proposalId)
      .getOrElse(ProposalDelivery._unknown(request, "Missing fixture for exact correlation and producer-scoped proposal key; external outcome unknown"))

/** Reserve before invocation, exchange once, and explicitly preserve uncertain results.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
object ProposalDelivery:
  def execute(store: PreparationStore, expectedRevision: Long, request: ProposalRequest, transport: ProposalTransport): Either[LocalError, LocalState] =
    store.read().flatMap { current =>
      val key = ProposalKey(request.proposal.producerId, request.proposal.proposalId)
      val prior = current.deliveries.reverse.find(attempt => ProposalKey(attempt.request.proposal.producerId, attempt.request.proposal.proposalId) == key)
      if current.revision != expectedRevision then
        Left(LocalError("revision-conflict", s"Expected revision $expectedRevision but found ${current.revision}", LocalWriteOutcome.NotApplied, Some(current.revision)))
      else _validate_request(request).left.map(errors => LocalPreparationStore._invalid(errors, Some(current.revision))).flatMap { _ =>
        if !current.bundle.proposals.find(proposal => ProposalKey(proposal.producerId, proposal.proposalId) == key).contains(request.proposal) then
          Left(LocalError("invalid", "Request payload must exactly equal stored proposal at producer-scoped key", LocalWriteOutcome.NotApplied, Some(current.revision)))
        else if current.deliveries.exists(_.request.correlationId == request.correlationId) then
          Left(LocalError("duplicate-correlation", "Correlation ID already reserved", LocalWriteOutcome.NotApplied, Some(current.revision)))
        else if prior.exists(_.observation.exists(_.outcome != ProposalOutcome.Unknown)) then
          Left(LocalError("terminal", "Proposal key already has a terminal observation", LocalWriteOutcome.NotApplied, Some(current.revision)))
        else if request.operation == DeliveryOperation.Query && prior.isEmpty then
          Left(LocalError("unresolved-required", "Query requires a prior pending or Unknown attempt", LocalWriteOutcome.NotApplied, Some(current.revision)))
        else
          store.compareAndSet(expectedRevision, LocalDraft(current.bundle, current.deliveries :+ DeliveryAttempt(request, None))).flatMap { reserved =>
            val observation = try
              val returned = transport.exchange(request)
              if returned == null then _unknown(request, "Malformed null observation; external outcome unknown")
              else if returned.correlationId != request.correlationId || returned.producerId != key.producerId || returned.proposalId != key.proposalId then
                _unknown(request, "Observation correlation or producer-scoped proposal key mismatch; external outcome unknown")
              else _validate_observation(returned) match
                case Right(valid) => valid
                case Left(errors) => _unknown(request, s"Malformed observation: ${errors.map(_.message).mkString("; ")}; external outcome unknown")
            catch case NonFatal(error) => _unknown(request, s"Port exception: ${error.getClass.getSimpleName}: ${error.getMessage}; external outcome unknown")
            val attempts = reserved.deliveries.updated(reserved.deliveries.size - 1, DeliveryAttempt(request, Some(observation)))
            store.compareAndSet(reserved.revision, LocalDraft(reserved.bundle, attempts)).left.map { cause =>
              LocalError("result-not-recorded", s"Result recording was not confirmed: ${cause.code}: ${cause.message}; cause write outcome ${cause.writeOutcome}", LocalWriteOutcome.Unknown, cause.observedRevision)
            }
          }
      }
    }

  private[local] def _unknown(request: ProposalRequest, reason: String): ProposalObservation =
    ProposalObservation(request.correlationId, request.proposal.producerId, request.proposal.proposalId, ProposalOutcome.Unknown, None, None, reason)

  private[local] def _validate_request(request: ProposalRequest): Either[Vector[ModelError], ProposalRequest] =
    val proposal = request.proposal
    val fields = Vector("correlationId" -> request.correlationId, "proposal.producerId" -> proposal.producerId,
      "proposal.proposalId" -> proposal.proposalId, "proposal.producingRunId" -> proposal.producingRunId,
      "proposal.title" -> proposal.title, "proposal.summary" -> proposal.summary, "proposal.materialRef.id" -> proposal.materialRef.id)
    val errors = Vector.newBuilder[ModelError]
    fields.foreach { (field, value) =>
      if !LocalPreparationStore._nonblank(value) then errors += ModelError(s"$$.$field", "blank", "Nonblank metadata is required")
    }
    if proposal.materialRef.revision <= 0 then errors += ModelError("$.proposal.materialRef.revision", "revision", "Positive material revision required")
    if proposal.evidenceRefs.isEmpty then errors += ModelError("$.proposal.evidenceRefs", "empty", "Proposal citations required")
    if proposal.evidenceRefs.distinct != proposal.evidenceRefs then errors += ModelError("$.proposal.evidenceRefs", "duplicate", "Duplicate citation")
    proposal.evidenceRefs.zipWithIndex.foreach { (ref, index) =>
      if !LocalPreparationStore._nonblank(ref.id) || ref.revision <= 0 then errors += ModelError(s"$$.proposal.evidenceRefs[$index]", "revision", "Nonblank identity and positive revision required")
    }
    val result = errors.result()
    if result.isEmpty then Right(request) else Left(result)

  private[local] def _validate_observation(observation: ProposalObservation): Either[Vector[ModelError], ProposalObservation] =
    val errors = Vector.newBuilder[ModelError]
    Vector("correlationId" -> observation.correlationId, "producerId" -> observation.producerId,
      "proposalId" -> observation.proposalId, "message" -> observation.message).foreach { (field, value) =>
      if !LocalPreparationStore._nonblank(value) then errors += ModelError(s"$$.$field", "blank", "Nonblank metadata is required")
    }
    observation.outcome match
      case ProposalOutcome.Confirmed =>
        if !observation.receiptId.exists(LocalPreparationStore._nonblank) then errors += ModelError("$.receiptId", "confirmed", "Confirmed observation requires nonblank receipt")
        if !observation.candidateRef.exists(ref => LocalPreparationStore._nonblank(ref.id) && ref.revision > 0) then
          errors += ModelError("$.candidateRef", "confirmed", "Confirmed observation requires positive versioned Candidate reference")
      case _ =>
        if observation.receiptId.nonEmpty || observation.candidateRef.nonEmpty then
          errors += ModelError("$.outcome", "nonconfirmed", "Nonconfirmed observations require null receipt and Candidate")
    val result = errors.result()
    if result.isEmpty then Right(observation) else Left(result)
