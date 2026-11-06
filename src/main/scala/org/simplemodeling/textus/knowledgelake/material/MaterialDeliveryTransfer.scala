package org.simplemodeling.textus.knowledgelake.material

import io.circe.Json
import org.goldenport.cncf.filestore.*
import org.goldenport.datatype.MimeType

/* Private provider sessions; never ordinary status or diagnostics.
 * @since Oct. 25, 2026
 * @version Nov.  6, 2026
 */
private[knowledgelake] final case class MaterialDeliveryRoute(method: String, reason: Option[String])
private[knowledgelake] object MaterialDeliveryRoute {
  def select(store: FileStore, preferDirect: Boolean): MaterialDeliveryRoute =
    if (!preferDirect) MaterialDeliveryRoute("tklMultipart", Some("operatorSelectedServerTransfer"))
    else store match {
      case provider: ReservedDirectUploadFileStore if provider.directUploadProtocol == "googleDriveResumable" =>
        MaterialDeliveryRoute("googleDriveResumable", None)
      case _ => MaterialDeliveryRoute("tklMultipart", Some("providerDirectUploadUnsupported"))
    }
}

private[knowledgelake] final case class MaterialDeliveryDirectTransfer(
  resourceId: String, transferRef: String, upload: DirectFileUpload,
  fileReference: Option[FileStoreRef] = None, storageRoot: Option[FileStoreRef] = None
) {
  def json: Json = Json.obj("resourceId" -> Json.fromString(resourceId), "transferRef" -> Json.fromString(transferRef),
    "parent" -> MaterialDeliveryDocument.referenceJson(upload.parent), "name" -> Json.fromString(upload.name),
    "mediaType" -> Json.fromString(upload.mediaType.value), "byteLength" -> Json.fromLong(upload.byteLength),
    "protocol" -> Json.fromString(upload.protocol), "uploadUri" -> Json.fromString(upload.uploadUri)).mapObject { fields =>
      fileReference.fold(fields)(reference => fields.add("fileReference", MaterialDeliveryDocument.referenceJson(reference))
        .add("storageRoot", storageRoot.fold(Json.Null)(MaterialDeliveryDocument.referenceJson)))
    }
  def reservation: Option[DirectFileUploadReservation] = fileReference.map(DirectFileUploadReservation(upload, _, storageRoot))
  override def toString: String = "MaterialDeliveryDirectTransfer(<redacted>)"
}

private[knowledgelake] object MaterialDeliveryDirectTransfer {
  def parse(json: Json): Option[MaterialDeliveryDirectTransfer] = for {
    fields <- json.asObject.filter(value => {
      val keys = Set("resourceId", "transferRef", "parent", "name", "mediaType", "byteLength", "protocol", "uploadUri")
      value.keys.toSet == keys || value.keys.toSet == keys ++ Set("fileReference", "storageRoot")
    })
    resource <- fields("resourceId").flatMap(_.asString).filter(_.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,127}"))
    reference <- fields("transferRef").flatMap(_.asString).filter(value => value.nonEmpty && value.length <= 2048 && !value.exists(Character.isISOControl))
    parent <- fields("parent").flatMap(MaterialDeliveryDocument.parseReference)
    name <- fields("name").flatMap(_.asString)
    media <- fields("mediaType").flatMap(_.asString)
    length <- fields("byteLength").flatMap(_.asNumber).flatMap(_.toLong).filter(_ >= 0)
    protocol <- fields("protocol").flatMap(_.asString).filter(_ == "googleDriveResumable")
    uri <- fields("uploadUri").flatMap(_.asString).filter(value => value.nonEmpty && value.length <= 16384 && !value.exists(Character.isISOControl))
    file <- fields("fileReference").fold[Option[Option[FileStoreRef]]](Some(None))(value => MaterialDeliveryDocument.parseReference(value).map(Some(_)))
    root <- fields("storageRoot").fold[Option[Option[FileStoreRef]]](Some(None))(value =>
      if (value.isNull) Some(None) else MaterialDeliveryDocument.parseReference(value).map(Some(_)))
    _ <- Option.when((file.toVector ++ root.toVector).forall(_.store == parent.store) &&
      file.forall(value => value != parent && !root.contains(value)))(())
  } yield MaterialDeliveryDirectTransfer(resource, reference, DirectFileUpload(parent, name, MimeType(media), length, protocol, uri), file, root)
}
