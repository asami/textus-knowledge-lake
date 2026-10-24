package org.simplemodeling.textus.knowledgelake.material

import java.io.{ByteArrayInputStream, IOException, InputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import io.circe.Json
import org.goldenport.{Consequence, ConsequenceT}
import org.goldenport.bag.{Bag, BagBackend, BagMetadata, BinaryBag}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.filestore.*
import org.goldenport.cncf.unitofwork.{ExecUowM, UnitOfWork, UnitOfWorkInterpreter}
import org.goldenport.datatype.MimeType
import org.goldenport.http.HttpResponse
import org.goldenport.observation.Taxonomy
import org.goldenport.record.Record
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Bounded package interpretation with a controlled public FileStoreChildren capability.
  * @since Oct. 25, 2026
  * @version Oct. 25, 2026
  */
final class MaterialPackageReadSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import MaterialPackageReadSpec.*
  import MaterialReferenceFixture.{failure, replace}

  "Trusted stored-package bindings" should {
    "admit only exact bounded configuration" which {
      "retain exact logical identities and redact every physical binding display" in {
        Given("a trusted subject and an explicit package root unrelated to its Material URN")
        val text = catalogJson().noSpaces
        When("the package catalog admits and resolves that exact subject and reference")
        val catalog = MaterialPackageReadCatalog.parseC(text).TAKE
        val binding = catalog.resolve(subject, reference).TAKE
        Then("the configuration retains spelling without deriving keys and displays no private values")
        binding.materialref shouldBe reference
        binding.rootkey shouldBe rootKey
        binding.parentkey shouldBe parentKey
        binding.storename shouldBe storeName
        binding.connectionid shouldBe MaterialReferenceFixture.connectionId
        catalog.authorized("other-subject") shouldBe empty
        failure(catalog.resolve("other-subject", reference)).observation.taxonomy shouldBe Taxonomy.securityPermissionDenied
        failure(catalog.resolve(subject, reference + " ")).observation.taxonomy shouldBe Taxonomy.resourceNotFound
        catalog.toString shouldBe "MaterialPackageReadCatalog(<redacted>)"
        binding.toString shouldBe "MaterialPackageReadBinding(<redacted>)"
      }

      "reject malformed schemas shapes duplicate semantic keys and oversized configuration without echo" in {
        Given("closed configuration fields with malformed and duplicate-key variants")
        val valid = catalogJson().noSpaces
        val variants = Vector(null, "", "[]", "{", " " * 131073,
          replace(catalogJson(), "schema", Json.fromString("private-schema-marker")).noSpaces,
          replace(catalogJson(), "materials", Json.obj()).noSpaces,
          replace(catalogJson(), "extra", Json.fromString("private-value-marker")).noSpaces,
          valid.replace("\"schema\":", "\"schema\":\"private-value-marker\",\"schema\":"),
          valid.replace("\"materialRef\":", "\"materialRef\":\"private-value-marker\",\"materialRef\":"),
          valid.replace("\"rootKey\":", "\"\\u0072ootKey\":\"private-value-marker\",\"rootKey\":"))
        When("strict configuration admission interprets each supplied document")
        val results = variants.map(MaterialPackageReadCatalog.parseC)
        Then("every rejection has the fixed structured configuration failure")
        results.foreach(_configuration_invalid)
      }

      "reject invalid subjects URNs physical keys and duplicate bindings" in {
        Given("exact trusted entries with invalid identity authorization and storage fields")
        val entry = bindingJson()
        val storage = entry.hcursor.downField("storage").focus.get
        val badsubjects = Vector(Json.arr(), Json.arr(Json.fromString(subject), Json.fromString(subject)),
          Json.arr(Json.fromString("")), Json.arr(Json.fromString("bad\nsubject")),
          Json.arr(Json.fromString("x" * 257)), Json.fromValues(Vector.fill(129)(Json.fromString(subject))))
        val badreferences = Vector("urn:knowledge-lake:opaque:item", " " + reference, reference + " ",
          "URN:textus:klake:fixture:material:item", "urn:textus:klake::material:item",
          "urn:textus:klake:fixture:material:../item", "urn:textus:klake:fixture:material:a//b",
          "urn:textus:klake:fixture:material:item%2fother")
        val badstorage = Vector("provider" -> "s3", "connectionId" -> "bad connection", "storeName" -> "",
          "rootKey" -> "a/b", "parentKey" -> "parent%20key", "rootKey" -> parentKey,
          "parentKey" -> ("p" * 257)).map { case (key, value) => replace(storage, key, Json.fromString(value)) } ++
          Vector(replace(storage, "unknown", Json.fromString("private-value-marker")))
        val badentries = badsubjects.map(value => replace(entry, "authorizedSubjects", value)) ++
          badreferences.map(value => replace(entry, "materialRef", Json.fromString(value))) ++
          badstorage.map(value => replace(entry, "storage", value)) ++ Vector(Json.Null)
        val duplicates = Vector(catalogJson(Vector(entry, entry)),
          catalogJson(Vector(entry, bindingJson(reference = reference + "-other"))),
          catalogJson(Vector.fill(33)(entry)))
        When("the closed entry shape and catalog-wide uniqueness are admitted")
        val results = badentries.map(value => MaterialPackageReadCatalog.parseC(catalogJson(Vector(value)).noSpaces)) ++
          duplicates.map(value => MaterialPackageReadCatalog.parseC(value.noSpaces))
        Then("no malformed subject root or logical identity becomes an admitted binding")
        results.foreach(_configuration_invalid)
      }

      "retain literal valid location spelling across bounded generated identities" in {
        Given("generated valid lake and logical leaf identifiers with independent opaque physical keys")
        val samples = for {
          lake <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.take(24).mkString)
          leaf <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.take(24).mkString)
        } yield lake -> leaf
        When("one hundred bounded identities are admitted without normalization")
        val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100),
          Prop.forAll(samples) { case (lake, leaf) =>
            val selected = s"urn:textus:klake:$lake:material:2026/$leaf"
            MaterialPackageReadCatalog.parseC(catalogJson(Vector(bindingJson(reference = selected))).noSpaces)
              .toOption.exists(catalog => catalog.resolve(subject, selected).toOption.exists(binding =>
                binding.materialref == selected && binding.rootkey == rootKey))
          })
        Then("each exact roundtrip preserves its URN and independently supplied physical root")
        checked.passed shouldBe true
        checked.succeeded should be >= 100
        checked.discarded shouldBe 0
      }
    }
  }

  "Stored Material package reader" should {
    "resolve complete bounded package metadata" which {
      "expose original roles order sizes and logical provenance without reading any asset body" in {
        Given("a package with two ordered originals and one derived document")
        val manifest = manifestJson()
        val fixture = new Store(manifest)
        When("the native interpreter resolves the admitted package")
        val snapshot = evaluate(fixture).TAKE
        Then("only the manifest body is read and the public projection contains logical original resources")
        snapshot.materialref shouldBe reference
        snapshot.title shouldBe title
        snapshot.originals.map(_.resourceid) shouldBe Vector("image-1", "audio-1")
        snapshot.originals.map(_.role) shouldBe Vector("cover", "narration")
        snapshot.originals.map(_.mediatype) shouldBe Vector("image/png", "audio/ogg")
        snapshot.originals.map(_.bytelength) shouldBe Vector(Some(4L), None)
        fixture.reads shouldBe Vector(manifestRef)
        fixture.stats shouldBe Vector(rootRef, manifestRef)
        fixture.listings.map(_._1) shouldBe Vector(rootRef, assetsRef, derivedRef)
        fixture.listings.map(_._2).distinct shouldBe Vector(50)
        val metadata = snapshot.metadata
        metadata.fields.map(_.key).toSet shouldBe Set("materialRef", "title", "resources", "provenance")
        metadata.getAsC[Vector[Record]]("resources").TAKE.get.map(_.getString("resourceId").get) shouldBe Vector("image-1", "audio-1")
        metadata.getAsC[Vector[Record]]("provenance").TAKE.get.head shouldBe
          Record.data("kind" -> "material-package", "reference" -> reference)
        privateMarkers.foreach(marker => metadata.print should not include marker)
        snapshot.toString shouldBe "MaterialPackageSnapshot(<redacted>)"
        snapshot.originals.foreach(_.toString shouldBe "MaterialPackageOriginal(<redacted>)")
      }

      "accept absent observed lengths without inventing public byteLength values" in {
        Given("a valid manifest and original files whose list and stat sizes are unknown")
        val fixture = new Store()
        fixture.rootpages = Vector(FileStoreChildPage(rootChildren(manifestJson()).map(_.copy(byteLength = None)), None))
        fixture.manifestmetadata = fixture.manifestmetadata.copy(byteLength = None)
        fixture.assetpages = Vector(FileStoreChildPage(originalChildren.map(_.copy(byteLength = None)), None))
        fixture.content = FileStoreContent(MimeType("application/json"),
          Bag.Instance(BagBackend.InMemory(manifestJson().noSpaces.getBytes(StandardCharsets.UTF_8))))
        When("the exact package is resolved with optional observations absent")
        val snapshot = evaluate(fixture).TAKE
        Then("valid originals remain available and absent sizes stay absent")
        snapshot.originals.map(_.bytelength) shouldBe Vector(None, None)
        snapshot.originals.foreach(_.logical.getAsC[Long]("byteLength").TAKE shouldBe None)
      }

      "follow actual continuation through empty pages and four independent bounded pages per collection" in {
        Given("root assets and derived listings whose useful entries arrive after empty continued pages")
        val fixture = new Store()
        fixture.rootpages = continuedPages(rootChildren(manifestJson()))
        fixture.assetpages = continuedPages(originalChildren)
        fixture.derivedpages = continuedPages(derivedChildren)
        When("the reader collects each bounded listing before selecting unique names")
        val snapshot = evaluate(fixture).TAKE
        Then("all twelve requests use limit fifty and only returned cursors within their own collection")
        snapshot.originals should have size 2
        Vector(rootRef, assetsRef, derivedRef).foreach { parent =>
          fixture.listings.filter(_._1 == parent).map(value => value._2 -> value._3) shouldBe
            Vector(50 -> None, 50 -> Some("next-1"), 50 -> Some("next-2"), 50 -> Some("next-3"))
        }
      }

      "accept exactly two hundred distinct children when the fourth page is terminal" in {
        Given("a full four-page root listing with one manifest and the required collections")
        val fixture = new Store()
        val children = rootChildren(manifestJson()) ++ (1 to 197).map(index =>
          metadata(FileStoreRef(storeName, s"Unrelated_$index"), s"unrelated-$index", rootRef)).toVector
        fixture.rootpages = children.grouped(50).zipWithIndex.map { case (page, index) =>
          FileStoreChildPage(page.toVector, Option.when(index < 3)(s"full-${index + 1}"))
        }.toVector
        When("the reader resolves names after completing the terminal fourth page")
        val result = evaluate(fixture)
        Then("the exact budget is valid without following unrelated children")
        result.isSuccess shouldBe true
        fixture.listings.count(_._1 == rootRef) shouldBe 4
        fixture.reads shouldBe Vector(manifestRef)
      }
    }

    "reject ambiguous incomplete or unbounded observations" which {
      Vector("root", "assets", "derived").foreach { collection =>
        s"reject duplicate labels across pages in the $collection collection after collecting its continuation" in {
          Given("distinct physical references with the same required display label on different pages")
          val fixture = new Store()
          val parent = collectionRef(collection)
          val original = collection match {
            case "root" => rootChildren(manifestJson()).head
            case "assets" => originalChildren.head
            case _ => derivedChildren.head
          }
          val first = collection match {
            case "root" => rootChildren(manifestJson())
            case "assets" => originalChildren
            case _ => derivedChildren
          }
          fixture.setPages(collection, Vector(FileStoreChildPage(first, Some("ambiguous-next")),
            FileStoreChildPage(Vector(original.copy(reference = FileStoreRef(storeName, "DuplicatePrivateFile"))), None)))
          When("the full bounded listing resolves the required exact filename")
          val result = evaluate(fixture)
          Then("ambiguity fails without selecting a first label or reading any original")
          _integrity_invalid(result)
          fixture.listings.filter(_._1 == parent).map(_._3) shouldBe Vector(None, Some("ambiguous-next"))
          fixture.reads.forall(_ == manifestRef) shouldBe true
        }
      }

      Vector("repeated-cursor", "continued-fourth-page", "duplicate-reference", "oversized-page").foreach { defect =>
        s"stop $defect without requesting an additional page" in {
          Given("a root listing that violates one independently specified pagination bound")
          val fixture = new Store()
          fixture.rootpages = defect match {
            case "repeated-cursor" => Vector(FileStoreChildPage(Vector.empty, Some("same")), FileStoreChildPage(Vector.empty, Some("same")))
            case "continued-fourth-page" => Vector.tabulate(4)(index => FileStoreChildPage(Vector.empty, Some(s"next-$index")))
            case "duplicate-reference" => Vector(FileStoreChildPage(rootChildren(manifestJson()), Some("duplicate")),
              FileStoreChildPage(Vector(rootChildren(manifestJson()).head), None))
            case _ => Vector(FileStoreChildPage(Vector.tabulate(51)(index =>
              metadata(FileStoreRef(storeName, s"Many_$index"), s"many-$index", rootRef)), None))
          }
          When("the reader follows the actual bounded listing")
          val result = evaluate(fixture)
          Then("the integrity failure ends pagination without manifest media or retry")
          _integrity_invalid(result)
          fixture.listings.size shouldBe (defect match {
            case "continued-fourth-page" => 4
            case "repeated-cursor" | "duplicate-reference" => 2
            case _ => 1
          })
          fixture.reads shouldBe empty
        }
      }

      Vector("root-id", "root-kind", "root-parent", "missing-manifest", "missing-assets", "missing-derived",
        "manifest-kind", "manifest-mime", "manifest-parent", "manifest-size", "stat-id", "stat-name",
        "stat-size", "assets-kind", "assets-parent", "original-missing", "original-kind", "original-mime",
        "original-parent", "derived-mime").foreach { defect =>
        s"reject $defect without substituting another physical file" in {
          Given("one missing or inconsistent required package observation")
          val fixture = new Store()
          val children = rootChildren(manifestJson())
          val rootchange = defect match {
            case "root-id" => fixture.rootmetadata.copy(reference = assetsRef)
            case "root-kind" => fixture.rootmetadata.copy(kind = FileStoreKind.File)
            case "root-parent" => fixture.rootmetadata.copy(parents = Vector.empty)
            case _ => fixture.rootmetadata
          }
          fixture.rootmetadata = rootchange
          fixture.rootpages = Vector(FileStoreChildPage(defect match {
            case "missing-manifest" => children.filterNot(_.name == "material.json")
            case "missing-assets" => children.filterNot(_.name == "assets")
            case "missing-derived" => children.filterNot(_.name == "derived")
            case "manifest-kind" => children.map(value => if (value.name == "material.json") value.copy(kind = FileStoreKind.Collection) else value)
            case "manifest-mime" => children.map(value => if (value.name == "material.json") value.copy(mediaType = MimeType("text/plain")) else value)
            case "manifest-parent" => children.map(value => if (value.name == "material.json") value.copy(parents = Vector.empty) else value)
            case "manifest-size" => children.map(value => if (value.name == "material.json") value.copy(byteLength = Some(65537L)) else value)
            case "assets-kind" => children.map(value => if (value.name == "assets") value.copy(kind = FileStoreKind.File) else value)
            case "assets-parent" => children.map(value => if (value.name == "assets") value.copy(parents = Vector.empty) else value)
            case _ => children
          }, None))
          fixture.manifestmetadata = defect match {
            case "stat-id" => fixture.manifestmetadata.copy(reference = assetsRef)
            case "stat-name" => fixture.manifestmetadata.copy(name = "another.json")
            case "stat-size" => fixture.manifestmetadata.copy(byteLength = Some(fixture.manifestmetadata.byteLength.get + 1))
            case _ => fixture.manifestmetadata
          }
          fixture.assetpages = Vector(FileStoreChildPage(defect match {
            case "original-missing" => originalChildren.tail
            case "original-kind" => originalChildren.updated(0, originalChildren.head.copy(kind = FileStoreKind.Collection))
            case "original-mime" => originalChildren.updated(0, originalChildren.head.copy(mediaType = MimeType("text/html")))
            case "original-parent" => originalChildren.updated(0, originalChildren.head.copy(parents = Vector(rootRef)))
            case _ => originalChildren
          }, None))
          if (defect == "derived-mime") fixture.derivedpages = Vector(FileStoreChildPage(
            derivedChildren.map(_.copy(mediaType = MimeType("text/plain"))), None))
          When("the admitted root and exact manifest resources are interpreted")
          val result = evaluate(fixture)
          Then("the fixed integrity failure retains no replacement file or asset media read")
          _integrity_invalid(result)
          fixture.reads.forall(_ == manifestRef) shouldBe true
        }
      }

      "preserve native listing failure Conclusions without retry or reclassification" in {
        Given("a native provider denial on the first child listing")
        val fixture = new Store()
        val native = Consequence.securityPermissionDenied[FileStoreChildPage]("Native provider denied this listing")
        fixture.listfailure = Some(native)
        When("the exact configured root reaches the native listing boundary")
        val result = evaluate(fixture)
        Then("the original Conclusion survives unchanged after one stat and one list")
        failure(result) should be theSameInstanceAs failure(native)
        fixture.stats shouldBe Vector(rootRef)
        fixture.listings shouldBe Vector((rootRef, 50, None))
        fixture.reads shouldBe empty
      }
    }

    "admit bounded strictly decoded manifest bytes" which {
      Vector("valid", "read-failure", "close-failure", "invalid-utf8", "invalid-json", "oversized").foreach { outcome =>
        s"close the single manifest stream exactly once after $outcome" in {
          Given("a tracked materialized Bag whose stream records its byte bound and close lifetime")
          val bytes = outcome match {
            case "invalid-utf8" => Vector[Byte](-61, 40)
            case "invalid-json" => "{private-value-marker".getBytes(StandardCharsets.UTF_8).toVector
            case "oversized" => Vector.fill[Byte](65537)(32)
            case _ => manifestJson().noSpaces.getBytes(StandardCharsets.UTF_8).toVector
          }
          val bag = new TrackedBag(bytes, failRead = outcome == "read-failure", failClose = outcome == "close-failure")
          val fixture = new Store()
          fixture.rootpages = Vector(FileStoreChildPage(rootChildren(manifestJson()).map(_.copy(byteLength = None)), None))
          fixture.manifestmetadata = fixture.manifestmetadata.copy(byteLength = None)
          fixture.content = FileStoreContent(MimeType("application/json"), bag)
          When("the reader opens and consumes the bounded manifest Bag")
          val result = evaluate(fixture)
          Then("one readNBytes call requests 65537 bytes and the opened stream always closes once")
          bag.opens shouldBe 1
          bag.bounds shouldBe Vector(65537)
          bag.closes shouldBe 1
          if (outcome == "valid") result.TAKE.title shouldBe title else _integrity_invalid(result)
        }
      }

      Vector("invalid-utf8", "duplicate-key", "escaped-duplicate-key", "invalid-json", "too-many-bytes",
        "listed-length", "bag-length", "bag-mime", "url-bag", "unreadable-bag").foreach { defect =>
        s"contain $defect as a redacted package integrity failure" in {
          Given("a selected manifest with malformed content or an inconsistent length observation")
          val fixture = new Store()
          val text = manifestJson().noSpaces
          val bytes = defect match {
            case "invalid-utf8" => Array[Byte](-61, 40)
            case "duplicate-key" => text.replace("\"title\":", "\"title\":\"private-value-marker\",\"title\":").getBytes(StandardCharsets.UTF_8)
            case "escaped-duplicate-key" => text.replace("\"title\":", "\"\\u0074itle\":\"private-value-marker\",\"title\":").getBytes(StandardCharsets.UTF_8)
            case "invalid-json" => "{private-value-marker".getBytes(StandardCharsets.UTF_8)
            case "too-many-bytes" => Array.fill[Byte](65537)(32)
            case _ => text.getBytes(StandardCharsets.UTF_8)
          }
          val declared = if (defect == "listed-length") bytes.length.toLong + 1 else bytes.length.toLong.min(65536)
          fixture.rootpages = Vector(FileStoreChildPage(rootChildren(manifestJson()).map(value =>
            if (value.name == "material.json") value.copy(byteLength = Some(declared)) else value), None))
          fixture.manifestmetadata = fixture.manifestmetadata.copy(byteLength = Some(declared))
          val bag = defect match {
            case "url-bag" => Bag.Instance(BagBackend.Url(java.net.URI.create("https://example.invalid/private-value-marker").toURL))
            case "unreadable-bag" => Bag.Instance(BagBackend.File(Paths.get("target/material-package-read/nonexistent-private-value-marker")))
            case _ => Bag.Instance(BagBackend.InMemory(bytes), BagMetadata(size =
              if (defect == "bag-length") Some(bytes.length.toLong + 1) else None))
          }
          fixture.content = FileStoreContent(MimeType(if (defect == "bag-mime") "text/plain" else "application/json"), bag)
          When("the reader bounds bytes closes its stream and applies strict UTF8 and duplicate-key JSON admission")
          val result = evaluate(fixture)
          Then("exceptions and malformed data expose only the fixed structured package failure")
          _integrity_invalid(result)
          fixture.reads shouldBe Vector(manifestRef)
          fixture.listings.map(_._1) shouldBe Vector(rootRef)
        }
      }

      "accept a valid manifest at the exact sixty-four-kibibyte byte boundary" in {
        Given("valid manifest JSON padded with legal trailing whitespace to exactly 65536 bytes")
        val fixture = new Store()
        val text = manifestJson().noSpaces
        val bytes = (text + (" " * (65536 - text.getBytes(StandardCharsets.UTF_8).length))).getBytes(StandardCharsets.UTF_8)
        fixture.rootpages = Vector(FileStoreChildPage(rootChildren(manifestJson()).map(value =>
          if (value.name == "material.json") value.copy(byteLength = Some(65536L)) else value), None))
        fixture.manifestmetadata = fixture.manifestmetadata.copy(byteLength = Some(65536L))
        fixture.content = FileStoreContent(MimeType("application/json"), Bag.binary(bytes))
        When("the reader applies its actual-byte bound")
        val result = evaluate(fixture)
        Then("the inclusive byte limit retains the valid package")
        result.TAKE.title shouldBe title
      }
    }

    "interpret the exact existing package manifest shape" which {
      "reject malformed domain metadata asset shapes and derived lineage" in {
        Given("the existing plan-shaped manifest and invalid exact-field variants")
        val valid = manifestJson()
        val assets = valid.hcursor.get[Vector[Json]]("assets").toOption.get
        val original = assets.head
        val derived = assets.last
        val invalid = Vector(
          replace(valid, "schema", Json.fromString("another-schema")), replace(valid, "id", Json.fromString(reference + "-other")),
          replace(valid, "title", Json.fromString("")), replace(valid, "title", Json.fromString("x" * 4097)),
          replace(valid, "createdAt", Json.fromString("not-an-instant")), replace(valid, "provenance", Json.arr()),
          replace(valid, "owner", Json.fromString("bad\nowner")), replace(valid, "createdBy", Json.Null),
          replace(valid, "classifications", Json.arr(Json.fromString("x" * 257))),
          replace(valid, "classifications", Json.fromValues(Vector.fill(129)(Json.fromString("class")))),
          replace(valid, "extra", Json.True), replace(valid, "assets", Json.arr()),
          replace(valid, "assets", Json.fromValues(Vector.fill(129)(original))),
          replace(valid, "assets", Json.arr(original, original)),
          replace(valid, "assets", Json.arr(replace(original, "role", Json.fromString("bad\rrole")))),
          replace(valid, "assets", Json.arr(replace(original, "mediaType", Json.fromString("image/png; charset=UTF-8")))),
          replace(valid, "assets", Json.arr(replace(original, "unknown", Json.True))),
          replace(valid, "assets", Json.arr(original, replace(derived, "derivedFrom", Json.arr()))),
          replace(valid, "assets", Json.arr(original, replace(derived, "derivedFrom", Json.arr(Json.fromString("assets/missing"))))),
          replace(valid, "assets", Json.arr(original, replace(derived, "derivedFrom",
            Json.arr(Json.fromString("assets/image-1"), Json.fromString("assets/image-1"))))),
          replace(valid, "assets", Json.arr(original, replace(derived, "mediaType", Json.fromString("text/plain")))),
          replace(valid, "assets", Json.arr(original, replace(derived, "path", Json.fromString("derived/image-1.json")))),
          replace(valid, "assets", Json.arr(derived)))
        When("each manifest is interpreted through the same selected native capability")
        val results = invalid.map(document => evaluate(new Store(document)))
        Then("malformed fields duplicates absent originals and invalid lineage never become logical metadata")
        results.foreach(_integrity_invalid)
      }

      "reject generated traversal URI escape and nested paths without normalization" in {
        Given("bounded generated resource IDs combined with forbidden path syntax")
        val samples = for {
          id <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.take(16).mkString)
          path <- Gen.oneOf(s"assets/../$id", s"assets/a/$id", s"assets/%2e%2e/$id", s"assets\\$id",
            s"https://example.invalid/$id", s"/assets/$id", s"derived/$id", s"assets/$id?query")
        } yield path
        When("one hundred generated unsafe paths are submitted as literal manifest entries")
        val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100),
          Prop.forAll(samples) { path =>
            val valid = manifestJson()
            val original = valid.hcursor.get[Vector[Json]]("assets").toOption.get.head
            val changed = replace(valid, "assets", Json.arr(replace(original, "path", Json.fromString(path))))
            val result = evaluate(new Store(changed))
            result.toOption.isEmpty && failure(result).displayMessage == integrityMessage
          })
        Then("every unsafe literal is rejected as a package integrity failure")
        checked.passed shouldBe true
        checked.succeeded should be >= 100
        checked.discarded shouldBe 0
      }

      "permit a package without derived entries without requiring or scanning a derived collection" in {
        Given("a manifest containing originals only and no derived collection")
        val manifest = manifestJson(includeDerived = false)
        val fixture = new Store(manifest)
        fixture.rootpages = Vector(FileStoreChildPage(rootChildren(manifest).filterNot(_.name == "derived"), None))
        When("the original-only package is resolved")
        val snapshot = evaluate(fixture).TAKE
        Then("only root and assets are enumerated and both originals remain ordered")
        snapshot.originals.map(_.resourceid) shouldBe Vector("image-1", "audio-1")
        fixture.listings.map(_._1) shouldBe Vector(rootRef, assetsRef)
      }
    }
  }

  private def _configuration_invalid(result: Consequence[?]): Unit = {
    failure(result).observation.taxonomy shouldBe Taxonomy(Taxonomy.Category.Configuration, Taxonomy.Symptom.Invalid)
    failure(result).displayMessage shouldBe "Material package read configuration is invalid"
    (privateMarkers :+ "private-value-marker").foreach(marker => result.display should not include marker)
  }

  private def _integrity_invalid(result: Consequence[?]): Unit = {
    failure(result).observation.taxonomy shouldBe Taxonomy.resourceInvalid
    failure(result).displayMessage shouldBe integrityMessage
    (privateMarkers :+ "private-value-marker").foreach(marker => result.display should not include marker)
  }
}

