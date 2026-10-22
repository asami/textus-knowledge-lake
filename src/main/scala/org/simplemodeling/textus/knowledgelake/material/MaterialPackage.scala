package org.simplemodeling.textus.knowledgelake.material

import io.circe.{Json, Printer}
import org.goldenport.Consequence
import org.goldenport.cncf.context.PrincipalId

/*
 * TKL owns logical package identity and layout. Planning performs no IO.
 *
 * @since   Oct. 21, 2026
 * @version Oct. 21, 2026
 */
final class MaterialLocation private (val lake: String, val logicalPath: String) {
  def urn: String = s"urn:textus:klake:$lake:material:$logicalPath"
  override def toString: String = "MaterialLocation(<selected>)"
}

object MaterialLocation {
  def createC(lake: String, logicalPath: String): Consequence[MaterialLocation] = {
    val validlake = Option(lake).exists(_.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))
    val validpath = Option(logicalPath).exists(path => path.nonEmpty && path.length <= 1024 &&
      path.split("/", -1).forall(_.matches("[A-Za-z0-9][A-Za-z0-9._-]*")))
    if (validlake && validpath) Consequence.success(new MaterialLocation(lake, logicalPath))
    else Consequence.argumentInvalid("Invalid TKL material location")
  }
}

final case class MaterialPackageOriginal(path: String, original: MaterialOriginalContribution)

final case class MaterialPackageDocument(path: String, mediaType: String, body: String) {
  override def toString: String = "MaterialPackageDocument(<planned>)"
}

final case class MaterialPackagePlan(
  contribution: MaterialContribution,
  location: MaterialLocation,
  manifest: Json,
  originals: Vector[MaterialPackageOriginal],
  documents: Vector[MaterialPackageDocument]
) {
  override def toString: String = "MaterialPackagePlan(<planned>)"
}

object MaterialPackage {
  private val _json_printer = Printer.noSpaces.copy(escapeNonAscii = true)

  def planC(contribution: MaterialContribution, location: MaterialLocation,
    owner: PrincipalId, createdBy: PrincipalId): Consequence[MaterialPackagePlan] = {
    if (contribution == null || location == null || !_valid_principal(owner) || !_valid_principal(createdBy))
      Consequence.argumentInvalid("Invalid Material package request")
    else {
      val originals = contribution.originals.map(original => MaterialPackageOriginal(s"assets/${original.resourceId}", original))
      val documents = contribution.documents.map(document => MaterialPackageDocument(
        s"derived/${document.resourceId}.json", "application/json", _json_printer.print(document.body)))
      val originalassets = originals.map(planned => _asset(planned.path, planned.original.role, planned.original.mediaType))
      val documentassets = contribution.documents.zip(documents).map { case (document, planned) =>
        _asset(planned.path, document.role, planned.mediaType).mapObject(_.add("derivedFrom",
          Json.fromValues(document.derivedFrom.map(id => Json.fromString(s"assets/$id")))))
      }
      val manifest = Json.obj(
        "schema" -> Json.fromString("textus.material/1"),
        "id" -> Json.fromString(location.urn),
        "title" -> Json.fromString(contribution.title),
        "createdAt" -> Json.fromString(contribution.createdAt),
        "classifications" -> Json.fromValues(contribution.classifications.map(Json.fromString)),
        "provenance" -> contribution.provenance,
        "owner" -> Json.fromString(owner.value),
        "createdBy" -> Json.fromString(createdBy.value),
        "assets" -> Json.fromValues(originalassets ++ documentassets))
      Consequence.success(MaterialPackagePlan(contribution, location, manifest, originals, documents))
    }
  }

  private def _asset(path: String, role: String, media: String): Json =
    Json.obj("path" -> Json.fromString(path), "role" -> Json.fromString(role), "mediaType" -> Json.fromString(media))

  private def _valid_principal(principal: PrincipalId): Boolean =
    Option(principal).flatMap(value => Option(value.value)).exists(value =>
      value.trim.nonEmpty && !value.exists(Character.isISOControl))
}
