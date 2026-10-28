package org.simplemodeling.textus.knowledgelake.material

import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import scala.util.Using
import cats.syntax.all.*
import io.circe.Json
import org.goldenport.{Consequence, ConsequenceT}
import org.goldenport.bag.{BagBackend, BinaryBag}
import org.goldenport.cncf.filestore.*
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.record.Record

/** Resource-version observation around a completed managed original read.
  * @since Oct. 28, 2026
  * @version Oct. 28, 2026
  */
private[knowledgelake] object MaterialEvidenceObservation {
  private val _observation_limitation =
    "Source version observed around a completed original read; no version-pinned historical access or retention is promised."
  private val _unverified_limitation =
    "Original read completed, but an unchanged source version could not be established; no snapshot or retention is promised."

  def selectionC(resourceIds: Vector[String]): Consequence[Vector[String]] = {
    if (resourceIds.size <= 128 && resourceIds.distinct.size == resourceIds.size &&
        resourceIds.forall(MaterialCatalog.validText(_, 256))) Consequence.success(resourceIds)
    else Consequence.argumentInvalid("Material evidence selection is invalid")
  }

  def observe(materialRef: String, resource: MaterialResourceBinding, store: FileStore): ExecUowM[Record] = {
    for {
      before <- store.stat(FileStoreRef(store.name, resource.storage.resourcekey))
      _ <- _pure(_metadata(before, resource, store.name))
      content <- store.read(before.reference)
      length <- _pure(_consume(content, resource))
      after <- store.stat(before.reference)
      _ <- _pure(_metadata(after, resource, store.name))
      _ <- _pure(if (Vector(before.byteLength, after.byteLength).flatten.forall(_ == length) &&
          !(before.version.nonEmpty && after.version.nonEmpty && before.version != after.version)) Consequence.unit else _invalid)
    } yield {
      val version = before.version.filter(value => after.version.contains(value))
      Record.data("evidenceRef" -> _identity(materialRef, resource, store.name),
        "materialRef" -> materialRef, "resourceId" -> resource.resourceid,
        "retrievalState" -> (if (version.nonEmpty) "available" else "unverified"),
        "limitation" -> (if (version.nonEmpty) _observation_limitation else _unverified_limitation)) ++
        version.map(value => Record.data("revision" -> value)).getOrElse(Record.empty)
    }
  }

  private def _identity(materialref: String, resource: MaterialResourceBinding, storename: String): String = {
    val binding = Json.fromValues(Vector("textus.material-evidence-identity/1", materialref, resource.resourceid,
      storename, resource.storage.connectionid, resource.storage.resourcekey, resource.storage.parentkey).map(Json.fromString))
    val canonical = binding.noSpaces.flatMap(character =>
      if (character > 127) "\\u" + f"${character.toInt}%04x" else character.toString)
    val digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8))
    val qualifier = digest.map(value => f"${value & 0xff}%02x").mkString
    s"urn:textus:klake:evidence:$qualifier"
  }

  private def _metadata(metadata: FileStoreMetadata, resource: MaterialResourceBinding, storename: String): Consequence[Unit] = {
    if (metadata.reference == FileStoreRef(storename, resource.storage.resourcekey) &&
        metadata.kind == FileStoreKind.File &&
        metadata.parents.contains(FileStoreRef(storename, resource.storage.parentkey)) &&
        metadata.mediaType.value == resource.mediatype && metadata.byteLength.forall(_ >= 0) &&
        resource.bytelength.forall(value => metadata.byteLength.contains(value)) &&
        metadata.version.forall(value => MaterialCatalog.validText(value, 2048) && value.trim.nonEmpty)) Consequence.unit else _invalid
  }

  private def _consume(content: FileStoreContent, resource: MaterialResourceBinding): Consequence[Long] = {
    content.content match {
      case bag: BinaryBag if content.mediaType.value == resource.mediatype && (bag.backend match {
        case BagBackend.Url(_) => false
        case _ => bag.metadata.size.forall(_ >= 0)
      }) =>
        Consequence(Using.resource(bag.openInputStream())(_.transferTo(OutputStream.nullOutputStream())))
          .recoverWith(_ => _invalid[Long]).flatMap { length =>
            if ((bag.metadata.size.toVector ++ resource.bytelength.toVector).forall(_ == length)) Consequence.success(length)
            else _invalid
          }
      case _ => _invalid
    }
  }

  private def _invalid[A]: Consequence[A] = Consequence.resourceInvalid("Material evidence original changed or could not be confirmed")
  private def _pure[A](value: Consequence[A]): ExecUowM[A] = ConsequenceT.fromConsequence(value)
}
