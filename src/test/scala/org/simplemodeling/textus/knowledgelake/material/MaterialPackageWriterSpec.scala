package org.simplemodeling.textus.knowledgelake.material

import java.io.{ByteArrayInputStream, IOException, InputStream}
import java.net.{URI, URL, URLConnection, URLStreamHandler}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.atomic.AtomicInteger
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}
import cats.syntax.all.*
import io.circe.{Json, Printer, parser}
import org.goldenport.{Consequence, ConsequenceT}
import org.goldenport.bag.{Bag, BagBackend, BagMetadata}
import org.goldenport.cncf.bootstrap.{BootstrapConfig, CncfBootstrap}
import org.goldenport.cncf.component.{Component, ComponentId, ComponentInstanceId}
import org.goldenport.cncf.context.{ExecutionContext, GlobalContext, GlobalRuntimeContext, PrincipalId}
import org.goldenport.cncf.filestore.{FileStore, FileStoreContent, FileStoreKind, FileStoreMetadata, FileStoreRef, GoogleDriveCredentials, GoogleDriveFileStore}
import org.goldenport.cncf.http.HttpDriver
import org.goldenport.cncf.log.LogBackendHolder
import org.goldenport.cncf.observability.ConclusionDiagnostics
import org.goldenport.cncf.unitofwork.{ExecProgram, ExecUowM, UnitOfWork, UnitOfWorkInterpreter, UnitOfWorkOp}
import org.goldenport.datatype.{ContentType, MimeType}
import org.goldenport.http.{HttpResponse, HttpStatus}
import org.goldenport.observation.Taxonomy
import org.goldenport.protocol.{Property, Protocol}
import org.goldenport.record.Record
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * Actual native Drive FileStore programs against counted test-owned physical files.
 * This synthetic evidence establishes neither cloud access nor logical registration.
 *
 * @since   Oct. 21, 2026
 * @version Oct. 21, 2026
 */