object MaterialPackageReadSpec {
  val subject = MaterialReferenceFixture.owner
  val reference = "urn:textus:klake:fixture-lake:material:2026/package-1"
  val title = "Stored evidence Ω"
  val storeName = "fixture-package-store"
  val rootKey = "PrivatePackageRoot_1"
  val parentKey = "PrivatePackageParent_1"
  val rootRef = FileStoreRef(storeName, rootKey)
  val parentRef = FileStoreRef(storeName, parentKey)
  val manifestRef = FileStoreRef(storeName, "PrivateManifest_1")
  val assetsRef = FileStoreRef(storeName, "PrivateAssets_1")
  val derivedRef = FileStoreRef(storeName, "PrivateDerived_1")
  val imageRef = FileStoreRef(storeName, "PrivateImage_1")
  val audioRef = FileStoreRef(storeName, "PrivateAudio_1")
  val derivedFileRef = FileStoreRef(storeName, "PrivateDocument_1")
  val integrityMessage = "Stored Material package is incomplete or ambiguous"
  val privateMarkers = MaterialReferenceFixture.privateMarkers ++ Vector(storeName, rootKey, parentKey,
    manifestRef.key, assetsRef.key, derivedRef.key, imageRef.key, audioRef.key, derivedFileRef.key,
    "remote-owner-private", "remote-creator-private", "remote-provenance-private")

