package org.simplemodeling.textus.knowledgelake.material

import io.circe.Json
import io.circe.jawn.JawnParser
import org.goldenport.Consequence
import org.goldenport.cncf.config.ComponentParameterKey

/*
 * One explicit owning-host binding; admission performs no credential or provider IO.
 *
 * @since   Oct. 22, 2026
 * @version Oct. 22, 2026
 */
private[knowledgelake] final class MaterialRegistrationConfiguration private (
  val id: String,
  val allowedsubjects: Vector[String],
  val sourceformat: String,
  val connectionid: String,
  val storename: String,
  val sourceparentkey: String,
  val destinationparentkey: String,
  val lake: String,
  val logicalparent: String
) {
  override def toString: String = "MaterialRegistrationConfiguration(<redacted>)"
}

private[knowledgelake] object MaterialRegistrationConfiguration {
  val configurationKey: ComponentParameterKey[String] =
    ComponentParameterKey.optionalString("material.registration.configuration")
  private val _parser = new JawnParser(Some(131072), false)
  private val _keys = Set("schema", "id", "allowedSubjects", "sourceFormat", "connectionId", "storeName",
    "sourceParentKey", "destinationParentKey", "lake", "logicalParent")

  private[knowledgelake] def _parse(text: String): Consequence[MaterialRegistrationConfiguration] = {
    val admitted = for {
      source <- Option(text).filter(_.length <= 131072)
      json <- _parser.parse(source).toOption
      fields <- json.asObject.filter(_.keys.toSet == _keys)
      _ <- fields("schema").filter(_ == Json.fromString("textus.knowledge-lake.material-registration/1"))
      id <- fields("id").flatMap(_token)
      subjects <- fields("allowedSubjects").flatMap(_.asArray).filter(_.nonEmpty).flatMap(values =>
        MaterialContribution._sequence(values.map(_.asString.filter(value => value.length <= 256 && !value.contains('*') &&
          value.exists(character => !character.isWhitespace) && !value.exists(Character.isISOControl)))))
      _ <- Option.when(subjects.distinct.size == subjects.size)(())
      format <- fields("sourceFormat").flatMap(_.asString).filter(_ == "nict-book-material-transfer/1")
      connection <- fields("connectionId").flatMap(_token)
      store <- fields("storeName").flatMap(_token)
      sourceparent <- fields("sourceParentKey").flatMap(_parent)
      destinationparent <- fields("destinationParentKey").flatMap(_parent)
      lake <- fields("lake").flatMap(_token)
      logicalparent <- fields("logicalParent").flatMap(_.asString).filter(value =>
        value.isEmpty || MaterialLocation.createC(lake, value).isSuccess)
    } yield new MaterialRegistrationConfiguration(id, subjects, format, connection, store, sourceparent,
      destinationparent, lake, logicalparent)
    admitted.fold[Consequence[MaterialRegistrationConfiguration]](
      Consequence.configurationInvalid("Material registration configuration is invalid"))(Consequence.success)
  }

  private def _token(json: Json): Option[String] =
    json.asString.filter(_.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))
  private def _parent(json: Json): Option[String] = json.asString.filter(_.matches("[A-Za-z0-9_-]{1,256}"))
}
