package org.simplemodeling.textus.knowledgelake.material

import io.circe.{Json, Printer, parser}
import scala.util.Using
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import org.simplemodeling.textus.knowledgelake.KnowledgeLakeComponent
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Real PostgreSQL and controlled provider observations; no live provider writes.
 * @since Nov.  9, 2026
 * @version Nov.  9, 2026
 */
@org.scalatest.tags.Slow
final class MaterialDeliveryReconciliationOperationsSpec extends AnyWordSpec with Matchers with GivenWhenThen with MaterialRegistrationPostgresqlSpec {
  import MaterialRegistrationPersistenceFixture.*
  import MaterialDeliveryOperationsFixture.*
  import MaterialRegistrationOperationsFixture.{Fault, HttpPeer, prepare, replace}

  private def _get_only(root: Path): Map[String, String] = {
    val result = prepare()(root)
    val file = root.resolve(".textus/config.json")
    val current = parser.parse(Files.readString(file, StandardCharsets.UTF_8)).toOption.get
    val plan = Json.obj("schema" -> Json.fromString("cncf.runtime-http-execution.v1"),
      "baseUrl" -> Json.fromString("https://www.googleapis.com/"), "limits" -> Json.obj(
        "connectTimeoutMillis" -> Json.fromInt(1000), "readTimeoutMillis" -> Json.fromInt(2000), "responseBytes" -> Json.fromInt(65536)),
      "grants" -> Json.arr(Json.obj("componentId" -> Json.fromString(KnowledgeLakeComponent.name),
        "origins" -> Json.arr(Json.fromString("https://www.googleapis.com")), "methods" -> Json.arr(Json.fromString("GET")))))
    Files.writeString(file, replace(current, "textus.subsystem.http-execution.plan", Json.fromString(plan.noSpaces)).noSpaces,
      StandardCharsets.UTF_8)
    result
  }

  private def _replace_document(database: Database, document: Json): Unit = database.connection { connection =>
    Using.resource(connection.prepareStatement("UPDATE " + quote(deliveryTable) + " SET document = ?")) { statement =>
      statement.setString(1, Printer.noSpaces.copy(escapeNonAscii = true).print(document))
      statement.executeUpdate() shouldBe 1
    }
  }

  private def _revision(database: Database): Long = database.connection { connection =>
    Using.resource(connection.createStatement()) { statement =>
      Using.resource(statement.executeQuery("SELECT " + quote(revisionName) + " FROM " + quote(deliveryTable))) { rows =>
        rows.next() shouldBe true
        rows.getLong(1)
      }
    }
  }

