package org.simplemodeling.textus.knowledgelake.material

import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.cncf.filestore.FileStoreRef
import org.simplemodeling.model.datatype.EntityId

/*
 * Durable private resource checkpoints. Busy survives an unknown provider outcome;
 * its presence forbids automatic provider replay. Scoped upload sessions are
 * private delivery state; no general server credential is retained here.
 * @since   Oct. 25, 2026
 * @version Nov.  6, 2026
 */
private[knowledgelake] final case class MaterialDeliveryFile(path: String, reference: FileStoreRef, parent: FileStoreRef, storageRoot: Option[FileStoreRef] = None) {
  def json: Json = Json.obj("path" -> Json.fromString(path), "reference" -> MaterialDeliveryDocument.referenceJson(reference),
    "parent" -> MaterialDeliveryDocument.referenceJson(parent),
    "storageRoot" -> storageRoot.fold(Json.Null)(MaterialDeliveryDocument.referenceJson))
  override def toString: String = "MaterialDeliveryFile(<confirmed>)"
}

private[knowledgelake] final case class MaterialDeliveryDocument(
  requestId: String, configuration: MaterialDeliveryConfiguration, registrationId: EntityId,
  registration: MaterialRegistrationDocument, root: Option[FileStoreRef] = None,
  assets: Option[FileStoreRef] = None, derived: Option[FileStoreRef] = None,
  files: Vector[MaterialDeliveryFile] = Vector.empty, manifest: Option[FileStoreRef] = None,
  busy: Option[String] = None,
  transferMethod: String = "tklMultipart", transferReason: Option[String] = None,
  transfers: Vector[MaterialDeliveryDirectTransfer] = Vector.empty,
  verifications: Vector[MaterialDeliveryVerification] = Vector.empty,
  collectionCheckpoint: Option[MaterialDeliveryCollectionCheckpoint] = None
) {
  def json: Json = Json.obj("schema" -> Json.fromString("textus.material-delivery/1"),
    "requestId" -> Json.fromString(requestId), "configuration" -> configuration.json,
    "registrationId" -> Json.fromString(registrationId.value), "registration" -> registration.json,
    "root" -> root.fold(Json.Null)(MaterialDeliveryDocument.referenceJson),
    "assets" -> assets.fold(Json.Null)(MaterialDeliveryDocument.referenceJson),
    "derived" -> derived.fold(Json.Null)(MaterialDeliveryDocument.referenceJson),
    "files" -> Json.fromValues(files.map(_.json)), "manifest" -> manifest.fold(Json.Null)(MaterialDeliveryDocument.referenceJson),
    "busy" -> busy.fold(Json.Null)(Json.fromString), "transferMethod" -> Json.fromString(transferMethod),
    "transferReason" -> transferReason.fold(Json.Null)(Json.fromString), "transfers" -> Json.fromValues(transfers.map(_.json)),
    "verifications" -> Json.fromValues(verifications.map(_.json)),
    "collectionCheckpoint" -> collectionCheckpoint.fold(Json.Null)(_.json))
  def originalUploaded(resourceId: String): Boolean = files.exists(_.path == s"assets/$resourceId")
  def verification(resourceId: String): MaterialDeliveryVerification =
    verifications.find(_.resourceId == resourceId).getOrElse(MaterialDeliveryVerification(resourceId))
  def integrityUpgradeRequired: Boolean = transferMethod == "googleDriveResumable" &&
    (registration.contribution.originals.exists(_.contentDigest.isEmpty) || transfers.exists(_.reservation.isEmpty))
  def originalsComplete: Boolean = registration.contribution.originals.forall(value => originalUploaded(value.resourceId) &&
    value.contentDigest.forall(expected => verification(value.resourceId).state == MaterialDeliveryVerificationState.Verified &&
      verification(value.resourceId).observedDigest.contains(expected)))
  def withVerification(value: MaterialDeliveryVerification): MaterialDeliveryDocument = {
    val values = verifications.filterNot(_.resourceId == value.resourceId) :+ value
    copy(verifications = registration.contribution.originals.flatMap(original => values.find(_.resourceId == original.resourceId)))
  }
  def packageC: Consequence[StoredMaterialPackage] = for {
    plan <- MaterialPackage.planC(registration.contribution, registration.location, registration.owner, registration.createdBy)
    result <- (root, manifest) match {
      case (Some(root), Some(manifest)) =>
        val paths = plan.originals.map(_.path) ++ plan.documents.map(_.path)
        if (originalsComplete && busy.forall(_ == "registration") && paths.toSet == files.map(_.path).toSet)
          Consequence.success(new StoredMaterialPackage(plan, root, manifest, paths.map(path => {
            val file = files.find(_.path == path).get
            new StoredMaterialPackageFile(file.path, file.reference, file.parent)
          })))
        else Consequence.resourceInvalid("Material delivery package is incomplete")
      case _ => Consequence.resourceInvalid("Material delivery package is incomplete")
    }
  } yield result
  override def toString: String = "MaterialDeliveryDocument(<private>)"
}

