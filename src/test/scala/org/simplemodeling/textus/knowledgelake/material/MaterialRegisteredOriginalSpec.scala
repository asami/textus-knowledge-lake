package org.simplemodeling.textus.knowledgelake.material

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.nio.file.attribute.PosixFilePermissions
import java.time.Instant
import java.util.Base64
import scala.util.Using
import io.circe.{Json, parser}
import org.goldenport.{Conclusion, Consequence, ConsequenceException}
import org.goldenport.bag.{Bag, BinaryBag}
import org.goldenport.cncf.entity.EntitySnapshot
import org.goldenport.cncf.oauth.OAuthConnectionRuntime
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.http.HttpResponse
import org.goldenport.observation.{Descriptor, Taxonomy}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.test.matchers.ConclusionMatchers
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.knowledgelake.KnowledgeLakeComponent

/**
 * Native PostgreSQL registrations and controlled native Google FileStore reads.
 * Synthetic custody and HTTP evidence do not establish real Google acceptance.
 *
 * @since   Oct. 22, 2026
 * @version Oct. 22, 2026
 */
final class MaterialRegisteredOriginalSpec extends AnyWordSpec with Matchers with GivenWhenThen
    with ConclusionMatchers with MaterialRegistrationPostgresqlSpec {
  import MaterialRegistrationPersistenceFixture.*
  import MaterialRegisteredOriginalSpec.*

  "Registered Material original" should {
    "read only the confirmed authorized original" which {
      "retain exact bytes Bag headers native record and custody through reads and a fresh runtime" in {
        Given("durable PostgreSQL and an explicitly registered owning component with synthetic private custody and a read binding")
        withDatabase(native_container) { database =>
          database.durability shouldBe ("on" -> "on")
          val retained = withSession(database, _parameters(), prepare = _prepare()) { session =>
            val seed = _seed(session)
            val before = database.snapshot()
            val custody = _custody(session.root)
            val effects = seed.destination.events -> seed.source.calls
            val idle = new MaterialReferenceFixture.Driver(Vector.empty)
            When("generated metadata and search queries resolve the same native Registered Material")
            val metadata = MaterialReferenceFixture.record(session.query("getMaterial", Vector("materialRef" -> seed.reference), driver = Some(idle)))
            val search = MaterialReferenceFixture.record(session.query("searchMaterials", driver = Some(idle)))
            Then("logical metadata and search need no HTTP grant acquisition or native mutation")
            metadata.getString("materialRef") shouldBe Some(seed.reference)
            metadata.getString("title") shouldBe Some(seed.snapshot.entity.document.contribution.title)
            search.print should include(seed.reference)
            idle.calls shouldBe empty
            idle.acquisitions shouldBe 0
            metadata.print should not include "physical-"
            metadata.print should not include binding
            database.snapshot() shouldBe before

            Given("native provider metadata for the second original's exact acknowledged key and parent and independent harmless PNG bytes")
            val media = MaterialReferenceFixture.mediaResponse(content = png)
            val driver = new MaterialReferenceFixture.Driver(Vector(MaterialReferenceFixture.jsonResponse(_metadata(seed)), media))
            When("readOriginal runs through generated dispatch ActionEngine managed credentials and native FileStore")
            val result = session.query("readOriginal", _selection(seed.reference), driver = Some(driver))
            val response = _response(result)
            val bytes = Using.resource(response.bag.openInputStream())(_.readAllBytes().toVector)
            val loaded = take(session.get(seed.snapshot.entity.id))
            Then("the exact materialized PNG Bag and safe headers survive without registration or source replay")
            response shouldBe a[HttpResponse.Binary]
            response.bag shouldBe a[BinaryBag]
            response.bag should be theSameInstanceAs media.bag
            response.bag.metadata.size shouldBe Some(png.size.toLong)
            bytes shouldBe png
            response.mime.value shouldBe "image/png"
            response.headerValue("Cache-Control") shouldBe Some("private, no-store")
            response.headerValue("X-Content-Type-Options") shouldBe Some("nosniff")
            response.headerValue("Content-Security-Policy") shouldBe Some("default-src 'none'; sandbox")
            response.headerValue("Content-Disposition") shouldBe Some("attachment; filename=\"original\"")
            _expect_calls(driver, seed, 2)
            seed.destination.bytes(seed.file.reference) shouldBe png
            loaded.entity.id shouldBe seed.snapshot.entity.id
            loaded.entity.attributes shouldBe seed.snapshot.entity.attributes
            loaded.entity.document.json shouldBe seed.snapshot.entity.document.json
            loaded.entity.document.contribution.json shouldBe _contribution.json
            loaded.revision.value shouldBe 3L
            database.snapshot() shouldBe before
            (seed.destination.events -> seed.source.calls) shouldBe effects
            _custody(session.root) shouldBe custody
            _private_markers(seed).foreach(marker => response.header.print should not include marker)
            Vector("operator.json", "grant.json").foreach { name =>
              Files.getPosixFilePermissions(session.root.resolve(name)) shouldBe PosixFilePermissions.fromString("rw-------")
            }
            (seed.snapshot, before)
          }

          Given("a new native component interpreter and synthetic owning-TKL grant under a new owned root against the same database")
          withSession(database, _parameters(), prepare = _prepare()) { reopened =>
            val snapshot = take(reopened.get(retained._1.entity.id))
            val file = snapshot.entity.document.stored.get.files(1)
            val metadata = _metadata(file.reference.key, file.parent.key)
            val media = MaterialReferenceFixture.mediaResponse(content = png)
            val driver = new MaterialReferenceFixture.Driver(Vector(MaterialReferenceFixture.jsonResponse(metadata), media))
            val custody = _custody(reopened.root)
            When("the same exact logical reference and original are read without a catalog or registration replay")
            val response = _response(reopened.query("readOriginal", _selection(snapshot.entity.document.location.urn), driver = Some(driver)))
            val bytes = Using.resource(response.bag.openInputStream())(_.readAllBytes().toVector)
            Then("the persisted key and parent remain authoritative and all native data and custody remain unchanged")
            snapshot.entity.id shouldBe retained._1.entity.id
            snapshot.revision shouldBe retained._1.revision
            snapshot.entity.attributes shouldBe retained._1.entity.attributes
            snapshot.entity.document.json shouldBe retained._1.entity.document.json
            bytes shouldBe png
            response.bag should be theSameInstanceAs media.bag
            driver.calls.map(_.path) shouldBe Vector(_metadata_url(file.reference.key), _media_url(file.reference.key))
            driver.calls.map(_.headers("Authorization")) shouldBe Vector.fill(2)(s"Bearer $token")
            driver.acquisitions shouldBe 2
            database.snapshot() shouldBe retained._2
            _custody(reopened.root) shouldBe custody
          }
        }
      }
    }

    "admit native authority and original identity before private binding or credentials" which {
      "withhold foreign missing incomplete denied anonymous and non-original selections without provider effects" in {
        Given("normal Registered foreign Prepared and failed Materializing records with no configured OAuth custody")
        withDatabase(native_container) { database => withSession(database, _parameters(), prepare = _prepare("absent")) { session =>
          val own = _seed(session)
          val hidden = _seed(session, Some("foreign-session"))
          val prepared = take(session.create(_contribution))
          val incomplete = take(session.create(_contribution))
          val failedroot = Files.createTempDirectory(session.root, "incomplete-")
          val destination = new Destination(Files.createDirectory(failedroot.resolve("destination")), database, Some("collection:assets"))
          val source = new Source
          val writer = take(MaterialPackageWriter.createC(destination, lake, logicalParent, destination.parent))
          When("normal native materialization retains a failed Materializing state")
          val failed = session.execute[EntitySnapshot[MaterialRegistrationRecord]]("materialize-incomplete")(_.materialize(incomplete.entity.id, writer, source))
          Then("the actual provider failure and persisted incomplete state remain distinct")
          failure(failed) shouldBe failure(destination.error)
          take(session.get(incomplete.entity.id)).entity.document.status.value shouldBe "materializing"

          Given("the complete native and source/destination baselines before unauthorized or unavailable reads")
          val before = database.snapshot()
          val custody = _custody(session.root)
          val effects = Vector(own, hidden).map(seed => seed.destination.events -> seed.source.calls)
          val incompleteeffects = destination.events -> source.calls
          val cases = Vector(
            (_selection(hidden.reference), Some("owner-session"), Consequence.resourceNotFound[Unit]("Material was not found"), "MaterialRegistrationSupport.scala"),
            (_selection("urn:missing:registered"), Some("owner-session"), Consequence.resourceNotFound[Unit]("Material was not found"), "MaterialRegistrationSupport.scala"),
            (_selection(prepared.entity.document.location.urn), Some("owner-session"), Consequence.resourceNotFound[Unit]("Material was not found"), "MaterialRegistrationSupport.scala"),
            (_selection(incomplete.entity.document.location.urn), Some("owner-session"), Consequence.resourceNotFound[Unit]("Material was not found"), "MaterialRegistrationSupport.scala"),
            (_selection(own.reference, "missing"), Some("owner-session"), Consequence.resourceNotFound[Unit]("Material resource was not found"), "MaterialServiceFactory.scala"),
            (_selection(own.reference, "review"), Some("owner-session"), Consequence.resourceNotFound[Unit]("Material resource was not found"), "MaterialServiceFactory.scala"))
          cases.foreach { case (fields, caller, expected, originfile) =>
            val driver = new MaterialReferenceFixture.Driver(Vector.empty)
            When("the generated read resolves this exact selection under the actual admitted caller")
            val result = session.query("readOriginal", fields, session = caller, driver = Some(driver))
            Then("the native safe failure precedes custody and HTTP and preserves all baselines")
            _expect_conclusion(failure(result), failure(expected), originfile)
            driver.calls shouldBe empty
            driver.acquisitions shouldBe 0
            database.snapshot() shouldBe before
            _custody(session.root) shouldBe custody
            Vector(own, hidden).map(seed => seed.destination.events -> seed.source.calls) shouldBe effects
            (destination.events -> source.calls) shouldBe incompleteeffects
            _private_markers(own).foreach(marker => result.display should not include marker)
          }

          Given("an anonymous caller with the same known original and no custody authority")
          val anonymousdriver = new MaterialReferenceFixture.Driver(Vector.empty)
          val searches = session.store.searches.size
          When("the generated authenticated-only read Operation receives the anonymous selection")
          val anonymous = session.query("readOriginal", _selection(own.reference), session = None, driver = Some(anonymousdriver))
          Then("native Operation authentication refuses the request before any datastore or provider lookup")
          failure(anonymous).observation.taxonomy shouldBe Taxonomy.securityAuthenticationRequired
          session.store.searches.size shouldBe searches
          anonymousdriver.calls shouldBe empty
          anonymousdriver.acquisitions shouldBe 0
          database.snapshot() shouldBe before
          _custody(session.root) shouldBe custody
          Vector(own, hidden).map(seed => seed.destination.events -> seed.source.calls) shouldBe effects
          (destination.events -> source.calls) shouldBe incompleteeffects
          _private_markers(own).foreach(marker => anonymous.display should not include marker)

          Given("the actual owner of a Registered row whose native read permission is refused by test security setup")
          database.security(own.snapshot.entity.id, permission = readDeniedPermission)
          val deniedbefore = database.snapshot()
          val driver = new MaterialReferenceFixture.Driver(Vector.empty)
          When("that owner reads the known original through native UserPermission filtering")
          val denied = session.query("readOriginal", _selection(own.reference), driver = Some(driver))
          Then("the hidden native result is not found without acquiring custody HTTP or mutation authority")
          _expect_conclusion(failure(denied), failure(Consequence.resourceNotFound[Unit]("Material was not found")),
            "MaterialRegistrationSupport.scala")
          driver.calls shouldBe empty
          driver.acquisitions shouldBe 0
          database.snapshot() shouldBe deniedbefore
          _custody(session.root) shouldBe custody
          Vector(own, hidden).map(seed => seed.destination.events -> seed.source.calls) shouldBe effects
          (destination.events -> source.calls) shouldBe incompleteeffects
        } }
      }

      "retain native ambiguity rather than choosing an authorized persisted mapping" in {
        Given("two normal Registered identities whose exact native observed update shares one Registered document")
        withDatabase(native_container) { database => withSession(database, _parameters(), prepare = _prepare()) { session =>
          val first = _seed(session)
          val second = _seed(session)
          When("a normal native observed update supplies the first retained document to the second identity")
          val updated = take(session.execute[EntitySnapshot[MaterialRegistrationRecord]]("arrange-ambiguity")(_.replaceDocument(second.snapshot, first.snapshot.entity.document)))
          Then("both separately issued native identities retain the same exact logical reference")
          updated.entity.id should not be first.snapshot.entity.id
          updated.entity.document.location.urn shouldBe first.reference

          Given("the complete native baseline before resolving an ambiguous original")
          val before = database.snapshot()
          val custody = _custody(session.root)
          val effects = Vector(first, second).map(seed => seed.destination.events -> seed.source.calls)
          val driver = new MaterialReferenceFixture.Driver(Vector.empty)
          When("readOriginal resolves the exact duplicated authorized reference")
          val result = session.query("readOriginal", _selection(first.reference), driver = Some(driver))
          Then("the unchanged native getMaterial conflict propagates before credentials or provider selection")
          _expect_conclusion(failure(result), failure(Consequence.operationConflict[Unit]("getMaterial",
            Vector(Descriptor.Facet.Reason("material-reference-ambiguous")))), "MaterialRegistrationSupport.scala")
          driver.calls shouldBe empty
          driver.acquisitions shouldBe 0
          database.snapshot() shouldBe before
          _custody(session.root) shouldBe custody
          Vector(first, second).map(seed => seed.destination.events -> seed.source.calls) shouldBe effects
        } }
      }
    }

    "resolve only an explicitly configured registered store" which {
      Vector("absent", "empty", "unknown-alias", "store-mismatch").foreach { state =>
        s"fail $state before native custody or HTTP without a bootstrap fallback" in {
          Given(s"a normal Registered mapping and $state operator bindings with no OAuth configuration")
          val bindingtext = state match {
            case "absent" => None
            case "empty" => Some(_bindings(Vector.empty).noSpaces)
            case "unknown-alias" => Some(_bindings(Vector(_binding(id = "another-alias"))).noSpaces)
            case _ => Some(_bindings(Vector(_binding(storename = "another-store"))).noSpaces)
          }
          withDatabase(native_container) { database => withSession(database, _parameters(bindingtext), prepare = _prepare("absent")) { session =>
            val seed = _seed(session)
            val expected = if (state == "store-mismatch") _resource_invalid else
              Consequence.serviceUnavailable[Unit]("Registered Material original access is unavailable")
            _expect_failure(session, seed, Vector.empty, expected, 0, "MaterialServiceFactory.scala")
          } }
        }
      }
    }

    "admit a strict private operator configuration at component initialization" which {
      "accept exact and empty bindings with redacted displays without SQL or provider work" in {
        Given("an empty durable database and exact valid or explicitly empty operator binding configurations")
        withDatabase(native_container) { database =>
          Vector(_bindings(), _bindings(Vector.empty)).foreach { document =>
            val before = database.snapshot()
            When("the normal registered component initializes the supplied bindings exactly once")
            withSession(database, _parameters(Some(document.noSpaces))) { session =>
              val parsed = take(MaterialReferenceRuntime._parse_bindings(document.noSpaces))
              val resolved = session.component._material_read_binding(binding)
              Then("only the configured alias exists and diagnostic displays reveal no connection or store values")
              resolved.isDefined shouldBe document.hcursor.downField("bindings").focus.get.asArray.get.nonEmpty
              parsed.toString shouldBe "MaterialReadBindings(<redacted>)"
              resolved.foreach(_.toString shouldBe "MaterialReadBinding(<redacted>)")
              database.snapshot() shouldBe before
            }
          }
        }
      }

      "reject malformed shapes schemas types duplicate semantic fields identifiers and mixed modes without entering the body" in {
        Given("an empty native database and bounded malformed operator configuration cases containing private markers")
        val valid = _bindings()
        val entry = _binding()
        val badentries = Vector("id", "provider", "connectionId", "storeName").map(key => entry.mapObject(_.remove(key))) ++ Vector(
          _replace(entry, "id", Json.Null),
          _replace(entry, "id", Json.fromString("/private-marker")),
          _replace(entry, "connectionId", Json.fromString("private marker")),
          _replace(entry, "storeName", Json.fromString("x" * 129)),
          _replace(entry, "provider", Json.fromString("unsupported-private-marker")),
          _replace(entry, "provider", Json.fromBoolean(true)),
          _replace(entry, "token", Json.fromString(token)),
          _replace(entry, "resourceKey", Json.fromString("private-physical-marker")),
          _replace(entry, "parentKey", Json.fromString("private-parent-marker")),
          _replace(entry, "path", Json.fromString("private-path-marker")),
          _replace(entry, "connectionId", Json.fromInt(1)), _replace(entry, "storeName", Json.arr()))
        val malformed = Vector("{private-marker", "null", "[]",
          _replace(valid, "schema", Json.fromString("private-schema-marker")).noSpaces,
          _replace(valid, "schema", Json.fromInt(1)).noSpaces,
          valid.mapObject(_.remove("schema")).noSpaces,
          valid.mapObject(_.remove("bindings")).noSpaces,
          _replace(valid, "unknown", Json.fromString(token)).noSpaces,
          _replace(valid, "bindings", Json.obj()).noSpaces,
          _replace(valid, "bindings", Json.arr(Json.Null)).noSpaces,
          _bindings(Vector(entry, entry)).noSpaces,
          valid.noSpaces.replace("\"schema\":", "\"schema\":\"private-marker\",\"schema\":"),
          valid.noSpaces.replace("\"connectionId\":", "\"connectionId\":\"private-marker\",\"connectionId\":"),
          valid.noSpaces.replace("\"id\":", "\"\\u0069d\":\"private-marker\",\"id\":"),
          " " * 131073) ++ badentries.map(value => _bindings(Vector(value)).noSpaces)
        val variants = malformed.map(text => _parameters(Some(text))) ++ Vector(
          Map(MaterialReferenceRuntime.registeredBindingsKey.name -> _bindings(Vector.empty).noSpaces),
          Map(MaterialReferenceRuntime.modeKey.name -> "bootstrap", MaterialReferenceRuntime.registeredBindingsKey.name -> valid.noSpaces),
          _parameters() + (MaterialReferenceRuntime.catalogKey.name -> MaterialReferenceFixture.catalog(Vector.empty).noSpaces))
        withDatabase(native_container) { database =>
          val before = database.snapshot()
          variants.foreach { parameters =>
            var entered = false
            When("normal component initialization admits the supplied mode and binding configuration")
            val rejected = intercept[ConsequenceException] { withSession(database, parameters) { _ => entered = true } }
            Then("fixed Configuration Invalid prevents the body SQL changes and private value disclosure")
            val conclusion = failure(rejected.consequence)
            conclusion.observation.taxonomy shouldBe Taxonomy(Taxonomy.Category.Configuration, Taxonomy.Symptom.Invalid)
            Set("Material registered read binding configuration is invalid",
              "Bootstrap Material reference mode cannot include registered read bindings",
              "Registered Material reference mode cannot include a bootstrap catalog") should contain(conclusion.displayMessage)
            Vector(token, "private-marker", "private-schema-marker", "private-physical-marker", "private-path-marker").foreach { marker =>
              rejected.consequence.display should not include marker
            }
            entered shouldBe false
            database.snapshot() shouldBe before
          }
        }
      }
    }

    "confirm native metadata and content without retries or replay" which {
      Vector("id", "parent", "kind", "mime", "length", "unknown-length", "malformed").foreach { mismatch =>
        s"reject $mismatch stat metadata after exactly one GET" in {
          Given(s"a Registered original and native stat metadata with $mismatch inconsistency")
          withDatabase(native_container) { database => withSession(database, _parameters(), prepare = _prepare()) { session =>
            val seed = _seed(session)
            val metadata = _metadata(seed)
            val changed = mismatch match {
              case "id" => _replace(metadata, "id", Json.fromString("OtherPhysicalFile"))
              case "parent" => _replace(metadata, "parents", Json.arr(Json.fromString("OtherPhysicalParent")))
              case "kind" => _replace(metadata, "mimeType", Json.fromString("application/vnd.google-apps.folder"))
              case "mime" => _replace(metadata, "mimeType", Json.fromString("text/html"))
              case "length" => _replace(metadata, "size", Json.fromString((png.size + 1).toString))
              case "unknown-length" => metadata.mapObject(_.remove("size"))
              case _ => Json.obj("private" -> Json.fromString(token))
            }
            val expected = if (Set("id", "malformed")(mismatch))
              Consequence.resourceInvalid[Unit]("Google Drive file metadata is invalid") else _resource_invalid
            val originfile = if (Set("id", "malformed")(mismatch)) "GoogleDriveFileStore.scala" else "MaterialServiceFactory.scala"
            _expect_failure(session, seed, Vector(MaterialReferenceFixture.jsonResponse(changed)), expected, 1, originfile)
          } }
        }
      }

      Vector("mime", "length").foreach { mismatch =>
        s"reject $mismatch content after one stat and one media GET" in {
          Given(s"confirmed Registered metadata followed by media with changed $mismatch")
          withDatabase(native_container) { database => withSession(database, _parameters(), prepare = _prepare()) { session =>
            val seed = _seed(session)
            val media = if (mismatch == "mime") MaterialReferenceFixture.mediaResponse(mediaType = "text/html", content = png)
              else MaterialReferenceFixture.mediaResponse(content = png.dropRight(1))
            _expect_failure(session, seed, Vector(MaterialReferenceFixture.jsonResponse(_metadata(seed)), media), _resource_invalid, 2,
              "MaterialServiceFactory.scala")
          } }
        }
      }

      Vector(401, 403, 404, 429, 503).foreach { status =>
        s"preserve the exact safe native provider conclusion for status $status without retry" in {
          Given(s"a Registered original whose native provider stat or media responds $status with private body content")
          withDatabase(native_container) { database => withSession(database, _parameters(), prepare = _prepare()) { session =>
            val seed = _seed(session)
            val response = MaterialReferenceFixture.jsonResponse(Json.obj("private" -> Json.fromString(_private_markers(seed).mkString(" "))), status)
            val expected = status match {
              case 401 | 403 => Consequence.securityPermissionDenied[Unit]("Google Drive file access was denied")
              case 404 => Consequence.resourceNotFound[Unit]("Google Drive file was not found")
              case _ => Consequence.serviceUnavailable[Unit]("Google Drive file service is unavailable")
            }
            _expect_failure(session, seed, Vector(response), expected, 1, "GoogleDriveFileStore.scala")
            _expect_failure(session, seed, Vector(MaterialReferenceFixture.jsonResponse(_metadata(seed)), response), expected, 2,
              "GoogleDriveFileStore.scala")
          } }
        }
      }
    }

    "retain native managed connection custody" which {
      Vector("absent", "disconnected", "unknown", "wrong-owner").foreach { state =>
        s"preserve $state native connection failure without driver acquisition" in {
          Given(s"a normally authorized Registered original with $state synthetic managed connection custody")
          val selected = if (state == "unknown") Some(_bindings(Vector(_binding(connectionid = "unconfigured-connection"))).noSpaces)
            else Some(_bindings().noSpaces)
          withDatabase(native_container) { database => withSession(database, _parameters(selected), prepare = _prepare(state)) { session =>
            val seed = _seed(session)
            val expected = state match {
              case "absent" => Consequence.serviceUnavailable[Unit]("Material original connection is unavailable")
              case "unknown" => Consequence.resourceNotFound[Unit]("OAuth connection is not configured")
              case "wrong-owner" => Consequence.securityPermissionDenied[Unit]("OAuth connection owner is not allowed")
              case _ => Consequence.securityPermissionDenied[Unit]("OAuth connection requires authorization")
            }
            val originfile = state match {
              case "absent" => "MaterialServiceFactory.scala"
              case "unknown" => "UnitOfWorkInterpreter.scala"
              case _ => "OAuthConnection.scala"
            }
            _expect_failure(session, seed, Vector.empty, expected, 0, originfile)
          } }
        }
      }
    }
  }

  private def _response(result: Consequence[OperationResponse]): HttpResponse = take(result) match {
    case OperationResponse.Http(response) => response
    case _ => fail("A native binary original response is required")
  }
  private def _seed(session: Session, caller: Option[String] = Some("owner-session")): Seed = {
    Given("a complete contribution a counted independent PNG source and a real test-owned physical destination")
    val prepared = take(if (caller.contains("owner-session")) session.create(_contribution)
      else session.execute[EntitySnapshot[MaterialRegistrationRecord]]("create-original", caller)(_.create(_contribution)))
    val root = Files.createTempDirectory(session.root, "original-seed-")
    val destination = new Destination(Files.createDirectory(root.resolve("destination")), session.database)
    val source = new Source
    val writer = take(MaterialPackageWriter.createC(destination, lake, logicalParent, destination.parent))
    When("the normal native registration command materializes the Prepared identity exactly once")
    val snapshot = take(session.execute[EntitySnapshot[MaterialRegistrationRecord]]("materialize-original", caller)(_.materialize(prepared.entity.id, writer, source)))
    Then("the acknowledged Registered snapshot retains revision three and complete contribution with confirmed original and derived mappings")
    snapshot.entity.document.status.value shouldBe "registered"
    snapshot.revision.value shouldBe 3L
    snapshot.entity.document.contribution.json shouldBe _contribution.json
    snapshot.entity.document.stored.get.files.map(_.path) shouldBe Vector("assets/cover", "assets/detail", "derived/review.json")
    source.calls shouldBe _contribution.originals
    destination.events.last shouldBe "file:material.json"
    destination.releaseObservations()
    Seed(snapshot, destination, source)
  }
  private def _expect_calls(driver: MaterialReferenceFixture.Driver, seed: Seed, count: Int): Unit = {
    driver.calls.map(_.path) shouldBe Vector(_metadata_url(seed.file.reference.key), _media_url(seed.file.reference.key)).take(count)
    driver.calls.map(_.method) shouldBe Vector.fill(count)("GET")
    driver.calls.map(_.headers("Authorization")) shouldBe Vector.fill(count)(s"Bearer $token")
    driver.acquisitions shouldBe count
  }
  private def _expect_conclusion(actual: Conclusion, expected: Conclusion, originfile: String): Unit = {
    actual.observation.timestamp.isAfter(Instant.EPOCH) shouldBe true
    val sources = actual.observation.cause.descriptor.facets.collect { case source: Descriptor.Facet.SrcPos => source }
    sources.size shouldBe 1
    val source = sources.head
    source.pos.file should endWith(originfile)
    source.pos.line should be > 0
    source.pos.column should be > 0
    val facets = expected.observation.cause.descriptor.facets.filterNot(_.isInstanceOf[Descriptor.Facet.SrcPos]) :+ source
    val alignedexpected = expected.copy(observation = expected.observation.copy(
      cause = expected.observation.cause.copy(descriptor = Descriptor(facets))))
    actual should be_equal_conclusion(alignedexpected)
    actual.observation.callTree shouldBe expected.observation.callTree
  }
  private def _expect_failure(session: Session, seed: Seed, responses: Vector[HttpResponse], expected: Consequence[?], count: Int,
      originfile: String): Unit = {
    Given("the complete durable registration audit source destination and synthetic custody baselines")
    val before = session.database.snapshot()
    val custody = _custody(session.root)
    val events = seed.destination.events
    val reads = seed.source.calls
    val driver = new MaterialReferenceFixture.Driver(responses)
    When("generated readOriginal executes with the actual owning caller and selected provider responses")
    val result = session.query("readOriginal", _selection(seed.reference), driver = Some(driver))
    Then("the exact safe native conclusion survives with bounded GETs no retry and no mutation replay")
    _expect_conclusion(failure(result), failure(expected), originfile)
    _expect_calls(driver, seed, count)
    session.database.snapshot() shouldBe before
    seed.destination.events shouldBe events
    seed.source.calls shouldBe reads
    _custody(session.root) shouldBe custody
    _private_markers(seed).foreach(marker => result.display should not include marker)
  }
}

