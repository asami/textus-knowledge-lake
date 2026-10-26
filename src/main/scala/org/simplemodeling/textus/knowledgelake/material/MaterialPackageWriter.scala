package org.simplemodeling.textus.knowledgelake.material

import java.nio.charset.StandardCharsets
import cats.syntax.all.*
import io.circe.Printer
import org.goldenport.{Consequence, ConsequenceT}
import org.goldenport.bag.{Bag, BagBackend, TextBag}
import org.goldenport.cncf.context.PrincipalId
import org.goldenport.cncf.filestore.{FileStore, FileStoreKind, FileStoreMetadata, FileStoreRef}
import org.goldenport.cncf.unitofwork.{ExecProgram, ExecUowM}
import org.goldenport.datatype.MimeType

/*
 * Create-only native FileStore programs. Confirmation is not logical registration.
 *
 * @since   Oct. 21, 2026
 * @version Oct. 21, 2026
 */
trait MaterialOriginalSource {
  /** The owning adapter/Operation must authorize the source before invoking the writer. */
  def read(original: MaterialOriginalContribution): ExecUowM[MaterialOriginalContent]
}

final case class MaterialOriginalContent(reference: MaterialOriginalContribution, content: Bag) {
  override def toString: String = "MaterialOriginalContent(<borrowed>)"
}

final class StoredMaterialPackageFile private[material] (
  val path: String,
  private[knowledgelake] val _reference: FileStoreRef,
  private[knowledgelake] val _parent: FileStoreRef
) {
  override def toString: String = "StoredMaterialPackageFile(<confirmed>)"
}

final class StoredMaterialPackage private[material] (
  val plan: MaterialPackagePlan,
  private[knowledgelake] val _root: FileStoreRef,
  private[knowledgelake] val _manifest: FileStoreRef,
  private[knowledgelake] val _files: Vector[StoredMaterialPackageFile]
) {
  override def toString: String = "StoredMaterialPackage(<confirmed physical package>)"
}