  def bindingJson(reference: String = MaterialPackageReadSpec.reference, rootKey: String = MaterialPackageReadSpec.rootKey,
    subjects: Vector[String] = Vector(subject)): Json = Json.obj(
    "materialRef" -> Json.fromString(reference), "authorizedSubjects" -> Json.fromValues(subjects.map(Json.fromString)),
    "storage" -> Json.obj("provider" -> Json.fromString("google-drive"),
      "connectionId" -> Json.fromString(MaterialReferenceFixture.connectionId), "storeName" -> Json.fromString(storeName),
      "rootKey" -> Json.fromString(rootKey), "parentKey" -> Json.fromString(parentKey)))

  def catalogJson(entries: Vector[Json] = Vector(bindingJson())): Json = Json.obj(
    "schema" -> Json.fromString("textus.knowledge-lake.material-packages/1"), "materials" -> Json.fromValues(entries))

  def manifestJson(reference: String = MaterialPackageReadSpec.reference, title: String = MaterialPackageReadSpec.title,
    includeDerived: Boolean = true): Json = {
    val originals = Vector(Json.obj("path" -> Json.fromString("assets/image-1"), "role" -> Json.fromString("cover"),
      "mediaType" -> Json.fromString("image/png")), Json.obj("path" -> Json.fromString("assets/audio-1"),
      "role" -> Json.fromString("narration"), "mediaType" -> Json.fromString("audio/ogg")))
    val derived = Option.when(includeDerived)(Json.obj("path" -> Json.fromString("derived/capture.json"),
      "role" -> Json.fromString("capture"), "mediaType" -> Json.fromString("application/json"),
      "derivedFrom" -> Json.arr(Json.fromString("assets/image-1")))).toVector
    Json.obj("schema" -> Json.fromString("textus.material/1"), "id" -> Json.fromString(reference),
      "title" -> Json.fromString(title), "createdAt" -> Json.fromString("2026-10-25T00:00:00Z"),
      "classifications" -> Json.arr(Json.fromString("evidence")),
      "provenance" -> Json.obj("private" -> Json.fromString("remote-provenance-private")),
      "owner" -> Json.fromString("remote-owner-private"), "createdBy" -> Json.fromString("remote-creator-private"),
      "assets" -> Json.fromValues(originals ++ derived))
  }