final class MaterialPackageWriterSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import MaterialContributionSpec.{contributionJson, creator, failure, owner, replace, take}
  import MaterialPackageWriterSpec.*

  "TKL physical package writer" should {
    "create complete packages through the native interpreter" which {
      "construct a pure program and store a media-free contribution with material.json last" in {
        Given("an admitted media-free contribution, explicit destination mapping and a test-owned physical peer")
        val raw = contributionJson(rich = false)
        val contribution = take(MaterialContribution.parseC(raw))
        _with_environment { environment =>
          _with_harness(environment) { fixture =>
            val source = fixture.sources(contribution, Vector.empty)
            val writer = fixture.writer()
            When("the writer program is constructed and subsequently interpreted with the native TKL Component grant")
            val program = writer.materialize(contribution, _location, owner, creator, source)
            val before = (fixture.peer.events, fixture.destination.calls, source.calls, fixture.acquisitions)
            val stored = take(fixture.evaluate(program))
            fixture.peer.stop()
            val snapshot = fixture.snapshot(stored)
            Then("construction has no provider effects and physical readback contains complete JSON without fabricated originals")
            before shouldBe ((Vector.empty, Vector.empty, Vector.empty, 0))
            source.calls shouldBe empty
            fixture.peer.events shouldBe _media_free_events
            snapshot.assetsexists shouldBe false
            snapshot.manifest.hcursor.get[String]("schema").toOption shouldBe Some("textus.material/1")
            snapshot.manifest.hcursor.get[String]("id").toOption shouldBe Some(_location.urn)
            snapshot.manifest shouldBe stored.plan.manifest
            snapshot.documents shouldBe contribution.documents.map(_.body)
            snapshot.originals shouldBe empty
            stored._files.map(_.path) shouldBe Vector("derived/capture.json", "derived/review.json")
            stored._files.map(_._parent).distinct should have size 1
            stored._files.foreach(file => fixture.peer.disk(file._parent).getFileName.toString shouldBe "derived")
            fixture.peer.authorizationMatches shouldBe true
            fixture.peer.requests.map(_.method).toSet shouldBe Set("POST", "PUT")
            snapshot.filesizes shouldBe fixture.peer.puts.map(_.metadata.size.get)
            fixture.peer.initiatedSizes shouldBe snapshot.filesizes
            stored.toString shouldBe "StoredMaterialPackage(<confirmed physical package>)"
          }
        }
      }

      "store image and audio octets before complete derived documents and literal provenance" in {
        Given("a rich neutral contribution retaining image selection, audio history and producer provenance")
        val contribution = take(MaterialContribution.parseC(contributionJson()))
        _with_environment { environment =>
          _with_harness(environment) { fixture =>
            val source = fixture.sources(contribution, _payloads)
            When("the real GoogleDriveFileStore and UnitOfWorkInterpreter create the package")
            val stored = take(fixture.evaluate(fixture.writer().materialize(contribution, _location, owner, creator, source)))
            fixture.peer.stop()
            val snapshot = fixture.snapshot(stored)
            val paths = snapshot.manifest.hcursor.get[Vector[Json]]("assets").toOption.get
              .map(_.hcursor.get[String]("path").toOption.get)
            val borrowed = _payloads.indices.map(index => fixture.peer.served(s"source_$index")).toVector
            Then("independently read files preserve exact full JSON and original binary bytes in the TKL-owned order")
            snapshot.originals shouldBe _payloads
            snapshot.documents shouldBe contribution.documents.map(_.body)
            snapshot.manifest shouldBe stored.plan.manifest
            snapshot.manifest.hcursor.downField("provenance").focus shouldBe Some(contribution.provenance)
            snapshot.manifest.hcursor.get[String]("title").toOption shouldBe Some(contribution.title)
            snapshot.manifest.hcursor.get[String]("createdAt").toOption shouldBe Some(contribution.createdAt)
            snapshot.manifest.hcursor.get[Vector[String]]("classifications").toOption shouldBe Some(contribution.classifications)
            snapshot.manifest.hcursor.get[String]("owner").toOption shouldBe Some(owner.value)
            snapshot.manifest.hcursor.get[String]("createdBy").toOption shouldBe Some(creator.value)
            snapshot.manifest.hcursor.downField("sourceMaterialEntityId").focus shouldBe None
            paths shouldBe Vector("assets/image-1", "assets/image-2", "assets/audio-1", "derived/capture.json", "derived/review.json")
            stored._files.map(_.path) shouldBe paths
            stored._files.take(3).map(_._parent).distinct should have size 1
            stored._files.drop(3).map(_._parent).distinct should have size 1
            stored._files.head._parent should not be stored._files.last._parent
            stored._files.take(3).foreach(file => fixture.peer.disk(file._parent).getFileName.toString shouldBe "assets")
            stored._files.drop(3).foreach(file => fixture.peer.disk(file._parent).getFileName.toString shouldBe "derived")
            source.calls shouldBe contribution.originals
            fixture.peer.events shouldBe _rich_events
            borrowed.zip(fixture.peer.puts.take(3)).foreach { case (original, stored) => stored should be theSameInstanceAs original }
            fixture.peer.initiatedSizes shouldBe snapshot.filesizes
            snapshot.filesizes shouldBe fixture.peer.puts.map(_.metadata.size.get)
            fixture.peer.requests.filter(_.path.contains("alt=media")).foreach(request =>
              request.properties.head shouldBe Property("http.response-body-mode", "binary", None))
            (stored._files.map(_._reference) ++ Vector(stored._root, stored._manifest, _parent)).distinct.size shouldBe 8
            stored.plan.contribution should be theSameInstanceAs contribution
          }
        }
      }

      "store an empty minimum contribution with derived collection but no invented resource" in {
        Given("a valid contribution with no originals and no documents")
        val contribution = take(MaterialContribution.parseC(contributionJson(rich = false, documents = false)))
        _with_environment { environment =>
          _with_harness(environment) { fixture =>
            val source = fixture.sources(contribution, Vector.empty)
            When("the native writer materializes the minimum package")
            val stored = take(fixture.evaluate(fixture.writer().materialize(contribution, _location, owner, creator, source)))
            fixture.peer.stop()
            val snapshot = fixture.snapshot(stored)
            Then("only the root, derived collection and final manifest are created")
            fixture.peer.events shouldBe Vector("collection:item-1", "collection:derived", "init:material.json", "put:material.json")
            snapshot.derivedexists shouldBe true
            snapshot.assetsexists shouldBe false
            snapshot.manifest.hcursor.get[Vector[Json]]("assets").toOption shouldBe Some(Vector.empty)
            stored._files shouldBe empty
            source.calls shouldBe empty
          }
        }
      }

      "keep repeated display labels as fresh packages with separately readable bytes" in {
        Given("two explicitly requested create-only attempts at the same logical location")
        val first = take(MaterialContribution.parseC(contributionJson(rich = false, title = "first")))
        val second = take(MaterialContribution.parseC(contributionJson(rich = false, title = "second")))
        _with_environment { environment =>
          _with_harness(environment) { fixture =>
            val source = fixture.sources(first, Vector.empty)
            val writer = fixture.writer()
            When("the caller independently invokes both attempts")
            val a = take(fixture.evaluate(writer.materialize(first, _location, owner, creator, source)))
            val b = take(fixture.evaluate(writer.materialize(second, _location, owner, creator, source)))
            fixture.peer.stop()
            val sa = fixture.snapshot(a)
            val sb = fixture.snapshot(b)
            Then("fresh native references retain both manifests without discovery or overwrite")
            a._root should not be b._root
            a._manifest should not be b._manifest
            sa.manifest.hcursor.get[String]("title").toOption shouldBe Some("first")
            sb.manifest.hcursor.get[String]("title").toOption shouldBe Some("second")
            fixture.peer.events shouldBe _media_free_events ++ _media_free_events
            fixture.peer.requests.map(_.method).toSet shouldBe Set("POST", "PUT")
          }
        }
      }
    }

    "validate destination and request before provider access" which {
      "reject malformed destination mappings while admitting literal opaque physical keys" in {
        Given("a native provider and unsafe or absent destination configuration")
        _with_environment { environment =>
          _with_harness(environment) { fixture =>
            val store: FileStore = fixture.destination
            val invalid = Vector((null: FileStore, _lake, _prefix, _parent),
              (store, null: String, _prefix, _parent), (store, "", _prefix, _parent),
              (store, " lake", _prefix, _parent), (store, "x" * 129, _prefix, _parent),
              (store, _lake, null: String, _parent), (store, _lake, "2026//10", _parent),
              (store, _lake, "2026/../10", _parent), (store, _lake, "2026/10/", _parent),
              (store, _lake, _prefix, null: FileStoreRef), (store, _lake, _prefix, FileStoreRef("other", "private-parent-marker")),
              (store, _lake, _prefix, FileStoreRef(_store_name, "")),
              (store, _lake, _prefix, FileStoreRef(_store_name, "x" * 4097)),
              (store, _lake, _prefix, FileStoreRef(_store_name, "private\nparent")))
            When("the pure writer factory admits the explicit logical/physical mapping")
            val rejected = invalid.map { case (provider, lake, prefix, parent) => MaterialPackageWriter.createC(provider, lake, prefix, parent) } ++
              Vector(null, "", "bad\nstore").map(name => MaterialPackageWriter.createC(new Destination(store, name), _lake, _prefix, _parent))
            val opaque = take(MaterialPackageWriter.createC(store, _lake, "", FileStoreRef(_store_name, "bucket/資料/parent")))
            Then("invalid configuration returns a fixed argument failure without constructing any provider program")
            rejected.foreach { result =>
              failure(result).observation.taxonomy shouldBe Taxonomy.argumentInvalid
              failure(result).displayMessage shouldBe "Invalid Material package destination"
              failure(result).display should not include "private-parent-marker"
            }
            opaque.toString shouldBe "MaterialPackageWriter(<configured>)"
            fixture.destination.calls shouldBe empty
            fixture.peer.events shouldBe empty
            fixture.acquisitions shouldBe 0
          }
        }
      }

      "reject null inputs and locations outside exactly one child of the configured logical parent" in {
        Given("an admitted contribution and invalid request values for an explicit configured prefix")
        val contribution = take(MaterialContribution.parseC(contributionJson(rich = false)))
        val invalidlocations = Vector(null, take(MaterialLocation.createC("another-lake", "2026/10/item-1")),
          take(MaterialLocation.createC(_lake, "2026/100/item-1")), take(MaterialLocation.createC(_lake, "2026/10/item-1/child")),
          take(MaterialLocation.createC(_lake, "2026/10")), take(MaterialLocation.createC(_lake, "item-1")))
        _with_environment { environment =>
          _with_harness(environment) { fixture =>
            val writer = fixture.writer()
            val source = fixture.sources(contribution, Vector.empty)
            When("the invalid pure request programs are constructed and interpreted")
            val programs = invalidlocations.map(location => writer.materialize(contribution, location, owner, creator, source)) ++
              Vector(writer.materialize(null, _location, owner, creator, source),
                writer.materialize(contribution, _location, null, creator, source),
                writer.materialize(contribution, _location, owner, PrincipalId(" "), source),
                writer.materialize(contribution, _location, owner, creator, null))
            val results = programs.map(program => fixture.evaluate(program))
            Then("all requests fail before a source read, driver acquisition or destination program")
            results.foreach(result => failure(result).displayMessage shouldBe "Invalid Material package request")
            source.calls shouldBe empty
            fixture.destination.calls shouldBe empty
            fixture.peer.events shouldBe empty
            fixture.acquisitions shouldBe 0
          }
        }
      }

      "pass contribution admission failures unchanged before provider access and support an explicit lake-root mapping" in {
        Given("malformed contribution JSON and a separately admitted root-level logical leaf")
        val raw = contributionJson(rich = false)
        val admitted = MaterialContribution.parseC(replace(raw, "documents", Json.Null))
        val contribution = take(MaterialContribution.parseC(raw))
        val rootlocation = take(MaterialLocation.createC(_lake, "root-item"))
        _with_environment { environment =>
          _with_harness(environment) { fixture =>
            val source = fixture.sources(contribution, Vector.empty)
            When("admission feeds the writer, followed by one valid request under an explicitly empty parent")
            val rejected = fixture.evaluate(ConsequenceT.fromConsequence[ExecProgram, MaterialContribution](admitted)
              .flatMap(value => fixture.writer().materialize(value, _location, owner, creator, source)))
            val before = fixture.peer.events
            val stored = take(fixture.evaluate(fixture.writer("").materialize(contribution, rootlocation, owner, creator, source)))
            Then("the original admission Conclusion survives and the lake-root writer selects exactly the requested leaf")
            failure(rejected).isMatch(failure(admitted)) shouldBe true
            before shouldBe empty
            fixture.peer.events.head shouldBe "collection:root-item"
            stored.plan.location should be theSameInstanceAs rootlocation
          }
        }
      }

      "retain native Component HTTP denial without acquiring the physical driver" in {
        Given("a native Component scope with no matching HTTP grant")
        val contribution = take(MaterialContribution.parseC(contributionJson(rich = false)))
        _with_environment { environment =>
          _with_harness(environment) { fixture =>
            val id = ComponentId("org.example.UnadmittedMaterialOwner")
            val component = Component.create(id.name, id, ComponentInstanceId.default(id), Protocol.empty)
            component.withScopeContext(environment.context.scope)
            val selected = environment.context.withScope(component.scopeContext)
            val nativeuow = new UnitOfWork(selected) {
              override def httpDriver: HttpDriver = throw new AssertionError("Denied HTTP must not acquire a driver")
            }
            val source = fixture.sources(contribution, Vector.empty)
            When("the TKL writer reaches the real native HTTP admission boundary")
            val native = new UnitOfWorkInterpreter(nativeuow).interpret(UnitOfWorkOp.HttpPost(
              "https://www.googleapis.com/drive/v3/files?supportsAllDrives=true", Some("{}"), Map.empty))
            val result = fixture.evaluate(fixture.writer().materialize(contribution, _location, owner, creator, source), selected)
            Then("the native policy Conclusion passes through without provider or source access")
            failure(result).isMatch(failure(native)) shouldBe true
            ConclusionDiagnostics.classify(failure(result)).policy shouldBe Some("http.execution.grant")
            fixture.acquisitions shouldBe 0
            fixture.peer.events shouldBe empty
            source.calls shouldBe empty
          }
        }
      }
    }

    "stop unconfirmed attempts without replay" which {
      Vector("reference", "null result", "null Bag", "unknown length", "wrong length", "URL Bag").foreach { scenario =>
        s"reject inconsistent original $scenario before its destination create" in {
          Given("a successful native original read with a changed capability response")
          val contribution = take(MaterialContribution.parseC(contributionJson()))
          val changedraw = replace(contribution.json, "originals", Json.fromValues(
            contribution.json.hcursor.get[Vector[Json]]("originals").toOption.get.zipWithIndex.map { case (original, index) =>
              if (index == 0) replace(original, "sourceReference", Json.obj("private" -> Json.fromString("changed"))) else original
            }))
          val changedreference = take(MaterialContribution.parseC(changedraw)).originals.head
          val opens = new AtomicInteger
          _with_environment { environment =>
            _with_harness(environment) { fixture =>
              val source = fixture.sources(contribution, _payloads, value => scenario match {
                case "reference" => value.copy(reference = changedreference)
                case "null result" => null
                case "null Bag" => value.copy(content = null)
                case "unknown length" => value.copy(content = Bag.Instance(BagBackend.InMemory(Array[Byte](0, -128, -1, 42))))
                case "wrong length" => value.copy(content = Bag.binary(Array[Byte](0)))
                case _ => value.copy(content = _url_bag(opens))
              })
              When("the writer verifies the exact returned original declaration and materialized known-length Bag")
              val result = fixture.evaluate(fixture.writer().materialize(contribution, _location, owner, creator, source))
              Then("the fixed resource failure stops after the read without opening or uploading the rejected Bag")
              failure(result).observation.taxonomy shouldBe Taxonomy.resourceInvalid
              failure(result).displayMessage shouldBe "Original material source is inconsistent"
              fixture.peer.events shouldBe _rich_events.take(4)
              fixture.peer.puts shouldBe empty
              source.calls.size shouldBe 1
              opens.get() shouldBe 0
              result.toOption shouldBe None
            }
          }
        }
      }

      Vector("null metadata", "null reference", "store", "empty key", "large key", "control key", "parent", "null parents", "label", "kind", "MIME", "null MIME", "unknown length", "length").foreach { scenario =>
        s"reject malformed create confirmation $scenario before continuing" in {
          Given("a real native original create followed by a malformed successful confirmation")
          val contribution = take(MaterialContribution.parseC(contributionJson()))
          _with_environment { environment =>
            _with_harness(environment) { fixture =>
              val source = fixture.sources(contribution, _payloads)
              val destination = new Destination(fixture.provider, _store_name, (index, metadata) => if (index != 3) metadata else scenario match {
                case "null metadata" => null
                case "null reference" => metadata.copy(reference = null)
                case "store" => metadata.copy(reference = metadata.reference.copy(store = "foreign-store"))
                case "empty key" => metadata.copy(reference = metadata.reference.copy(key = ""))
                case "large key" => metadata.copy(reference = metadata.reference.copy(key = "x" * 4097))
                case "control key" => metadata.copy(reference = metadata.reference.copy(key = "private\nkey"))
                case "parent" => metadata.copy(parents = Vector(_parent))
                case "null parents" => metadata.copy(parents = null)
                case "label" => metadata.copy(name = "wrong")
                case "kind" => metadata.copy(kind = FileStoreKind.Collection)
                case "MIME" => metadata.copy(mediaType = MimeType("text/plain"))
                case "null MIME" => metadata.copy(mediaType = null)
                case "unknown length" => metadata.copy(byteLength = None)
                case _ => metadata.copy(byteLength = Some(5L))
              })
              val writer = take(MaterialPackageWriter.createC(destination, _lake, _prefix, _parent))
              When("the writer checks the native create's returned metadata")
              val result = fixture.evaluate(writer.materialize(contribution, _location, owner, creator, source))
              Then("the fixed confirmation failure stops before another original or derived write")
              failure(result).observation.taxonomy shouldBe Taxonomy.resourceInvalid
              failure(result).displayMessage shouldBe "Material package storage confirmation is invalid"
              fixture.peer.events shouldBe _rich_events.take(6)
              source.calls.size shouldBe 1
              result.toOption shouldBe None
            }
          }
        }
      }

      "reject reuse of the configured parent or an earlier create reference at every confirmation stage" in {
        Given("all nine collection/file confirmations and both parent/prior-create reuse cases")
        val contribution = take(MaterialContribution.parseC(contributionJson()))
        _with_environment { environment =>
          for (position <- 1 to 9; reuseparent <- Vector(false, true)) {
            _with_harness(environment) { fixture =>
              var first = Option.empty[FileStoreRef]
              val destination = new Destination(fixture.provider, _store_name, (index, metadata) => {
                val previous = first
                if (first.isEmpty) first = Some(metadata.reference)
                if (index == position) metadata.copy(reference = if (reuseparent) _parent else previous.getOrElse(_parent)) else metadata
              })
              val source = fixture.sources(contribution, _payloads)
              val writer = take(MaterialPackageWriter.createC(destination, _lake, _prefix, _parent))
              When(s"confirmation $position reuses a parent or earlier reference")
              val result = fixture.evaluate(writer.materialize(contribution, _location, owner, creator, source))
              Then("no subsequent create, replay or confirmed package follows that exact position")
              failure(result).displayMessage shouldBe "Material package storage confirmation is invalid"
              destination.calls.size shouldBe position
              result.toOption shouldBe None
            }
          }
        }
      }

      "retain each native create-stage and source-read failure with the exact remaining sequence stopped" in {
        Given("every collection, source stat/read, upload initiation and upload completion in the rich package program")
        val contribution = take(MaterialContribution.parseC(contributionJson()))
        _with_environment { environment =>
          _rich_events.foreach { stage =>
            _with_harness(environment, Some(Failure(stage, false))) { fixture =>
              val source = fixture.sources(contribution, _payloads)
              When(s"the native interpreter reaches the failing $stage boundary")
              val result = fixture.evaluate(fixture.writer().materialize(contribution, _location, owner, creator, source))
              Then("the original native transport exception survives and no later request or replay occurs")
              failure(result).getException.get should be theSameInstanceAs fixture.peer.error
              fixture.peer.events shouldBe _rich_events.take(_rich_events.indexOf(stage) + 1)
              result.toOption shouldBe None
            }
          }
        }
      }

      "leave an actually written final manifest unconfirmed after a write-then-error outcome" in {
        Given("a native peer that persists material.json and then loses its successful confirmation")
        val contribution = take(MaterialContribution.parseC(contributionJson()))
        _with_environment { environment =>
          _with_harness(environment, Some(Failure("put:material.json", true))) { fixture =>
            val source = fixture.sources(contribution, _payloads)
            When("the final create writes remote bytes and returns a native transport failure")
            val result = fixture.evaluate(fixture.writer().materialize(contribution, _location, owner, creator, source))
            fixture.peer.stop()
            val directory = fixture.peer.createdRoot
            val manifest = _json(directory.resolve("material.json"))
            val capture = _json(directory.resolve("derived/capture.json"))
            val original = Files.readAllBytes(directory.resolve("assets/image-1")).toVector
            Then("bytes remain independently readable but no StoredMaterialPackage or logical acceptance is returned")
            result.toOption shouldBe None
            failure(result).getException.get should be theSameInstanceAs fixture.peer.error
            fixture.peer.events shouldBe _rich_events
            manifest.hcursor.get[String]("id").toOption shouldBe Some(_location.urn)
            manifest.hcursor.downField("provenance").focus shouldBe Some(contribution.provenance)
            capture shouldBe contribution.documents.head.body
            original shouldBe _payloads.head
          }
        }
      }
    }

    "retain lossless physical content" which {
      "roundtrip NUL, BMP, non-BMP and unpaired UTF16 regression strings" in {
        Given("explicit UTF16 regression strings embedded in metadata, provenance and complete document histories")
        val literals = Vector("\u0000", "雪é", "\ud83d\ude80", "\ud800", "\udfff")
        _with_environment { environment =>
          literals.foreach { literal =>
            val opaque = Json.obj("literal" -> Json.fromString(literal), "array" -> Json.arr(Json.Null, Json.fromString(literal)))
            val contribution = take(MaterialContribution.parseC(contributionJson(opaque = opaque, title = "title" + literal)))
            _with_harness(environment) { fixture =>
              val source = fixture.sources(contribution, _payloads)
              When("escaped standard JSON is stored as known-length UTF8 Bags by native programs")
              val stored = take(fixture.evaluate(fixture.writer().materialize(contribution, _location, owner, creator, source)))
              fixture.peer.stop()
              val snapshot = fixture.snapshot(stored)
              Then("independent file parsing restores exact string code units, JSON trees and binary bytes")
              snapshot.documents shouldBe contribution.documents.map(_.body)
              snapshot.originals shouldBe _payloads
              snapshot.manifest.hcursor.get[String]("title").toOption shouldBe Some("title" + literal)
              snapshot.manifest.hcursor.downField("provenance").focus shouldBe Some(contribution.provenance)
              fixture.peer.events shouldBe _rich_events
            }
          }
        }
      }

      "store and independently read at least one hundred arbitrary UTF16 and nonempty binary samples without discards" in {
        Given("bounded arbitrary UTF16 strings and one-to-thirty-two-octet binary payloads under a real native TKL grant")
        val samples = for {
          size <- Gen.choose(1, 32)
          payload <- Gen.listOfN(size, Gen.choose(-128, 127).map(_.toByte))
          length <- Gen.choose(0, 24)
          units <- Gen.listOfN(length, Gen.choose(0, 65535).map(_.toChar))
        } yield (payload.toVector, units.mkString)
        _with_environment { environment =>
          When("ScalaCheck creates and reads complete physical packages through the native interpreter")
          val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), Prop.forAll(samples) { case (payload, literal) =>
            val opaque = Json.obj("literal" -> Json.fromString(literal), "nested" -> Json.arr(Json.Null, Json.fromString(literal)))
            val contribution = take(MaterialContribution.parseC(contributionJson(byteLength = payload.size.toLong, opaque = opaque, title = "title" + literal)))
            _with_harness(environment) { fixture =>
              val source = fixture.sources(contribution, Vector.fill(3)(payload))
              val stored = take(fixture.evaluate(fixture.writer().materialize(contribution, _location, owner, creator, source)))
              fixture.peer.stop()
              val snapshot = fixture.snapshot(stored)
              snapshot.documents == contribution.documents.map(_.body) && snapshot.originals == Vector.fill(3)(payload) &&
                snapshot.manifest.hcursor.get[String]("title").toOption.contains("title" + literal) &&
                snapshot.manifest.hcursor.downField("provenance").focus.contains(contribution.provenance) &&
                fixture.peer.events == _rich_events
            }
          })
          Then("at least one hundred undiscarded physical roundtrips retain exact JSON/provenance and binary octets")
          checked.passed shouldBe true
          checked.succeeded should be >= 100
          checked.discarded shouldBe 0
        }
      }
    }
  }
}

