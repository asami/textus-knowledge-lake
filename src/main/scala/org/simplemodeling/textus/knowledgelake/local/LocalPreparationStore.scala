package org.simplemodeling.textus.knowledgelake.local

import java.nio.channels.{FileChannel, FileLock, OverlappingFileLockException}
import java.nio.charset.StandardCharsets
import java.nio.file.{AtomicMoveNotSupportedException, Files, LinkOption, Path, StandardCopyOption, StandardOpenOption}
import org.simplemodeling.textus.knowledgelake.model.*
import scala.util.control.NonFatal

/** Outcome of the failing local operation, independent of earlier successful writes.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
enum LocalWriteOutcome:
  case NotApplied, Unknown, Applied
final case class LocalError(code: String, message: String, writeOutcome: LocalWriteOutcome, observedRevision: Option[Long])
final case class LocalState(revision: Long, bundle: PreparationBundle, deliveries: Vector[DeliveryAttempt])
final case class LocalDraft(bundle: PreparationBundle, deliveries: Vector[DeliveryAttempt])
trait PreparationStore:
  def read(): Either[LocalError, LocalState]
  def compareAndSet(expectedRevision: Long, draft: LocalDraft): Either[LocalError, LocalState]
enum StoreWriteStage:
  case TemporarySynced, BeforePublish, AfterPublish
trait StoreWriteObserver:
  def observe(stage: StoreWriteStage): Unit
object StoreWriteObserver:
  val NOOP: StoreWriteObserver = new StoreWriteObserver:
    def observe(stage: StoreWriteStage): Unit = ()

/** Pure snapshot and append-only transition validation; no codecs or I/O.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
object LocalPreparationStore:
  val EMPTY: LocalState = LocalState(0L, PreparationBundle(Vector.empty, Vector.empty, Vector.empty, Vector.empty, Vector.empty, Vector.empty), Vector.empty)

  private[local] def _nonblank(value: String): Boolean =
    value.exists(character =>
      Character.getType(character) != Character.SPACE_SEPARATOR &&
        character != '\t' && character != '\n' && character != '\u000b' &&
        character != '\f' && character != '\r' && character != '\u2028' &&
        character != '\u2029' && character != '\ufeff'
    )

  private[local] def _invalid(errors: Vector[ModelError], revision: Option[Long]): LocalError =
    LocalError("invalid", errors.map(error => s"${error.path}: ${error.code}: ${error.message}").mkString("; "), LocalWriteOutcome.NotApplied, revision)

  def validateState(state: LocalState): Either[Vector[ModelError], LocalState] =
    val errors = Vector.newBuilder[ModelError]
    if state.revision < 0 || (state.revision == 0 && state != EMPTY) then
      errors += ModelError("$.revision", "revision", "Revision 0 is reserved for the exact virtual empty state")
    PreparationModel.validate(state.bundle).left.foreach(errors ++= _)
    _identity_errors(state.bundle).foreach(errors += _)
    val correlations = state.deliveries.map(_.request.correlationId)
    if correlations.distinct.size != correlations.size then
      errors += ModelError("$.deliveries", "duplicate", "Correlation IDs must be globally unique")
    state.deliveries.zipWithIndex.foreach { (attempt, index) =>
      val base = s"$$.deliveries[$index]"
      ProposalDelivery._validate_request(attempt.request).left.foreach(errors ++= _)
      val request = attempt.request
      val key = ProposalKey(request.proposal.producerId, request.proposal.proposalId)
      if !state.bundle.proposals.find(proposal => ProposalKey(proposal.producerId, proposal.proposalId) == key).contains(request.proposal) then
        errors += ModelError(s"$base.request.proposal", "payload", "Request must equal the stored producer-scoped proposal")
      attempt.observation.foreach { observation =>
        ProposalDelivery._validate_observation(observation).left.foreach(errors ++= _)
        if observation.correlationId != request.correlationId || observation.producerId != key.producerId || observation.proposalId != key.proposalId then
          errors += ModelError(s"$base.observation", "correlation", "Observation must match its request correlation and producer-scoped key")
      }
      val prior = state.deliveries.take(index).reverse.find(previous =>
        ProposalKey(previous.request.proposal.producerId, previous.request.proposal.proposalId) == key)
      if prior.isEmpty && request.operation == DeliveryOperation.Query then
        errors += ModelError(s"$base.request.operation", "lifecycle", "Query requires a prior unresolved attempt")
      if prior.exists(_.observation.exists(_.outcome != ProposalOutcome.Unknown)) then
        errors += ModelError(s"$base.request.operation", "lifecycle", "A terminal key cannot be invoked again")
    }
    val result = errors.result()
    if result.isEmpty then Right(state) else Left(result)

  def validateTransition(previous: LocalState, draft: LocalDraft): Either[Vector[ModelError], LocalDraft] =
    val errors = Vector.newBuilder[ModelError]
    validateState(previous).left.foreach(errors ++= _)
    validateState(LocalState(1L, draft.bundle, draft.deliveries)).left.foreach(errors ++= _)
    def _prefix_[A](oldvalues: Vector[A], newvalues: Vector[A], field: String): Unit =
      if newvalues.take(oldvalues.size) != oldvalues then
        errors += ModelError(s"$$.bundle.$field", "immutable", "Existing objects and their order are immutable prefixes")
    _prefix_(previous.bundle.resources, draft.bundle.resources, "resources")
    _prefix_(previous.bundle.evidence, draft.bundle.evidence, "evidence")
    _prefix_(previous.bundle.contexts, draft.bundle.contexts, "contexts")
    _prefix_(previous.bundle.runs, draft.bundle.runs, "runs")
    _prefix_(previous.bundle.materials, draft.bundle.materials, "materials")
    _prefix_(previous.bundle.proposals, draft.bundle.proposals, "proposals")
    if draft.deliveries.size < previous.deliveries.size then
      errors += ModelError("$.deliveries", "immutable", "Existing attempts cannot be deleted")
    previous.deliveries.zip(draft.deliveries).zipWithIndex.foreach { (pair, index) =>
      val (oldattempt, newattempt) = pair
      if oldattempt.request != newattempt.request || (oldattempt.observation.nonEmpty && oldattempt.observation != newattempt.observation) then
        errors += ModelError(s"$$.deliveries[$index]", "immutable", "Request and completed observation are immutable")
    }
    draft.deliveries.drop(previous.deliveries.size).zipWithIndex.foreach { (attempt, index) =>
      if attempt.observation.nonEmpty then
        errors += ModelError(s"$$.deliveries[${previous.deliveries.size + index}].observation", "pending", "New attempts must be reserved pending")
    }
    val result = errors.result()
    if result.isEmpty then Right(draft) else Left(result)

  private def _identity_errors(bundle: PreparationBundle): Vector[ModelError] =
    val errors = Vector.newBuilder[ModelError]
    bundle.runs.map(_.input).groupBy(_.id).foreach { (id, inputs) =>
      if inputs.distinct.size > 1 then errors += ModelError("$.bundle.runs", "immutable-input", s"RunInput ID $id denotes different values")
    }
    val snapshots = bundle.evidence.map(_.snapshot) ++ bundle.contexts.flatMap(_.existingInformation.map(_.snapshot)) ++
      bundle.runs.flatMap(run => run.input.evidence.map(_.snapshot) ++ run.input.context.existingInformation.map(_.snapshot))
    snapshots.groupBy(_.id).foreach { (id, values) =>
      if values.distinct.size > 1 then errors += ModelError("$.bundle.snapshots", "immutable-snapshot", s"InputSnapshot ID $id denotes different values")
    }
    errors.result()

/** Local lock/CAS adapter with one atomic publication and explicit uncertainty.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
final class FilePreparationStore(directory: Path, observer: StoreWriteObserver = StoreWriteObserver.NOOP) extends PreparationStore:
  private val _directory = directory
  private val _observer = observer
  private val _state = directory.resolve("state.json")
  private val _lock = directory.resolve("state.lock")

  def read(): Either[LocalError, LocalState] =
    try
      _check_paths()
      if !Files.exists(_state, LinkOption.NOFOLLOW_LINKS) then Right(LocalPreparationStore.EMPTY)
      else
        val channel = FileChannel.open(_state, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)
        val text = try
          val reader = java.nio.channels.Channels.newReader(channel, StandardCharsets.UTF_8.newDecoder(), -1)
          val builder = new java.lang.StringBuilder
          val buffer = new Array[Char](4096)
          var count = reader.read(buffer)
          while count != -1 do
            builder.append(buffer, 0, count)
            count = reader.read(buffer)
          builder.toString
        finally channel.close()
        LocalFlowJson.decodeState(text).left.map(errors => LocalError("invalid-state", errors.map(_.message).mkString("; "), LocalWriteOutcome.NotApplied, None))
    catch
      case NonFatal(error) => Left(LocalError("io", s"Cannot read local state: ${error.getMessage}", LocalWriteOutcome.NotApplied, None))

  def compareAndSet(expectedRevision: Long, draft: LocalDraft): Either[LocalError, LocalState] =
    var temporary: Option[Path] = None
    var moveinvoked = false
    var published = false
    var observed: Option[Long] = None
    try
      _check_paths()
      Files.createDirectories(_directory)
      _check_paths()
      val channel = FileChannel.open(_lock, StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)
      try
        val acquired = try Option(channel.tryLock()) catch
          case _: OverlappingFileLockException => None
        acquired match
          case None => Left(LocalError("busy", "Local state lock is already held", LocalWriteOutcome.NotApplied, None))
          case Some(lock) =>
            try
              read().flatMap { current =>
                observed = Some(current.revision)
                if current.revision != expectedRevision then
                  Left(LocalError("revision-conflict", s"Expected revision $expectedRevision but found ${current.revision}", LocalWriteOutcome.NotApplied, observed))
                else if current.revision == Long.MaxValue then
                  Left(LocalError("revision-exhausted", "Local store revision is exhausted", LocalWriteOutcome.NotApplied, observed))
                else
                  LocalPreparationStore.validateTransition(current, draft).left.map(errors => LocalPreparationStore._invalid(errors, observed)).flatMap { valid =>
                    val next = LocalState(current.revision + 1L, valid.bundle, valid.deliveries)
                    LocalFlowJson.encodeState(next).left.map(errors => LocalPreparationStore._invalid(errors, observed)).map { text =>
                      val target = Files.createTempFile(_directory, ".state-", ".tmp")
                      temporary = Some(target)
                      val output = FileChannel.open(target, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)
                      try
                        val bytes = StandardCharsets.UTF_8.encode(text)
                        while bytes.hasRemaining do output.write(bytes)
                        output.force(true)
                      finally output.close()
                      _observer.observe(StoreWriteStage.TemporarySynced)
                      _observer.observe(StoreWriteStage.BeforePublish)
                      _check_paths()
                      moveinvoked = true
                      Files.move(target, _state, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                      published = true
                      _observer.observe(StoreWriteStage.AfterPublish)
                      next
                    }
                  }
              }
            finally lock.release()
      finally channel.close()
    catch
      case error: AtomicMoveNotSupportedException if !published =>
        Left(LocalError("io", s"Atomic publication unavailable: ${error.getMessage}", LocalWriteOutcome.NotApplied, observed))
      case NonFatal(error) =>
        val outcome = if moveinvoked then LocalWriteOutcome.Unknown else LocalWriteOutcome.NotApplied
        Left(LocalError("io", s"Local publication failed: ${error.getMessage}", outcome, if outcome == LocalWriteOutcome.Unknown then None else observed))
    finally
      temporary.foreach { target =>
        try Files.deleteIfExists(target) catch case NonFatal(_) => ()
      }

  private def _check_paths(): Unit =
    if Files.exists(_directory, LinkOption.NOFOLLOW_LINKS) &&
      (Files.isSymbolicLink(_directory) || !Files.isDirectory(_directory, LinkOption.NOFOLLOW_LINKS)) then
      throw new java.io.IOException("Store directory must be a nonsymlink directory")
    Vector(_state, _lock).foreach { target =>
      if Files.exists(target, LinkOption.NOFOLLOW_LINKS) &&
        (Files.isSymbolicLink(target) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) then
        throw new java.io.IOException(s"Store file must be regular and nonsymlink: ${target.getFileName}")
    }
