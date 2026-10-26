package org.simplemodeling.textus.knowledgelake.material

import io.circe.Json
import org.goldenport.Consequence

/*
 * Admitted provider-neutral contribution values; opaque source data grants no access.
 *
 * @since   Oct. 21, 2026
 * @version Oct. 21, 2026
 */
final class MaterialContribution private (
  val title: String,
  val createdAt: String,
  val classifications: Vector[String],
  val provenance: Json,
  val originals: Vector[MaterialOriginalContribution],
  val documents: Vector[MaterialDocumentContribution],
  private val _json: Json
) {
  def json: Json = _json
  override def toString: String = "MaterialContribution(<admitted>)"
}

object MaterialContribution {
  private val _keys = Set("schema", "title", "createdAt", "classifications", "provenance", "originals", "documents")

  def parseC(json: Json): Consequence[MaterialContribution] = {
    val admitted = for {
      fields <- Option(json).flatMap(_.asObject).filter(_.keys.toSet == _keys)
      _ <- fields("schema").filter(_ == Json.fromString("textus.material-contribution/1"))
      title <- fields("title").flatMap(_.asString).filter(_nonblank)
      createdat <- fields("createdAt").flatMap(_.asString).filter(_nonblank)
      classifications <- fields("classifications").flatMap(_.asArray).flatMap(values =>
        _sequence(values.map(_.asString.filter(_nonblank))))
      provenance <- fields("provenance").filter(_.isObject)
      originals <- fields("originals").flatMap(_.asArray).flatMap(values => _sequence(values.map(MaterialOriginalContribution._parse)))
      documents <- fields("documents").flatMap(_.asArray).flatMap(values => _sequence(values.map(MaterialDocumentContribution._parse)))
      ids = originals.map(_.resourceId) ++ documents.map(_.resourceId)
      _ <- Option.when(ids.distinct.size == ids.size)(())
      _ <- Option.when(documents.forall(_.derivedFrom.forall(originals.map(_.resourceId).contains)))(())
    } yield new MaterialContribution(title, createdat, classifications, provenance, originals, documents, json)
    admitted.fold[Consequence[MaterialContribution]](
      Consequence.argumentInvalid("Invalid Material contribution"))(Consequence.success)
  }

  private[material] def _nonblank(value: String): Boolean = value.exists(character => !character.isWhitespace)
  private[material] def _resource_id(value: String): Boolean = value.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,127}")
  private[material] def _sequence[A](values: Vector[Option[A]]): Option[Vector[A]] =
    if (values.forall(_.isDefined)) Some(values.flatten) else None
}

final class MaterialOriginalContribution private (
  val resourceId: String,
  val role: String,
  val mediaType: String,
  val byteLength: Long,
  val sourceReference: Json,
  private val _json: Json
) {
  override def toString: String = "MaterialOriginalContribution(<admitted>)"
  override def equals(other: Any): Boolean = other match {
    case value: MaterialOriginalContribution => _json == value._json
    case _ => false
  }
  override def hashCode: Int = _json.hashCode
}

object MaterialOriginalContribution {
  private val _keys = Set("resourceId", "role", "mediaType", "byteLength", "sourceReference")
  private[material] def _parse(json: Json): Option[MaterialOriginalContribution] = {
    for {
      fields <- json.asObject.filter(_.keys.toSet == _keys)
      id <- fields("resourceId").flatMap(_.asString).filter(MaterialContribution._resource_id)
      role <- fields("role").flatMap(_.asString).filter(MaterialContribution._nonblank)
      media <- fields("mediaType").flatMap(_.asString)
        .filter(_.matches("[A-Za-z0-9!#$%&'*+.^_`|~-]+/[A-Za-z0-9!#$%&'*+.^_`|~-]+"))
      length <- fields("byteLength").flatMap(_.asNumber).flatMap(_.toLong).filter(_ >= 0L)
      source <- fields("sourceReference").filter(_.isObject)
    } yield new MaterialOriginalContribution(id, role, media, length, source, json)
  }
}

final class MaterialDocumentContribution private (
  val resourceId: String,
  val role: String,
  val body: Json,
  val derivedFrom: Vector[String]
) {
  override def toString: String = "MaterialDocumentContribution(<admitted>)"
}

object MaterialDocumentContribution {
  private val _keys = Set("resourceId", "role", "body", "derivedFrom")
  private[material] def _parse(json: Json): Option[MaterialDocumentContribution] = {
    for {
      fields <- json.asObject.filter(_.keys.toSet == _keys)
      id <- fields("resourceId").flatMap(_.asString).filter(MaterialContribution._resource_id)
      role <- fields("role").flatMap(_.asString).filter(MaterialContribution._nonblank)
      body <- fields("body")
      sources <- fields("derivedFrom").flatMap(_.asArray).flatMap(values =>
        MaterialContribution._sequence(values.map(_.asString.filter(MaterialContribution._resource_id))))
      _ <- Option.when(sources.distinct.size == sources.size)(())
    } yield new MaterialDocumentContribution(id, role, body, sources)
  }
}
