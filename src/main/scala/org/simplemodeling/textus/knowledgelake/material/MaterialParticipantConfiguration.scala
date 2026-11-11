package org.simplemodeling.textus.knowledgelake.material

import io.circe.{Json, parser}
import org.goldenport.Consequence
import org.goldenport.cncf.config.ComponentParameterKey
import org.goldenport.cncf.security.AuthenticationProviderIdentity

/** Explicit server-owned same-origin participant trust, never a client claim.
  * @since   Nov.  6, 2026
  * @version Nov.  6, 2026
  */
private[knowledgelake] final case class MaterialParticipantConfiguration(provider: AuthenticationProviderIdentity)

private[knowledgelake] object MaterialParticipantConfiguration {
  val configurationKey: ComponentParameterKey[String] = ComponentParameterKey.optionalString("material.participant.configuration")
  def parseC(text: String): Consequence[MaterialParticipantConfiguration] = {
    val admitted = for {
      source <- Option(text).filter(_.length <= 16384)
      json <- parser.parse(source).toOption
      fields <- json.asObject.filter(_.keys.toSet == Set("schema", "profile", "trustedProvider"))
      _ <- fields("schema").filter(_ == Json.fromString("textus.material-participant/1"))
      _ <- fields("profile").filter(_ == Json.fromString("same-origin-user-account"))
      provider <- fields("trustedProvider").flatMap(_.asObject).filter(_.keys.toSet == Set("bindingName", "providerName", "componentName"))
      binding <- provider("bindingName").flatMap(_.asString).filter(_name)
      name <- provider("providerName").flatMap(_.asString).filter(_name)
      component <- provider("componentName").flatMap(_.asString).filter(_name)
    } yield MaterialParticipantConfiguration(AuthenticationProviderIdentity(binding, name, component))
    admitted.fold[Consequence[MaterialParticipantConfiguration]](
      Consequence.configurationInvalid("Invalid Material participant trust configuration"))(Consequence.success)
  }
  private def _name(value: String): Boolean = value.nonEmpty && value.length <= 256 && value.trim == value &&
    !value.exists(character => Character.isISOControl(character) || character == '*')
}
