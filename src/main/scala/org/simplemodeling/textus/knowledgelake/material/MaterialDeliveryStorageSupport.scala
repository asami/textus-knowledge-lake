package org.simplemodeling.textus.knowledgelake.material

import java.nio.charset.StandardCharsets
import cats.syntax.all.*
import io.circe.Printer
import org.goldenport.Consequence
import org.goldenport.bag.{Bag, BagBackend, TextBag}
import org.goldenport.cncf.entity.{EntityPersistent, EntityPersistentCreate, EntitySnapshot}
import org.goldenport.cncf.filestore.*
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.datatype.MimeType
import org.simplemodeling.model.datatype.EntityId

/* Checkpoint each confirmed native FileStore effect before the next effect.
 * Unknown writes remain busy and require reconciliation, never create replay.
 * @since   Oct. 25, 2026
 * @version Nov.  6, 2026
 */
private[knowledgelake] trait MaterialDeliveryStorageSupport extends MaterialDeliverySupport {
  protected final def prepare_direct_transfer(identity: EntityId, resourceId: String): ExecUowM[org.goldenport.record.Record] = for {
    loaded <- load_delivery(identity)
    declaration <- exec_from(_direct_declaration(loaded.entity.document, resourceId))
    result <- if (loaded.entity.document.integrityUpgradeRequired || loaded.entity.document.originalUploaded(resourceId)) delivery_projection(loaded.entity)
      .map(value => org.goldenport.record.Record.data("delivery" -> value))
      else for {
        _ <- exec_from(if (loaded.entity.document.busy.isEmpty) Consequence.unit
          else delivery_conflict("prepareOriginalTransfer", "delivery-reconciliation-required"))
        store <- exec_from(_direct_store(loaded.entity.document.configuration))
        root <- _ensure_collection(loaded, store, "root")
        assets <- _ensure_collection(root, store, "assets")
        prepared <- assets.entity.document.transfers.find(_.resourceId == resourceId) match {
          case Some(_) => exec_from(Consequence.success(assets))
          case None => for {
            begun <- save_delivery(assets, assets.entity.document.copy(busy = Some("directPrepare:" + resourceId)))
            reservation <- store.prepareReservedDirectUpload(begun.entity.document.assets.get, resourceId,
              MimeType(declaration.mediaType), declaration.byteLength)
            transfer = MaterialDeliveryDirectTransfer(resourceId, opaque_id("knowledge-lake.material-transfer"), reservation.upload,
              Some(reservation.fileReference), reservation.storageRoot)
            checkpoint <- exec_from(MaterialDeliveryDocument.parseC(begun.entity.document.copy(
              transfers = begun.entity.document.transfers :+ transfer, busy = None).json))
            saved <- save_delivery(begun, checkpoint)
          } yield saved
        }
        transfer = prepared.entity.document.transfers.find(_.resourceId == resourceId).get
        observed <- _inspect_direct(prepared, store, transfer)
        projection <- delivery_projection(observed._1.entity)
      } yield {
        val base = org.goldenport.record.Record.data("delivery" -> projection)
        observed._2.fold(base)(offset => base ++ org.goldenport.record.Record.data("instruction" -> org.goldenport.record.Record.data(
          "transferRef" -> transfer.transferRef, "resourceId" -> resourceId, "method" -> transfer.upload.protocol,
          "uploadSessionUri" -> transfer.upload.uploadUri, "mediaType" -> transfer.upload.mediaType.value,
          "byteLength" -> transfer.upload.byteLength, "nextOffset" -> offset)))
      }
  } yield result

  protected final def confirm_direct_transfer(identity: EntityId, resourceId: String, transferRef: String,
    result: io.circe.Json): ExecUowM[MaterialDeliveryRecord] = for {
    loaded <- load_delivery(identity)
    declaration <- exec_from(_direct_declaration(loaded.entity.document, resourceId))
    transfer <- exec_from(loaded.entity.document.transfers.find(value => value.resourceId == resourceId && value.transferRef == transferRef)
      .fold[Consequence[MaterialDeliveryDirectTransfer]](Consequence.argumentInvalid("Unknown Material original transfer"))(Consequence.success))
    _ <- exec_from(if (result.asObject.exists(fields => fields.keys.toSet == Set("outcome", "byteLength") &&
      fields("outcome").contains(io.circe.Json.fromString("uploaded")) &&
      fields("byteLength").flatMap(_.asNumber).flatMap(_.toLong).contains(declaration.byteLength))) Consequence.unit
      else Consequence.argumentInvalid("Invalid Material transfer result"))
    confirmed <- if (loaded.entity.document.integrityUpgradeRequired || loaded.entity.document.originalUploaded(resourceId)) exec_from(Consequence.success(loaded))
      else for {
        _ <- exec_from(if (loaded.entity.document.busy.isEmpty) Consequence.unit
          else delivery_conflict("confirmOriginalTransfer", "delivery-reconciliation-required"))
        store <- exec_from(_direct_store(loaded.entity.document.configuration))
        observed <- _inspect_direct(loaded, store, transfer)
        _ <- exec_from(if (observed._2.isEmpty) Consequence.unit
          else Consequence.serviceUnavailable("Material original is not yet confirmed by the provider"))
      } yield observed._1
  } yield confirmed.entity

  private def _direct_declaration(document: MaterialDeliveryDocument, resourceid: String): Consequence[MaterialOriginalContribution] =
    if (document.transferMethod != "googleDriveResumable") delivery_conflict("directTransfer", "server-transfer-required")
    else document.registration.contribution.originals.find(_.resourceId == resourceid)
      .fold[Consequence[MaterialOriginalContribution]](Consequence.argumentInvalid("Unknown Material delivery original"))(Consequence.success)

  private def _direct_store(configuration: MaterialDeliveryConfiguration): Consequence[ReservedDirectUploadFileStore] =
    _store(configuration).flatMap {
      case store: ReservedDirectUploadFileStore if store.directUploadProtocol == "googleDriveResumable" => Consequence.success(store)
      case _ => Consequence.serviceUnavailable("Prepared direct transfer provider is unavailable")
    }

  private def _inspect_direct(snapshot: EntitySnapshot[MaterialDeliveryRecord], store: ReservedDirectUploadFileStore,
    transfer: MaterialDeliveryDirectTransfer): ExecUowM[(EntitySnapshot[MaterialDeliveryRecord], Option[Long])] = for {
    reservation <- exec_from(transfer.reservation.fold[Consequence[DirectFileUploadReservation]](
      Consequence.resourceInvalid("Material delivery requires an integrity upgrade"))(Consequence.success))
    _ <- exec_from(if (snapshot.entity.document.busy.isEmpty &&
      snapshot.entity.document.verification(transfer.resourceId).eligible) Consequence.unit
      else delivery_conflict("directTransfer", "delivery-reconciliation-required"))
    begun <- save_delivery(snapshot, snapshot.entity.document.copy(busy = Some("verification:" + transfer.resourceId)))
    observed <- store.inspectReservedDirectUpload(reservation)
    result <- observed match {
      case DirectFileUploadStatus.Pending(offset) => save_delivery(begun,
        begun.entity.document.copy(busy = None)).map(value => (value, Some(offset)))
      case DirectFileUploadStatus.Expired => save_delivery(begun,
        begun.entity.document.copy(busy = Some("expiredSession:" + transfer.resourceId))).map(value => (value, None))
      case DirectFileUploadStatus.Completed(metadata) => for {
        reference <- exec_from(_confirmation(metadata, transfer.upload.parent, transfer.upload.name, FileStoreKind.File,
          Some(transfer.upload.mediaType.value), Some(transfer.upload.byteLength), begun.entity.document))
        saved <- save_delivery(begun, begun.entity.document.copy(files = begun.entity.document.files :+
          MaterialDeliveryFile("assets/" + transfer.resourceId, reference, transfer.upload.parent, metadata.storageRoot)))
        checked <- _verify_metadata(saved, transfer.resourceId, metadata)
      } yield (checked, None)
    }
  } yield result

  protected final def upload_delivery(input: MaterialDeliveryOriginalInput): ExecUowM[MaterialDeliveryRecord] = for {
    identity <- exec_from(EntityId.parse(input.deliveryRef))
    loaded <- load_delivery(identity)
    document = loaded.entity.document
    _ <- exec_from(if (document.transferMethod == "tklMultipart") Consequence.unit
      else delivery_conflict("uploadOriginal", "direct-transfer-required"))
    declaration <- exec_from(document.registration.contribution.originals.find(_.resourceId == input.resourceId)
      .fold[Consequence[MaterialOriginalContribution]](Consequence.argumentInvalid("Unknown Material delivery original"))(Consequence.success))
    _ <- exec_from(if (input.content.contentType.mimeType.print == declaration.mediaType &&
      input.content.value.metadata.size.contains(declaration.byteLength) && !(input.content.value.backend match {
        case BagBackend.Url(_) => true
        case _ => false
      })) Consequence.unit else Consequence.argumentInvalid("Material delivery original does not match its declaration"))
    result <- if (document.originalUploaded(input.resourceId)) exec_from(Consequence.success(loaded))
      else for {
        _ <- exec_from(if (document.busy.isEmpty) Consequence.unit else delivery_conflict("uploadOriginal", "delivery-reconciliation-required"))
        store <- exec_from(_store(document.configuration))
        root <- _ensure_collection(loaded, store, "root")
        assets <- _ensure_collection(root, store, "assets")
        stored <- _write_file(assets, store, s"assets/${input.resourceId}", assets.entity.document.assets.get,
          declaration.mediaType, input.content.value, "original:" + input.resourceId)
        checked <- if (declaration.contentDigest.isEmpty) exec_from(Consequence.success(stored))
          else _recheck_original(stored, store, declaration)
      } yield checked
  } yield result.entity

  protected final def finalize_delivery(identity: EntityId): ExecUowM[MaterialDeliveryRecord] = for {
    loaded <- load_delivery(identity)
    projection <- delivery_projection(loaded.entity)
    result <- if (projection.getString("state").contains("registered")) exec_from(Consequence.success(loaded.entity))
      else if (loaded.entity.document.integrityUpgradeRequired) exec_from(Consequence.success(loaded.entity))
      else for {
        _ <- exec_from(if (loaded.entity.document.busy.forall(_ == "registration")) Consequence.unit
          else delivery_conflict("finalize", "delivery-incomplete-or-reconciliation-required"))
        checked <- _recheck_originals(loaded)
        result <- if (!checked.entity.document.originalsComplete && checked.entity.document.registration.contribution.originals.exists(value =>
          value.contentDigest.isDefined && checked.entity.document.originalUploaded(value.resourceId))) exec_from(Consequence.success(checked.entity))
          else if (checked.entity.document.manifest.isDefined) _register_delivery(checked)
          else _finalize_ready(checked)
      } yield result
  } yield result

  private def _finalize_ready(loaded: EntitySnapshot[MaterialDeliveryRecord]): ExecUowM[MaterialDeliveryRecord] = for {
        _ <- exec_from(if (loaded.entity.document.originalsComplete && loaded.entity.document.busy.isEmpty) Consequence.unit
          else delivery_conflict("finalize", "delivery-incomplete-or-reconciliation-required"))
        store <- exec_from(_store(loaded.entity.document.configuration))
        root <- _ensure_collection(loaded, store, "root")
        derived <- _ensure_collection(root, store, "derived")
        plan <- exec_from(MaterialPackage.planC(derived.entity.document.registration.contribution,
          derived.entity.document.registration.location, derived.entity.document.registration.owner,
          derived.entity.document.registration.createdBy))
        documents <- plan.documents.foldLeft(exec_from(Consequence.success(derived))) { (previous, document) =>
          previous.flatMap { current =>
            if (current.entity.document.files.exists(_.path == document.path)) exec_from(Consequence.success(current))
            else _write_file(current, store, document.path, current.entity.document.derived.get,
              document.mediaType, _json(document.body), "document:" + document.path)
          }
        }
        manifest <- _write_file(documents, store, "material.json", documents.entity.document.root.get,
          "application/json", _json(Printer.noSpaces.copy(escapeNonAscii = true).print(plan.manifest)), "manifest")
        registered <- _register_delivery(manifest)
  } yield registered

  private def _recheck_originals(snapshot: EntitySnapshot[MaterialDeliveryRecord]): ExecUowM[EntitySnapshot[MaterialDeliveryRecord]] = {
    val eligible = snapshot.entity.document.registration.contribution.originals.filter(value =>
      value.contentDigest.isDefined && snapshot.entity.document.originalUploaded(value.resourceId) &&
        snapshot.entity.document.verification(value.resourceId).eligible)
    if (eligible.isEmpty) exec_from(Consequence.success(snapshot))
    else for {
      store <- exec_from(_store(snapshot.entity.document.configuration))
      result <- eligible.foldLeft(exec_from(Consequence.success(snapshot))) { (previous, original) =>
        previous.flatMap(current => _recheck_original(current, store, original))
      }
    } yield result
  }

  private def _recheck_original(snapshot: EntitySnapshot[MaterialDeliveryRecord], store: FileStore,
    original: MaterialOriginalContribution): ExecUowM[EntitySnapshot[MaterialDeliveryRecord]] = {
    val file = snapshot.entity.document.files.find(_.path == "assets/" + original.resourceId).get
    for {
      _ <- exec_from(if (snapshot.entity.document.busy.isEmpty &&
        snapshot.entity.document.verification(original.resourceId).eligible) Consequence.unit
        else delivery_conflict("finalize", "delivery-reconciliation-required"))
      begun <- save_delivery(snapshot, snapshot.entity.document.copy(busy = Some("verification:" + original.resourceId)))
      metadata <- store.stat(file.reference)
      _ <- exec_from(if (metadata.reference == file.reference && metadata.parents == Vector(file.parent) &&
        metadata.name == original.resourceId && metadata.kind == FileStoreKind.File &&
        metadata.mediaType.value == original.mediaType && metadata.byteLength.contains(original.byteLength) &&
        metadata.storageRoot == file.storageRoot) Consequence.unit
        else Consequence.resourceInvalid("Invalid Material delivery provider confirmation"))
      saved <- _verify_metadata(begun, original.resourceId, metadata)
    } yield saved
  }

  private def _verify_metadata(snapshot: EntitySnapshot[MaterialDeliveryRecord], resourceid: String,
    metadata: FileStoreMetadata): ExecUowM[EntitySnapshot[MaterialDeliveryRecord]] = {
    val document = snapshot.entity.document
    val original = document.registration.contribution.originals.find(_.resourceId == resourceid).get
    original.contentDigest match {
      case None => exec_from(Consequence.success(snapshot))
      case Some(expected) =>
        val check = document.verification(resourceid)
        if (!check.eligible) exec_from(Consequence.success(snapshot))
        else save_delivery(snapshot, document.withVerification(check.observe(expected, metadata.contentDigest).copy(
          fileReference = Some(metadata.reference), storageRoot = metadata.storageRoot)).copy(busy = None))
    }
  }

  private def _store(configuration: MaterialDeliveryConfiguration): Consequence[FileStore] = for {
    _ <- if (core.component.exists {
      case runtime: GoogleDriveConnectionRuntime => runtime.hasGoogleDriveConfiguration
      case _ => false
    }) Consequence.unit else Consequence.serviceUnavailable("Material delivery provider connection is unavailable")
    store <- delivery_provider(configuration)
  } yield store

  private def _ensure_collection(snapshot: EntitySnapshot[MaterialDeliveryRecord], store: FileStore,
    slot: String): ExecUowM[EntitySnapshot[MaterialDeliveryRecord]] = {
    val document = snapshot.entity.document
    val retained = slot match { case "root" => document.root; case "assets" => document.assets; case _ => document.derived }
    if (retained.isDefined) exec_from(Consequence.success(snapshot))
    else {
      val parent = if (slot == "root") FileStoreRef(store.name, document.configuration.parentKey) else document.root.get
      val label = if (slot == "root") document.registration.location.logicalPath.split('/').last else slot
      for {
        begun <- save_delivery(snapshot, document.copy(busy = Some("collection:" + slot)))
        metadata <- store.createCollection(parent, label)
        reference <- exec_from(_confirmation(metadata, parent, label, FileStoreKind.Collection, None, None, begun.entity.document))
        next = slot match {
          case "root" => begun.entity.document.copy(root = Some(reference), busy = None)
          case "assets" => begun.entity.document.copy(assets = Some(reference), busy = None)
          case _ => begun.entity.document.copy(derived = Some(reference), busy = None)
        }
        saved <- save_delivery(begun, next)
      } yield saved
    }
  }

  private def _write_file(snapshot: EntitySnapshot[MaterialDeliveryRecord], store: FileStore, path: String,
    parent: FileStoreRef, media: String, content: Bag, busy: String): ExecUowM[EntitySnapshot[MaterialDeliveryRecord]] = for {
    begun <- save_delivery(snapshot, snapshot.entity.document.copy(busy = Some(busy)))
    metadata <- store.create(parent, path.split('/').last, MimeType(media), content)
    reference <- exec_from(_confirmation(metadata, parent, path.split('/').last, FileStoreKind.File,
      Some(media), content.metadata.size, begun.entity.document))
    next = if (path == "material.json") begun.entity.document.copy(manifest = Some(reference), busy = None)
      else begun.entity.document.copy(files = begun.entity.document.files :+ MaterialDeliveryFile(path, reference, parent, metadata.storageRoot), busy = None)
    saved <- save_delivery(begun, next)
  } yield saved

  private def _register_delivery(snapshot: EntitySnapshot[MaterialDeliveryRecord]): ExecUowM[MaterialDeliveryRecord] = {
    given EntityPersistentCreate[MaterialDeliveryRegistrationDraft] = MaterialDeliveryEntity.registrationCreate
    given EntityPersistent[MaterialRegistrationRecord] = MaterialRegistrationEntity.persistent
    for {
      _ <- exec_from(if (snapshot.entity.document.originalsComplete && !snapshot.entity.document.integrityUpgradeRequired &&
        snapshot.entity.document.busy.forall(_ == "registration")) Consequence.unit
        else delivery_conflict("finalize", "delivery-reconciliation-required"))
      begun <- if (snapshot.entity.document.busy.contains("registration")) exec_from(Consequence.success(snapshot))
        else save_delivery(snapshot, snapshot.entity.document.copy(busy = Some("registration")))
      document <- exec_from(completed_delivery_registration(begun.entity.document))
      claimed <- entity_claim_or_load[MaterialDeliveryRegistrationDraft, MaterialRegistrationRecord](
        MaterialDeliveryRegistrationDraft(begun.entity.document.registrationId, document))
      registered <- entity_load_snapshot[MaterialRegistrationRecord](claimed.id)
      _ <- exec_from(if (registered.entity.document.json == document.json) Consequence.unit
        else Consequence.resourceInvalid("Material delivery registration does not match"))
      saved <- save_delivery(begun, begun.entity.document.copy(busy = None))
    } yield saved.entity
  }

  private def _confirmation(metadata: FileStoreMetadata, parent: FileStoreRef, label: String,
    kind: FileStoreKind, mime: Option[String], size: Option[Long], document: MaterialDeliveryDocument): Consequence[FileStoreRef] = {
    val seen = Vector(Some(parent), document.root, document.assets, document.derived, document.manifest).flatten ++ document.files.map(_.reference)
    val valid = Option(metadata).exists(value => value.reference.store == parent.store &&
      value.reference.key.nonEmpty && value.reference.key.length <= 4096 && !value.reference.key.exists(Character.isISOControl) &&
      !seen.contains(value.reference) && value.name == label && value.kind == kind && value.parents == Vector(parent) &&
      mime.forall(expected => value.mediaType.value == expected) && size.forall(expected => value.byteLength.contains(expected)))
    if (valid) Consequence.success(metadata.reference) else Consequence.resourceInvalid("Invalid Material delivery provider confirmation")
  }
  private def _json(text: String): TextBag = TextBag.Instance(Bag.fromBytes(text.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)
}