  def metadata(reference: FileStoreRef, label: String, parent: FileStoreRef,
    mediaType: String = "application/json", byteLength: Option[Long] = None, collection: Boolean = false): FileStoreMetadata =
    FileStoreMetadata(reference, label, MimeType(if (collection) "application/vnd.google-apps.folder" else mediaType),
      byteLength, Vector(parent), if (collection) FileStoreKind.Collection else FileStoreKind.File)

  def rootChildren(manifest: Json): Vector[FileStoreMetadata] = Vector(
    metadata(manifestRef, "material.json", rootRef, byteLength = Some(manifest.noSpaces.getBytes(StandardCharsets.UTF_8).length.toLong)),
    metadata(assetsRef, "assets", rootRef, collection = true), metadata(derivedRef, "derived", rootRef, collection = true))

  def originalChildren: Vector[FileStoreMetadata] = Vector(
    metadata(imageRef, "image-1", assetsRef, "image/png", Some(4L)),
    metadata(audioRef, "audio-1", assetsRef, "audio/ogg"))

  def derivedChildren: Vector[FileStoreMetadata] = Vector(metadata(derivedFileRef, "capture.json", derivedRef))

  def continuedPages(children: Vector[FileStoreMetadata]): Vector[FileStoreChildPage] = Vector(
    FileStoreChildPage(Vector.empty, Some("next-1")), FileStoreChildPage(children.take(1), Some("next-2")),
    FileStoreChildPage(Vector.empty, Some("next-3")), FileStoreChildPage(children.drop(1), None))