  "Collection reconciliation" should {
    "reject every mismatched durable binding before provider access or transition" in {
      Given("an isolated test-owned PostgreSQL delivery with a retained unknown native reservation")
      withDatabase(native_container) { database =>
        prepareDatabase(database)
        val peer = new CheckpointPeer(database, new HttpPeer(database,
          fault = Some(Fault(2, MaterialReferenceFixture.jsonResponse(Json.obj(), 503), after = true))))
        withSession(database, parameters(), prepare = prepare()) { session =>
          val reference = record(session.query("prepare", fields, driver = Some(peer), service = "MaterialDelivery")).getString("deliveryRef").get
          session.query("uploadOriginal", upload(reference, 0), driver = Some(peer), service = "MaterialDelivery").isSuccess shouldBe false
          val original = documents(database).head
          val checkpoint = original.hcursor.downField("collectionCheckpoint").focus.get
          val parent = checkpoint.hcursor.downField("parent").focus.get
          val current = _revision(database)
          val effects = peer.delegate.effects.size
          val variants: Vector[(String, Json => Json)] = Vector(
            "caller" -> ((value: Json) => replace(value, "caller", Json.fromString("different-owner"))),
            "parent" -> ((value: Json) => replace(value, "parent", Json.obj("store" -> parent.hcursor.downField("store").focus.get, "key" -> Json.fromString("different-parent")))),
            "name" -> ((value: Json) => replace(value, "name", Json.fromString("different-name"))),
            "slot" -> ((value: Json) => replace(value, "slot", Json.fromString("assets"))),
            "operation" -> ((value: Json) => replace(value, "operation", Json.fromString("different-delivery:collection:root"))),
            "reference" -> ((value: Json) => replace(value, "reference", parent)),
            "storage root" -> ((value: Json) => replace(value, "storageRoot", Json.obj("store" -> Json.fromString("different-store"), "key" -> Json.fromString("root")))))
          When("the retained checkpoint is corrupted for each native observation state")
          for {
            state <- Vector("unknown", "notDispatched", "confirmed")
            (label, change) <- variants
          } {
            val changed = replace(change(checkpoint), "observation", Json.fromString(state))
            val candidate = replace(original, "collectionCheckpoint", changed)
            _replace_document(database, candidate)
            withClue(label + " / " + state + ": ") {
              if (label == "operation") MaterialDeliveryDocument.parseC(candidate).isSuccess shouldBe true
              session.query("reconcile", Vector("deliveryRef" -> reference, "revision" -> current), driver = Some(peer), service = "MaterialDelivery").isSuccess shouldBe false
              documents(database).head shouldBe candidate
              _revision(database) shouldBe current
              peer.delegate.effects.size shouldBe effects
            }
          }
          Then("no mismatched checkpoint can be inspected or consumed")
          Vector("unknown", "notDispatched", "confirmed").foreach { state =>
            val changed = replace(checkpoint, "observation", Json.fromString(state))
            val busy = replace(replace(original, "collectionCheckpoint", changed), "busy", Json.fromString("collection:assets"))
            _replace_document(database, busy)
            session.query("reconcile", Vector("deliveryRef" -> reference, "revision" -> current), driver = Some(peer), service = "MaterialDelivery").isSuccess shouldBe false
            documents(database).head shouldBe busy
            _revision(database) shouldBe current
            peer.delegate.effects.size shouldBe effects
            Given("a syntactically valid reservation colliding with an existing package identity")
            val collision = replace(replace(original, "collectionCheckpoint", changed), "manifest", checkpoint.hcursor.downField("reference").focus.get)
            MaterialDeliveryDocument.parseC(collision).isSuccess shouldBe true
            _replace_document(database, collision)
            When("the owner attempts reconciliation")
            session.query("reconcile", Vector("deliveryRef" -> reference, "revision" -> current), driver = Some(peer), service = "MaterialDelivery").isSuccess shouldBe false
            Then("the package collision causes no provider or persistence effects")
            documents(database).head shouldBe collision
            _revision(database) shouldBe current
            peer.delegate.effects.size shouldBe effects
          }
        }
      }
    }

    "decode a legacy checkpoint without inventing a reservation" in {
      Given("a previously persisted delivery document before collection observation existed")
      withDatabase(native_container) { database =>
        prepareDatabase(database)
        withSession(database, parameters(), prepare = prepare()) { session =>
          record(session.query("prepare", fields, service = "MaterialDelivery"))
          val current = documents(database).head
          val legacy = current.mapObject(_.remove("collectionCheckpoint").add("busy", Json.fromString("collection:root")))
          When("the old shape is restored")
          val restored = MaterialDeliveryDocument.parseC(legacy)
          Then("it retains the busy state without manufacturing a native reservation")
          restored.isSuccess shouldBe true
          restored.TAKE.busy shouldBe Some("collection:root")
          restored.TAKE.collectionCheckpoint shouldBe None
        }
      }
    }

    "release only an actual native pre-dispatch refusal at the current owner revision" in {
      Given("a native host policy allowing reservation reads but refusing the collection POST")
      withDatabase(native_container) { database =>
        prepareDatabase(database)
        val peer = new CheckpointPeer(database, new HttpPeer(database))
        withSession(database, parameters(), prepare = _get_only) { session =>
          val reference = record(session.query("prepare", fields, driver = Some(peer), service = "MaterialDelivery")).getString("deliveryRef").get
          When("upload reaches the native pre-dispatch admission boundary")
          session.query("uploadOriginal", upload(reference, 0), driver = Some(peer), service = "MaterialDelivery").isSuccess shouldBe false
          val status = record(session.query("get", Vector("deliveryRef" -> reference), service = "MaterialDelivery"))
          Then("the refusal is durably distinguished from an HTTP response and no POST reaches the provider")
          documents(database).head.hcursor.downField("collectionCheckpoint").get[String]("observation").toOption shouldBe Some("notDispatched")
          peer.delegate.effects.count(_.method == "POST") shouldBe 0
          val revision = status.getAsC[Long]("revision").TAKE.get
          When("the authenticated owner reconciles that exact revision")
          val result = record(session.query("reconcile", Vector("deliveryRef" -> reference, "revision" -> revision), service = "MaterialDelivery"))
          Then("the checkpoint is released without a provider create")
          result.getString("state") shouldBe Some("prepared")
          peer.delegate.effects.count(_.method == "POST") shouldBe 0
        }
      }
    }

    "keep an absent or incomplete reserved-identity lookup blocked" in {
      Given("a possibly successful create whose independent identity lookup is absent or incomplete")
      Vector(404, 200).foreach { code =>
      withDatabase(native_container) { database =>
        prepareDatabase(database)
        val peer = new CheckpointPeer(database, new HttpPeer(database,
          fault = Some(Fault(2, MaterialReferenceFixture.jsonResponse(Json.obj(), 503), after = true))),
          getOverride = path => Option.when(path.contains("/files/ReservedResource_"))(MaterialReferenceFixture.jsonResponse(Json.obj(), code)))
        withSession(database, parameters(), prepare = prepare()) { session =>
          val reference = record(session.query("prepare", fields, driver = Some(peer), service = "MaterialDelivery")).getString("deliveryRef").get
          session.query("uploadOriginal", upload(reference, 0), driver = Some(peer), service = "MaterialDelivery").isSuccess shouldBe false
          val status = record(session.query("get", Vector("deliveryRef" -> reference), service = "MaterialDelivery"))
          val revision = status.getAsC[Long]("revision").TAKE.get
          val creates = peer.delegate.confirmed.count(_.collection)
          When("the owner asks for reconciliation of the current revision")
          val result = record(session.query("reconcile", Vector("deliveryRef" -> reference, "revision" -> revision), driver = Some(peer), service = "MaterialDelivery"))
          Then("the lookup is not treated as evidence of non-dispatch or a reason to create again")
          result.getString("state") shouldBe Some("reconciliationRequired")
          documents(database).head.hcursor.downField("collectionCheckpoint").get[String]("observation").toOption shouldBe Some("unknown")
          peer.delegate.confirmed.count(_.collection) shouldBe creates
        }
      }
      }
    }

    "retain an uncertain create and resolve only its reserved identity after reopen" in {
      Given("a provider that creates the reserved root but returns an unknown response")
      withDatabase(native_container) { database =>
        prepareDatabase(database)
        val peer = new CheckpointPeer(database, new HttpPeer(database,
          fault = Some(Fault(2, MaterialReferenceFixture.jsonResponse(Json.obj(), 503), after = true))))
        val retained = withSession(database, parameters(), prepare = prepare()) { session =>
          val prepared = record(session.query("prepare", fields, driver = Some(peer), service = "MaterialDelivery"))
          val reference = prepared.getString("deliveryRef").get
          When("upload attempts the reserved root creation")
          session.query("uploadOriginal", upload(reference, 0), driver = Some(peer), service = "MaterialDelivery").isSuccess shouldBe false
          val status = record(session.query("get", Vector("deliveryRef" -> reference), driver = Some(peer), service = "MaterialDelivery"))
          Then("the native checkpoint and root reservation survive without replay")
          status.getString("state") shouldBe Some("reconciliationRequired")
          documents(database).head.hcursor.downField("collectionCheckpoint").get[String]("observation").toOption shouldBe Some("unknown")
          peer.delegate.confirmed.count(_.collection) shouldBe 1
          val revision = status.getAsC[Long]("revision").TAKE.get
          val count = peer.delegate.effects.size
          session.query("reconcile", Vector("deliveryRef" -> reference, "revision" -> (revision - 1)), driver = Some(peer), service = "MaterialDelivery").isSuccess shouldBe false
          session.query("reconcile", Vector("deliveryRef" -> reference, "revision" -> revision), session = Some("foreign-session"), driver = Some(peer), service = "MaterialDelivery").isSuccess shouldBe false
          peer.delegate.effects.size shouldBe count
          (reference, revision, count)
        }
        Given("the same database reopened by the actual owner")
        withSession(database, parameters(), prepare = prepare()) { session =>
          When("the owner reconciles the exact current revision")
          val result = record(session.query("reconcile", Vector("deliveryRef" -> retained._1, "revision" -> retained._2), driver = Some(peer), service = "MaterialDelivery"))
          Then("the original reserved root is retained without another creation")
          result.getString("state") shouldBe Some("prepared")
          peer.delegate.confirmed.count(_.collection) shouldBe 1
          peer.delegate.effects.drop(retained._3).forall(_.method == "GET") shouldBe true
          documents(database).head.hcursor.downField("collectionCheckpoint").focus shouldBe Some(Json.Null)
        }
      }
    }
  }
}