private object MaterialPackageWriterSpec {
  import MaterialContributionSpec.take
  private val _store_name = "tkl-package-drive"
  private val _lake = "explicit-test-lake"
  private val _prefix = "2026/10"
  private val _token = "synthetic-tkl-package-token"
  private val _component_id = ComponentId("org.simplemodeling.textus.KnowledgeLake")
  private val _parent = FileStoreRef(_store_name, "destination_parent")
  private val _source_parent = FileStoreRef(_store_name, "source_parent")
  private val _location = take(MaterialLocation.createC(_lake, s"$_prefix/item-1"))
  private val _payloads = Vector(Vector[Byte](0, -128, -1, 42), Vector[Byte](1, 0, 2, -128), Vector[Byte](-1, 0, -128, 2))
  private val _folder_mime = "application/vnd.google-apps.folder"
  private val _json_printer = Printer.noSpaces.copy(escapeNonAscii = true)
  private val _media_free_events = Vector("collection:item-1", "collection:derived", "init:capture.json", "put:capture.json",
    "init:review.json", "put:review.json", "init:material.json", "put:material.json")
  private val _rich_events = Vector("collection:item-1", "collection:assets",
    "stat:source_0", "read:source_0", "init:image-1", "put:image-1",
    "stat:source_1", "read:source_1", "init:image-2", "put:image-2",
    "stat:source_2", "read:source_2", "init:audio-1", "put:audio-1", "collection:derived",
    "init:capture.json", "put:capture.json", "init:review.json", "put:review.json", "init:material.json", "put:material.json")

