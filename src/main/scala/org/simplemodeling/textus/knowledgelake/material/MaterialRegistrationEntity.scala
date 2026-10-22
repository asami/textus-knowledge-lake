package org.simplemodeling.textus.knowledgelake.material

import io.circe.{Json, Printer, parser}
import org.goldenport.Consequence
import org.goldenport.cncf.datastore.DataStoreTextSearchField
import org.goldenport.cncf.directive.Query
import org.goldenport.cncf.entity.{EntityPersistent, EntityPersistentCreate, EntityQuery, EntityRevisionBinding, EntityRevisionRepresentation, EntitySearchScope, EntityVisibilityScope}
import org.goldenport.record.Record
import org.simplemodeling.model.datatype.{EntityCollectionId, EntityId, EntityRevision}

/*
 * Native Entity codecs; the Entity boundary owns IDs, authorization and revisions.
 *
 * @since   Oct. 21, 2026
 * @version Oct. 21, 2026
 */
private[knowledgelake] final case class MaterialRegistrationRecord(id: EntityId, revision: EntityRevision,
  document: MaterialRegistrationDocument, attributes: Record) {
  override def toString: String = "MaterialRegistrationRecord(<redacted>)"
}
private[knowledgelake] final case class MaterialRegistrationDraft(document: MaterialRegistrationDocument) {
  override def toString: String = "MaterialRegistrationDraft(<redacted>)"
}

private[knowledgelake] object MaterialRegistrationEntity {
  val collectionId: EntityCollectionId = EntityCollectionId("textus", "knowledge_lake", "MaterialPackageRegistration")
  val ownerName: String = "MaterialPackageRegistration"
  val revisionBinding: EntityRevisionBinding = EntityRevisionBinding(EntityRevisionRepresentation.Embedded)
  private val _title_search = DataStoreTextSearchField("titleSearchV1", caseInsensitive = true)
  private val _reference_search = DataStoreTextSearchField("referenceSearchV1", caseInsensitive = true)
  private val _domain_fields = Set("id", "revision", "document", "materialRef", "state", "titleSearchV1", "referenceSearchV1")
  private val _printer = Printer.noSpaces.copy(escapeNonAscii = true)
  private val _error = "Invalid Material registration entity"

  def searchQuery(text: Option[String]): EntityQuery[MaterialRegistrationRecord] = {
    val where = Query.And(Vector(Query.Eq("state", "registered"), Query.Or(Vector(
      _title_search.contains(text.getOrElse("")), _reference_search.contains(text.getOrElse(""))))))
    EntityQuery(collectionId, Query.plan(Record.empty, where = where), scope = EntitySearchScope.Store,
      visibilityScope = Some(EntityVisibilityScope.Owner))
  }

  def referenceQuery(materialRef: String): EntityQuery[MaterialRegistrationRecord] =
    EntityQuery(collectionId, Query.plan(Record.empty, where = Query.And(Vector(
      Query.Eq("state", "registered"), Query.Eq("materialRef", materialRef)))), scope = EntitySearchScope.Store,
      visibilityScope = Some(EntityVisibilityScope.Owner))

  val persistent: EntityPersistent[MaterialRegistrationRecord] = new EntityPersistent[MaterialRegistrationRecord] {
    def id(entity: MaterialRegistrationRecord): EntityId = entity.id
    def toRecord(entity: MaterialRegistrationRecord): Record =
      Record(entity.attributes.fields.filterNot(field => _domain_fields(field.key))) ++ Record.dataAuto(
        "id" -> entity.id, "revision" -> entity.revision.value, "document" -> entity.document.json,
        "materialRef" -> entity.document.location.urn, "state" -> entity.document.status.value,
        "titleSearchV1" -> _title_search.toStoreValue(entity.document.contribution.title),
        "referenceSearchV1" -> _reference_search.toStoreValue(entity.document.location.urn))
    def fromRecord(record: Record): Consequence[MaterialRegistrationRecord] = {
      val admitted = for {
        _ <- Option.when(_domain_fields.forall(key => record.fields.count(_.key == key) == 1))(())
        identity <- record.getAny("id") match {
          case Some(value: EntityId) => EntityId.parse(value.value).toOption.filter(_.value == value.value)
          case Some(value: String) => EntityId.parse(value).toOption.filter(_.value == value)
          case _ => None
        }
        _ <- Option.when(identity.collection == collectionId)(())
        revision <- revisionBinding.revision(record).toOption
        json <- record.getAny("document").collect { case value: Json => value }
        document <- MaterialRegistrationDocument.parseC(json).toOption
        _ <- Option.when(record.getString("materialRef").contains(document.location.urn) &&
          record.getString("state").contains(document.status.value))(())
        _ <- Option.when(record.getAny("titleSearchV1").collect { case value: String => value }
          .contains(_title_search.toStoreValue(document.contribution.title)) &&
          record.getAny("referenceSearchV1").collect { case value: String => value }
            .contains(_reference_search.toStoreValue(document.location.urn)))(())
      } yield MaterialRegistrationRecord(identity, revision, document,
        Record(record.fields.filterNot(field => _domain_fields(field.key))))
      admitted.fold[Consequence[MaterialRegistrationRecord]](Consequence.argumentInvalid(_error))(Consequence.success)
    }
    override def toStoreRecord(entity: MaterialRegistrationRecord): Record = toRecord(entity)
      .upsertSingle("id", entity.id.value).upsertSingle("document", _printer.print(entity.document.json))
    override def fromStoreRecord(record: Record): Consequence[MaterialRegistrationRecord] =
      if (record.fields.count(_.key == "document") != 1) Consequence.argumentInvalid(_error)
      else record.getAny("document") match {
        case Some(value: String) => parser.parse(value).toOption match {
          case Some(json) => fromRecord(record.upsertSingle("document", json))
          case _ => Consequence.argumentInvalid(_error)
        }
        case _ => Consequence.argumentInvalid(_error)
      }
  }
  val createPersistent: EntityPersistentCreate[MaterialRegistrationDraft] = new EntityPersistentCreate[MaterialRegistrationDraft] {
    def id(entity: MaterialRegistrationDraft): Option[EntityId] = None
    def collection(entity: MaterialRegistrationDraft): EntityCollectionId = collectionId
    def toRecord(entity: MaterialRegistrationDraft): Record = Record.dataAuto(
      "document" -> entity.document.json, "materialRef" -> entity.document.location.urn, "state" -> entity.document.status.value,
      "titleSearchV1" -> _title_search.toStoreValue(entity.document.contribution.title),
      "referenceSearchV1" -> _reference_search.toStoreValue(entity.document.location.urn))
    override def toStoreRecord(entity: MaterialRegistrationDraft): Record =
      toRecord(entity).upsertSingle("document", _printer.print(entity.document.json))
  }
}
