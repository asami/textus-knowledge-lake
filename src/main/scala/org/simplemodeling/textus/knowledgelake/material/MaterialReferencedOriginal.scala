package org.simplemodeling.textus.knowledgelake.material

import io.circe.Json
import org.goldenport.cncf.workspace.{FileObservedReadResult, FileSourceStability, FileSourceVersion}

/* Durable content evidence is distinct from transient provider identity.
 * @since   Nov. 12, 2026
 * @version Nov. 12, 2026
 */
private[knowledgelake] final case class MaterialReferencedOriginal(
  resourceId: String,
  bindingId: String,
  workspace: String,
  path: String,
  baseline: MaterialContentIntegrity.Baseline,
  before: Option[MaterialReferencedOriginal.Version],
  after: Option[MaterialReferencedOriginal.Version],
  stability: String
) {
  def json: Json = Json.obj(
    "resourceId" -> Json.fromString(resourceId), "bindingId" -> Json.fromString(bindingId),
    "workspace" -> Json.fromString(workspace), "path" -> Json.fromString(path),
    "sha256" -> Json.fromString(baseline.sha256), "byteLength" -> Json.fromLong(baseline.byteLength),
    "before" -> before.fold(Json.Null)(_.json), "after" -> after.fold(Json.Null)(_.json),
    "stability" -> Json.fromString(stability))
}

private[knowledgelake] object MaterialReferencedOriginal {
  final case class Version(size: Long, modifiedMillis: Long, createdMillis: Long) {
    def json: Json = Json.obj("size" -> Json.fromLong(size), "modifiedMillis" -> Json.fromLong(modifiedMillis),
      "createdMillis" -> Json.fromLong(createdMillis))
  }
  private def _version(value: FileSourceVersion): Version =
    Version(value.size, value.modifiedMillis, value.createdMillis)

  def observed(resourceId: String, binding: MaterialReferenceBinding, result: FileObservedReadResult): Option[MaterialReferencedOriginal] =
    result.bytes.map(bytes => MaterialReferencedOriginal(resourceId, binding.id, binding.workspace, binding.path, MaterialContentIntegrity.baseline(bytes),
      result.before.map(_version), result.after.map(_version), result.stability match {
        case FileSourceStability.Changed => "changed"
        case FileSourceStability.Unknown => "unknown"
      }))

  def parse(json: Json): Option[MaterialReferencedOriginal] = for {
    fields <- json.asObject.filter(_.keys.toSet == Set("resourceId", "bindingId", "workspace", "path", "sha256", "byteLength", "before", "after", "stability"))
    resource <- fields("resourceId").flatMap(_.asString).filter(MaterialContribution._resource_id)
    binding <- fields("bindingId").flatMap(_.asString).filter(_.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))
    workspace <- fields("workspace").flatMap(_.asString).filter(value => org.goldenport.cncf.workspace.WorkspaceId.parseC(value).isSuccess)
    path <- fields("path").flatMap(_.asString).filter(value => org.goldenport.cncf.workspace.WorkspaceFilePath.parseC(value).isSuccess)
    hash <- fields("sha256").flatMap(_.asString)
    size <- fields("byteLength").flatMap(_.asNumber).flatMap(_.toLong)
    baseline <- MaterialContentIntegrity.restore(hash, size)
    before <- fields("before").flatMap(_parse_version)
    after <- fields("after").flatMap(_parse_version)
    stability <- fields("stability").flatMap(_.asString).filter(Set("unknown", "changed"))
  } yield MaterialReferencedOriginal(resource, binding, workspace, path, baseline, before, after, stability)

  private def _parse_version(json: Json): Option[Option[Version]] =
    if (json.isNull) Some(None) else for {
      fields <- json.asObject.filter(_.keys.toSet == Set("size", "modifiedMillis", "createdMillis"))
      size <- fields("size").flatMap(_.asNumber).flatMap(_.toLong).filter(_ >= 0)
      modified <- fields("modifiedMillis").flatMap(_.asNumber).flatMap(_.toLong)
      created <- fields("createdMillis").flatMap(_.asNumber).flatMap(_.toLong)
    } yield Some(Version(size, modified, created))
}