  def collectionRef(collection: String): FileStoreRef = collection match {
    case "root" => rootRef
    case "assets" => assetsRef
    case "derived" => derivedRef
  }

  private[material] def evaluate(store: Store): Consequence[MaterialPackageReader.Snapshot] = {
    val binding = MaterialPackageReadCatalog.parseC(catalogJson().noSpaces).TAKE.resolve(subject, reference).TAKE
    new UnitOfWorkInterpreter(new UnitOfWork(ExecutionContext.create())).run(MaterialPackageReader._read(binding, store))
  }

  final class TrackedBag(bytes: Vector[Byte], failRead: Boolean = false, failClose: Boolean = false) extends BinaryBag {
    val bag: Bag = Bag.Instance(BagBackend.InMemory(bytes.toArray))
    private var _opens = 0
    private var _closes = 0
    private var _bounds = Vector.empty[Int]
    def opens: Int = _opens
    def closes: Int = _closes
    def bounds: Vector[Int] = _bounds
    override def openInputStream(): InputStream = {
      _opens += 1
      new ByteArrayInputStream(bytes.toArray) {
        override def readNBytes(len: Int): Array[Byte] = {
          _bounds = _bounds :+ len
          if (failRead) throw new IOException("private-value-marker") else super.readNBytes(len)
        }
        override def close(): Unit = {
          _closes += 1
          if (failClose) throw new IOException("private-value-marker") else super.close()
        }
      }
    }
  }

