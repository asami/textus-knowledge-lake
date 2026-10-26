package org.simplemodeling.textus.knowledgelake.material

import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.cncf.context.PrincipalId
import org.goldenport.cncf.filestore.FileStoreRef
import org.goldenport.observation.Descriptor

/*
 * Immutable native lifecycle document; physical receipts confer no authority.
 *
 * @since   Oct. 21, 2026
 * @version Oct. 21, 2026
 */
private[knowledgelake] final class MaterialRegistrationDocument private (
  val contribution: MaterialContribution,
  val location: MaterialLocation,
  val storageBinding: String,
  val owner: PrincipalId,
  val createdBy: PrincipalId,
  val status: MaterialRegistrationDocument.Status,
  val stored: Option[MaterialRegistrationDocument.Stored]
) {
  import MaterialRegistrationDocument.*
  def json: Json = Json.fromFields(Vector(
    "schema" -> Json.fromString("textus.material-registration/1"),
    "contribution" -> contribution.json, "lake" -> Json.fromString(location.lake),
    "logicalPath" -> Json.fromString(location.logicalPath), "storageBinding" -> Json.fromString(storageBinding),
    "owner" -> Json.fromString(owner.value), "createdBy" -> Json.fromString(createdBy.value),
    "status" -> Json.fromString(status.value)) ++ stored.map(value => "stored" -> value.json))

  def beginC: Consequence[MaterialRegistrationDocument] =
    if (status == Status.Prepared) Consequence.success(new MaterialRegistrationDocument(
      contribution, location, storageBinding, owner, createdBy, Status.Materializing, None))
    else Consequence.operationConflict("materializeMaterial", Vector(Descriptor.Facet.Reason("registration-not-prepared")))

  def completeC(receipt: StoredMaterialPackage): Consequence[MaterialRegistrationDocument] = {
    if (status != Status.Materializing) Consequence.operationConflict("materializeMaterial", Vector(Descriptor.Facet.Reason("registration-not-materializing")))
    else {
      val admitted = for {
        expected <- MaterialPackage.planC(contribution, location, owner, createdBy).toOption
        actual <- Option(receipt)
        _ <- Option.when(actual.plan.contribution.json == contribution.json &&
          actual.plan.location.lake == location.lake && actual.plan.location.logicalPath == location.logicalPath &&
          actual.plan.manifest == expected.manifest)(())
        mapping <- Stored._parse(actual._root, actual._manifest,
          actual._files.map(value => new File(value.path, value._reference, value._parent)), expected)
      } yield new MaterialRegistrationDocument(contribution, location, storageBinding, owner, createdBy,
        Status.Registered, Some(mapping))
      admitted.fold[Consequence[MaterialRegistrationDocument]](
        Consequence.resourceInvalid("Invalid Material registration storage receipt"))(Consequence.success)
    }
  }
  override def toString: String = "MaterialRegistrationDocument(<redacted>)"
}

