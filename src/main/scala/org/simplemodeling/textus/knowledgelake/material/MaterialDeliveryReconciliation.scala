package org.simplemodeling.textus.knowledgelake.material

import io.circe.Json
import org.goldenport.cncf.filestore.*

/** Private durable native collection-write evidence. It is never caller supplied. */
private[knowledgelake] final case class MaterialDeliveryCollectionCheckpoint(
  slot: String, reservation: FileStoreCollectionReservation, observation: String
) {
  def json: Json = Json.obj(
    "slot" -> Json.fromString(slot),
    "operation" -> Json.fromString(reservation.binding.operation),
    "caller" -> Json.fromString(reservation.binding.caller),
    "parent" -> MaterialDeliveryDocument.referenceJson(reservation.binding.parent),
    "name" -> Json.fromString(reservation.binding.name),
    "reference" -> MaterialDeliveryDocument.referenceJson(reservation.reference),
    "storageRoot" -> reservation.storageRoot.fold(Json.Null)(MaterialDeliveryDocument.referenceJson),
    "observation" -> Json.fromString(observation))
}

private[knowledgelake] object MaterialDeliveryCollectionCheckpoint {
  private val _fields = Set("slot", "operation", "caller", "parent", "name", "reference", "storageRoot", "observation")
  private val _states = Set("reserved", "notDispatched", "possiblyDispatched", "confirmed", "unknown")

  def parse(json: Json): Option[MaterialDeliveryCollectionCheckpoint] = for {
    fields <- json.asObject.filter(_.keys.toSet == _fields)
    slot <- fields("slot").flatMap(_.asString).filter(Set("root", "assets", "derived"))
    operation <- fields("operation").flatMap(_.asString).filter(_valid_text)
    caller <- fields("caller").flatMap(_.asString).filter(_valid_text)
    parent <- fields("parent").flatMap(MaterialDeliveryDocument.parseReference)
    name <- fields("name").flatMap(_.asString).filter(_valid_text)
    reference <- fields("reference").flatMap(MaterialDeliveryDocument.parseReference)
    root <- fields("storageRoot").flatMap(value => if (value.isNull) Some(None) else MaterialDeliveryDocument.parseReference(value).map(Some(_)))
    state <- fields("observation").flatMap(_.asString).filter(_states)
    _ <- Option.when(reference.store == parent.store && reference != parent && root.forall(_.store == parent.store))(())
  } yield MaterialDeliveryCollectionCheckpoint(slot,
    FileStoreCollectionReservation(FileStoreWriteBinding(operation, caller, parent, name), reference, root), state)

  private def _valid_text(value: String): Boolean = value.nonEmpty && value.length <= 4096 && !value.exists(Character.isISOControl)
}