  /** Controlled capability observations, never an alternate Operation interpreter. */
  final class Store(manifest: Json = manifestJson()) extends FileStoreChildren {
    val name: String = storeName
    var rootmetadata: FileStoreMetadata = metadata(rootRef, "package-1", parentRef, collection = true)
    var manifestmetadata: FileStoreMetadata = rootChildren(manifest).head
    var content: FileStoreContent = FileStoreContent(MimeType("application/json"),
      Bag.binary(manifest.noSpaces.getBytes(StandardCharsets.UTF_8)))
    var rootpages: Vector[FileStoreChildPage] = Vector(FileStoreChildPage(rootChildren(manifest), None))
    var assetpages: Vector[FileStoreChildPage] = Vector(FileStoreChildPage(originalChildren, None))
    var derivedpages: Vector[FileStoreChildPage] = Vector(FileStoreChildPage(derivedChildren, None))
    var listfailure: Option[Consequence[FileStoreChildPage]] = None
    private var _stats = Vector.empty[FileStoreRef]
    private var _reads = Vector.empty[FileStoreRef]
    private var _listings = Vector.empty[(FileStoreRef, Int, Option[String])]
    def stats: Vector[FileStoreRef] = _stats
    def reads: Vector[FileStoreRef] = _reads
    def listings: Vector[(FileStoreRef, Int, Option[String])] = _listings

