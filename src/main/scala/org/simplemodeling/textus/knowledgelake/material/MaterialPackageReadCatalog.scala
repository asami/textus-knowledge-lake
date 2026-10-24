package org.simplemodeling.textus.knowledgelake.material

import io.circe.{Json, JsonObject}
import io.circe.jawn.JawnParser
import org.goldenport.Consequence

/** Trusted, bounded bindings for existing packages; configuration is never a public projection.
  * @since Oct. 25, 2026
  * @version Oct. 25, 2026
  */
private[knowledgelake] final class MaterialPackageReadCatalog private (
  private val _materials: Vector[MaterialPackageReadCatalog.Binding]
) {
  def authorized(subjectId: String): Vector[MaterialPackageReadCatalog.Binding] =
    _materials.filter(_.authorizedsubjects.contains(subjectId))

  def resolve(subjectId: String, materialRef: String): Consequence[MaterialPackageReadCatalog.Binding] =
    _materials.find(_.materialref == materialRef) match {
      case None => Consequence.resourceNotFound("Material was not found")
      case Some(binding) if !binding.authorizedsubjects.contains(subjectId) =>
        Consequence.securityPermissionDenied("Material read is not allowed")
      case Some(binding) => Consequence.success(binding)
    }

  override def toString: String = "MaterialPackageReadCatalog(<redacted>)"
}

private[knowledgelake] object MaterialPackageReadCatalog {
  final class Binding private[MaterialPackageReadCatalog] (
    val materialref: String,
    val authorizedsubjects: Vector[String],
    val connectionid: String,
    val storename: String,
    val rootkey: String,
    val parentkey: String
  ) {
    override def toString: String = "MaterialPackageReadBinding(<redacted>)"
  }

  private val _parser = new JawnParser(Some(131072), false)

  def parseC(text: String): Consequence[MaterialPackageReadCatalog] = {
    val admitted = for {
      source <- Option(text).filter(_.length <= 131072)
      json <- _parser.parse(source).toOption
      fields <- _fields(json, Set("schema", "materials"))
      _ <- fields("schema").filter(_ == Json.fromString("textus.knowledge-lake.material-packages/1"))
      values <- fields("materials").flatMap(_.asArray).filter(_.size <= 32)
      bindings <- MaterialContribution._sequence(values.map(_binding))
      _ <- Option.when(bindings.map(_.materialref).distinct.size == bindings.size &&
        bindings.map(value => value.storename -> value.rootkey).distinct.size == bindings.size)(())
    } yield new MaterialPackageReadCatalog(bindings)
    admitted.fold[Consequence[MaterialPackageReadCatalog]](
      Consequence.configurationInvalid("Material package read configuration is invalid"))(Consequence.success)
  }

  private def _binding(json: Json): Option[Binding] = for {
    fields <- _fields(json, Set("materialRef", "authorizedSubjects", "storage"))
    reference <- fields("materialRef").flatMap(_.asString).filter(_valid_reference)
    rawsubjects <- fields("authorizedSubjects").flatMap(_.asArray).filter(values => values.nonEmpty && values.size <= 128)
    subjects <- MaterialContribution._sequence(rawsubjects.map(_.asString.filter(MaterialCatalog.validText(_, 256))))
    _ <- Option.when(subjects.distinct.size == subjects.size)(())
    storage <- fields("storage").flatMap(value => _fields(value,
      Set("provider", "connectionId", "storeName", "rootKey", "parentKey")))
    _ <- storage("provider").filter(_ == Json.fromString("google-drive"))
    connection <- storage("connectionId").flatMap(_token)
    store <- storage("storeName").flatMap(_token)
    root <- storage("rootKey").flatMap(_key)
    parent <- storage("parentKey").flatMap(_key).filter(_ != root)
  } yield new Binding(reference, subjects, connection, store, root, parent)

  private def _fields(json: Json, keys: Set[String]): Option[JsonObject] = json.asObject.filter(_.keys.toSet == keys)
  private def _token(json: Json): Option[String] = json.asString.filter(_.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))
  private def _key(json: Json): Option[String] = json.asString.filter(_.matches("[A-Za-z0-9_-]{1,256}"))

  private def _valid_reference(reference: String): Boolean = {
    val prefix = "urn:textus:klake:"
    if (!reference.startsWith(prefix)) false
    else {
      val suffix = reference.substring(prefix.length)
      val boundary = suffix.indexOf(":material:")
      boundary > 0 && MaterialLocation.createC(suffix.substring(0, boundary),
        suffix.substring(boundary + ":material:".length)).toOption.exists(_.urn == reference)
    }
  }
}