  final case class Environment(root: Path, context: ExecutionContext)
  final case class Failure(stage: String, afterwrite: Boolean)
  final case class Request(method: String, path: String, headers: Map[String, String], properties: Vector[Property]) {
    override def toString: String = "Request(<synthetic private headers>)"
  }
  final case class Entry(id: String, name: String, mime: String, parent: String, disk: Path, kind: FileStoreKind)
  final case class Snapshot(manifest: Json, documents: Vector[Json], originals: Vector[Vector[Byte]],
    assetsexists: Boolean, derivedexists: Boolean, filesizes: Vector[Long])

  final class Cleanup(action: () => Unit) extends AutoCloseable {
    def close(): Unit = action()
  }

  final class Peer(root: Path, failure: Option[Failure]) extends HttpDriver {
    private var _entries = Map(
      _parent.key -> Entry(_parent.key, "destination", _folder_mime, "", Files.createDirectory(root.resolve("destination")), FileStoreKind.Collection),
      _source_parent.key -> Entry(_source_parent.key, "source", _folder_mime, "", Files.createDirectory(root.resolve("source")), FileStoreKind.Collection))
    private var _sessions = Map.empty[String, Json]
    private var _next_id = 0
    private var _events = Vector.empty[String]
    private var _requests = Vector.empty[Request]
    private var _puts = Vector.empty[Bag]
    private var _served = Map.empty[String, Bag]
    private var _stopped = false
    val error = new IOException("Synthetic TKL package transport failure")