    def setPages(collection: String, pages: Vector[FileStoreChildPage]): Unit = collection match {
      case "root" => rootpages = pages
      case "assets" => assetpages = pages
      case "derived" => derivedpages = pages
    }
    def stat(reference: FileStoreRef): ExecUowM[FileStoreMetadata] = {
      _stats = _stats :+ reference
      _pure(if (reference == rootRef) Consequence.success(rootmetadata)
        else if (reference == manifestRef) Consequence.success(manifestmetadata)
        else Consequence.resourceInvalid("Unexpected controlled stat"))
    }
    def read(reference: FileStoreRef): ExecUowM[FileStoreContent] = {
      _reads = _reads :+ reference
      _pure(if (reference == manifestRef) Consequence.success(content)
        else Consequence.resourceInvalid("Asset bodies must not be fetched for package metadata"))
    }
    def listChildren(parent: FileStoreRef, limit: Int, pageToken: Option[String] = None): ExecUowM[FileStoreChildPage] = {
      val index = _listings.count(_._1 == parent)
      _listings = _listings :+ ((parent, limit, pageToken))
      val pages = if (parent == rootRef) rootpages else if (parent == assetsRef) assetpages
        else if (parent == derivedRef) derivedpages else Vector.empty
      _pure(listfailure.getOrElse(pages.lift(index).fold[Consequence[FileStoreChildPage]](
        Consequence.resourceInvalid("Unexpected controlled listing"))(Consequence.success)))
    }
    def createCollection(parent: FileStoreRef, name: String): ExecUowM[FileStoreMetadata] =
      _pure(Consequence.resourceInvalid("Readonly package resolution must not create collections"))
    def create(parent: FileStoreRef, name: String, mediaType: MimeType, content: Bag): ExecUowM[FileStoreMetadata] =
      _pure(Consequence.resourceInvalid("Readonly package resolution must not create files"))
    private def _pure[A](value: Consequence[A]): ExecUowM[A] = ConsequenceT.fromConsequence(value)
  }

