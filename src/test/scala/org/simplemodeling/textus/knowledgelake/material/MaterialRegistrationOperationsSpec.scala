package org.simplemodeling.textus.knowledgelake.material

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.time.Instant
import scala.util.Using
import io.circe.{Json, parser}
import org.goldenport.{Conclusion, Consequence, ConsequenceException}
import org.goldenport.cncf.context.IdGenerationContext
import org.goldenport.cncf.entity.EntitySnapshot
import org.goldenport.convert.ValueReader
import org.goldenport.http.HttpResponse
import org.goldenport.observation.{Cause, Descriptor}
import org.goldenport.protocol.Request
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.goldenport.test.matchers.ConclusionMatchers
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.model.datatype.{EntityCollectionId, EntityId}
import org.simplemodeling.model.statemachine.PostStatus
import org.simplemodeling.textus.knowledgelake.{value => dto}

/*
 * Public generated dispatch, required PostgreSQL and controlled native provider programs.
 * Synthetic provider evidence is distinct from actual Google/App acceptance.
 *
 * @since   Oct. 22, 2026
 * @version Oct. 22, 2026
 */
final class MaterialRegistrationOperationsSpec extends AnyWordSpec with Matchers with GivenWhenThen
    with ConclusionMatchers with MaterialRegistrationPostgresqlSpec {
  import MaterialRegistrationPersistenceFixture.*
  import MaterialRegistrationOperationsFixture.*

  "Public Material registration" should {
    "preserve native identity complete input and acknowledged lifecycle" which {
      "create Prepared without credentials or physical effects and inspect it after reopening without host bindings" in {
        Given("required durable PostgreSQL and registered mode with explicit source configuration but no OAuth or read binding")
        withDatabase(native_container) { database =>
          database.durability shouldBe ("on" -> "on")
          val retained = withSession(database, parameters(readBindings = false), prepare = prepare("absent")) { session =>
            val peer = new HttpPeer(database)
            val beforecustody = custody(session.root)
            val sourcebaseline = sources
            When("the generated create Operation receives one complete neutral contribution")
            val record = _record(session.query("createMaterialRegistration", createFields(), driver = Some(peer)))
            val result = take(dto.MaterialRegistrationResult.createC(record))
            val identity = take(EntityId.parse(result.registrationId))
            val physical = database.snapshot()
            Then("native issuance commits one privately owned Prepared row and returns exactly four logical fields")
            _expect_result(record, result.registrationId, 1L, "prepared", result.materialRef)
            identity.collection shouldBe MaterialRegistrationEntity.collectionId
            result.registrationId should not be result.materialRef
            result.registrationId should not be "cover"
            result.registrationId should not be "detail"
            sourceKeys.foreach(sourceid => result.registrationId should not be sourceid)
            result.materialRef should startWith(s"urn:textus:klake:$lake:material:$logicalParent/")
            physical.rows should have size 1
            physical.writes shouldBe Vector(result.registrationId -> "INSERT")
            val row = physical.rows.head
            row("owner_id") shouldBe Some(principal)
            row("created_by") shouldBe Some(principal)
            take(summon[ValueReader[PostStatus]].readC(row("post_status").get)) shouldBe PostStatus.Published
            parser.parse(row("permission").get).toOption shouldBe parser.parse(ownerPermission).toOption
            val document = _document(row)
            document.hcursor.downField("contribution").focus shouldBe Some(contribution)
            document.hcursor.get[String]("owner").toOption shouldBe Some(principal)
            document.hcursor.get[String]("createdBy").toOption shouldBe Some(principal)
            document.hcursor.get[String]("storageBinding").toOption shouldBe Some(binding)
            document.hcursor.get[String]("lake").toOption shouldBe Some(lake)
            document.hcursor.get[String]("logicalPath").toOption.get.split('/').dropRight(1).mkString("/") shouldBe logicalParent
            peer.effects shouldBe empty
            peer.confirmed shouldBe empty
            sources shouldBe sourcebaseline
            custody(session.root) shouldBe beforecustody

            Given("the committed Prepared reference and its complete SQL and custody baseline")
            When("normal generated status search and exact metadata requests inspect that planned reference")
            val status = _record(session.query("getMaterialRegistration", selector(result.registrationId), driver = Some(peer)))
            val search = _record(session.query("searchMaterials", driver = Some(peer)))
            val metadata = session.query("getMaterial", Vector("materialRef" -> result.materialRef), driver = Some(peer))
            Then("status is authoritative while ordinary Material access excludes Prepared without writes")
            status shouldBe record
            take(search.getAsC[Vector[Record]]("items")).get shouldBe empty
            _expect_conclusion(failure(metadata), failure(Consequence.resourceNotFound[Unit]("Material was not found")), "MaterialRegistrationSupport.scala")
            database.snapshot() shouldBe physical
            peer.effects shouldBe empty
            custody(session.root) shouldBe beforecustody
            (result, physical)
          }

          Given("a fresh component and interpreter with registered mode alone on the same real database")
          withSession(database, parameters(None, readBindings = false), prepare = prepare("absent")) { session =>
            val peer = new HttpPeer(database)
            val beforecustody = custody(session.root)
            When("the public status Operation reloads the actual native identity without registration OAuth or read configuration")
            val status = _record(session.query("getMaterialRegistration", selector(retained._1.registrationId), driver = Some(peer)))
            Then("the original committed Prepared identity revision and planned reference survive without provider or SQL mutations")
            _expect_result(status, retained._1.registrationId, 1L, "prepared", retained._1.materialRef)
            database.snapshot() shouldBe retained._2
            peer.effects shouldBe empty
            peer.confirmed shouldBe empty
            custody(session.root) shouldBe beforecustody
          }
        }
      }

      "materialize through native custody after durable revision two and read the exact second saved original after reopening" in {
        Given("durable PostgreSQL complete distinct PNG originals one configured connection and synthetic private drive-scope custody")
        withDatabase(native_container) { database =>
          val peer = new HttpPeer(database)
          val retained = withSession(database, parameters(), prepare = prepare()) { session =>
            val beforecustody = custody(session.root)
            val sourcebaseline = sources
            val prepared = _create(session, peer)
            val before = database.snapshot()
            pngs(0) should not be pngs(1)
            When("the generated materialize Operation selects the native Prepared identity")
            val response = _record(session.query("materializeMaterial", selector(prepared.registrationId), driver = Some(peer)))
            val physical = database.snapshot()
            val native = take(session.get(take(EntityId.parse(prepared.registrationId))))
            Then("the same identity reaches actual revision three only after the native INSERT UPDATE UPDATE sequence")
            _expect_result(response, prepared.registrationId, 3L, "registered", prepared.materialRef)
            physical.writes shouldBe Vector(prepared.registrationId -> "INSERT", prepared.registrationId -> "UPDATE", prepared.registrationId -> "UPDATE")
            native.entity.document.contribution.json shouldBe contribution
            native.revision.value shouldBe 3L
            physical.rows.head("state") shouldBe Some("registered")
            physical.rows.head(revisionName) shouldBe Some("3")
            _document(physical.rows.head) shouldBe native.entity.document.json
            Vector("owner_id", "created_by", "post_status", "permission").foreach(key => physical.rows.head(key) shouldBe before.rows.head(key))
            _expect_materializing_effects(peer, prepared.registrationId)
            peer.effects.map(_.headers("Authorization")).distinct shouldBe Vector(s"Bearer $token")
            peer.effects.filter(_.method == "GET").map(_.path) shouldBe sourceKeys.flatMap(key => Vector(statUrl(key), mediaUrl(key)))
            peer.effects.map(_.path).foreach(path => path should not include "untrusted")
            _expect_package(peer, native)
            sources shouldBe sourcebaseline
            custody(session.root) shouldBe beforecustody
            Vector("operator.json", "grant.json").foreach(name =>
              Files.getPosixFilePermissions(session.root.resolve(name)) shouldBe PosixFilePermissions.fromString("rw-------"))
            peer.releaseObservations()

            Given("the confirmed Registered mapping and the complete SQL source and custody baselines")
            val effectcount = peer.effects.size
            When("generated search metadata status and second-original reads resolve the registered logical Material")
            val page = _record(session.query("searchMaterials", driver = Some(peer)))
            val metadata = _record(session.query("getMaterial", Vector("materialRef" -> prepared.materialRef), driver = Some(peer)))
            val status = _record(session.query("getMaterialRegistration", selector(prepared.registrationId), driver = Some(peer)))
            val media = _response(session.query("readOriginal", Vector("materialRef" -> prepared.materialRef, "resourceId" -> "detail"), driver = Some(peer)))
            val bytes = Using.resource(media.bag.openInputStream())(_.readAllBytes().toVector)
            Then("logical results omit private mappings and the exact acknowledged second file supplies unchanged PNG bytes and safe headers")
            take(page.getAsC[Vector[Record]]("items")).get.map(_.getString("materialRef")) shouldBe Vector(Some(prepared.materialRef))
            metadata.getString("materialRef") shouldBe Some(prepared.materialRef)
            metadata.getString("title") shouldBe contribution.hcursor.get[String]("title").toOption
            _expect_result(status, prepared.registrationId, 3L, "registered", prepared.materialRef)
            bytes shouldBe pngs(1)
            media shouldBe a[HttpResponse.Binary]
            media.bag shouldBe a[org.goldenport.bag.BinaryBag]
            media.mime.value shouldBe "image/png"
            media.headerValue("Cache-Control") shouldBe Some("private, no-store")
            media.headerValue("X-Content-Type-Options") shouldBe Some("nosniff")
            media.headerValue("Content-Security-Policy") shouldBe Some("default-src 'none'; sandbox")
            media.headerValue("Content-Disposition") shouldBe Some("attachment; filename=\"original\"")
            val secondfile = native.entity.document.stored.get.files(1)
            peer.effects.drop(effectcount).map(_.path) shouldBe Vector(statUrl(secondfile.reference.key), mediaUrl(secondfile.reference.key))
            Vector(token, binding, "untrusted-source-owner", secondfile.reference.key, sourceParent).foreach(marker => metadata.print should not include marker)
            database.snapshot() shouldBe physical
            custody(session.root) shouldBe beforecustody
            sources shouldBe sourcebaseline
            peer.releaseObservations()
            (prepared, physical, native.entity.document.json)
          }

          Given("a new owning component and native interpreter on the same database with fresh synthetic custody")
          withSession(database, parameters(), prepare = prepare()) { session =>
            val beforecustody = custody(session.root)
            val effectcount = peer.effects.size
            val confirmed = peer.confirmed
            When("status and the same second original are requested after reinitialization")
            val status = _record(session.query("getMaterialRegistration", selector(retained._1.registrationId), driver = Some(peer)))
            val media = _response(session.query("readOriginal", Vector("materialRef" -> retained._1.materialRef, "resourceId" -> "detail"), driver = Some(peer)))
            val bytes = Using.resource(media.bag.openInputStream())(_.readAllBytes().toVector)
            val loaded = take(session.get(take(EntityId.parse(retained._1.registrationId))))
            Then("the persisted mapping and actual native status survive with only two GETs and no replay or native write")
            _expect_result(status, retained._1.registrationId, 3L, "registered", retained._1.materialRef)
            loaded.entity.document.json shouldBe retained._3
            bytes shouldBe pngs(1)
            peer.effects.drop(effectcount).map(_.method) shouldBe Vector("GET", "GET")
            peer.confirmed shouldBe confirmed
            database.snapshot() shouldBe retained._2
            custody(session.root) shouldBe beforecustody
            peer.releaseObservations()
            _no_replay(session, peer, retained._1, "registered", 3L)
          }
        }
      }
    }

    "admit explicitly shared parents and an empty original vector" which {
      "materialize an empty contribution without a source read through the same admitted destination writer" in {
        Given("an explicit shared source/destination parent and a complete neutral contribution with empty original and document vectors")
        val selected = replace(configuration, "destinationParentKey", Json.fromString(sourceParent))
        val document = replace(replace(contribution, "originals", Json.arr()), "documents", Json.arr())
        withDatabase(native_container) { database => withSession(database, parameters(Some(selected.noSpaces)), prepare = prepare()) { session =>
          val peer = new HttpPeer(database)
          val beforecustody = custody(session.root)
          val sourcebaseline = sources
          When("generated create and materialize dispatch retain the empty source while using the normal managed writer")
          val prepared = take(dto.MaterialRegistrationResult.createC(_record(session.query("createMaterialRegistration",
            createFields(document), driver = Some(peer)))))
          val registered = _record(session.query("materializeMaterial", selector(prepared.registrationId), driver = Some(peer)))
          Then("the native lifecycle still reaches Registered and writes only root derived and manifest with no source GET")
          _expect_result(registered, prepared.registrationId, 3L, "registered", prepared.materialRef)
          _expect_state(database.snapshot(), prepared.registrationId, "registered", 3L)
          _document(database.snapshot().rows.head).hcursor.downField("contribution").focus shouldBe Some(document)
          peer.effects.map(_.method) shouldBe Vector("POST", "POST", "POST", "PUT")
          peer.effects.foreach(effect => _expect_state(effect.before, prepared.registrationId, "materializing", 2L))
          peer.confirmed.head.parent shouldBe sourceParent
          peer.confirmed.map(_.label).tail shouldBe Vector("derived", "material.json")
          val manifest = parser.parse(new String(peer.confirmed.last.bytes.toArray, StandardCharsets.UTF_8)).toOption.get
          manifest.hcursor.downField("assets").focus shouldBe Some(Json.arr())
          manifest.hcursor.get[String]("id").toOption shouldBe Some(prepared.materialRef)
          sources shouldBe sourcebaseline
          custody(session.root) shouldBe beforecustody
          peer.releaseObservations()
        } }
      }
    }

    "admit caller mode and owning configuration in order" which {
      "refuse every anonymous Operation before native persistence or provider effects" in {
        Given("a configured registered component and a native Prepared identity with unchanged source and custody")
        withDatabase(native_container) { database => withSession(database, parameters(), prepare = prepare()) { session =>
          val peer = new HttpPeer(database)
          val prepared = _create(session, peer)
          Vector("createMaterialRegistration" -> createFields(), "getMaterialRegistration" -> selector(prepared.registrationId),
            "materializeMaterial" -> selector(prepared.registrationId)).foreach { case (operation, fields) =>
            val request = Request.of(component = session.component.name, service = "Material", operation = operation)
            _expect_no_effect(session, operation, fields, None,
              Consequence.securityAuthenticationRequired[Unit](s"Authenticated user is required: ${request.name}"), "ActionCall.scala")
          }
        } }
      }

      "refuse configured-subject exclusion before source admission database creation or provider use" in {
        Given("a valid configuration admitting only another exact subject and malformed source provenance")
        val selected = replace(configuration, "allowedSubjects", Json.arr(Json.fromString(foreign)))
        withDatabase(native_container) { database => withSession(database, parameters(Some(selected.noSpaces)), prepare = prepare()) { session =>
          val native = take(session.create(take(MaterialContribution.parseC(contribution))))
          val expected = Consequence.securityPermissionDenied[Unit]("Material registration caller is not admitted")
          _expect_no_effect(session, "createMaterialRegistration", createFields(changedSource("receipt", "providerId", Json.fromString("private-marker"))),
            Some("owner-session"), expected, "MaterialServiceFactory.scala")
          _expect_no_effect(session, "materializeMaterial", selector(native.entity.id.value), Some("owner-session"), expected,
            "MaterialServiceFactory.scala")
        } }
      }

      Vector("missing", "bootstrap").foreach { mode =>
        s"leave $mode registration commands explicitly unavailable without changing status independence" in {
          Given(s"a native Prepared registration and $mode host registration capability")
          val selected = if (mode == "bootstrap") Map.empty[String, String] else parameters(None, readBindings = false)
          withDatabase(native_container) { database => withSession(database, selected, prepare = prepare("absent")) { session =>
            val prepared = take(session.create(take(MaterialContribution.parseC(contribution))))
            val expected = Consequence.serviceUnavailable[Unit](if (mode == "bootstrap") "Material registration requires registered mode"
              else "Material registration is unavailable")
            _expect_no_effect(session, "createMaterialRegistration", createFields(), Some("owner-session"), expected, "MaterialServiceFactory.scala")
            _expect_no_effect(session, "materializeMaterial", selector(prepared.entity.id.value), Some("owner-session"), expected, "MaterialServiceFactory.scala")
            if (mode == "bootstrap") _expect_no_effect(session, "getMaterialRegistration", selector(prepared.entity.id.value),
              Some("owner-session"), expected, "MaterialServiceFactory.scala")
            else {
              val peer = new HttpPeer(database)
              val before = database.snapshot()
              When("public status resolves the native Prepared identity without any registration or OAuth configuration")
              val status = _record(session.query("getMaterialRegistration", selector(prepared.entity.id.value), driver = Some(peer)))
              Then("the actual snapshot is available with no physical or native mutation")
              _expect_result(status, prepared.entity.id.value, 1L, "prepared", prepared.entity.document.location.urn)
              database.snapshot() shouldBe before
              peer.effects shouldBe empty
            }
          } }
        }
      }

      "keep exact native read and write authorization before credential or physical execution" in {
        Given("normal owned and foreign Prepared identities plus missing wrong-collection and read-denied selectors")
        withDatabase(native_container) { database => withSession(database, parameters(), prepare = prepare()) { session =>
          val peer = new HttpPeer(database)
          val prepared = _create(session, peer)
          val identity = take(EntityId.parse(prepared.registrationId))
          val issuer = IdGenerationContext.deterministic(IdGenerationContext.DEFAULT_NAMESPACE, "public-registration-selectors")
          val missing = issuer.entityId(MaterialRegistrationEntity.collectionId)
          val wrong = issuer.entityId(EntityCollectionId("textus", "knowledge_lake", "Other"))
          Vector("getMaterialRegistration", "materializeMaterial").foreach { operation =>
            Vector((identity, Some("foreign-session")), (missing, Some("owner-session")), (wrong, Some("owner-session"))).foreach {
              case (selected, caller) =>
                val expected = session.get(selected, caller)
                _expect_no_effect(session, operation, selector(selected.value), caller, expected, _source_file(failure(expected)))
            }
            val malformed = EntityId.parse("not-a-native-registration-id")
            _expect_no_effect(session, operation, selector("not-a-native-registration-id"), Some("owner-session"), malformed,
              _source_file(failure(malformed)))
          }
          Given("the actual owner whose native row explicitly refuses reads")
          database.security(identity, permission = readDeniedPermission)
          Vector("getMaterialRegistration", "materializeMaterial").foreach { operation =>
            val expected = session.get(identity)
            _expect_no_effect(session, operation, selector(identity.value), Some("owner-session"), expected, _source_file(failure(expected)))
          }
          Given("the actual owner with normal read-only native permission")
          database.security(identity, permission = readOnlyPermission)
          _expect_no_effect(session, "materializeMaterial", selector(identity.value), Some("owner-session"),
            Consequence.securityPermissionDenied[Unit]("Owner permission is insufficient for update.", Cause.Kind.Permission,
              Seq(Descriptor.Facet.Reason("owner"), Descriptor.Facet.Permission("owner"),
                Descriptor.Facet.Parameter.argument("access-kind"), Descriptor.Facet.Value("update"), Descriptor.Facet.Id(principal))),
            "OperationAccessPolicy.scala")
        } }
      }

      Vector("absent", "disconnected", "wrong-owner", "unknown").foreach { state =>
        s"retain exact $state managed custody failure without a provider effect" in {
          Given(s"a normally created Prepared identity with $state synthetic owning-TKL custody")
          val selected = if (state == "unknown") replace(configuration, "connectionId", Json.fromString("unknown-connection")) else configuration
          withDatabase(native_container) { database => withSession(database, parameters(Some(selected.noSpaces)), prepare = prepare(state)) { session =>
            val peer = new HttpPeer(database)
            val prepared = _create(session, peer)
            val expected = state match {
              case "absent" => Consequence.serviceUnavailable[Unit]("Material registration connection is unavailable")
              case "unknown" => Consequence.resourceNotFound[Unit]("OAuth connection is not configured")
              case "wrong-owner" => Consequence.securityPermissionDenied[Unit]("OAuth connection owner is not allowed")
              case _ => Consequence.securityPermissionDenied[Unit]("OAuth connection requires authorization")
            }
            if (state == "absent") _expect_no_effect(session, "materializeMaterial", selector(prepared.registrationId),
              Some("owner-session"), expected, "MaterialServiceFactory.scala")
            else {
              val beforecustody = custody(session.root)
              val sourcebaseline = sources
              When("public materialization reaches ordinary native custody after its durable acknowledged transition")
              val result = session.query("materializeMaterial", selector(prepared.registrationId), driver = Some(peer))
              Then("the original native custody Conclusion survives without provider execution and committed Materializing is retained")
              _expect_conclusion(failure(result), failure(expected), if (state == "unknown") "UnitOfWorkInterpreter.scala" else "OAuthConnection.scala")
              _expect_state(database.snapshot(), prepared.registrationId, "materializing", 2L)
              peer.effects shouldBe empty
              peer.confirmed shouldBe empty
              custody(session.root) shouldBe beforecustody
              sources shouldBe sourcebaseline
              _no_replay(session, peer, prepared, "materializing", 2L)
            }
          } }
        }
      }
    }

    "admit the complete strict configuration before requests" which {
      "retain exact valid values with a redacted display and allow a shared source and destination parent" in {
        Given("explicit bounded configuration strings including preserved subjects and an explicitly shared physical parent")
        withDatabase(native_container) { database =>
          Vector(configuration, replace(configuration, "logicalParent", Json.fromString("")),
            replace(configuration, "allowedSubjects", Json.arr(Json.fromString(" exact subject "), Json.fromString(principal))),
            replace(configuration, "destinationParentKey", Json.fromString(sourceParent))).foreach { document =>
            val before = database.snapshot()
            When("normal component initialization admits this exact registered host configuration")
            withSession(database, parameters(Some(document.noSpaces)), prepare = prepare("absent")) { session =>
              Then("admission is pure preserves literal values and exposes only a redacted diagnostic")
              val admitted = session.component._material_registration_configuration.get
              admitted.toString shouldBe "MaterialRegistrationConfiguration(<redacted>)"
              admitted.logicalparent shouldBe document.hcursor.get[String]("logicalParent").toOption.get
              admitted.allowedsubjects shouldBe document.hcursor.get[Vector[String]]("allowedSubjects").toOption.get
              admitted.sourceparentkey shouldBe document.hcursor.get[String]("sourceParentKey").toOption.get
              admitted.destinationparentkey shouldBe document.hcursor.get[String]("destinationParentKey").toOption.get
              database.snapshot() shouldBe before
              custody(session.root) shouldBe empty
            }
          }
        }
      }

      "reject duplicate semantic fields unknown missing malformed unbounded and bootstrap configurations without echoing values" in {
        Given("an unchanged empty database and strict invalid configuration cases including escaped semantic duplicates")
        val valid = configuration
        val fields = valid.asObject.get.keys.toVector
        val invalid = Vector("{private-marker", "null", "[]", " " * 131073,
          replace(valid, "unknown", Json.fromString("private-marker")).noSpaces,
          replace(valid, "schema", Json.fromString("private-schema-marker")).noSpaces,
          replace(valid, "sourceFormat", Json.fromString("private-format-marker")).noSpaces,
          valid.noSpaces.replace("\"schema\":", "\"schema\":\"private-marker\",\"schema\":"),
          valid.noSpaces.replace("\"id\":", "\"\\u0069d\":\"private-marker\",\"id\":")) ++
          fields.map(key => valid.mapObject(_.remove(key)).noSpaces) ++ fields.map(key => replace(valid, key, Json.Null).noSpaces) ++
          Vector("id", "connectionId", "storeName", "lake").flatMap(key => Vector("", "private marker", "/private-marker", "x" * 129)
            .map(value => replace(valid, key, Json.fromString(value)).noSpaces)) ++
          Vector("sourceParentKey", "destinationParentKey").flatMap(key => Vector("", "private/path", "private marker", "x" * 257)
            .map(value => replace(valid, key, Json.fromString(value)).noSpaces)) ++
          Vector("/private-marker", "private//path", "private/../path", "private\\path", "private?path", "x" * 1025)
            .map(value => replace(valid, "logicalParent", Json.fromString(value)).noSpaces) ++
          Vector(Json.arr(), Json.arr(Json.fromString(principal), Json.fromString(principal)), Json.arr(Json.fromString("*")),
            Json.arr(Json.fromString("*private-marker")), Json.arr(Json.fromString("private*marker")), Json.arr(Json.fromString("private-marker*")),
            Json.arr(Json.fromString(" ")), Json.arr(Json.fromString("private\u0000marker")), Json.arr(Json.fromString("x" * 257)),
            Json.arr(Json.fromInt(1)), Json.obj()).map(value => replace(valid, "allowedSubjects", value).noSpaces)
        val variants = invalid.map(text => parameters(Some(text), readBindings = false)) ++ Vector(
          Map(MaterialRegistrationConfiguration.configurationKey.name -> valid.noSpaces),
          Map(MaterialReferenceRuntime.modeKey.name -> "bootstrap", MaterialRegistrationConfiguration.configurationKey.name -> valid.noSpaces))
        withDatabase(native_container) { database =>
          val before = database.snapshot()
          variants.foreach { selected =>
            var entered = false
            When("the component initializes with this supplied owning registration configuration")
            val rejected = intercept[ConsequenceException] { withSession(database, selected) { _ => entered = true } }
            Then("fixed structured configuration failure precedes any request body or SQL mutation and echoes no original marker")
            val conclusion = failure(rejected.consequence)
            val expected = if (selected.get(MaterialReferenceRuntime.modeKey.name).contains("registered"))
              Consequence.configurationInvalid[Unit]("Material registration configuration is invalid")
            else Consequence.configurationInvalid[Unit]("Bootstrap Material reference mode cannot include registration configuration")
            _expect_conclusion(conclusion, failure(expected), if (selected.get(MaterialReferenceRuntime.modeKey.name).contains("registered"))
              "MaterialRegistrationConfiguration.scala" else "MaterialRegistrationRuntime.scala")
            Vector("private-marker", "private-schema-marker", "private-format-marker", token).foreach(marker => rejected.consequence.display should not include marker)
            entered shouldBe false
            database.snapshot() shouldBe before
          }
        }
      }
    }

    "validate exact source and retained destination without rebinding" which {
      Vector(("original", "id", Json.fromString("private-mismatch")), ("original", "mediaType", Json.fromString("text/plain")),
        ("original", "byteLength", Json.fromLong(1L)), ("receipt", "providerId", Json.fromString("private-provider")),
        ("receipt", "remoteId", Json.fromString("private/path")), ("receipt", "remoteId", Json.fromString("x" * 257)),
        ("receipt", "remoteId", Json.Null)).zipWithIndex.foreach { case ((section, key, value), index) =>
        s"reject source correspondence case $index before native creation or custody" in {
          Given("a complete neutral contribution whose retained original source declaration fails exact correspondence")
          withDatabase(native_container) { database => withSession(database, parameters(), prepare = prepare()) { session =>
            _expect_no_effect(session, "createMaterialRegistration", createFields(changedSource(section, key, value)),
              Some("owner-session"), Consequence.argumentInvalid[Unit]("Material original source reference is invalid"), "MaterialRegistrationSource.scala")
          } }
        }
      }

      Vector("id", "lake", "logicalParent").foreach { key =>
        s"reject a changed configured $key while retaining the original Prepared destination" in {
          Given("a normally created native Prepared document retained across a changed owning-host destination configuration")
          withDatabase(native_container) { database =>
            val retained = withSession(database, parameters(), prepare = prepare()) { session =>
              val peer = new HttpPeer(database)
              _create(session, peer) -> database.snapshot()
            }
            val selected = replace(configuration, key, Json.fromString("other-destination"))
            withSession(database, parameters(Some(selected.noSpaces)), prepare = prepare()) { session =>
              _expect_no_effect(session, "materializeMaterial", selector(retained._1.registrationId), Some("owner-session"),
                Consequence.resourceInvalid[Unit]("Material registration does not match its configured destination"), "MaterialServiceFactory.scala")
              database.snapshot() shouldBe retained._2
            }
          }
        }
      }

      "repeat pure source admission for an existing native contribution before custody" in {
        Given("a normal native Prepared contribution with invalid retained source receipt and no OAuth configuration")
        withDatabase(native_container) { database => withSession(database, parameters(), prepare = prepare("absent")) { session =>
          val native = take(session.create(take(MaterialContribution.parseC(changedSource("receipt", "providerId", Json.fromString("other"))))))
          _expect_no_effect(session, "materializeMaterial", selector(native.entity.id.value), Some("owner-session"),
            Consequence.argumentInvalid[Unit]("Material original source reference is invalid"), "MaterialRegistrationSource.scala")
        } }
      }
    }

    "retain durable progress on exact source or provider failures" which {
      Vector("id", "parent", "kind", "mime", "length", "unknown-length", "malformed").foreach { mismatch =>
        s"stop $mismatch stat failure before source bytes without replay or deletion" in {
          Given(s"a public Prepared registration and first-source $mismatch stat response")
          val stat = sourceMetadata(sourceKeys.head)
          val changed = mismatch match {
            case "id" => replace(stat, "id", Json.fromString("WrongExactSource"))
            case "parent" => replace(stat, "parents", Json.arr(Json.fromString("OtherParent")))
            case "kind" => replace(stat, "mimeType", Json.fromString("application/vnd.google-apps.folder"))
            case "mime" => replace(stat, "mimeType", Json.fromString("text/html"))
            case "length" => replace(stat, "size", Json.fromString((pngs.head.size + 1).toString))
            case "unknown-length" => stat.mapObject(_.remove("size"))
            case _ => Json.obj("private" -> Json.fromString(token))
          }
          val native = Set("id", "malformed")(mismatch)
          _source_failure(Some(changed), None, None, if (native) Consequence.resourceInvalid[Unit]("Google Drive file metadata is invalid")
            else Consequence.resourceInvalid[Unit]("Material original source does not match its configured resource"),
            if (native) "GoogleDriveFileStore.scala" else "MaterialRegistrationSource.scala", 3)
        }
      }
      Vector("mime", "length").foreach { mismatch =>
        s"stop $mismatch source content failure before copying any original" in {
          Given(s"a public Prepared registration and first-source content with changed $mismatch")
          val response = if (mismatch == "mime") MaterialReferenceFixture.mediaResponse(mediaType = "text/html", content = pngs.head)
            else MaterialReferenceFixture.mediaResponse(content = pngs.head.dropRight(1))
          _source_failure(None, Some(response), None,
            Consequence.resourceInvalid[Unit]("Material original source does not match its configured resource"), "MaterialRegistrationSource.scala", 4)
        }
      }
      Vector(401, 403, 404, 429, 503).foreach { status =>
        Vector(2, 3).foreach { index =>
          s"preserve exact native status $status on source effect $index without retry" in {
            Given(s"a public Prepared registration whose exact source request at index $index responds $status")
            val response = MaterialReferenceFixture.jsonResponse(Json.obj("private" -> Json.fromString(token)), status)
            val expected = status match {
              case 401 | 403 => Consequence.securityPermissionDenied[Unit]("Google Drive file access was denied")
              case 404 => Consequence.resourceNotFound[Unit]("Google Drive file was not found")
              case _ => Consequence.serviceUnavailable[Unit]("Google Drive file service is unavailable")
            }
            _source_failure(None, None, Some(Fault(index, response)), expected, "GoogleDriveFileStore.scala", index + 1)
          }
        }
      }
    }

    "preserve native update acknowledgement and nonPrepared ordering" which {
      Vector((1, true), (2, false), (2, true)).foreach { case (update, aftercommit) =>
        s"retain actual saved status for update $update afterCommit $aftercommit without physical replay" in {
          Given("a public Prepared registration and the native SQL commit hook with an exact selected acknowledgement failure")
          withDatabase(native_container) { database =>
            val peer = new HttpPeer(database)
            val retained = withSession(database, parameters(), prepare = prepare()) { session =>
              val prepared = _create(session, peer)
              val beforecustody = custody(session.root)
              val sourcebaseline = sources
              session.store.failAfter(update, afterCommit = aftercommit)
              When("the generated materialize Operation reaches that actual native observed update")
              val result = session.query("materializeMaterial", selector(prepared.registrationId), driver = Some(peer))
              val physical = database.snapshot()
              val state = if (update == 2 && aftercommit) "registered" else "materializing"
              val revision = if (state == "registered") 3L else 2L
              Then("the same native failure instance survives while independent SQL and public status report actual durability")
              failure(result) should be theSameInstanceAs failure(if (aftercommit) session.store.unknown else session.store.rejected)
              _expect_conclusion(failure(result), failure(if (aftercommit) session.store.unknown else session.store.rejected),
                "EntityVersionedMutation.scala")
              _expect_state(physical, prepared.registrationId, state, revision)
              if (update == 1) { peer.effects shouldBe empty; peer.confirmed shouldBe empty }
              else {
                peer.confirmed.map(_.label) should contain("material.json")
                _expect_materializing_effects(peer, prepared.registrationId)
                peer.confirmed.find(_.label == "detail").get.bytes shouldBe pngs(1)
                peer.releaseObservations()
              }
              custody(session.root) shouldBe beforecustody
              sources shouldBe sourcebaseline
              _no_replay(session, peer, prepared, state, revision)
              (prepared, physical, state, revision)
            }

            Given("a new registered runtime on the same database without registration configuration or custody")
            withSession(database, parameters(None, readBindings = false), prepare = prepare("absent")) { session =>
              _no_replay(session, peer, retained._1, retained._3, retained._4)
              database.snapshot() shouldBe retained._2
            }
          }
        }
      }
    }
  }

  private def _record(result: Consequence[OperationResponse]): Record = MaterialReferenceFixture.record(result)
  private def _response(result: Consequence[OperationResponse]): HttpResponse = take(result) match {
    case OperationResponse.Http(response) => response
    case _ => fail("A binary original response is required")
  }
  private def _document(row: Map[String, Option[String]]): Json = parser.parse(row("document").get).toOption.get
  private def _create(session: Session, peer: HttpPeer): dto.MaterialRegistrationResult = {
    When("normal generated create dispatch admits the complete source and commits the native Prepared identity")
    val record = _record(session.query("createMaterialRegistration", createFields(), driver = Some(peer)))
    val result = take(dto.MaterialRegistrationResult.createC(record))
    Then("creation acknowledges actual revision one without any provider effect")
    _expect_result(record, result.registrationId, 1L, "prepared", result.materialRef)
    peer.effects shouldBe empty
    result
  }
  private def _expect_result(record: Record, identity: String, revision: Long, state: String, reference: String): Unit =
    record shouldBe dto.MaterialRegistrationResult(identity, revision, state, reference).toRecord()

  private def _expect_state(physical: PhysicalState, identity: String, state: String, revision: Long): Unit = {
    physical.rows should have size 1
    physical.rows.head("id") shouldBe Some(identity)
    physical.rows.head("state") shouldBe Some(state)
    physical.rows.head(revisionName) shouldBe Some(revision.toString)
    physical.writes shouldBe Vector(identity -> "INSERT") ++ Vector.fill((revision - 1).toInt)(identity -> "UPDATE")
    _document(physical.rows.head).hcursor.get[String]("status").toOption shouldBe Some(state)
    if (state == "materializing") _document(physical.rows.head).hcursor.downField("stored").focus shouldBe None
  }
  private def _expect_materializing_effects(peer: HttpPeer, identity: String): Unit = {
    peer.effects should not be empty
    peer.effects.foreach { effect =>
      _expect_state(effect.before, identity, "materializing", 2L)
      _document(effect.before.rows.head).hcursor.downField("contribution").focus shouldBe Some(contribution)
    }
  }
  private def _expect_package(peer: HttpPeer, native: EntitySnapshot[MaterialRegistrationRecord]): Unit = {
    val stored = native.entity.document.stored.get
    val leaf = native.entity.document.location.logicalPath.split('/').last
    val root = peer.confirmed.find(_.label == leaf).get
    val assets = peer.confirmed.find(_.label == "assets").get
    val derived = peer.confirmed.find(_.label == "derived").get
    root.parent shouldBe destinationParent
    assets.parent shouldBe root.key
    derived.parent shouldBe root.key
    stored.root.key shouldBe root.key
    stored.root.store shouldBe storeName
    stored.files.map(_.path) shouldBe Vector("assets/cover", "assets/detail", "derived/review.json", "derived/empty.json")
    Vector("cover", "detail").zipWithIndex.foreach { case (label, index) =>
      val actual = peer.confirmed.find(_.label == label).get
      actual.bytes shouldBe pngs(index)
      actual.media shouldBe "image/png"
      actual.parent shouldBe assets.key
      stored.files(index).reference.key shouldBe actual.key
      stored.files(index).reference.store shouldBe storeName
      stored.files(index).parent.key shouldBe assets.key
    }
    contribution.hcursor.downField("documents").focus.get.asArray.get.zipWithIndex.foreach { case (document, index) =>
      val label = document.hcursor.get[String]("resourceId").toOption.get + ".json"
      val actual = peer.confirmed.find(_.label == label).get
      parser.parse(new String(actual.bytes.toArray, StandardCharsets.UTF_8)).toOption shouldBe document.hcursor.downField("body").focus
      actual.media shouldBe "application/json"
      actual.parent shouldBe derived.key
      stored.files(index + 2).reference.key shouldBe actual.key
      stored.files(index + 2).parent.key shouldBe derived.key
    }
    val manifest = peer.confirmed.find(_.label == "material.json").get
    manifest.parent shouldBe root.key
    stored.manifest.key shouldBe manifest.key
    val json = parser.parse(new String(manifest.bytes.toArray, StandardCharsets.UTF_8)).toOption.get
    json.hcursor.get[String]("id").toOption shouldBe Some(native.entity.document.location.urn)
    json.hcursor.get[String]("schema").toOption shouldBe Some("textus.material/1")
    json.hcursor.downField("provenance").focus shouldBe contribution.hcursor.downField("provenance").focus
    json.hcursor.downField("assets").focus.get.asArray.get.map(_.hcursor.get[String]("path").toOption.get) shouldBe stored.files.map(_.path)
    val expectedassets = Vector("cover", "detail").map(id => Json.obj("path" -> Json.fromString("assets/" + id),
      "role" -> Json.fromString("original"), "mediaType" -> Json.fromString("image/png"))) ++ Vector(
      Json.obj("path" -> Json.fromString("derived/review.json"), "role" -> Json.fromString("review-history"),
        "mediaType" -> Json.fromString("application/json"),
        "derivedFrom" -> Json.arr(Json.fromString("assets/detail"), Json.fromString("assets/cover"))),
      Json.obj("path" -> Json.fromString("derived/empty.json"), "role" -> Json.fromString("retained-null"),
        "mediaType" -> Json.fromString("application/json"), "derivedFrom" -> Json.arr()))
    json shouldBe Json.obj("schema" -> Json.fromString("textus.material/1"),
      "id" -> Json.fromString(native.entity.document.location.urn),
      "title" -> contribution.hcursor.downField("title").focus.get,
      "createdAt" -> contribution.hcursor.downField("createdAt").focus.get,
      "classifications" -> contribution.hcursor.downField("classifications").focus.get,
      "provenance" -> contribution.hcursor.downField("provenance").focus.get,
      "owner" -> Json.fromString(principal), "createdBy" -> Json.fromString(principal), "assets" -> Json.fromValues(expectedassets))
    peer.confirmed.last shouldBe manifest
    peer.effects.filter(_.method == "PUT").map(_.bytes) shouldBe peer.confirmed.filterNot(_.collection).map(_.bytes)
    peer.effects.filter(_.method == "POST").flatMap(_.request).map(_.hcursor.get[String]("name").toOption.get) shouldBe
      Vector(leaf, "assets", "cover", "detail", "derived", "review.json", "empty.json", "material.json")
  }
  private def _source_file(conclusion: Conclusion): String = conclusion.observation.cause.descriptor.facets.collectFirst {
    case source: Descriptor.Facet.SrcPos => source.pos.file
  }.getOrElse(fail("A native failure source location is required"))
  private def _expect_conclusion(actual: Conclusion, expected: Conclusion, originfile: String): Unit = {
    actual.observation.timestamp.isAfter(Instant.EPOCH) shouldBe true
    val sources = actual.observation.cause.descriptor.facets.collect { case source: Descriptor.Facet.SrcPos => source }
    sources should have size 1
    val source = sources.head
    source.pos.file should endWith(originfile)
    source.pos.line should be > 0
    source.pos.column should be > 0
    val facets = expected.observation.cause.descriptor.facets.filterNot(_.isInstanceOf[Descriptor.Facet.SrcPos]) :+ source
    val aligned = expected.copy(observation = expected.observation.copy(cause = expected.observation.cause.copy(descriptor = Descriptor(facets))))
    actual should be_equal_conclusion(aligned)
    actual.observation.callTree shouldBe expected.observation.callTree
  }
  private def _expect_no_effect(session: Session, operation: String, fields: Vector[(String, Any)], caller: Option[String],
    expected: Consequence[?], originfile: String): Unit = {
    Given("complete independently committed SQL audit source physical destination and custody baselines")
    val before = session.database.snapshot()
    val beforecustody = custody(session.root)
    val sourcebaseline = sources
    val peer = new HttpPeer(session.database)
    When("the generated Operation interprets that exact request under the actual native caller")
    val result = session.query(operation, fields, session = caller, driver = Some(peer))
    Then("the structured refusal precedes provider effects and every retained baseline remains unchanged")
    _expect_conclusion(failure(result), failure(expected), originfile)
    session.database.snapshot() shouldBe before
    peer.effects shouldBe empty
    peer.confirmed shouldBe empty
    sources shouldBe sourcebaseline
    custody(session.root) shouldBe beforecustody
  }
  private def _no_replay(session: Session, peer: HttpPeer, prepared: dto.MaterialRegistrationResult, state: String, revision: Long): Unit = {
    Given("the actual retained nonPrepared row and complete native provider source and custody baselines")
    val before = session.database.snapshot()
    val beforecustody = custody(session.root)
    val effects = peer.effects
    val confirmed = peer.confirmed
    val sourcebaseline = sources
    When("public status inspects saved progress and public materialization is explicitly requested again")
    val status = _record(session.query("getMaterialRegistration", selector(prepared.registrationId), driver = Some(peer)))
    val repeated = session.query("materializeMaterial", selector(prepared.registrationId), driver = Some(peer))
    Then("actual state and revision survive and native notPrepared conflict precedes configuration custody or physical replay")
    _expect_result(status, prepared.registrationId, revision, state, prepared.materialRef)
    _expect_conclusion(failure(repeated), failure(Consequence.operationConflict[Unit]("materializeMaterial",
      Vector(Descriptor.Facet.Reason("registration-not-prepared")))), "MaterialRegistration.scala")
    session.database.snapshot() shouldBe before
    peer.effects shouldBe effects
    peer.confirmed shouldBe confirmed
    custody(session.root) shouldBe beforecustody
    sources shouldBe sourcebaseline
  }
  private def _source_failure(stat: Option[Json], content: Option[HttpResponse], fault: Option[Fault], expected: Consequence[?],
    originfile: String, count: Int): Unit = withDatabase(native_container) { database =>
    val peer = new HttpPeer(database, fault, stat, content)
    val retained = withSession(database, parameters(), prepare = prepare()) { session =>
      val prepared = _create(session, peer)
      val beforecustody = custody(session.root)
      val sourcebaseline = sources
      When("generated materialization reaches the selected source or provider response after acknowledged native Materializing")
      val result = session.query("materializeMaterial", selector(prepared.registrationId), driver = Some(peer))
      val physical = database.snapshot()
      Then("the exact Conclusion survives with bounded provider effects retained collections and no original copy or rollback")
      _expect_conclusion(failure(result), failure(expected), originfile)
      _expect_state(physical, prepared.registrationId, "materializing", 2L)
      _expect_materializing_effects(peer, prepared.registrationId)
      peer.effects should have size count
      peer.effects.map(_.method) shouldBe Vector("POST", "POST", "GET", "GET").take(count)
      peer.effects.drop(2).map(_.path) shouldBe Vector(statUrl(sourceKeys.head), mediaUrl(sourceKeys.head)).take(count - 2)
      peer.effects.map(_.headers("Authorization")).distinct shouldBe Vector(s"Bearer $token")
      peer.confirmed.map(_.collection) shouldBe Vector(true, true)
      peer.confirmed.map(_.parent).head shouldBe destinationParent
      sources shouldBe sourcebaseline
      custody(session.root) shouldBe beforecustody
      peer.releaseObservations()
      _no_replay(session, peer, prepared, "materializing", 2L)
      prepared -> physical
    }
    Given("a new component on the failed registration with no host registration or OAuth configuration")
    withSession(database, parameters(None, readBindings = false), prepare = prepare("absent")) { session =>
      _no_replay(session, peer, retained._1, "materializing", 2L)
      database.snapshot() shouldBe retained._2
    }
  }
}