private[knowledgelake] object MaterialDeliveryDocument {
  private val _keys = Set("schema", "requestId", "configuration", "registrationId", "registration", "root", "assets", "derived", "files", "manifest", "busy")
  def parseC(json: Json): Consequence[MaterialDeliveryDocument] = {
    val admitted = for {
      fields <- json.asObject.filter(value => _keys.subsetOf(value.keys.toSet) &&
        (value.keys.toSet -- _keys).subsetOf(Set("transferMethod", "transferReason", "transfers", "verifications", "collectionCheckpoint")))
      _ <- fields("schema").filter(_ == Json.fromString("textus.material-delivery/1"))
      request <- fields("requestId").flatMap(_.asString).filter(_.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,127}"))
      configuration <- fields("configuration").flatMap(value => MaterialDeliveryConfiguration.parseC(value.noSpaces).toOption)
      identity <- fields("registrationId").flatMap(_.asString).flatMap(value => EntityId.parse(value).toOption)
        .filter(_.collection == MaterialRegistrationEntity.collectionId)
      registration <- fields("registration").flatMap(value => MaterialRegistrationDocument.parseC(value).toOption)
      _ <- Option.when(registration.storageBinding == configuration.id && registration.location.lake == configuration.lake &&
        registration.location.logicalPath.split("/", -1).dropRight(1).mkString("/") == configuration.logicalParent)(())
      root <- fields("root").flatMap(_optional_reference)
      assets <- fields("assets").flatMap(_optional_reference)
      derived <- fields("derived").flatMap(_optional_reference)
      manifest <- fields("manifest").flatMap(_optional_reference)
      values <- fields("files").flatMap(_.asArray)
      files <- MaterialContribution._sequence(values.map { value => for {
        entry <- value.asObject.filter(fields => Set("path", "reference", "parent").subsetOf(fields.keys.toSet) &&
          (fields.keys.toSet -- Set("path", "reference", "parent")).subsetOf(Set("storageRoot")))
        path <- entry("path").flatMap(_.asString)
        reference <- entry("reference").flatMap(_parse_reference)
        parent <- entry("parent").flatMap(_parse_reference)
        root <- entry("storageRoot").fold[Option[Option[FileStoreRef]]](Some(None))(_optional_reference)
      } yield MaterialDeliveryFile(path, reference, parent, root) })
      busy <- fields("busy").flatMap(value => if (value.isNull) Some(None) else value.asString.map(Some(_)))
      _ <- Option.when(busy.forall(value => value.nonEmpty && value.length <= 256 && !value.exists(Character.isISOControl)))(())
      plan <- MaterialPackage.planC(registration.contribution, registration.location, registration.owner, registration.createdBy).toOption
      paths = (plan.originals.map(_.path) ++ plan.documents.map(_.path)).toSet
      method <- fields("transferMethod").fold[Option[String]](Some("tklMultipart"))(_.asString)
        .filter(Set("tklMultipart", "googleDriveResumable"))
      reason <- fields("transferReason").fold[Option[Option[String]]](Some(None))(value =>
        if (value.isNull) Some(None) else value.asString.filter(Set("operatorSelectedServerTransfer", "providerDirectUploadUnsupported")).map(Some(_)))
      sessions <- fields("transfers").fold[Option[Vector[Json]]](Some(Vector.empty))(_.asArray)
      transfers <- MaterialContribution._sequence(sessions.map(MaterialDeliveryDirectTransfer.parse))
      _ <- Option.when(transfers.map(_.resourceId).distinct.size == transfers.size &&
        transfers.map(_.transferRef).distinct.size == transfers.size &&
        transfers.flatMap(_.fileReference).distinct.size == transfers.flatMap(_.fileReference).size &&
        transfers.forall(transfer => method == "googleDriveResumable" && assets.contains(transfer.upload.parent) &&
          registration.contribution.originals.exists(original => original.resourceId == transfer.resourceId &&
            original.resourceId == transfer.upload.name && original.mediaType == transfer.upload.mediaType.value &&
            original.byteLength == transfer.upload.byteLength) &&
          transfer.fileReference.forall(reference => reference.store == configuration.storeName &&
            !Vector(root, assets, derived, manifest).flatten.contains(reference) &&
            files.filterNot(_.path == "assets/" + transfer.resourceId).forall(_.reference != reference)) &&
          files.find(_.path == "assets/" + transfer.resourceId).forall(file => transfer.fileReference.forall(_ == file.reference) &&
            (transfer.fileReference.isEmpty || transfer.storageRoot == file.storageRoot))))(())
      checks <- fields("verifications").fold[Option[Vector[Json]]](Some(Vector.empty))(_.asArray)
      verifications <- MaterialContribution._sequence(checks.map(MaterialDeliveryIntegrity.parse))
      _ <- Option.when(verifications.map(_.resourceId).distinct.size == verifications.size &&
        verifications.map(_.resourceId) == registration.contribution.originals.map(_.resourceId).filter(id => verifications.exists(_.resourceId == id)) &&
        verifications.forall(value => registration.contribution.originals.find(_.resourceId == value.resourceId).exists(original =>
          MaterialDeliveryIntegrity.consistent(value, original, files.find(_.path == "assets/" + value.resourceId)))))(())
      checkpoint <- fields("collectionCheckpoint").fold[Option[Option[MaterialDeliveryCollectionCheckpoint]]](Some(None))(value =>
        if (value.isNull) Some(None) else MaterialDeliveryCollectionCheckpoint.parse(value).map(Some(_)))
      _ <- Option.when(checkpoint.forall(value => {
        val parent = if (value.slot == "root") Some(FileStoreRef(configuration.storeName, configuration.parentKey)) else root
        val label = if (value.slot == "root") registration.location.logicalPath.split('/').last else value.slot
        val retained = value.slot match { case "root" => root; case "assets" => assets; case _ => derived }
        busy.contains("collection:" + value.slot) && retained.isEmpty && parent.contains(value.reservation.binding.parent) &&
          value.reservation.binding.name == label && value.reservation.binding.caller == registration.owner.value &&
          value.reservation.reference.store == configuration.storeName
      }))(())
      _ <- Option.when(files.map(_.path).distinct.size == files.size && files.map(_.reference).distinct.size == files.size &&
        files.forall(value => paths(value.path) &&
          !Vector(root, assets, derived, manifest).flatten.contains(value.reference) &&
          (if (value.path.startsWith("assets/")) assets.contains(value.parent) else derived.contains(value.parent)) &&
          value.storageRoot.forall(_.store == configuration.storeName)) &&
        (files.flatMap(value => Vector(value.reference, value.parent)) ++ Vector(root, assets, derived, manifest).flatten)
          .forall(_.store == configuration.storeName))(())
    } yield MaterialDeliveryDocument(request, configuration, identity, registration, root, assets, derived, files, manifest, busy, method, reason, transfers, verifications, checkpoint)
    admitted.fold[Consequence[MaterialDeliveryDocument]](Consequence.argumentInvalid("Invalid Material delivery document"))(Consequence.success)
  }
  def referenceJson(reference: FileStoreRef): Json = Json.obj("store" -> Json.fromString(reference.store), "key" -> Json.fromString(reference.key))
  def parseReference(json: Json): Option[FileStoreRef] = _parse_reference(json)
  private def _parse_reference(json: Json): Option[FileStoreRef] = for {
    fields <- json.asObject.filter(_.keys.toSet == Set("store", "key"))
    store <- fields("store").flatMap(_.asString).filter(value => value.nonEmpty && !value.exists(Character.isISOControl))
    key <- fields("key").flatMap(_.asString).filter(value => value.nonEmpty && value.length <= 4096 && !value.exists(Character.isISOControl))
  } yield FileStoreRef(store, key)
  private def _optional_reference(json: Json): Option[Option[FileStoreRef]] =
    if (json.isNull) Some(None) else _parse_reference(json).map(Some(_))
}
