package org.simplemodeling.textus.knowledgelake.material

import io.circe.Json
import io.circe.jawn.JawnParser
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
  private val _parser = new JawnParser(Some(262144), false)
  def validSource(value: String): Boolean = value.nonEmpty && value.length <= 2048 &&
    value.exists(character => !character.isWhitespace) && !value.exists(Character.isISOControl)
  def parseC(text: String): Consequence[MaterialDeliveryLegacyDecision] = {
    val admitted = for {
      input <- Option(text).filter(_.length <= 262144)
      json <- _parser.parse(input).toOption
      fields <- json.asObject.filter(_.keys.toSet == Set("schema", "adjudication", "owner", "delivery", "request", "revision", "contribution", "configuration", "source"))
      _ <- fields("schema").filter(_ == Json.fromString("textus.material-delivery-legacy-decision/1"))
      _ <- fields("adjudication").filter(_ == Json.fromString("hostAdjudicatedOriginalRefusalBeforeDispatch"))
      owner <- fields("owner").flatMap(_.asString).filter(validSource)
      delivery <- fields("delivery").flatMap(_.asString).filter(validSource)
      request <- fields("request").flatMap(_.asString).filter(_.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,127}"))
      revision <- fields("revision").flatMap(_.asNumber).flatMap(_.toLong).filter(_ >= 0)
      contribution <- fields("contribution").filter(value => MaterialContribution.parseC(value).isSuccess)
      configuration <- fields("configuration").flatMap(value => MaterialDeliveryConfiguration.parseC(value.noSpaces).toOption)
      source <- fields("source").flatMap(_.asString).filter(validSource)
    } yield MaterialDeliveryLegacyDecision(owner, delivery, request, revision, contribution, configuration, source)
    admitted.fold[Consequence[MaterialDeliveryLegacyDecision]](
      Consequence.configurationInvalid("Invalid Material delivery legacy decision"))(Consequence.success)
  }
}


/** Trusted initialization control, never accepted from a delivery document or request. */
private[knowledgelake] final case class MaterialDeliveryLegacyDecision(
  owner: String, delivery: String, request: String, revision: Long,
  contribution: Json, configuration: MaterialDeliveryConfiguration, source: String
) {
  def admits(identity: String, subject: String, current: Long, document: MaterialDeliveryDocument,
    configured: Option[MaterialDeliveryConfiguration]): Boolean =
    owner == subject && delivery == identity && request == document.requestId && revision == current &&
      document.registration.owner.value == subject &&
      document.registration.status == MaterialRegistrationDocument.Status.Prepared && document.registration.stored.isEmpty &&
      configuration.allowedSubjects.contains(subject) &&
      configured.contains(configuration) && document.configuration == configuration &&
      contribution == document.registration.contribution.json &&
      document.registration.contribution.originals.nonEmpty &&
      document.registration.contribution.originals.forall(_.contentDigest.isDefined) &&
      document.busy.contains("collection:root") && document.collectionCheckpoint.isEmpty &&
      document.root.isEmpty && document.assets.isEmpty && document.derived.isEmpty &&
      document.files.isEmpty && document.manifest.isEmpty && document.transfers.isEmpty &&
      document.verifications.forall(value => value.state == MaterialDeliveryVerificationState.Pending &&
        value.attempts == 0 && value.reason.isEmpty && value.observedDigest.isEmpty &&
        value.fileReference.isEmpty && value.storageRoot.isEmpty) && document.legacyAudit.isEmpty
  override def toString: String = "MaterialDeliveryLegacyDecision(<host-owned>)"
}

private[knowledgelake] final case class MaterialDeliveryLegacyAudit(source: String, revision: Long) {
  def json: Json = Json.obj("adjudication" -> Json.fromString("hostAdjudicatedOriginalRefusalBeforeDispatch"),
    "source" -> Json.fromString(source), "previousRevision" -> Json.fromLong(revision),
    "previousBusy" -> Json.fromString("collection:root"))
  override def toString: String = "MaterialDeliveryLegacyAudit(<private>)"
}

private[knowledgelake] object MaterialDeliveryLegacyAudit {
  def parse(json: Json): Option[MaterialDeliveryLegacyAudit] = for {
    fields <- json.asObject.filter(_.keys.toSet == Set("adjudication", "source", "previousRevision", "previousBusy"))
    _ <- fields("adjudication").filter(_ == Json.fromString("hostAdjudicatedOriginalRefusalBeforeDispatch"))
    source <- fields("source").flatMap(_.asString).filter(MaterialDeliveryLegacyRecovery.validSource)
    revision <- fields("previousRevision").flatMap(_.asNumber).flatMap(_.toLong).filter(_ >= 0)
    _ <- fields("previousBusy").filter(_ == Json.fromString("collection:root"))
  } yield MaterialDeliveryLegacyAudit(source, revision)
}
