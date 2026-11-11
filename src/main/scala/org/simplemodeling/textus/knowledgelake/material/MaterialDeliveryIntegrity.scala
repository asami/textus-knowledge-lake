package org.simplemodeling.textus.knowledgelake.material

import io.circe.Json
import org.goldenport.cncf.filestore.{FileStoreContentDigest, FileStoreRef}

/* Independent provider observations, separate from physical placement.
 * Expected bytes belong exclusively to the immutable contribution.
 * @since Oct. 25, 2026
 * @version Nov.  6, 2026
 */
private[knowledgelake] enum MaterialDeliveryVerificationState(val value: String) {
  case Pending extends MaterialDeliveryVerificationState("pending")
  case Verifying extends MaterialDeliveryVerificationState("verifying")
  case Verified extends MaterialDeliveryVerificationState("verified")
  case Failed extends MaterialDeliveryVerificationState("failed")
}

private[knowledgelake] final case class MaterialDeliveryVerification(
  resourceId: String,
  state: MaterialDeliveryVerificationState = MaterialDeliveryVerificationState.Pending,
  attempts: Int = 0,
  reason: Option[String] = None,
  observedDigest: Option[FileStoreContentDigest] = None,
  fileReference: Option[FileStoreRef] = None, storageRoot: Option[FileStoreRef] = None
) {
  def eligible: Boolean = attempts < 3 && (state == MaterialDeliveryVerificationState.Pending || state == MaterialDeliveryVerificationState.Verifying)
  def observe(expected: FileStoreContentDigest, observed: Option[FileStoreContentDigest]): MaterialDeliveryVerification = {
    if (!eligible) this
    else observed match {
      case Some(value) if value == expected => copy(state = MaterialDeliveryVerificationState.Verified,
        attempts = attempts + 1, reason = None, observedDigest = Some(value))
      case Some(value) => copy(state = MaterialDeliveryVerificationState.Failed, attempts = attempts + 1,
        reason = Some("integrityMismatch"), observedDigest = Some(value))
      case None if attempts + 1 == 3 => copy(state = MaterialDeliveryVerificationState.Failed,
        attempts = 3, reason = Some("verificationUnavailable"), observedDigest = None)
      case None => copy(state = MaterialDeliveryVerificationState.Verifying, attempts = attempts + 1,
        reason = None, observedDigest = None)
    }
  }
  def json: Json = Json.obj("resourceId" -> Json.fromString(resourceId), "state" -> Json.fromString(state.value),
    "attempts" -> Json.fromInt(attempts), "reason" -> reason.fold(Json.Null)(Json.fromString),
    "observedDigest" -> observedDigest.fold(Json.Null)(MaterialDeliveryIntegrity.digestJson),
    "fileReference" -> fileReference.fold(Json.Null)(MaterialDeliveryDocument.referenceJson),
    "storageRoot" -> storageRoot.fold(Json.Null)(MaterialDeliveryDocument.referenceJson))
  override def toString: String = "MaterialDeliveryVerification(<private>)"
}

private[knowledgelake] object MaterialDeliveryIntegrity {
  def digestJson(digest: FileStoreContentDigest): Json = Json.obj(
    "algorithm" -> Json.fromString(digest.algorithm), "value" -> Json.fromString(digest.value))
  def parseDigest(json: Json): Option[FileStoreContentDigest] = for {
    fields <- json.asObject.filter(_.keys.toSet == Set("algorithm", "value"))
    _ <- fields("algorithm").flatMap(_.asString).filter(_ == "sha256")
    value <- fields("value").flatMap(_.asString)
    digest <- FileStoreContentDigest.sha256C(value).toOption
  } yield digest
  def parse(json: Json): Option[MaterialDeliveryVerification] = for {
    fields <- json.asObject.filter(_.keys.toSet == Set("resourceId", "state", "attempts", "reason", "observedDigest", "fileReference", "storageRoot"))
    resource <- fields("resourceId").flatMap(_.asString).filter(MaterialContribution._resource_id)
    text <- fields("state").flatMap(_.asString)
    state <- MaterialDeliveryVerificationState.values.find(_.value == text)
    attempts <- fields("attempts").flatMap(_.asNumber).flatMap(_.toInt).filter(value => value >= 0 && value <= 3)
    reason <- fields("reason").flatMap(value => if (value.isNull) Some(None)
      else value.asString.filter(Set("integrityMismatch", "verificationUnavailable")).map(Some(_)))
    digest <- fields("observedDigest").flatMap(value => if (value.isNull) Some(None) else parseDigest(value).map(Some(_)))
    reference <- fields("fileReference").flatMap(value => if (value.isNull) Some(None) else MaterialDeliveryDocument.parseReference(value).map(Some(_)))
    root <- fields("storageRoot").flatMap(value => if (value.isNull) Some(None) else MaterialDeliveryDocument.parseReference(value).map(Some(_)))
  } yield MaterialDeliveryVerification(resource, state, attempts, reason, digest, reference, root)
  def consistent(value: MaterialDeliveryVerification, original: MaterialOriginalContribution, file: Option[MaterialDeliveryFile]): Boolean = {
    import MaterialDeliveryVerificationState.*
    val placed = file.exists(candidate => value.fileReference.contains(candidate.reference) && value.storageRoot == candidate.storageRoot)
    original.contentDigest.exists { expected => value.state match {
      case Pending => value.attempts == 0 && value.reason.isEmpty && value.observedDigest.isEmpty &&
        value.fileReference.isEmpty && value.storageRoot.isEmpty
      case Verifying => placed && value.attempts >= 1 && value.attempts < 3 && value.reason.isEmpty && value.observedDigest.isEmpty
      case Verified => placed && value.attempts >= 1 && value.attempts <= 3 && value.reason.isEmpty && value.observedDigest.contains(expected)
      case Failed => placed && value.attempts >= 1 && value.attempts <= 3 && (value.reason match {
        case Some("integrityMismatch") => value.observedDigest.exists(_ != expected)
        case Some("verificationUnavailable") => value.attempts == 3 && value.observedDigest.isEmpty
        case _ => false
      })
    }}
  }
}
