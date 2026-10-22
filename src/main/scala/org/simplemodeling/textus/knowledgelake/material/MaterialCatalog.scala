package org.simplemodeling.textus.knowledgelake.material

import java.util.Locale
import io.circe.{Json, JsonObject, parser}
import org.goldenport.Consequence
import org.goldenport.record.Record

/**
 * Trusted bootstrap data. Only the logical projections may leave this package.
 *
 * @since   Oct. 21, 2026
 * @version Oct. 21, 2026
 */
private[knowledgelake] final case class MaterialStorage(connectionid: String, resourcekey: String, parentkey: String) {
  override def toString: String = "MaterialStorage(<redacted>)"
}
private[knowledgelake] final case class MaterialResourceBinding(resourceid: String, mediatype: String,
    bytelength: Option[Long], storage: MaterialStorage) {
  def logical: Record = Record.data("resourceId" -> resourceid, "role" -> "original", "mediaType" -> mediatype) ++
    bytelength.map(value => Record.data("byteLength" -> value)).getOrElse(Record.empty)
  override def toString: String = "MaterialResourceBinding(<redacted>)"
}
private[knowledgelake] final case class MaterialEntry(materialref: String, title: String,
    authorizedsubjects: Vector[String], resources: Vector[MaterialResourceBinding], provenance: Vector[Record]) {
  def summary: Record = Record.data("materialRef" -> materialref, "title" -> title)
  def metadata: Record = summary ++ Record.data("resources" -> resources.map(_.logical), "provenance" -> provenance)
  override def toString: String = "MaterialEntry(<redacted>)"
}
private[knowledgelake] final case class MaterialCatalog(materials: Vector[MaterialEntry]) {
  def search(subjectId: String, text: Option[String], offset: Int, limit: Int): Record = {
    val needle = text.getOrElse("").toLowerCase(Locale.ROOT)
    val matches = materials.filter(_.authorizedsubjects.contains(subjectId)).filter(entry =>
      entry.title.toLowerCase(Locale.ROOT).contains(needle) || entry.materialref.toLowerCase(Locale.ROOT).contains(needle))
      .sortBy(_.materialref)
    Record.data("items" -> matches.slice(offset, (offset.toLong + limit).min(Int.MaxValue).toInt).map(_.summary),
      "offset" -> offset, "limit" -> limit, "hasMore" -> (offset.toLong + limit < matches.size.toLong))
  }

  def resolve(subjectId: String, materialRef: String): Consequence[MaterialEntry] =
    materials.find(_.materialref == materialRef) match {
      case None => Consequence.resourceNotFound("Material was not found")
      case Some(entry) if !entry.authorizedsubjects.contains(subjectId) =>
        Consequence.securityPermissionDenied("Material read is not allowed")
      case Some(entry) => Consequence.success(entry)
    }

  override def toString: String = "MaterialCatalog(<redacted>)"
}

private[knowledgelake] object MaterialCatalog {
  def validText(value: String, maximum: Int): Boolean =
    Option(value).exists(text => text.nonEmpty && text.length <= maximum && !text.exists(Character.isISOControl))

  def parseC(text: String): Consequence[MaterialCatalog] = {
    val admitted = Option(text).filter(_.length <= 131072).flatMap(value => parser.parse(value).toOption)
      .flatMap(_root)
    admitted.fold[Consequence[MaterialCatalog]](
      Consequence.configurationInvalid("Material reference catalog configuration is invalid"))(Consequence.success)
  }

  private def _root(json: Json): Option[MaterialCatalog] = for {
    fields <- _fields(json, Set("schema", "materials"))
    _ <- Option.when(fields("schema").contains(Json.fromString("textus.knowledge-lake.material-catalog/1")))(())
    entries <- fields("materials").flatMap(_.asArray).flatMap(values => _sequence(values.map(_entry)))
    _ <- Option.when(entries.map(_.materialref).distinct.size == entries.size)(())
  } yield MaterialCatalog(entries)

  private def _entry(json: Json): Option[MaterialEntry] = for {
    fields <- _fields(json, Set("materialRef", "title", "authorizedSubjects", "resources", "provenance"))
    reference <- _text(fields, "materialRef", 2048)
    title <- _text(fields, "title", 4096)
    subjects <- fields("authorizedSubjects").flatMap(_.asArray).flatMap(values =>
      _sequence(values.map(_.asString.filter(validText(_, 256)))))
    _ <- Option.when(subjects.nonEmpty && subjects.distinct.size == subjects.size)(())
    resources <- fields("resources").flatMap(_.asArray).flatMap(values => _sequence(values.map(_resource)))
    _ <- Option.when(resources.map(_.resourceid).distinct.size == resources.size)(())
    provenance <- fields("provenance").flatMap(_.asArray).flatMap(values => _sequence(values.map(_provenance)))
  } yield MaterialEntry(reference, title, subjects, resources, provenance)

  private def _resource(json: Json): Option[MaterialResourceBinding] = for {
    fields <- json.asObject.filter(fields => fields.keys.toSet == Set("resourceId", "role", "mediaType", "storage") ||
      fields.keys.toSet == Set("resourceId", "role", "mediaType", "byteLength", "storage"))
    id <- _text(fields, "resourceId", 256)
    _ <- Option.when(fields("role").contains(Json.fromString("original")))(())
    mime <- fields("mediaType").flatMap(_.asString).filter(_.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+/[!#$%&'*+.^_`|~0-9A-Za-z-]+"))
    length <- fields("byteLength") match {
      case None => Some(None)
      case Some(value) => value.asNumber.flatMap(_.toLong).filter(_ >= 0).map(Some(_))
    }
    storage <- fields("storage").flatMap(_storage)
  } yield MaterialResourceBinding(id, mime, length, storage)

  private def _storage(json: Json): Option[MaterialStorage] = for {
    fields <- _fields(json, Set("provider", "connectionId", "resourceKey", "parentKey"))
    _ <- Option.when(fields("provider").contains(Json.fromString("google-drive")))(())
    connection <- fields("connectionId").flatMap(_.asString).filter(_.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))
    key <- fields("resourceKey").flatMap(_.asString).filter(_.matches("[A-Za-z0-9_-]{1,256}"))
    parent <- fields("parentKey").flatMap(_.asString).filter(_.matches("[A-Za-z0-9_-]{1,256}"))
  } yield MaterialStorage(connection, key, parent)

  private def _provenance(json: Json): Option[Record] = for {
    fields <- _fields(json, Set("kind", "reference"))
    kind <- _text(fields, "kind", 256)
    reference <- _text(fields, "reference", 2048)
  } yield Record.data("kind" -> kind, "reference" -> reference)

  private def _fields(json: Json, keys: Set[String]): Option[JsonObject] = json.asObject.filter(_.keys.toSet == keys)
  private def _text(fields: JsonObject, key: String, maximum: Int): Option[String] =
    fields(key).flatMap(_.asString).filter(validText(_, maximum))
  private def _sequence[A](values: Vector[Option[A]]): Option[Vector[A]] =
    if (values.forall(_.isDefined)) Some(values.flatten) else None
}