    def events: Vector[String] = _events
    def requests: Vector[Request] = _requests
    def puts: Vector[Bag] = _puts
    def served(id: String): Bag = _served(id)
    def disk(reference: FileStoreRef): Path = _entries(reference.key).disk
    def createdRoot: Path = _entries.values.find(_.parent == _parent.key).get.disk
    def stop(): Unit = { _stopped = true }
    def authorizationMatches: Boolean = _requests.forall(_.headers.get("Authorization").contains(s"Bearer $_token"))
    def initiatedSizes: Vector[Long] = _requests.filter(request => request.method == "POST" &&
      request.headers.contains("X-Upload-Content-Length")).map(_.headers("X-Upload-Content-Length").toLong)

    def seed(index: Int, original: MaterialOriginalContribution, payload: Vector[Byte]): FileStoreRef = {
      val id = s"source_$index"
      val path = _entries(_source_parent.key).disk.resolve(id)
      Files.write(path, payload.toArray)
      _entries = _entries.updated(id, Entry(id, "duplicate original display label", original.mediaType, _source_parent.key, path, FileStoreKind.File))
      FileStoreRef(_store_name, id)
    }

    def get(path: String, headers: Map[String, String], properties: Vector[Property]): HttpResponse = {
      val uri = new URI(path)
      val id = uri.getPath.split('/').last
      val media = Option(uri.getQuery).exists(_.split('&').contains("alt=media"))
      _request(Request("GET", path, headers, properties), s"${if (media) "read" else "stat"}:$id") {
        val entry = _entries(id)
        if (media) {
          val body = Bag.binary(Files.readAllBytes(entry.disk))
          _served = _served.updated(id, body)
          HttpResponse.Binary(HttpStatus.Ok, ContentType(MimeType(entry.mime), Some(StandardCharsets.UTF_8)), body)
        } else _response(_metadata(entry))
      }
    }

