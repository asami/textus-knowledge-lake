package org.simplemodeling.textus.knowledgelake.material

import io.circe.Json
import io.circe.jawn.JawnParser
import org.goldenport.Consequence
import org.goldenport.cncf.config.ComponentParameterKey
import org.goldenport.cncf.resource.ResourceReadLimits
import org.goldenport.cncf.workspace.{FileReadPolicy, FileTarget, WorkspaceFilePath, WorkspaceId}

/* Private host-admitted bindings; identities and paths confer no workspace grant.
 * @since   Nov. 12, 2026
 * @version Nov. 12, 2026
 */
private[knowledgelake] final case class MaterialReferenceBinding(id: String, workspace: String, path: String) {
  def target: FileTarget = FileTarget(WorkspaceId.parseC(workspace).TAKE)
  def filePath: WorkspaceFilePath = WorkspaceFilePath.parseC(path).TAKE
  override def toString: String = "MaterialReferenceBinding(<redacted>)"
}

private[knowledgelake] final class MaterialReferenceStorage private (
  val id: String, val allowedSubjects: Vector[String], val lake: String, val logicalParent: String,
  val destination: String, val maximumBytes: Int, val bindings: Vector[MaterialReferenceBinding]
) {
  def destinationTarget: FileTarget = FileTarget(WorkspaceId.parseC(destination).TAKE)
  def policy: FileReadPolicy = FileReadPolicy(ResourceReadLimits(maximumBytes))
  def resolve(id: String): Option[MaterialReferenceBinding] = bindings.find(_.id == id)
  override def toString: String = "MaterialReferenceStorage(<redacted>)"
}

private[knowledgelake] object MaterialReferenceStorage {
  val configurationKey: ComponentParameterKey[String] =
    ComponentParameterKey.optionalString("material.reference.local.configuration")
  private val _parser = new JawnParser(Some(131072), false)
  private def _token(value: String): Boolean = value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")
  def parseC(text: String): Consequence[MaterialReferenceStorage] = {
    val admitted = for {
      input <- Option(text).filter(_.length <= 131072)
      json <- _parser.parse(input).toOption
      fields <- json.asObject.filter(_.keys.toSet == Set("schema", "id", "allowedSubjects", "lake", "logicalParent", "destinationWorkspace", "maximumBytes", "bindings"))
      _ <- fields("schema").filter(_ == Json.fromString("textus.material-local-reference/1"))
      id <- fields("id").flatMap(_.asString).filter(_token)
      subjects <- fields("allowedSubjects").flatMap(_.asArray).filter(_.nonEmpty).flatMap(values =>
        MaterialContribution._sequence(values.map(_.asString.filter(value => value.nonEmpty && value.length <= 256 &&
          !value.contains('*') && !value.exists(Character.isISOControl)))))
      _ <- Option.when(subjects.distinct.size == subjects.size)(())
      lake <- fields("lake").flatMap(_.asString).filter(_token)
      parent <- fields("logicalParent").flatMap(_.asString).filter(value => value.isEmpty || MaterialLocation.createC(lake, value).isSuccess)
      destination <- fields("destinationWorkspace").flatMap(_.asString).filter(value => WorkspaceId.parseC(value).isSuccess)
      maximum <- fields("maximumBytes").flatMap(_.asNumber).flatMap(_.toInt).filter(value => ResourceReadLimits.createC(value).isSuccess)
      entries <- fields("bindings").flatMap(_.asArray).filter(_.nonEmpty).flatMap(values => MaterialContribution._sequence(values.map { value => for {
        binding <- value.asObject.filter(_.keys.toSet == Set("id", "workspace", "path"))
        bindingid <- binding("id").flatMap(_.asString).filter(_token)
        workspace <- binding("workspace").flatMap(_.asString).filter(value => WorkspaceId.parseC(value).isSuccess)
        path <- binding("path").flatMap(_.asString).filter(value => WorkspaceFilePath.parseC(value).isSuccess)
      } yield MaterialReferenceBinding(bindingid, workspace, path) }))
      _ <- Option.when(entries.map(_.id).distinct.size == entries.size)(())
    } yield new MaterialReferenceStorage(id, subjects, lake, parent, destination, maximum, entries)
    admitted.fold[Consequence[MaterialReferenceStorage]](
      Consequence.configurationInvalid("Local Material reference configuration is invalid"))(Consequence.success)
  }

  def bindingId(original: MaterialOriginalContribution): Option[String] = for {
    fields <- original.sourceReference.asObject.filter(_.keys.toSet == Set("schema", "bindingId", "workspace", "path"))
    _ <- fields("schema").filter(_ == Json.fromString("textus.material-source-binding/1"))
    id <- fields("bindingId").flatMap(_.asString).filter(_token)
    workspace <- fields("workspace").flatMap(_.asString).filter(value => WorkspaceId.parseC(value).isSuccess)
    path <- fields("path").flatMap(_.asString).filter(value => WorkspaceFilePath.parseC(value).isSuccess)
  } yield id

  def matches(original: MaterialOriginalContribution, binding: MaterialReferenceBinding): Boolean =
    bindingId(original).contains(binding.id) && original.sourceReference.hcursor.get[String]("workspace").toOption.contains(binding.workspace) &&
      original.sourceReference.hcursor.get[String]("path").toOption.contains(binding.path)
}
