package org.simplemodeling.textus.knowledgelake.material

import io.circe.{Json, parser}
import org.goldenport.Consequence
import org.goldenport.cncf.config.ComponentParameterKey

/*
 * Server-owned destination binding; never projected to Android or logs.
 * @since   Oct. 25, 2026
 * @version Nov.  6, 2026
 */
private[knowledgelake] final case class MaterialDeliveryConfiguration(
  id: String, label: String, allowedSubjects: Vector[String], connectionId: String,
  storeName: String, parentKey: String, lake: String, logicalParent: String, maxResourceBytes: Long,
  preferDirect: Boolean = true
) {
  def json: Json = Json.obj("schema" -> Json.fromString("textus.material-delivery-configuration/1"),
    "id" -> Json.fromString(id), "label" -> Json.fromString(label),
    "allowedSubjects" -> Json.fromValues(allowedSubjects.map(Json.fromString)),
    "connectionId" -> Json.fromString(connectionId), "storeName" -> Json.fromString(storeName),
    "parentKey" -> Json.fromString(parentKey), "lake" -> Json.fromString(lake),
    "logicalParent" -> Json.fromString(logicalParent), "maxResourceBytes" -> Json.fromLong(maxResourceBytes),
    "preferDirect" -> Json.fromBoolean(preferDirect))
  override def toString: String = "MaterialDeliveryConfiguration(<server-owned>)"
}

private[knowledgelake] object MaterialDeliveryConfiguration {
  val configurationKey: ComponentParameterKey[String] = ComponentParameterKey.optionalString("material.delivery.configuration")
  private val _keys = Set("schema", "id", "label", "allowedSubjects", "connectionId", "storeName", "parentKey", "lake", "logicalParent", "maxResourceBytes")
  def parseC(text: String): Consequence[MaterialDeliveryConfiguration] = {
    val admitted = for {
      source <- Option(text).filter(_.length <= 131072)
      json <- parser.parse(source).toOption
      fields <- json.asObject.filter(value => _keys.subsetOf(value.keys.toSet) && (value.keys.toSet -- _keys).subsetOf(Set("preferDirect")))
      _ <- fields("schema").filter(_ == Json.fromString("textus.material-delivery-configuration/1"))
      id <- fields("id").flatMap(_token)
      label <- fields("label").flatMap(_.asString).filter(_text)
      subjects <- fields("allowedSubjects").flatMap(_.asArray).filter(_.nonEmpty).flatMap(values =>
        MaterialContribution._sequence(values.map(_.asString.filter(value => _text(value) && !value.contains('*')))))
      _ <- Option.when(subjects.distinct.size == subjects.size)(())
      connection <- fields("connectionId").flatMap(_token)
      store <- fields("storeName").flatMap(_token)
      parent <- fields("parentKey").flatMap(_.asString).filter(_.matches("[A-Za-z0-9_-]{1,256}"))
      lake <- fields("lake").flatMap(_token)
      path <- fields("logicalParent").flatMap(_.asString).filter(value => value.isEmpty || MaterialLocation.createC(lake, value).isSuccess)
      maximum <- fields("maxResourceBytes").flatMap(_.asNumber).flatMap(_.toLong).filter(_ >= 0)
      direct <- fields("preferDirect").fold[Option[Boolean]](Some(true))(_.asBoolean)
    } yield MaterialDeliveryConfiguration(id, label, subjects, connection, store, parent, lake, path, maximum, direct)
    admitted.fold[Consequence[MaterialDeliveryConfiguration]](
      Consequence.configurationInvalid("Invalid Material delivery configuration"))(Consequence.success)
  }
  private def _token(json: Json): Option[String] = json.asString.filter(_.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))
  private def _text(value: String): Boolean = value.length <= 2048 && value.exists(character => !character.isWhitespace) && !value.exists(Character.isISOControl)
}