    def post(path: String, body: Option[String], headers: Map[String, String], properties: Vector[Property]): HttpResponse = {
      val json = parser.parse(body.get).toOption.get
      val label = json.hcursor.get[String]("name").toOption.get
      val folder = json.hcursor.get[String]("mimeType").toOption.contains(_folder_mime)
      _request(Request("POST", path, headers, properties), s"${if (folder) "collection" else "init"}:$label") {
        if (folder) _response(_metadata(_save(json, None)), 201)
        else {
          val location = s"https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&upload_id=session_${_sessions.size}"
          _sessions = _sessions.updated(location, json)
          _response(Json.obj(), headers = Record.data("Location" -> location))
        }
      }
    }

    def put(path: String, body: Option[String], headers: Map[String, String], properties: Vector[Property]): HttpResponse =
      throw new AssertionError("The native TKL package peer requires Bag PUT")

    override def putBag(path: String, body: Option[Bag], headers: Map[String, String], properties: Vector[Property]): HttpResponse = {
      val json = _sessions(path)
      val label = json.hcursor.get[String]("name").toOption.get
      _request(Request("PUT", path, headers, properties), s"put:$label") {
        val content = body.get
        _puts = _puts :+ content
        _response(_metadata(_save(json, Some(content))), 201)
      }
    }