private[material] object MaterialRegisteredOriginalSpec {
  import MaterialRegistrationPersistenceFixture.*
  val token: String = "registered-synthetic-token-private-marker"
  val png: Vector[Byte] = Base64.getDecoder.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aP1sAAAAASUVORK5CYII=").toVector
  private val _connection_id = "registered-fixture-drive"
  private val _scopes = Vector("https://www.googleapis.com/auth/drive.readonly")
  private val _redirect = s"http://127.0.0.1:8123/web/knowledge-lake/connections/$_connection_id/callback"
  private val _resource_invalid = Consequence.resourceInvalid[Unit]("Material original does not match its configured resource")
  private val _contribution: MaterialContribution = take(MaterialContribution.parseC(Json.obj(
    "schema" -> Json.fromString("textus.material-contribution/1"), "title" -> Json.fromString("Registered harmless PNG"),
    "createdAt" -> Json.fromString("source timestamp retained"), "classifications" -> Json.arr(Json.fromString("raw")),
    "provenance" -> Json.obj("complete" -> Json.arr(Json.Null, Json.fromString("opaque\u0000\uD800"))),
    "originals" -> Json.fromValues(Vector("cover", "detail").map(id => Json.obj(
      "resourceId" -> Json.fromString(id), "role" -> Json.fromString("original"), "mediaType" -> Json.fromString("image/png"),
      "byteLength" -> Json.fromLong(png.size.toLong), "sourceReference" -> Json.obj("opaque" -> Json.fromString("source-" + id))))),
    "documents" -> Json.arr(Json.obj("resourceId" -> Json.fromString("review"), "role" -> Json.fromString("review-history"),
      "body" -> Json.obj("history" -> Json.arr(Json.Null, Json.fromString("retained\u0000\uDC00"))),
      "derivedFrom" -> Json.arr(Json.fromString("detail")))))))
  final class Source extends MaterialOriginalSource {
    private var _calls = Vector.empty[MaterialOriginalContribution]
    def calls: Vector[MaterialOriginalContribution] = _calls
    def read(original: MaterialOriginalContribution): ExecUowM[MaterialOriginalContent] = {
      _calls :+= original
      lift(Consequence.success(MaterialOriginalContent(original, Bag.fromBytes(png.toArray))))
    }
  }
  final case class Seed(snapshot: EntitySnapshot[MaterialRegistrationRecord], destination: Destination, source: Source) {
    def reference: String = snapshot.entity.document.location.urn
    def file: MaterialRegistrationDocument.File = snapshot.entity.document.stored.get.files(1)
    override def toString: String = "RegisteredOriginalSeed(<redacted>)"
  }
  private def _replace(json: Json, key: String, value: Json): Json = json.mapObject(_.add(key, value))
  private def _binding(id: String = binding, connectionid: String = _connection_id, storename: String = "registration-test-files"): Json =
    Json.obj("id" -> Json.fromString(id), "provider" -> Json.fromString("google-drive"),
      "connectionId" -> Json.fromString(connectionid), "storeName" -> Json.fromString(storename))
  private def _bindings(entries: Vector[Json] = Vector(_binding())): Json = Json.obj(
    "schema" -> Json.fromString("textus.knowledge-lake.material-read-bindings/1"), "bindings" -> Json.fromValues(entries))
  private def _parameters(text: Option[String] = Some(_bindings().noSpaces)): Map[String, String] =
    Map(MaterialReferenceRuntime.modeKey.name -> "registered") ++ text.map(MaterialReferenceRuntime.registeredBindingsKey.name -> _)
  private def _selection(reference: String, resourceid: String = "detail"): Vector[(String, Any)] =
    Vector("materialRef" -> reference, "resourceId" -> resourceid)
  private def _metadata(seed: Seed): Json = _metadata(seed.file.reference.key, seed.file.parent.key)
  private def _metadata(key: String, parent: String): Json = Json.obj("id" -> Json.fromString(key),
    "name" -> Json.fromString("private-physical-display"), "mimeType" -> Json.fromString("image/png"),
    "size" -> Json.fromString(png.size.toString), "parents" -> Json.arr(Json.fromString(parent)), "trashed" -> Json.False)
  private def _metadata_url(key: String): String =
    s"https://www.googleapis.com/drive/v3/files/$key?supportsAllDrives=true&fields=id,name,mimeType,size,parents,trashed,version"
  private def _media_url(key: String): String = s"https://www.googleapis.com/drive/v3/files/$key?alt=media&supportsAllDrives=true"
  private def _private_markers(seed: Seed): Vector[String] = Vector(token, "registered-private-client", "registered-private-secret",
    "private-physical-display", seed.file.reference.key, seed.file.parent.key)
  private def _custody(root: Path): Map[String, Vector[Byte]] = Vector("operator.json", "grant.json").flatMap { name =>
    val file = root.resolve(name)
    Option.when(Files.exists(file))(name -> Files.readAllBytes(file).toVector)
  }.toMap
  private def _write_private(file: Path, json: Json): Unit = {
    Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
    Files.writeString(file, json.noSpaces, StandardCharsets.UTF_8)
    ()
  }
  private def _prepare(state: String = "connected"): Path => Map[String, String] = root => {
    val configfile = root.resolve(".textus/config.json")
    val config = parser.parse(Files.readString(configfile, StandardCharsets.UTF_8)).toOption.get
    val plan = Json.obj("schema" -> Json.fromString("cncf.runtime-http-execution.v1"),
      "baseUrl" -> Json.fromString("https://www.googleapis.com/"),
      "limits" -> Json.obj("connectTimeoutMillis" -> Json.fromInt(1000), "readTimeoutMillis" -> Json.fromInt(2000), "responseBytes" -> Json.fromInt(4096)),
      "grants" -> Json.arr(Json.obj("componentId" -> Json.fromString(KnowledgeLakeComponent.name),
        "origins" -> Json.arr(Json.fromString("https://www.googleapis.com")), "methods" -> Json.arr(Json.fromString("GET")))))
    Files.writeString(configfile, _replace(config, "textus.subsystem.http-execution.plan", Json.fromString(plan.noSpaces)).noSpaces, StandardCharsets.UTF_8)
    if (state == "absent") Map.empty
    else {
      val owner = if (state == "wrong-owner") foreign else principal
      val client = Json.obj("providerId" -> Json.fromString("google-drive"),
        "authorizationEndpoint" -> Json.fromString("https://accounts.google.com/o/oauth2/v2/auth"),
        "tokenEndpoint" -> Json.fromString("https://oauth2.googleapis.com/token"), "clientId" -> Json.fromString("registered-private-client"),
        "redirectUri" -> Json.fromString(_redirect), "requestedScopes" -> Json.fromValues(_scopes.map(Json.fromString)))
      val operator = root.resolve("operator.json")
      _write_private(operator, Json.obj("schema" -> Json.fromString("cncf.oauth-connection/1"),
        "connectionId" -> Json.fromString(_connection_id), "ownerId" -> Json.fromString(owner), "provider" -> Json.fromString("google-drive"),
        "clientId" -> Json.fromString("registered-private-client"), "clientSecret" -> Json.fromString("registered-private-secret"),
        "redirectUri" -> Json.fromString(_redirect), "scopes" -> Json.fromValues(_scopes.map(Json.fromString)),
        "credentialFile" -> Json.fromString(root.resolve("grant.json").toString), "returnPath" -> Json.fromString("/web/nict-editing-studio")))
      if (state != "disconnected") _write_private(root.resolve("grant.json"), Json.obj(
        "schema" -> Json.fromString("cncf.oauth-credential/1"), "connectionId" -> Json.fromString(_connection_id),
        "ownerId" -> Json.fromString(owner), "client" -> client, "accessToken" -> Json.fromString(token),
        "grantedScopes" -> Json.fromValues(_scopes.map(Json.fromString)), "expiresAt" -> Json.fromString("2099-10-21T00:00:00Z")))
      Map(OAuthConnectionRuntime.configurationKey.name -> operator.toUri.toString)
    }
  }
}
