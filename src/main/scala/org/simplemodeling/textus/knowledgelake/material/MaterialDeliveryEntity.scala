package org.simplemodeling.textus.knowledgelake.material

import java.nio.charset.StandardCharsets
import java.time.Instant
import io.circe.{Json, Printer, parser}
import org.goldenport.Consequence
import org.goldenport.cncf.entity.{EntityPersistent, EntityPersistentCreate, EntityRevisionBinding, EntityRevisionRepresentation}
import org.goldenport.record.Record
import org.simplemodeling.model.datatype.{EntityCollectionId, EntityId, EntityRevision}

/*
 * Stable caller-scoped correlation uses the native special bridge and ordinary
 * Entity claim/read authorization. Material identity uses normal native issuance.
 * @since   Oct. 25, 2026
 * @version Nov.  6, 2026
 */
private[knowledgelake] final case class MaterialDeliveryRecord(
  id: EntityId, revision: EntityRevision, document: MaterialDeliveryDocument, attributes: Record
) {
  override def toString: String = "MaterialDeliveryRecord(<private>)"
}
private[knowledgelake] final case class MaterialDeliveryDraft(id: EntityId, document: MaterialDeliveryDocument)
private[knowledgelake] final case class MaterialDeliveryRegistrationDraft(id: EntityId, document: MaterialRegistrationDocument)

private[knowledgelake] object MaterialDeliveryEntity {
  val collectionId: EntityCollectionId = EntityCollectionId("textus", "knowledge_lake", "MaterialDelivery")
  val ownerName: String = "MaterialDelivery"
  val revisionBinding: EntityRevisionBinding = EntityRevisionBinding(EntityRevisionRepresentation.Embedded)
  private val _domain = Set("id", "revision", "document")
  private val _printer = Printer.noSpaces.copy(escapeNonAscii = true)

  def correlationC(subject: String, requestId: String): Consequence[EntityId] = {
    if (Option(subject).exists(value => value.nonEmpty && value.length <= 256 && !value.exists(Character.isISOControl)) &&
      Option(requestId).exists(_.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,127}"))) {
      val bytes = (subject.length.toString + ":" + subject + ":" + requestId).getBytes(StandardCharsets.UTF_8)
      val entropy = bytes.map(value => Integer.toHexString((value & 0xff) | 0x100).substring(1)).mkString
      EntityId.bridgeFromParts(collectionId.major, collectionId.minor, collectionId, Instant.EPOCH, entropy).flatMap { identity =>
        if (identity.value.length <= 2048) Consequence.success(identity)
        else Consequence.argumentInvalid("Material delivery correlation is too long")
      }
    } else Consequence.argumentInvalid("Invalid Material delivery correlation")
  }

  val persistent: EntityPersistent[MaterialDeliveryRecord] = new EntityPersistent[MaterialDeliveryRecord] {
    def id(entity: MaterialDeliveryRecord): EntityId = entity.id
    def toRecord(entity: MaterialDeliveryRecord): Record =
      Record(entity.attributes.fields.filterNot(field => _domain(field.key))) ++ Record.dataAuto(
        "id" -> entity.id, "revision" -> entity.revision.value, "document" -> entity.document.json)
    def fromRecord(record: Record): Consequence[MaterialDeliveryRecord] = {
      val admitted = for {
        _ <- Option.when(_domain.forall(key => record.fields.count(_.key == key) == 1))(())
        identity <- record.getAny("id").flatMap {
          case value: EntityId => EntityId.parse(value.value).toOption
          case value: String => EntityId.parse(value).toOption
          case _ => None
        }.filter(_.collection == collectionId)
        revision <- revisionBinding.revision(record).toOption
        json <- record.getAny("document").collect { case value: Json => value }
        document <- MaterialDeliveryDocument.parseC(json).toOption
        expected <- correlationC(document.registration.owner.value, document.requestId).toOption
        _ <- Option.when(identity == expected)(())
      } yield MaterialDeliveryRecord(identity, revision, document, Record(record.fields.filterNot(field => _domain(field.key))))
      admitted.fold[Consequence[MaterialDeliveryRecord]](Consequence.argumentInvalid("Invalid Material delivery entity"))(Consequence.success)
    }
    override def toStoreRecord(entity: MaterialDeliveryRecord): Record =
      toRecord(entity).upsertSingle("id", entity.id.value).upsertSingle("document", _printer.print(entity.document.json))
    override def fromStoreRecord(record: Record): Consequence[MaterialDeliveryRecord] =
      record.getAny("document") match {
        case Some(text: String) if record.fields.count(_.key == "document") == 1 =>
          parser.parse(text).fold(_ => Consequence.argumentInvalid("Invalid Material delivery entity"),
            json => fromRecord(record.upsertSingle("document", json)))
        case _ => Consequence.argumentInvalid("Invalid Material delivery entity")
      }
  }
  val createPersistent: EntityPersistentCreate[MaterialDeliveryDraft] = new EntityPersistentCreate[MaterialDeliveryDraft] {
    def id(entity: MaterialDeliveryDraft): Option[EntityId] = Some(entity.id)
    def collection(entity: MaterialDeliveryDraft): EntityCollectionId = collectionId
    def toRecord(entity: MaterialDeliveryDraft): Record = Record.dataAuto("document" -> entity.document.json)
    override def toStoreRecord(entity: MaterialDeliveryDraft): Record =
      toRecord(entity).upsertSingle("document", _printer.print(entity.document.json))
  }
  val registrationCreate: EntityPersistentCreate[MaterialDeliveryRegistrationDraft] = new EntityPersistentCreate[MaterialDeliveryRegistrationDraft] {
    def id(entity: MaterialDeliveryRegistrationDraft): Option[EntityId] = Some(entity.id)
    def collection(entity: MaterialDeliveryRegistrationDraft): EntityCollectionId = MaterialRegistrationEntity.collectionId
    def toRecord(entity: MaterialDeliveryRegistrationDraft): Record =
      MaterialRegistrationEntity.createPersistent.toRecord(MaterialRegistrationDraft(entity.document))
    override def toStoreRecord(entity: MaterialDeliveryRegistrationDraft): Record =
      MaterialRegistrationEntity.createPersistent.toStoreRecord(MaterialRegistrationDraft(entity.document))
  }
}