    private def _request(request: Request, event: String)(body: => HttpResponse): HttpResponse = {
      if (_stopped) throw new AssertionError("A stopped TKL physical peer cannot be used")
      _requests = _requests :+ request
      _events = _events :+ event
      if (failure.exists(value => value.stage == event && !value.afterwrite)) throw error
      val response = body
      if (failure.exists(value => value.stage == event && value.afterwrite)) throw error
      response
    }

    private def _save(json: Json, content: Option[Bag]): Entry = {
      _next_id += 1
      val id = s"created_$_next_id"
      val label = json.hcursor.get[String]("name").toOption.get
      val mime = json.hcursor.get[String]("mimeType").toOption.get
      val parent = json.hcursor.get[Vector[String]]("parents").toOption.get.head
      val path = _entries(parent).disk.resolve(if (parent == _parent.key) id else label)
      val kind = if (mime == _folder_mime) FileStoreKind.Collection else FileStoreKind.File
      if (kind == FileStoreKind.Collection) Files.createDirectory(path)
      else {
        val in = content.get.openInputStream()
        try {
          val out = Files.newOutputStream(path)
          try in.transferTo(out) finally out.close()
        } finally in.close()
      }
      val entry = Entry(id, label, mime, parent, path, kind)
      _entries = _entries.updated(id, entry)
      entry
    }

    private def _metadata(entry: Entry): Json = Json.obj(
      "id" -> Json.fromString(entry.id), "name" -> Json.fromString(entry.name), "mimeType" -> Json.fromString(entry.mime),
      "parents" -> Json.arr(Json.fromString(entry.parent)), "trashed" -> Json.False, "version" -> Json.fromString("1"),
      "size" -> Json.fromString(if (entry.kind == FileStoreKind.Collection) "0" else Files.size(entry.disk).toString))
  }

  final class Destination(delegate: FileStore, override val name: String,
    change: (Int, FileStoreMetadata) => FileStoreMetadata = (_, metadata) => metadata) extends FileStore {
    private var _calls = Vector.empty[String]
    private var _confirmations = 0
    def calls: Vector[String] = _calls
    def stat(reference: FileStoreRef): ExecUowM[FileStoreMetadata] = delegate.stat(reference)
    def read(reference: FileStoreRef): ExecUowM[FileStoreContent] = delegate.read(reference)
    def createCollection(parent: FileStoreRef, name: String): ExecUowM[FileStoreMetadata] = {
      _calls = _calls :+ s"collection:$name"
      delegate.createCollection(parent, name).map(_confirm)
    }
    def create(parent: FileStoreRef, name: String, mediaType: MimeType, content: Bag): ExecUowM[FileStoreMetadata] = {
      _calls = _calls :+ s"file:$name"
      delegate.create(parent, name, mediaType, content).map(_confirm)
    }
    private def _confirm(metadata: FileStoreMetadata): FileStoreMetadata = {
      _confirmations += 1
      change(_confirmations, metadata)
    }
  }

  final class Sources(provider: FileStore, bindings: Vector[(MaterialOriginalContribution, FileStoreRef)],
    change: MaterialOriginalContent => MaterialOriginalContent) extends MaterialOriginalSource {
    private var _calls = Vector.empty[MaterialOriginalContribution]
    def calls: Vector[MaterialOriginalContribution] = _calls
    def read(original: MaterialOriginalContribution): ExecUowM[MaterialOriginalContent] = {
      _calls = _calls :+ original
      val reference = bindings.find(_._1 == original).get._2
      for {
        _ <- provider.stat(reference)
        media <- provider.read(reference)
      } yield change(MaterialOriginalContent(original, media.content))
    }
  }

