package org.simplemodeling.textus.knowledgelake.material

import org.goldenport.Consequence
import org.goldenport.bag.BinaryBag
import org.goldenport.datatype.MimeBody
import org.goldenport.protocol.Request

/*
 * Owning Operation input over native named multipart properties. A filename is
 * metadata only; it is never a provider key or a local path. No byte/provider IO.
 *
 * @since   Oct. 24, 2026
 *  version Oct. 25, 2026
 * @version Nov.  6, 2026
 */
private[knowledgelake] final case class MaterialDeliveryOriginalInput(
  deliveryRef: String, resourceId: String, content: MimeBody
) {
  override def toString: String = "MaterialDeliveryOriginalInput(<binary>)"
}

private[knowledgelake] object MaterialDeliveryOriginalInput {
  private val _required = Set("deliveryRef", "resourceId", "content")
  private val _allowed = _required + "content.filename"
  private val _mime = "[A-Za-z0-9!#$%&'*+.^_`|~-]+/[A-Za-z0-9!#$%&'*+.^_`|~-]+"

  def parseC(request: Request): Consequence[MaterialDeliveryOriginalInput] =
    MaterialDeliveryInput.propertiesC(request, _required, Set("content.filename")).flatMap { properties =>
    val names = properties.map(_.name)
    val admitted = for {
      _ <- Option.when(names.distinct.size == names.size && _required.subsetOf(names.toSet) && names.forall(_allowed)) (())
      fields = properties.map(value => value.name -> value.value).toMap
      reference <- fields.get("deliveryRef").collect { case value: String if _reference(value) => value }
      resource <- fields.get("resourceId").collect {
        case value: String if value.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,127}") => value
      }
      body <- fields.get("content").collect {
        case value: MimeBody if value.value.isInstanceOf[BinaryBag] && value.contentType.mimeType.print.matches(_mime) => value
      }
      _ <- Option.when(fields.get("content.filename").forall {
        case value: String => value.nonEmpty && value.length <= 2048 && !value.exists(Character.isISOControl)
        case _ => false
      })(())
    } yield MaterialDeliveryOriginalInput(reference, resource, body)
    admitted.fold[Consequence[MaterialDeliveryOriginalInput]](
      Consequence.argumentInvalid("Invalid Material delivery original input"))(Consequence.success)
    }

  private def _reference(value: String): Boolean =
    value.length <= 2048 && value.exists(character => !character.isWhitespace) && !value.exists(Character.isISOControl)
}
