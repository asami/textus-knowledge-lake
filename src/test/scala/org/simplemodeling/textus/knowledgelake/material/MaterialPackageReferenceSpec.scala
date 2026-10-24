package org.simplemodeling.textus.knowledgelake.material

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.bag.BinaryBag
import org.goldenport.cncf.action.QueryAction
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentDescriptor, ComponentInstanceId, ComponentInstanceMetadata, ComponentOrigin}
import org.goldenport.cncf.filestore.FileStoreRef
import org.goldenport.http.HttpResponse
import org.goldenport.observation.Taxonomy
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.knowledgelake.KnowledgeLakeComponent
import org.simplemodeling.textus.knowledgelake.impl.KnowledgeLakePrimaryFactory

/** Logical package access through the owning native factory, ingress, ActionCall and UOW.
  * Controlled responses contain no real cloud credentials or SQL mutations.
  * @since Oct. 25, 2026
  * @version Oct. 25, 2026
  */
final class MaterialPackageReferenceSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import MaterialPackageReadSpec.*
  import MaterialReferenceFixture.{Driver, failure, jsonResponse, mediaResponse, record, replace, withSession}

  private val _selection: Vector[(String, Any)] = Vector("materialRef" -> reference, "resourceId" -> "image-1")

  "Native stored-package Material queries" should {
    "resolve the unchanged logical public contract" which {
      "bind all three existing queries to the owning native factory in explicit Packages mode" in {
        Given("the normal owning component initialized with only an explicit trusted package catalog")
        withSession(catalogText = None, referenceMode = "packages", packageCatalogText = Some(catalogJson().noSpaces)) { session =>
          val requests = Vector(session.request("searchMaterials"), session.request("getMaterial", Vector("materialRef" -> reference)),
            session.request("readOriginal", _selection))
          When("the existing generated definitions interpret the logical requests")
          val actions = requests.map(session.component.logic.makeOperationRequest)
          Then("the same public operations use the owning native query factory without bootstrap inference")
          actions.foreach(_.TAKE shouldBe a[QueryAction])
          session.component.operationDefinitions.map(_.name).toSet shouldBe Set("searchMaterials", "getMaterial", "readOriginal",
            "createMaterialRegistration", "getMaterialRegistration", "materializeMaterial")
          session.component._material_reference_mode shouldBe MaterialReferenceRuntime.Mode.Packages
          session.component.materialCatalog shouldBe None
          session.component._material_package_catalog should not be empty
          session.component.factoryOption should not be empty
        }
      }

      "read the real package title and expose only original logical metadata and server-issued provenance" in {
        Given("a trusted package with private remote ownership and provenance and native controlled Drive responses")
        withSession(catalogText = None, referenceMode = "packages", packageCatalogText = Some(catalogJson().noSpaces)) { session =>
          val before = session.custody
          val driver = new Driver(packageResponses())
          When("the provider-admitted owner requests exact Material metadata through ActionCall and native UOW")
          val result = session.execute("getMaterial", Vector("materialRef" -> reference), driver = driver)
          Then("the result projects only the logical contract and fetches no original or derived body")
          val metadata = record(result)
          metadata.fields.map(_.key).toSet shouldBe Set("materialRef", "title", "resources", "provenance")
          metadata.getString("materialRef") shouldBe Some(reference)
          metadata.getString("title") shouldBe Some(title)
          val resources = metadata.getAsC[Vector[Record]]("resources").TAKE.get
          resources.map(_.getString("resourceId").get) shouldBe Vector("image-1", "audio-1")
          resources.map(_.getString("role").get) shouldBe Vector("cover", "narration")
          resources.map(_.getString("mediaType").get) shouldBe Vector("image/png", "audio/ogg")
          resources.head.getAsC[Long]("byteLength").TAKE shouldBe Some(4L)
          resources.last.getAsC[Long]("byteLength").TAKE shouldBe None
          resources.head.fields.map(_.key).toSet shouldBe Set("resourceId", "role", "mediaType", "byteLength")
          resources.last.fields.map(_.key).toSet shouldBe Set("resourceId", "role", "mediaType")
          metadata.getAsC[Vector[Record]]("provenance").TAKE shouldBe Some(Vector(
            Record.data("kind" -> "material-package", "reference" -> reference)))
          privateMarkers.foreach(marker => metadata.print should not include marker)
          driver.calls.map(_.path) shouldBe _package_paths
          driver.calls.map(_.method).distinct shouldBe Vector("GET")
          driver.calls.foreach(_.headers("Authorization") shouldBe s"Bearer ${MaterialReferenceFixture.token}")
          session.custody shouldBe before
        }
      }

      "filter authorization before provider access counts stable ordering and paging" in {
        Given("reverse-ordered visible package bindings and an unauthorized matching package")
        val lastref = "urn:textus:klake:fixture-lake:material:z"
        val firstref = "urn:textus:klake:fixture-lake:material:a"
        val hiddenref = "urn:textus:klake:fixture-lake:material:hidden"
        val lastroot = FileStoreRef(storeName, "PrivateVisibleZ")
        val firstroot = FileStoreRef(storeName, "PrivateVisibleA")
        val catalog = catalogJson(Vector(bindingJson(lastref, lastroot.key),
          bindingJson(hiddenref, "PrivateHiddenRoot", Vector("other-subject")), bindingJson(firstref, firstroot.key)))
        withSession(catalogText = None, referenceMode = "packages", packageCatalogText = Some(catalog.noSpaces)) { session =>
          val responses = packageResponses(manifestJson(lastref, "Match Z"), lastroot) ++
            packageResponses(manifestJson(firstref, "match A"), firstroot)
          val firstdriver = new Driver(responses)
          val seconddriver = new Driver(responses)
          When("the admitted owner searches both case-insensitive pages of the visible package titles")
          val first = record(session.execute("searchMaterials", Vector("text" -> "MATCH", "limit" -> 1), driver = firstdriver))
          val second = record(session.execute("searchMaterials", Vector("text" -> "MATCH", "offset" -> 1, "limit" -> 1), driver = seconddriver))
          Then("only visible roots are resolved sequentially and exact-URN order determines the two pages")
          first.getAsC[Vector[Record]]("items").TAKE.get.map(_.getString("materialRef").get) shouldBe Vector(firstref)
          first.getAsC[Boolean]("hasMore").TAKE shouldBe Some(true)
          second.getAsC[Vector[Record]]("items").TAKE.get.map(_.getString("materialRef").get) shouldBe Vector(lastref)
          second.getAsC[Boolean]("hasMore").TAKE shouldBe Some(false)
          second.getAsC[Int]("offset").TAKE shouldBe Some(1)
          second.getAsC[Int]("limit").TAKE shouldBe Some(1)
          Vector(firstdriver, seconddriver).foreach { driver =>
            driver.calls should have size 12
            driver.calls.head.path shouldBe _stat_path(lastroot)
            driver.calls(6).path shouldBe _stat_path(firstroot)
            driver.calls.foreach(_.path should not include "PrivateHiddenRoot")
          }
          Vector(first, second).foreach { page =>
            page.fields.map(_.key).toSet shouldBe Set("items", "offset", "limit", "hasMore")
            page.getAsC[Vector[Record]]("items").TAKE.get.foreach(_.fields.map(_.key).toSet shouldBe Set("materialRef", "title"))
            page.print should not include hiddenref
          }
        }
      }

      "match literal references and safely return an empty page beyond the integer boundary" in {
        Given("one visible stored package and a maximal valid offset")
        withSession(catalogText = None, referenceMode = "packages", packageCatalogText = Some(catalogJson().noSpaces)) { session =>
          val matched = new Driver(packageResponses())
          val overflow = new Driver(packageResponses())
          When("the owner searches the uppercased reference and requests a later overflowing slice")
          val first = record(session.execute("searchMaterials", Vector("text" -> reference.toUpperCase(java.util.Locale.ROOT)), driver = matched))
          val empty = record(session.execute("searchMaterials", Vector("offset" -> Int.MaxValue, "limit" -> 100), driver = overflow))
          Then("reference matching retains exact returned identity and offset plus limit never wraps")
          first.getAsC[Vector[Record]]("items").TAKE.get.head.getString("materialRef") shouldBe Some(reference)
          empty.getAsC[Vector[Record]]("items").TAKE shouldBe Some(Vector.empty)
          empty.getAsC[Boolean]("hasMore").TAKE shouldBe Some(false)
          empty.getAsC[Int]("offset").TAKE shouldBe Some(Int.MaxValue)
        }
      }
    }

    "transfer exact original octets through the existing native transport" which {
      "reuse the exact binary Bag and every existing private original-response header" in {
        Given("an admitted package and an original containing non-UTF8 bytes")
        withSession(catalogText = None, referenceMode = "packages", packageCatalogText = Some(catalogJson().noSpaces)) { session =>
          val payload = Vector[Byte](0, -1, -128, 42)
          val media = mediaResponse(content = payload)
          val driver = new Driver(packageResponses() ++ Vector(jsonResponse(metadataJson(originalChildren.head)), media))
          val before = session.custody
          When("readOriginal privately resolves the package and reuses the original transport")
          val result = session.execute("readOriginal", _selection, driver = driver)
          val response = _binary(result)
          val returned = _bytes(response.bag.asInstanceOf[BinaryBag])
          Then("full octets and the same Bag survive without text encoding provider pointers or altered headers")
          response shouldBe a[HttpResponse.Binary]
          response.bag should be theSameInstanceAs media.bag
          returned shouldBe payload
          response.mime.value shouldBe "image/png"
          _private_headers(response)
          driver.calls.map(_.path) shouldBe _package_paths ++ Vector(_stat_path(imageRef), _media_path(imageRef))
          driver.calls.map(_.method).distinct shouldBe Vector("GET")
          privateMarkers.foreach(marker => response.header.print should not include marker)
          session.custody shouldBe before
        }
      }

      "preserve arbitrary bounded non-UTF8 original octets across native reads" in {
        Given("one native Packages session and generated four-byte originals containing invalid UTF8 octets")
        val payloads = Gen.listOfN(2, Gen.choose(-128, 127)).map(values => Vector[Byte](-1, -128) ++ values.map(_.toByte))
        withSession(catalogText = None, referenceMode = "packages", packageCatalogText = Some(catalogJson().noSpaces)) { session =>
          When("one hundred generated payloads pass through public readOriginal ActionCalls")
          val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100),
            Prop.forAll(payloads) { payload =>
              val media = mediaResponse(content = payload)
              val driver = new Driver(packageResponses() ++ Vector(jsonResponse(metadataJson(originalChildren.head)), media))
              session.execute("readOriginal", _selection, driver = driver).toOption.exists {
                case OperationResponse.Http(response) => response.bag.isInstanceOf[BinaryBag] &&
                  _bytes(response.bag.asInstanceOf[BinaryBag]) == payload && driver.calls.size == 8
                case _ => false
              }
            })
          Then("every complete original preserves byte equality without discarded samples")
          checked.passed shouldBe true
          checked.succeeded should be >= 100
          checked.discarded shouldBe 0
        }
      }

      Vector("capture", "missing-original").foreach { resource =>
        s"exclude $resource from original selection after admitted package resolution" in {
          Given("a valid package whose derived document and unknown ID are not original resources")
          withSession(catalogText = None, referenceMode = "packages", packageCatalogText = Some(catalogJson().noSpaces)) { session =>
            val driver = new Driver(packageResponses())
            When("readOriginal resolves the package before selecting the supplied logical ID")
            val result = session.execute("readOriginal", Vector("materialRef" -> reference, "resourceId" -> resource), driver = driver)
            Then("the logical selection is not found and no asset or derived body is fetched")
            failure(result).observation.taxonomy shouldBe Taxonomy.resourceNotFound
            driver.calls.map(_.path) shouldBe _package_paths
          }
        }
      }

      Vector("parent", "mime", "length", "kind", "id", "content-mime", "content-length").foreach { defect =>
        s"retain existing original transport confirmation for $defect" in {
          Given("a valid package followed by one mismatched original stat or media observation")
          withSession(catalogText = None, referenceMode = "packages", packageCatalogText = Some(catalogJson().noSpaces)) { session =>
            val original = originalChildren.head
            val metadata = defect match {
              case "parent" => original.copy(parents = Vector(rootRef))
              case "mime" => original.copy(mediaType = org.goldenport.datatype.MimeType("text/html"))
              case "length" => original.copy(byteLength = Some(5L))
              case "kind" => original.copy(mediaType = org.goldenport.datatype.MimeType("application/vnd.google-apps.folder"))
              case "id" => original.copy(reference = audioRef)
              case _ => original
            }
            val responses = packageResponses() :+ jsonResponse(metadataJson(metadata))
            val driver = new Driver(responses ++ (if (defect == "content-mime") Vector(mediaResponse("text/html"))
              else if (defect == "content-length") Vector(mediaResponse(content = Vector[Byte](1))) else Vector.empty))
            When("the exact selected original reaches the unchanged stat and binary transport checks")
            val result = session.execute("readOriginal", _selection, driver = driver)
            Then("the invalid original is never returned or retried")
            failure(result).observation.taxonomy shouldBe Taxonomy.resourceInvalid
            driver.calls.size shouldBe (if (defect.startsWith("content-")) 8 else 7)
            privateMarkers.foreach(marker => result.display should not include marker)
          }
        }
      }
    }

    "admit trusted subjects before independent connection authority" which {
      "reject unsafe logical resource selectors and invalid paging before provider access" in {
        Given("a native Packages component and request fields outside the existing bounded logical contract")
        withSession(catalogText = None, connection = "absent", referenceMode = "packages", packageCatalogText = Some(catalogJson().noSpaces)) { session =>
          val searches = Vector(Vector("text" -> ("x" * 257)), Vector("text" -> "bad\nquery"),
            Vector("offset" -> -1), Vector("limit" -> 0), Vector("limit" -> 101))
          val resources = Vector("", "assets/image-1", "../image-1", "image%2d1", "image\\1", "x" * 129)
          val requests = searches.map("searchMaterials" -> _) ++ resources.map(resource =>
            "readOriginal" -> Vector("materialRef" -> reference, "resourceId" -> resource))
          requests.foreach { case (operation, fields) =>
            val driver = new Driver(Vector.empty)
            When("the public native query interprets a malformed bounded logical argument")
            val result = session.execute(operation, fields, driver = driver)
            Then("argument admission rejects the request before connection lookup or any root listing")
            failure(result).observation.taxonomy shouldBe Taxonomy.argumentInvalid
            driver.calls shouldBe empty
            session.acquisitions shouldBe 0
          }
        }
      }

      "deny anonymous callers before observing catalog connection or provider state" in {
        Given("a configured Packages view and no provider-authenticated caller")
        withSession(catalogText = None, connection = "absent", referenceMode = "packages", packageCatalogText = Some(catalogJson().noSpaces)) { session =>
          Vector("searchMaterials" -> Vector.empty[(String, Any)], "getMaterial" -> Vector("materialRef" -> reference),
            "readOriginal" -> _selection).foreach { case (operation, fields) =>
            val driver = new Driver(Vector.empty)
            When("an anonymous request enters the normal native query admission path")
            val result = session.execute(operation, fields, sessionId = None, driver = driver)
            Then("native authentication is required before any provider lookup")
            failure(result).observation.taxonomy shouldBe Taxonomy.securityAuthenticationRequired
            driver.calls shouldBe empty
            session.acquisitions shouldBe 0
          }
        }
      }

      "hide unauthorized packages and reject known and unknown exact references before connection access" in {
        Given("a trusted package authorized for the owner and an absent managed connection")
        withSession(catalogText = None, connection = "absent", referenceMode = "packages", packageCatalogText = Some(catalogJson().noSpaces)) { session =>
          val driver = new Driver(Vector.empty)
          When("another authenticated subject searches and requests the known package")
          val page = record(session.execute("searchMaterials", sessionId = Some("material-other-session"), driver = driver))
          val denied = session.execute("getMaterial", Vector("materialRef" -> reference), sessionId = Some("material-other-session"), driver = driver)
          val read = session.execute("readOriginal", _selection, sessionId = Some("material-other-session"), driver = driver)
          val unknown = session.execute("getMaterial", Vector("materialRef" -> (reference + " ")), driver = driver)
          Then("empty paging and exact authorization failures precede connection availability and provider access")
          page.getAsC[Vector[Record]]("items").TAKE shouldBe Some(Vector.empty)
          page.getAsC[Boolean]("hasMore").TAKE shouldBe Some(false)
          failure(denied).observation.taxonomy shouldBe Taxonomy.securityPermissionDenied
          failure(read).observation.taxonomy shouldBe Taxonomy.securityPermissionDenied
          failure(unknown).observation.taxonomy shouldBe Taxonomy.resourceNotFound
          driver.calls shouldBe empty
          session.acquisitions shouldBe 0
        }
      }

      "never treat remote owner createdBy or provenance as trusted authorization" in {
        Given("a package manifest claiming another subject while trusted configuration authorizes only the fixture owner")
        val manifest = replace(replace(manifestJson(), "owner", Json.fromString("other-subject")),
          "createdBy", Json.fromString("other-subject"))
        withSession(catalogText = None, referenceMode = "packages", packageCatalogText = Some(catalogJson().noSpaces)) { session =>
          val ownerdriver = new Driver(packageResponses(manifest))
          val otherdriver = new Driver(Vector.empty)
          When("the configured subject and the remote-claimed subject each request the same logical Material")
          val admitted = session.execute("getMaterial", Vector("materialRef" -> reference), driver = ownerdriver)
          val denied = session.execute("getMaterial", Vector("materialRef" -> reference),
            sessionId = Some("material-other-session"), driver = otherdriver)
          Then("trusted configuration authorizes the owner and remote metadata grants no provider access to the other subject")
          record(admitted).getString("materialRef") shouldBe Some(reference)
          record(admitted).print should not include "other-subject"
          failure(denied).observation.taxonomy shouldBe Taxonomy.securityPermissionDenied
          otherdriver.calls shouldBe empty
        }
      }

      "enforce native managed connection ownership independently of trusted Material subjects" in {
        Given("both fixture subjects are trusted Material readers but only the first owns the OAuth connection")
        val catalog = catalogJson(Vector(bindingJson(subjects = Vector(subject, "other-subject"))))
        withSession(catalogText = None, referenceMode = "packages", packageCatalogText = Some(catalog.noSpaces)) { session =>
          val driver = new Driver(Vector.empty)
          When("the second authorized Material subject resolves the owner's managed connection")
          val result = session.execute("getMaterial", Vector("materialRef" -> reference),
            sessionId = Some("material-other-session"), driver = driver)
          Then("native connection permission denies the request before a provider driver is acquired")
          failure(result).observation.taxonomy shouldBe Taxonomy.securityPermissionDenied
          driver.calls shouldBe empty
          session.acquisitions shouldBe 0
        }
      }

      Vector("absent", "disconnected").foreach { state =>
        s"retain independent $state connection failure for an authorized package" in {
          Given("a trusted package whose managed connection is unavailable or requires authorization")
          withSession(catalogText = None, connection = state, referenceMode = "packages", packageCatalogText = Some(catalogJson().noSpaces)) { session =>
            val driver = new Driver(Vector.empty)
            When("the authorized caller resolves the exact package")
            val result = session.execute("getMaterial", Vector("materialRef" -> reference), driver = driver)
            Then("the native connection failure survives without a bootstrap registered or provider fallback")
            failure(result).observation.taxonomy shouldBe (if (state == "absent") Taxonomy.serviceUnavailable else Taxonomy.securityPermissionDenied)
            driver.calls shouldBe empty
            session.acquisitions shouldBe 0
          }
        }
      }

      "use the explicitly selected native service account for package metadata and exact original bytes" in {
        Given("the same Packages view with CNCF's explicit synthetic service-account configuration")
        withSession(catalogText = None, connection = "service-account", referenceMode = "packages", packageCatalogText = Some(catalogJson().noSpaces)) { session =>
          val token = jsonResponse(Json.obj("access_token" -> Json.fromString("synthetic-package-service-token"),
            "token_type" -> Json.fromString("Bearer"), "expires_in" -> Json.fromInt(3600)))
          val driver = new Driver(Vector(token) ++ packageResponses() ++ Vector(jsonResponse(metadataJson(originalChildren.head)), mediaResponse()),
            tokenResponse = Some(token))
          When("the authorized native original query acquires the selected service account and reads the package")
          val result = session.execute("readOriginal", _selection, driver = driver)
          Then("the same exact original bytes use native token acquisition with no OAuth fallback")
          _bytes(_binary(result).bag.asInstanceOf[BinaryBag]) shouldBe MaterialReferenceFixture.bytes
          driver.calls.head.method shouldBe "POST"
          driver.calls.head.path shouldBe "https://oauth2.googleapis.com/token"
          driver.calls.tail.map(_.method).distinct shouldBe Vector("GET")
          driver.calls.tail.foreach(_.headers("Authorization") shouldBe "Bearer synthetic-package-service-token")
          _private_headers(_binary(result))
        }
      }

      Vector(401 -> Taxonomy.securityPermissionDenied, 403 -> Taxonomy.securityPermissionDenied,
        404 -> Taxonomy.resourceNotFound, 429 -> Taxonomy.serviceUnavailable, 503 -> Taxonomy.serviceUnavailable).foreach { case (status, taxonomy) =>
        s"preserve native listing status $status without retry or private provider-body disclosure" in {
          Given("an admitted package root followed by a native child-listing failure with private content")
          withSession(catalogText = None, referenceMode = "packages", packageCatalogText = Some(catalogJson().noSpaces)) { session =>
            val responses = packageResponses().take(1) :+ jsonResponse(Json.obj("private" -> Json.fromString(privateMarkers.mkString(" "))), status)
            val driver = new Driver(responses)
            When("the public metadata query enumerates its configured package root")
            val result = session.execute("getMaterial", Vector("materialRef" -> reference), driver = driver)
            Then("the native failure category propagates after exactly one root stat and one bounded listing")
            failure(result).observation.taxonomy shouldBe taxonomy
            driver.calls.map(_.path) shouldBe _package_paths.take(2)
            privateMarkers.foreach(marker => result.display should not include marker)
          }
        }
      }
    }

    "isolate all three explicit modes" which {
      "distinguish absent package capability from an explicitly empty authorized search" in {
        Given("Packages components with absent and explicitly empty trusted configuration")
        Vector(None, Some(catalogJson(Vector.empty).noSpaces)).foreach { setting =>
          withSession(catalogText = None, connection = "absent", referenceMode = "packages", packageCatalogText = setting) { session =>
            val driver = new Driver(Vector.empty)
            When("the admitted caller requests the first search page")
            val result = session.execute("searchMaterials", driver = driver)
            Then("absence is unavailable while explicit empty materials return items=[] without a provider connection")
            setting match {
              case None => failure(result).observation.taxonomy shouldBe Taxonomy.serviceUnavailable
              case Some(_) =>
                record(result).getAsC[Vector[Record]]("items").TAKE shouldBe Some(Vector.empty)
                record(result).getAsC[Boolean]("hasMore").TAKE shouldBe Some(false)
            }
            driver.calls shouldBe empty
            session.acquisitions shouldBe 0
          }
        }
      }

      "reject bootstrap and registered configuration mixtures including empty supplied values" in {
        Given("a normal native subsystem and explicit mode configurations that must never merge")
        withSession(connection = "absent") { session =>
          val variants = Vector(
            Map(MaterialReferenceRuntime.packagesKey.name -> ""),
            Map(MaterialReferenceRuntime.modeKey.name -> "bootstrap", MaterialReferenceRuntime.packagesKey.name -> catalogJson(Vector.empty).noSpaces),
            Map(MaterialReferenceRuntime.modeKey.name -> "registered", MaterialReferenceRuntime.packagesKey.name -> ""),
            Map(MaterialReferenceRuntime.modeKey.name -> "packages", MaterialReferenceRuntime.catalogKey.name -> ""),
            Map(MaterialReferenceRuntime.modeKey.name -> "packages", MaterialReferenceRuntime.registeredBindingsKey.name -> ""),
            Map(MaterialReferenceRuntime.modeKey.name -> "registered", MaterialReferenceRuntime.catalogKey.name -> ""),
            Map(MaterialReferenceRuntime.modeKey.name -> "bootstrap", MaterialReferenceRuntime.registeredBindingsKey.name -> ""))
          When("the actual public owning factory initializes each exact parameter map")
          val results = variants.map(parameters => _initialize(session, parameters))
          Then("each supplied incompatible view fails fixed configuration admission before any operation or provider work")
          results.foreach { result =>
            failure(result).observation.taxonomy shouldBe Taxonomy(Taxonomy.Category.Configuration, Taxonomy.Symptom.Invalid)
            privateMarkers.foreach(marker => result.display should not include marker)
          }
        }
      }

      "reject registration write configuration in Packages mode with its explicit structured branch" in {
        Given("the native factory with Packages mode and supplied registration configuration including empty text")
        withSession(connection = "absent") { session =>
          val settings = Vector("", "private-registration-marker", "{}").map(text => Map(
            MaterialReferenceRuntime.modeKey.name -> "packages", MaterialRegistrationConfiguration.configurationKey.name -> text))
          When("component initialization resolves the subordinate registration configuration")
          val results = settings.map(parameters => _initialize(session, parameters))
          Then("every supplied write configuration fails deterministically without MatchError or registration initialization")
          results.foreach { result =>
            failure(result).observation.taxonomy shouldBe Taxonomy(Taxonomy.Category.Configuration, Taxonomy.Symptom.Invalid)
            failure(result).displayMessage shouldBe "Package Material reference mode cannot include registration configuration"
            result.display should not include "private-registration-marker"
          }
        }
      }

      "reject padded unknown and empty modes and malformed package text at normal initialization" in {
        Given("unrecognized explicit mode spellings and malformed package configuration")
        withSession(connection = "absent") { session =>
          val variants = Vector("", " packages", "packages ", "Packages", "unknown").map(mode =>
            Map(MaterialReferenceRuntime.modeKey.name -> mode)) ++ Vector(Map(MaterialReferenceRuntime.modeKey.name -> "packages",
              MaterialReferenceRuntime.packagesKey.name -> "private-malformed-marker"))
          When("the actual owning factory admits the supplied mode and package text")
          val results = variants.map(parameters => _initialize(session, parameters))
          Then("configuration never infers normalizes or falls back to another view")
          results.foreach { result =>
            failure(result).observation.taxonomy shouldBe Taxonomy(Taxonomy.Category.Configuration, Taxonomy.Symptom.Invalid)
            result.display should not include "private-malformed-marker"
          }
        }
      }

      "keep registration commands unavailable in the readonly Packages view" in {
        Given("an admitted Packages component without registration configuration")
        withSession(catalogText = None, connection = "absent", referenceMode = "packages", packageCatalogText = Some(catalogJson(Vector.empty).noSpaces)) { session =>
          val requests = Vector("createMaterialRegistration" -> Vector("contribution" -> manifestJson()),
            "getMaterialRegistration" -> Vector("registrationId" -> "opaque-registration"),
            "materializeMaterial" -> Vector("registrationId" -> "opaque-registration"))
          requests.foreach { case (operation, fields) =>
            val driver = new Driver(Vector.empty)
            When("an authenticated caller requests a registration operation in the readonly view")
            val result = session.execute(operation, fields, driver = driver)
            Then("the existing registered-mode requirement wins before parsing storage or issuing provider writes")
            failure(result).observation.taxonomy shouldBe Taxonomy.serviceUnavailable
            failure(result).displayMessage shouldBe "Material registration requires registered mode"
            driver.calls shouldBe empty
          }
        }
      }

      "preserve default Bootstrap and explicit Registered initialization without inferring Packages from stored roots" in {
        Given("the ordinary default fixture and an explicitly selected Registered component")
        withSession(connection = "absent") { session =>
          When("default bootstrap metadata is read and Registered initializes without catalog or package parameters")
          val bootstrap = session.execute("getMaterial", Vector("materialRef" -> MaterialReferenceFixture.materialRef))
          val registered = _initialize(session, Map(MaterialReferenceRuntime.modeKey.name -> "registered"))
          Then("bootstrap keeps its old logical identity and Registered retains only its own explicit view")
          record(bootstrap).getString("materialRef") shouldBe Some(MaterialReferenceFixture.materialRef)
          val runtime = registered.TAKE.asInstanceOf[MaterialReferenceRuntime]
          runtime._material_reference_mode shouldBe MaterialReferenceRuntime.Mode.Registered
          runtime.materialCatalog shouldBe None
          runtime._material_package_catalog shouldBe None
          session.acquisitions shouldBe 0
        }
      }
    }
  }

  private def _initialize(session: MaterialReferenceFixture.Session, settings: Map[String, String]): Consequence[Component] = {
    val id = KnowledgeLakeComponent.componentId
    val descriptor = ComponentDescriptor(name = Some(id.name), componentName = Some(id.name), schemaVersion = Some(3), componentId = Some(id))
    KnowledgeLakePrimaryFactory.createPrimaryC(ComponentCreate(session.subsystem, ComponentOrigin.Builtin, Vector(descriptor))
      .withInstanceMetadata(ComponentInstanceMetadata(id.name, ComponentInstanceId.default(id).instance, settings, componentId = Some(id))))
  }

  private def _binary(result: Consequence[OperationResponse]): HttpResponse = result.TAKE match {
    case OperationResponse.Http(response) => response
    case _ => fail("Expected a private native binary Material original")
  }

  private def _bytes(bag: BinaryBag): Vector[Byte] = {
    val stream = bag.openInputStream()
    try stream.readAllBytes().toVector finally stream.close()
  }

  private def _private_headers(response: HttpResponse): Unit = {
    response.headerValue("Cache-Control") shouldBe Some("private, no-store")
    response.headerValue("X-Content-Type-Options") shouldBe Some("nosniff")
    response.headerValue("Content-Security-Policy") shouldBe Some("default-src 'none'; sandbox")
    response.headerValue("Content-Disposition") shouldBe Some("attachment; filename=\"original\"")
  }

  private def _stat_path(reference: FileStoreRef): String =
    s"https://www.googleapis.com/drive/v3/files/${reference.key}?supportsAllDrives=true&fields=id,name,mimeType,size,parents,trashed,version"
  private def _media_path(reference: FileStoreRef): String =
    s"https://www.googleapis.com/drive/v3/files/${reference.key}?alt=media&supportsAllDrives=true"
  private def _listing_path(parent: FileStoreRef): String = {
    val parameters = Vector("q" -> s"'${parent.key}' in parents and trashed = false", "supportsAllDrives" -> "true",
      "includeItemsFromAllDrives" -> "true", "pageSize" -> "50",
      "fields" -> "nextPageToken,incompleteSearch,files(id,name,mimeType,size,parents,trashed,version)")
    "https://www.googleapis.com/drive/v3/files?" + parameters.map { case (key, value) =>
      key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8)
    }.mkString("&")
  }
  private def _package_paths: Vector[String] = Vector(_stat_path(rootRef), _listing_path(rootRef), _stat_path(manifestRef),
    _media_path(manifestRef), _listing_path(assetsRef), _listing_path(derivedRef))
}