private[knowledgelake] object MaterialRegistrationDocument {
  enum Status(val value: String) {
    case Prepared extends Status("prepared")
    case Materializing extends Status("materializing")
    case Registered extends Status("registered")
  }
  final class File private[material] (val path: String, val reference: FileStoreRef, val parent: FileStoreRef) {
    def json: Json = Json.obj("path" -> Json.fromString(path), "reference" -> _reference_json(reference),
      "parent" -> _reference_json(parent))
    override def toString: String = "MaterialRegistrationFile(<redacted>)"
  }
  final class Stored private (val root: FileStoreRef, val manifest: FileStoreRef, val files: Vector[File]) {
    def json: Json = Json.obj("root" -> _reference_json(root), "manifest" -> _reference_json(manifest),
      "files" -> Json.fromValues(files.map(_.json)))
    override def toString: String = "MaterialRegistrationStored(<redacted>)"
  }
  object Stored {
    private[material] def _parse(root: FileStoreRef, manifest: FileStoreRef, files: Vector[File],
      plan: MaterialPackagePlan): Option[Stored] = {
      val paths = plan.originals.map(_.path) ++ plan.documents.map(_.path)
      val references = Vector(root, manifest) ++ files.map(_.reference) ++ files.map(_.parent)
      val physical = Vector(root, manifest) ++ files.map(_.reference)
      val originalparents = files.take(plan.originals.size).map(_.parent).distinct
      val documentparents = files.drop(plan.originals.size).map(_.parent).distinct
      val parents = (originalparents ++ documentparents).distinct
      Option.when(files.map(_.path) == paths && references.forall(_valid_reference) &&
        references.map(_.store).distinct.size == 1 && physical.distinct.size == physical.size &&
        parents.forall(value => !physical.contains(value)) && originalparents.size <= 1 &&
        documentparents.size <= 1 && (originalparents.isEmpty || documentparents.isEmpty ||
          originalparents.head != documentparents.head))(new Stored(root, manifest, files))
    }
  }
  private val _keys = Set("schema", "contribution", "lake", "logicalPath", "storageBinding", "owner", "createdBy", "status")
  private val _error = "Invalid Material registration document"

  def preparedC(contribution: MaterialContribution, location: MaterialLocation, storageBinding: String,
    owner: PrincipalId, createdBy: PrincipalId): Consequence[MaterialRegistrationDocument] = {
    if (Option(storageBinding).exists(_.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")) &&
      MaterialPackage.planC(contribution, location, owner, createdBy).isSuccess)
      Consequence.success(new MaterialRegistrationDocument(contribution, location, storageBinding,
        owner, createdBy, Status.Prepared, None))
    else Consequence.argumentInvalid(_error)
  }

  def parseC(json: Json): Consequence[MaterialRegistrationDocument] = {
    val admitted = for {
      fields <- Option(json).flatMap(_.asObject)
      statusvalue <- fields("status").flatMap(_.asString)
      status <- Status.values.find(_.value == statusvalue)
      _ <- Option.when(fields.keys.toSet == (_keys ++ Option.when(status == Status.Registered)("stored")))(())
      _ <- fields("schema").filter(_ == Json.fromString("textus.material-registration/1"))
      contribution <- fields("contribution").flatMap(value => MaterialContribution.parseC(value).toOption)
      lake <- fields("lake").flatMap(_.asString)
      path <- fields("logicalPath").flatMap(_.asString)
      location <- MaterialLocation.createC(lake, path).toOption
      binding <- fields("storageBinding").flatMap(_.asString)
      owner <- fields("owner").flatMap(_.asString)
      creator <- fields("createdBy").flatMap(_.asString)
      prepared <- preparedC(contribution, location, binding, PrincipalId(owner), PrincipalId(creator)).toOption
      mapping <- if (status == Status.Registered) for {
        value <- fields("stored").flatMap(_.asObject).filter(_.keys.toSet == Set("root", "manifest", "files"))
        root <- value("root").flatMap(_parse_reference)
        manifest <- value("manifest").flatMap(_parse_reference)
        entries <- value("files").flatMap(_.asArray)
        files <- MaterialContribution._sequence(entries.map { entry => for {
          fields <- entry.asObject.filter(_.keys.toSet == Set("path", "reference", "parent"))
          path <- fields("path").flatMap(_.asString)
          reference <- fields("reference").flatMap(_parse_reference)
          parent <- fields("parent").flatMap(_parse_reference)
        } yield new File(path, reference, parent) })
        plan <- MaterialPackage.planC(contribution, location, prepared.owner, prepared.createdBy).toOption
        stored <- Stored._parse(root, manifest, files, plan)
      } yield Some(stored) else Some(None)
    } yield new MaterialRegistrationDocument(contribution, location, binding, prepared.owner, prepared.createdBy, status, mapping)
    admitted.fold[Consequence[MaterialRegistrationDocument]](Consequence.argumentInvalid(_error))(Consequence.success)
  }

  private def _reference_json(reference: FileStoreRef): Json = Json.obj(
    "store" -> Json.fromString(reference.store), "key" -> Json.fromString(reference.key))
  private def _valid_reference(reference: FileStoreRef): Boolean = Option(reference).exists(value =>
    Option(value.store).exists(text => text.nonEmpty && !text.exists(Character.isISOControl)) &&
      Option(value.key).exists(text => text.nonEmpty && text.length <= 4096 && !text.exists(Character.isISOControl)))
  private def _parse_reference(json: Json): Option[FileStoreRef] = for {
    fields <- json.asObject.filter(_.keys.toSet == Set("store", "key"))
    store <- fields("store").flatMap(_.asString)
    key <- fields("key").flatMap(_.asString)
    reference = FileStoreRef(store, key)
    _ <- Option.when(_valid_reference(reference))(())
  } yield reference
}