/** No replay or rollback: a failed attempt may have written remote bytes, including its manifest. */
final class MaterialPackageWriter private (
  private val _store: FileStore,
  private val _lake: String,
  private val _logical_parent: String,
  private val _parent: FileStoreRef
) {
  import MaterialPackageWriter.*

  override def toString: String = "MaterialPackageWriter(<configured>)"

  def materialize(contribution: MaterialContribution, location: MaterialLocation,
    owner: PrincipalId, createdBy: PrincipalId, originals: MaterialOriginalSource): ExecUowM[StoredMaterialPackage] = {
    val admitted = if (Option(location).exists(value => value.lake == _lake &&
      value.logicalPath.split("/", -1).dropRight(1).mkString("/") == _logical_parent) && originals != null)
      MaterialPackage.planC(contribution, location, owner, createdBy)
    else Consequence.argumentInvalid("Invalid Material package request")
    for {
      plan <- _lift(admitted)
      root <- _collection(_parent, location.logicalPath.split('/').last, Vector(_parent))
      assets <- if (plan.originals.nonEmpty) _collection(root, "assets", Vector(_parent, root)).map(value => Option(value))
        else _lift(Consequence.success(Option.empty[FileStoreRef]))
      copied <- plan.originals.foldLeft(_lift(Consequence.success(
        (Vector.empty[StoredMaterialPackageFile], Vector(_parent, root) ++ assets)))) { (previous, planned) =>
        for {
          state <- previous
          returned <- originals.read(planned.original)
          content <- _lift(_original_content(returned, planned.original))
          metadata <- _store.create(assets.get, planned.original.resourceId, MimeType(planned.original.mediaType), content)
          stored <- _lift(_confirmation(metadata, assets.get, planned.original.resourceId, FileStoreKind.File,
            Some(planned.original.mediaType), Some(planned.original.byteLength), state._2))
        } yield (state._1 :+ new StoredMaterialPackageFile(planned.path, stored, assets.get), state._2 :+ stored)
      }
      derived <- _collection(root, "derived", copied._2)
      documents <- plan.documents.foldLeft(_lift(Consequence.success((copied._1, copied._2 :+ derived)))) {
        (previous, document) =>
          for {
            state <- previous
            content = _json_content(document.body)
            label = document.path.split('/').last
            metadata <- _store.create(derived, label, _json_mime, content)
            stored <- _lift(_confirmation(metadata, derived, label, FileStoreKind.File,
              Some(_json_mime.value), content.metadata.size, state._2))
          } yield (state._1 :+ new StoredMaterialPackageFile(document.path, stored, derived), state._2 :+ stored)
      }
      manifestcontent = _json_content(_json_printer.print(plan.manifest))
      manifestmetadata <- _store.create(root, "material.json", _json_mime, manifestcontent)
      manifest <- _lift(_confirmation(manifestmetadata, root, "material.json", FileStoreKind.File,
        Some(_json_mime.value), manifestcontent.metadata.size, documents._2))
    } yield new StoredMaterialPackage(plan, root, manifest, documents._1)
  }

  private def _collection(parent: FileStoreRef, label: String, seen: Vector[FileStoreRef]): ExecUowM[FileStoreRef] =
    for {
      metadata <- _store.createCollection(parent, label)
      reference <- _lift(_confirmation(metadata, parent, label, FileStoreKind.Collection, None, None, seen))
    } yield reference

  private def _json_content(text: String): TextBag =
    TextBag.Instance(Bag.fromBytes(text.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)

  private def _confirmation(metadata: FileStoreMetadata, parent: FileStoreRef, label: String,
    kind: FileStoreKind, mime: Option[String], size: Option[Long], seen: Vector[FileStoreRef]): Consequence[FileStoreRef] = {
    val valid = Option(metadata).exists(value => _valid_reference(value.reference, _store.name) &&
      !seen.contains(value.reference) && value.name == label && value.kind == kind &&
      Option(value.parents).exists(_.contains(parent)) &&
      mime.forall(expected => Option(value.mediaType).exists(_.value == expected)) &&
      size.forall(expected => Option(value.byteLength).exists(_.contains(expected))))
    if (valid) Consequence.success(metadata.reference)
    else Consequence.resourceInvalid("Material package storage confirmation is invalid")
  }

  private def _original_content(returned: MaterialOriginalContent, reference: MaterialOriginalContribution): Consequence[Bag] = {
    val valid = Option(returned).exists(value => value.reference == reference && Option(value.content).exists { content =>
      Option(content.metadata).exists(_.size.contains(reference.byteLength)) && (content.backend match {
        case BagBackend.Url(_) => false
        case null => false
        case _ => true
      })
    })
    if (valid) Consequence.success(returned.content)
    else Consequence.resourceInvalid("Original material source is inconsistent")
  }
}

object MaterialPackageWriter {
  private val _json_mime = MimeType("application/json")
  private val _json_printer = Printer.noSpaces.copy(escapeNonAscii = true)

  def createC(store: FileStore, lake: String, logicalParent: String,
    parent: FileStoreRef): Consequence[MaterialPackageWriter] = {
    val valid = Option(store).exists(value => Option(value.name).exists(name =>
      name.nonEmpty && !name.exists(Character.isISOControl))) &&
      MaterialLocation.createC(lake, "package").isSuccess &&
      Option(logicalParent).exists(value => value.isEmpty || MaterialLocation.createC(lake, value).isSuccess) &&
      _valid_reference(parent, store.name)
    if (valid) Consequence.success(new MaterialPackageWriter(store, lake, logicalParent, parent))
    else Consequence.argumentInvalid("Invalid Material package destination")
  }

  private def _valid_reference(reference: FileStoreRef, name: String): Boolean =
    Option(reference).exists(value => value.store == name && Option(value.key).exists(key =>
      key.nonEmpty && key.length <= 4096 && !key.exists(Character.isISOControl)))

  private def _lift[A](outcome: Consequence[A]): ExecUowM[A] =
    ConsequenceT.fromConsequence[ExecProgram, A](outcome)
}
