package org.simplemodeling.textus.knowledgelake.material

import org.goldenport.Consequence
import org.goldenport.cncf.config.ComponentParameterKey
import org.goldenport.http.HttpRequest
import org.goldenport.protocol.{Property, Request}
import org.goldenport.record.{Field, Record}

/** Business input is checked against the original HTTP fields, before CNCF's
  * header-derived properties. An Authorization field in the body is still extra.
  *
  * @since Oct. 25, 2026
  * @version Nov.  9, 2026
  */
private[knowledgelake] object MaterialDeliveryInput {
  private val _invalid = "Invalid Material delivery input fields"

  def propertiesC(request: Request, required: Set[String], optional: Set[String] = Set.empty): Consequence[List[Property]] = {
    val allowed = required ++ optional
    val selected = request.source match {
      case Some(Request.Source.Http(http)) =>
        val raw = http.method match {
          case HttpRequest.GET if http.form.isEmpty && http.body.isEmpty => Some(http.query)
          case HttpRequest.POST if http.query.isEmpty => Some(http.form)
          case _ => None
        }
        // Only native POST filename metadata may arrive outside declared properties.
        val metadata = request.arguments.filter(argument =>
          http.method == HttpRequest.POST && optional("content.filename") && argument.name == "content.filename")
        val projected = request.properties.filter(property => allowed(property.name)) ++
          metadata.map(argument => Property(argument.name, argument.value, argument.spec))
        raw.filter { record =>
          val names = record.fields.map(_.key)
          metadata.size == request.arguments.size && projected.map(_.name).toSet == names.toSet &&
            names.distinct.size == names.size && required.subsetOf(names.toSet) && names.forall(allowed) &&
            record.fields.forall { field => field.value match {
              case Field.Value.Single(value) => projected.count(_.name == field.key) == 1 &&
                projected.find(_.name == field.key).exists(_.value == value)
              case _ => false
            } }
        }.map(_ => projected)
      case _ => Option.when(request.arguments.isEmpty)(request.properties)
    }
    selected.filter { properties =>
      val names = properties.map(_.name)
      request.switches.isEmpty && names.distinct.size == names.size &&
        required.subsetOf(names.toSet) && names.forall(allowed)
    }.fold[Consequence[List[Property]]](Consequence.argumentInvalid(_invalid))(Consequence.success)
  }

  def recordC(request: Request, keys: Set[String]): Consequence[Record] =
    propertiesC(request, keys).map(properties => Record.create(properties.map(property => property.name -> property.value)))
}

private[knowledgelake] object MaterialDeliveryLegacyRecovery {
  val configurationKey: ComponentParameterKey[String] =
    ComponentParameterKey.optionalString("material.delivery.legacy-recovery-decision")
}