  final class Harness(val peer: Peer, context: ExecutionContext) {
    val provider: GoogleDriveFileStore = take(GoogleDriveFileStore.createC(_store_name,
      take(GoogleDriveCredentials.accessTokenC(_token))))
    val destination = new Destination(provider, _store_name)
    private var _acquisitions = 0
    def acquisitions: Int = _acquisitions
    def writer(prefix: String = _prefix): MaterialPackageWriter =
      take(MaterialPackageWriter.createC(destination, _lake, prefix, _parent))
    def sources(contribution: MaterialContribution, payloads: Vector[Vector[Byte]],
      change: MaterialOriginalContent => MaterialOriginalContent = value => value): Sources = {
      val bindings = contribution.originals.zipWithIndex.map { case (original, index) =>
        original -> peer.seed(index, original, payloads(index))
      }
      new Sources(provider, bindings, change)
    }
    def evaluate[A](program: ExecUowM[A], selected: ExecutionContext = context): Consequence[A] = {
      val uow = new UnitOfWork(selected) {
        override def httpDriver: HttpDriver = { _acquisitions += 1; peer }
      }
      new UnitOfWorkInterpreter(uow).run(program)
    }
    def snapshot(stored: StoredMaterialPackage): Snapshot = {
      val directory = peer.disk(stored._root)
      val paths = stored._files.map(_.path) :+ "material.json"
      Snapshot(_json(directory.resolve("material.json")), stored.plan.documents.map(document => _json(directory.resolve(document.path))),
        stored.plan.originals.map(original => Files.readAllBytes(directory.resolve(original.path)).toVector),
        Files.exists(directory.resolve("assets")), Files.isDirectory(directory.resolve("derived")),
        paths.map(path => Files.size(directory.resolve(path))))
    }
  }

  private def _with_harness[A](environment: Environment, failure: Option[Failure] = None)(body: Harness => A): A = {
    val root = Files.createTempDirectory(environment.root, "physical-peer-")
    try {
      val peer = new Peer(root, failure)
      try body(new Harness(peer, environment.context)) finally peer.stop()
    } finally _remove_work(root)
  }

  private def _with_environment[A](body: Environment => A): A = {
    val previousruntime = GlobalRuntimeContext.current
    val previousglobal = Try(GlobalContext.globalContext).toOption
    val previouslog = LogBackendHolder.backend
    val workroot = Files.createDirectories(Paths.get("target/cncf/work/tkl-material-package").toAbsolutePath.normalize)
    val root = Files.createTempDirectory(workroot, "episode-")
    Using.Manager { use =>
      use(new Cleanup(() => _remove_work(root)))
      use(new Cleanup(() => previouslog.foreach(LogBackendHolder.install)))
      use(new Cleanup(() => previousglobal.foreach(GlobalContext.set)))
      use(new Cleanup(() => GlobalRuntimeContext.current = previousruntime))
      val plan = Json.obj("schema" -> Json.fromString("cncf.runtime-http-execution.v1"),
        "baseUrl" -> Json.fromString("https://www.googleapis.com/"),
        "limits" -> Json.obj("connectTimeoutMillis" -> Json.fromInt(1000), "readTimeoutMillis" -> Json.fromInt(2000), "responseBytes" -> Json.fromInt(1048576)),
        "grants" -> Json.arr(Json.obj("componentId" -> Json.fromString(_component_id.name),
          "origins" -> Json.arr(Json.fromString("https://www.googleapis.com")),
          "methods" -> Json.arr(Json.fromString("GET"), Json.fromString("POST"), Json.fromString("PUT")))))
      Files.writeString(Files.createDirectories(root.resolve(".textus")).resolve("config.json"),
        Json.obj("textus.subsystem.http-execution.plan" -> Json.fromString(plan.noSpaces)).noSpaces, StandardCharsets.UTF_8)
      val descriptor = root.resolve("test.yaml")
      Files.writeString(descriptor, """kind: test-descriptor
        |execution:
        |  profile: controlled
        |  key: tkl-material-package
        |  time:
        |    mode: manual
        |    start-at: '2026-10-21T00:00:00Z'
        |  random:
        |    mode: seeded
        |    seed: tkl-material-package
        |  ids:
        |    mode: deterministic
        |  scheduler:
        |    mode: manual
        |  ordering:
        |    mode: deterministic
        |""".stripMargin, StandardCharsets.UTF_8)
      val handle = take(CncfBootstrap.initialize(BootstrapConfig(cwd = root, args = Array(s"--textus.test.descriptor=$descriptor"))))
      use(new Cleanup(() => handle.close()))
      val component = Component.create(_component_id.name, _component_id, ComponentInstanceId.default(_component_id), Protocol.empty)
      component.withScopeContext(handle.subsystem.globalRuntimeContext)
      body(Environment(root, ExecutionContext.create().withScope(component.scopeContext)))
    }.get
  }

  private def _url_bag(opens: AtomicInteger): Bag = {
    val handler = new URLStreamHandler {
      override def openConnection(url: URL): URLConnection = new URLConnection(url) {
        def connect(): Unit = ()
        override def getInputStream(): InputStream = {
          opens.incrementAndGet()
          new ByteArrayInputStream(Array[Byte](0, -128, -1, 42))
        }
      }
    }
    Bag.Instance(BagBackend.Url(new URL("counted", "source", 0, "/body", handler)), BagMetadata(size = Some(4L)))
  }

  private def _response(json: Json, status: Int = 200, headers: Record = Record.empty): HttpResponse =
    HttpResponse.Text(HttpStatus.fromInt(status).getOrElse(HttpStatus.InternalServerError),
      ContentType(MimeType("application/json"), Some(StandardCharsets.UTF_8)),
      Bag.text(_json_printer.print(json), StandardCharsets.UTF_8), headers)

  private def _json(path: Path): Json = parser.parse(Files.readString(path, StandardCharsets.UTF_8)).toOption.get

  private def _remove_work(root: Path): Unit = Using.resource(Files.walk(root)) { paths =>
    paths.iterator().asScala.toVector.sortBy(_.getNameCount).reverse.foreach(path => Files.delete(path))
  }
}