  def metadataJson(value: FileStoreMetadata): Json = Json.obj(
    "id" -> Json.fromString(value.reference.key), "name" -> Json.fromString(value.name),
    "mimeType" -> Json.fromString(value.mediaType.value), "parents" -> Json.fromValues(value.parents.map(parent => Json.fromString(parent.key))),
    "trashed" -> Json.False).mapObject(fields => value.byteLength.fold(fields)(size => fields.add("size", Json.fromString(size.toString))))

  def childrenJson(children: Vector[FileStoreMetadata], nextPageToken: Option[String] = None): Json =
    Json.obj("files" -> Json.fromValues(children.map(metadataJson))).mapObject(fields =>
      nextPageToken.fold(fields)(token => fields.add("nextPageToken", Json.fromString(token))))

  def packageResponses(manifest: Json = manifestJson(), selectedRoot: FileStoreRef = rootRef): Vector[HttpResponse] = {
    val children = rootChildren(manifest).map(value => value.copy(parents = Vector(selectedRoot)))
    Vector(MaterialReferenceFixture.jsonResponse(metadataJson(metadata(selectedRoot, "package-1", parentRef, collection = true))),
      MaterialReferenceFixture.jsonResponse(childrenJson(children)), MaterialReferenceFixture.jsonResponse(metadataJson(children.head)),
      MaterialReferenceFixture.mediaResponse("application/json", manifest.noSpaces.getBytes(StandardCharsets.UTF_8).toVector),
      MaterialReferenceFixture.jsonResponse(childrenJson(originalChildren))) ++
      Option.when(manifest.hcursor.get[Vector[Json]]("assets").toOption.get.exists(_.hcursor.downField("derivedFrom").succeeded))(
        MaterialReferenceFixture.jsonResponse(childrenJson(derivedChildren))).toVector
  }
}
