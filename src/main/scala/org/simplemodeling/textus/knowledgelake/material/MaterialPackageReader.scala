package org.simplemodeling.textus.knowledgelake.material

import java.nio.ByteBuffer
import java.nio.charset.{CodingErrorAction, StandardCharsets}
import java.time.Instant
import cats.syntax.all.*
import io.circe.{Json, JsonObject}
import io.circe.jawn.JawnParser
import org.goldenport.{Consequence, ConsequenceT}
import org.goldenport.bag.BagBackend
import org.goldenport.cncf.filestore.*
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.record.Record

/** Reads one admitted package through the public native capability, without provider authority.
  * @since Oct. 25, 2026
  * @version Oct. 25, 2026
  */
private[knowledgelake] object MaterialPackageReader {
  final case class Original private[MaterialPackageReader] (
    resourceid: String, role: String, mediatype: String, bytelength: Option[Long],
    reference: FileStoreRef, parent: FileStoreRef, connectionid: String
  ) {
    def logical: Record = Record.data("resourceId" -> resourceid, "role" -> role, "mediaType" -> mediatype) ++
      bytelength.map(value => Record.data("byteLength" -> value)).getOrElse(Record.empty)
    def binding: MaterialResourceBinding = MaterialResourceBinding(resourceid, mediatype, bytelength,
      MaterialStorage(connectionid, reference.key, parent.key))
    override def toString: String = "MaterialPackageOriginal(<redacted>)"
  }
  final class Snapshot private[MaterialPackageReader] (
    val materialref: String, val title: String, val originals: Vector[Original]
  ) {
    def summary: Record = Record.data("materialRef" -> materialref, "title" -> title)
    def metadata: Record = summary ++ Record.data("resources" -> originals.map(_.logical),
      "provenance" -> Vector(Record.data("kind" -> "material-package", "reference" -> materialref)))
    override def toString: String = "MaterialPackageSnapshot(<redacted>)"
  }
  private final case class Asset(path: String, resourceid: String, role: String, mediatype: String, derivedfrom: Option[Vector[String]]) {
    override def toString: String = "MaterialPackageAsset(<redacted>)"
  }
  private final case class Manifest(title: String, assets: Vector[Asset]) {
    override def toString: String = "MaterialPackageManifest(<redacted>)"
  }
  private val _parser = new JawnParser(Some(65536), false)
  private val _json_mime = "application/json"

  private[knowledgelake] def _read(binding: MaterialPackageReadCatalog.Binding,
    store: FileStoreChildren): ExecUowM[Snapshot] = for {
    _ <- _pure(_require(store.name == binding.storename))
    root = FileStoreRef(binding.storename, binding.rootkey)
    parent = FileStoreRef(binding.storename, binding.parentkey)
    rootmetadata <- store.stat(root)
    _ <- _pure(_require(rootmetadata.reference == root && rootmetadata.kind == FileStoreKind.Collection && rootmetadata.parents.contains(parent)))
    children <- _children(store, root)
    listed <- _pure(_unique(children, "material.json").flatMap(value => _manifest_metadata(value, root).map(_ => value)))
    confirmed <- store.stat(listed.reference)
    _ <- _pure(_require(confirmed.reference == listed.reference).flatMap(_ => _manifest_metadata(confirmed, root)))
    content <- store.read(confirmed.reference)
    manifest <- _pure(_manifest_content(content, Vector(listed.byteLength, confirmed.byteLength), binding.materialref))
    assets <- _pure(_unique(children, "assets").flatMap(value => _collection(value, root)))
    originals <- _resolve(store, assets, manifest.assets.filter(_.derivedfrom.isEmpty), binding)
    _ <- if (manifest.assets.exists(_.derivedfrom.nonEmpty)) for {
      derived <- _pure(_unique(children, "derived").flatMap(value => _collection(value, root)))
      _ <- _resolve(store, derived, manifest.assets.filter(_.derivedfrom.nonEmpty), binding)
    } yield () else _pure(Consequence.unit)
  } yield new Snapshot(binding.materialref, manifest.title, originals)

  private def _children(store: FileStoreChildren, parent: FileStoreRef,
    token: Option[String] = None, cursors: Set[String] = Set.empty,
    accumulated: Vector[FileStoreMetadata] = Vector.empty, pages: Int = 0): ExecUowM[Vector[FileStoreMetadata]] = for {
    page <- store.listChildren(parent, 50, token)
    all = accumulated ++ page.items
    _ <- _pure(_require(page.items.size <= 50 && all.size <= 200 && all.map(_.reference).distinct.size == all.size &&
      page.items.forall(value => value.reference.store == store.name && value.reference != parent &&
        MaterialCatalog.validText(value.reference.key, 4096) && value.parents.contains(parent))))
    next <- page.nextPageToken match {
      case None => _pure(Consequence.success(all))
      case Some(cursor) =>
        val seen = cursors ++ token
        for {
          _ <- _pure(_require(pages + 1 < 4 && MaterialCatalog.validText(cursor, 2048) && !seen.contains(cursor)))
          result <- _children(store, parent, Some(cursor), seen, all, pages + 1)
        } yield result
    }
  } yield next

  private def _unique(children: Vector[FileStoreMetadata], label: String): Consequence[FileStoreMetadata] =
    children.filter(_.name == label) match {
      case Vector(value) => Consequence.success(value)
      case _ => _invalid
    }

  private def _collection(metadata: FileStoreMetadata, parent: FileStoreRef): Consequence[FileStoreRef] =
    _require(metadata.kind == FileStoreKind.Collection && metadata.reference.store == parent.store &&
      metadata.parents.contains(parent)).map(_ => metadata.reference)

  private def _manifest_metadata(metadata: FileStoreMetadata, parent: FileStoreRef): Consequence[Unit] =
    _require(metadata.name == "material.json" && metadata.kind == FileStoreKind.File &&
      metadata.reference.store == parent.store && metadata.parents.contains(parent) &&
      metadata.mediaType.value == _json_mime && metadata.byteLength.forall(value => value >= 0 && value <= 65536))

  private def _manifest_content(content: FileStoreContent, lengths: Vector[Option[Long]], reference: String): Consequence[Manifest] = {
    val decoded = Consequence.run {
      val bag = content.content
      val bounded = content.mediaType.value == _json_mime && bag != null && (bag.backend match {
        case BagBackend.Url(_) => false
        case _ => bag.metadata.size.forall(value => value >= 0 && value <= 65536)
      })
      if (!bounded) _invalid[Manifest]
      else {
        val read = Consequence {
          val stream = bag.openInputStream()
          try stream.readNBytes(65537) finally stream.close()
        }.recoverWith(_ => _invalid[Array[Byte]])
        read.flatMap { bytes =>
          if (bytes.length > 65536 || !(lengths :+ bag.metadata.size).flatten.forall(_ == bytes.length.toLong)) _invalid
          else Consequence {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString
          }.recoverWith(_ => _invalid[String]).flatMap { text =>
            Consequence(_parser.parse(text).toOption.flatMap(value => _manifest(value, reference)))
              .recoverWith(_ => _invalid[Option[Manifest]])
              .flatMap(_.fold[Consequence[Manifest]](_invalid)(Consequence.success))
          }
        }
      }
    }
    decoded.recoverWith(_ => _invalid)
  }

  private def _manifest(json: Json, reference: String): Option[Manifest] = for {
    fields <- _fields(json, Set("schema", "id", "title", "createdAt", "classifications", "provenance", "owner", "createdBy", "assets"))
    _ <- fields("schema").filter(_ == Json.fromString("textus.material/1"))
    _ <- fields("id").filter(_ == Json.fromString(reference))
    title <- _text(fields, "title", 4096)
    created <- fields("createdAt").flatMap(_.asString)
    _ <- Consequence(Instant.parse(created)).toOption
    classifications <- fields("classifications").flatMap(_.asArray).filter(_.size <= 128)
    _ <- MaterialContribution._sequence(classifications.map(_.asString.filter(MaterialCatalog.validText(_, 256))))
    _ <- fields("provenance").flatMap(_.asObject)
    _ <- _text(fields, "owner", 256)
    _ <- _text(fields, "createdBy", 256)
    rawassets <- fields("assets").flatMap(_.asArray).filter(_.size <= 128)
    assets <- MaterialContribution._sequence(rawassets.map(_asset))
    originalpaths = assets.filter(_.derivedfrom.isEmpty).map(_.path)
    _ <- Option.when(originalpaths.nonEmpty && assets.map(_.path).distinct.size == assets.size &&
      assets.map(_.resourceid).distinct.size == assets.size && assets.forall(_.derivedfrom.forall(_.forall(originalpaths.contains))))(())
  } yield Manifest(title, assets)

  private def _asset(json: Json): Option[Asset] = for {
    fields <- json.asObject.filter(value => value.keys.toSet == Set("path", "role", "mediaType") ||
      value.keys.toSet == Set("path", "role", "mediaType", "derivedFrom"))
    path <- fields("path").flatMap(_.asString)
    role <- _text(fields, "role", 256)
    mime <- fields("mediaType").flatMap(_.asString).filter(_.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+/[!#$%&'*+.^_`|~0-9A-Za-z-]+"))
    derived <- fields("derivedFrom") match {
      case None => Some(None)
      case Some(value) => for {
        values <- value.asArray.filter(_.nonEmpty)
        paths <- MaterialContribution._sequence(values.map(_.asString))
        _ <- Option.when(paths.distinct.size == paths.size && mime == _json_mime)(())
      } yield Some(paths)
    }
    id <- if (derived.isEmpty) Option.when(path.matches("assets/[A-Za-z0-9][A-Za-z0-9_-]{0,127}"))(path.substring(7))
      else Option.when(path.matches("derived/[A-Za-z0-9][A-Za-z0-9_-]{0,127}\\.json"))(path.substring(8, path.length - 5))
  } yield Asset(path, id, role, mime, derived)

  private def _resolve(store: FileStoreChildren, parent: FileStoreRef, assets: Vector[Asset],
    binding: MaterialPackageReadCatalog.Binding): ExecUowM[Vector[Original]] = for {
    children <- _children(store, parent)
    originals <- _pure(assets.foldLeft(Consequence.success(Vector.empty[Original]): Consequence[Vector[Original]]) { (previous, asset) =>
      for {
        resolved <- previous
        metadata <- _unique(children, asset.path.substring(asset.path.indexOf('/') + 1))
        _ <- _require(metadata.kind == FileStoreKind.File && metadata.parents.contains(parent) &&
          metadata.mediaType.value == asset.mediatype && metadata.byteLength.forall(_ >= 0))
      } yield resolved :+ Original(asset.resourceid, asset.role, asset.mediatype, metadata.byteLength,
        metadata.reference, parent, binding.connectionid)
    })
  } yield originals

  private def _fields(json: Json, keys: Set[String]): Option[JsonObject] = json.asObject.filter(_.keys.toSet == keys)
  private def _text(fields: JsonObject, key: String, maximum: Int): Option[String] =
    fields(key).flatMap(_.asString).filter(MaterialCatalog.validText(_, maximum))
  private def _require(valid: Boolean): Consequence[Unit] = if (valid) Consequence.unit else _invalid
  private def _invalid[A]: Consequence[A] = Consequence.resourceInvalid("Stored Material package is incomplete or ambiguous")
  private def _pure[A](value: Consequence[A]): ExecUowM[A] = ConsequenceT.fromConsequence(value)
}
