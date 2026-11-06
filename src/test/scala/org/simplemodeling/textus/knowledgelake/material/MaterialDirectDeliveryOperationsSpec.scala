package org.simplemodeling.textus.knowledgelake.material

import io.circe.Json
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/* Real isolated PostgreSQL and controlled provider; no live Google proof.
 * @since Oct. 25, 2026
 * @version Nov.  6, 2026
 */
final class MaterialDirectDeliveryOperationsSpec extends AnyWordSpec with Matchers with GivenWhenThen with MaterialRegistrationPostgresqlSpec {
  import MaterialRegistrationPersistenceFixture.*
  import MaterialDeliveryOperationsFixture.*
  import MaterialRegistrationOperationsFixture.{HttpPeer, prepare, pngs, integrityContribution}

  "Direct mobile Material delivery" should {
    "retain a direct route and private sessions, confirm provider results and register after restarting" in {
      Given("native TKL with direct-preferred Google capability and a controlled provider")
      withDatabase(native_container) { database =>
        prepareDatabase(database)
        val peer = new HttpPeer(database)
        val config = configuration().mapObject(_.add("preferDirect", Json.True))
        val retained = withSession(database, parameters(Some(config)), prepare = prepare()) { session =>
          val selected = record(session.query("getDestination", Vector.empty, driver = Some(peer), service = "MaterialDelivery"))
          selected.getString("transferMethod") shouldBe Some("googleDriveResumable")
          peer.effects shouldBe empty
          val delivery = record(session.query("prepare", Vector("requestId" -> requestId, "contribution" -> integrityContribution), driver = Some(peer), service = "MaterialDelivery"))
          val reference = delivery.getString("deliveryRef").get
          // This assertion grounds RecordEncoder spelling only; HTTP adapter spelling is a separate ingress/output contract.
          When("the native prepared delivery is encoded through the checked Record serializer")
          val wire = io.circe.parser.parse(org.goldenport.record.io.RecordEncoder.jsonC(delivery).TAKE)
            .fold(error => fail("Native delivery JSON did not parse: " + error.message), value => value)
          Then("the safe delivery fields and nested digest objects retain their exact declared native spelling")
          wire.asObject.get.keys.toSet shouldBe Set("deliveryRef", "requestId", "revision", "destinationId",
            "destinationLabel", "state", "resources", "transferMethod")
          wire.hcursor.get[String]("deliveryRef").toOption shouldBe Some(reference)
          wire.hcursor.get[String]("transferMethod").toOption shouldBe Some("googleDriveResumable")
          val resources = wire.hcursor.get[Vector[Json]]("resources")
            .fold(error => fail("Native delivery resources did not decode: " + error.message), value => value)
          resources should have size 2
          resources.zipWithIndex.foreach { case (resource, index) =>
            resource.asObject.get.keys.toSet shouldBe Set("resourceId", "mediaType", "byteLength", "state",
              "contentDigest", "verificationState", "verificationAttempts")
            resource.hcursor.downField("contentDigest").focus shouldBe Some(Json.obj(
              "algorithm" -> Json.fromString("sha256"),
              "value" -> Json.fromString(MaterialRegistrationOperationsFixture.sha256(pngs(index)))))
          }
          When("the first original's direct instruction is prepared twice")
          var trace = ""
          val prepared = record(session.query("prepareOriginalTransfer", Vector("deliveryRef" -> reference, "resourceId" -> "cover"),
            driver = Some(peer), service = "MaterialDelivery", observeTrace = Some(value => trace = value)))
          val instruction = prepared.getAsC[Record]("instruction").TAKE.get
          val repeated = record(session.query("prepareOriginalTransfer", Vector("deliveryRef" -> reference, "resourceId" -> "cover"),
            driver = Some(peer), service = "MaterialDelivery"))
          Then("one retained session is reused without TKL receiving image bytes")
          repeated.getAsC[Record]("instruction").TAKE.get shouldBe instruction
          trace should not be empty
          trace should not include instruction.getString("uploadSessionUri").get
          trace should not include "upload_id="
          trace should not include MaterialRegistrationOperationsFixture.token
          trace should not include "operations-private-secret"
          peer.effects.filter(_.method == "PUT").forall(_.bytes.isEmpty) shouldBe true
          val result = Json.obj("outcome" -> Json.fromString("uploaded"), "byteLength" -> Json.fromInt(pngs.head.size))
          val notification = Vector("deliveryRef" -> reference, "resourceId" -> "cover",
            "transferRef" -> instruction.getString("transferRef").get, "result" -> result)
          When("the client reports completion before the reserved bytes exist")
          val pending = session.query("confirmOriginalTransfer", notification, driver = Some(peer), service = "MaterialDelivery")
          Then("a known Pending result cannot confirm the original")
          pending.isSuccess shouldBe false
          When("the client sends the original bytes and reports completion again")
          peer.uploadFromMobile(instruction.getString("uploadSessionUri").get, pngs.head)
          val confirmed = record(session.query("confirmOriginalTransfer", notification, driver = Some(peer), service = "MaterialDelivery"))
          Then("the actual reserved original retains one verified private checkpoint")
          confirmed.print should not include "upload_id="
          documents(database).head.hcursor.downField("transfers").focus.get.asArray.get should have size 1
          val checkpoint = documents(database).head
          checkpoint.hcursor.downField("registration").downField("contribution").focus shouldBe Some(integrityContribution)
          checkpoint.hcursor.downField("transfers").downArray.downField("fileReference").get[String]("key").toOption should not be empty
          checkpoint.hcursor.downField("verifications").downArray.get[String]("state").toOption shouldBe Some("verified")
          reference
        }
        Given("a restarted deterministic episode with a distinct seed and a changed current policy selecting server transfer")
        val firstcheckpoint = documents(database).head
        withSession(database, parameters(), prepare = prepare(), executionSeed = "tkl-native-registration-restart") { session =>
          When("the existing delivery resumes its second original through its retained route")
          val current = record(session.query("get", Vector("deliveryRef" -> retained), driver = Some(peer), service = "MaterialDelivery"))
          Then("the current policy cannot replace the direct route")
          current.getString("transferMethod") shouldBe Some("googleDriveResumable")
          When("the client attempts mediated ingress for that direct delivery")
          val mediated = session.query("uploadOriginal", upload(retained, 1), driver = Some(peer), service = "MaterialDelivery")
          Then("the incompatible ingress fails")
          mediated.isSuccess shouldBe false
          When("the restarted episode prepares the second original through the retained direct route")
          val prepared = record(session.query("prepareOriginalTransfer", Vector("deliveryRef" -> retained, "resourceId" -> "detail"),
            driver = Some(peer), service = "MaterialDelivery"))
          val instruction = prepared.getAsC[Record]("instruction").TAKE.get
          Then("the second reservation has distinct transfer and file references while preserving the first original's proof and route")
          val checkpoint = documents(database).head
          val transfers = checkpoint.hcursor.get[Vector[Json]]("transfers").toOption.get
          transfers should have size 2
          transfers.map(_.hcursor.get[String]("transferRef").toOption.get).distinct should have size 2
          transfers.map(_.hcursor.downField("fileReference").focus.get).distinct should have size 2
          transfers.head shouldBe firstcheckpoint.hcursor.get[Vector[Json]]("transfers").toOption.get.head
          checkpoint.hcursor.get[Vector[Json]]("verifications").toOption.get.head shouldBe
            firstcheckpoint.hcursor.get[Vector[Json]]("verifications").toOption.get.head
          checkpoint.hcursor.get[String]("transferMethod").toOption shouldBe Some("googleDriveResumable")
          When("the client uploads the second original and recovers its lost local completion acknowledgement")
          // Simulate a client whose upload response is lost before its local checkpoint.
          peer.uploadFromMobile(instruction.getString("uploadSessionUri").get, pngs(1))
          val recovered = record(session.query("prepareOriginalTransfer", Vector("deliveryRef" -> retained, "resourceId" -> "detail"),
            driver = Some(peer), service = "MaterialDelivery"))
          Then("the actual completed transfer needs no further upload instruction")
          recovered.getAsC[Record]("instruction").TAKE shouldBe None
          When("the owner finalizes both retained original proofs")
          val registered = record(session.query("finalize", Vector("deliveryRef" -> retained), driver = Some(peer), service = "MaterialDelivery"))
          Then("status reconciliation keeps both direct uploads and only then returns a real registered Material reference")
          registered.getString("state") shouldBe Some("registered")
          registered.getString("materialRef") should not be empty
          val retainedproofs = documents(database).head.hcursor.get[Vector[Json]]("verifications").toOption.get
          retainedproofs.map(_.hcursor.get[String]("state").toOption.get) shouldBe Vector("verified", "verified")
          peer.mobileUploads should have size 2
          peer.effects.filter(_.method == "PUT").filter(_.headers.contains("Content-Range")).forall(_.bytes.isEmpty) shouldBe true
          peer.confirmed.filter(_.media == "image/png").map(_.bytes) shouldBe pngs
          database.snapshot().rows should have size 1
        }
      }
    }
  }
}
