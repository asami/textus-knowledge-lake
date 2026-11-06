package org.simplemodeling.textus.knowledgelake.material

import io.circe.Json
import scala.util.Using
import org.goldenport.cncf.http.HttpDriver
import org.goldenport.http.{HttpResponse, HttpStatus}
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/* Native PostgreSQL plus controlled provider: no real Google/device acceptance.
 * @since   Oct. 25, 2026
 * @version Nov.  6, 2026
 */
final class MaterialDeliveryOperationsSpec extends AnyWordSpec with Matchers with GivenWhenThen with MaterialRegistrationPostgresqlSpec {
  import MaterialRegistrationPersistenceFixture.*
  import MaterialDeliveryOperationsFixture.*
  import MaterialRegistrationOperationsFixture.{HttpPeer, Fault, prepare, pngs, contribution}

  "Native mobile Material delivery" should {
    "persist one delivery, upload originals, register and resolve it after reopening" in {
      Given("durable PostgreSQL, configured native TKL and controlled Google FileStore")
      withDatabase(native_container) { database =>
        prepareDatabase(database)
        val peer = new CheckpointPeer(database, new HttpPeer(database))
        val retained = withSession(database, parameters(), prepare = prepare()) { session =>
          When("one committed request is prepared twice through the generated public Operation")
          val first = record(session.query("prepare", fields, driver = Some(peer), service = "MaterialDelivery"))
          val second = record(session.query("prepare", fields, driver = Some(peer), service = "MaterialDelivery"))
          Then("native claim/load preserves one reference and one complete document without physical IO")
          first shouldBe second
          documents(database) should have size 1
          peer.delegate.effects shouldBe empty
          database.snapshot().rows shouldBe empty
          Given("the retained prepared delivery reference and its two pending originals")
          val reference = first.getString("deliveryRef").get
          first.getString("state") shouldBe Some("prepared")
          first.getString("materialRef") shouldBe None
          When("both original resources are uploaded through the native delivery Operation")
          val uploaded = (0 to 1).map(index => record(session.query("uploadOriginal", upload(reference, index),
            driver = Some(peer), service = "MaterialDelivery")))
          Then("the delivery is ready to register")
          uploaded.last.getString("state") shouldBe Some("readyToRegister")
          When("the complete delivery is finalized")
          val registered = record(session.query("finalize", Vector("deliveryRef" -> reference), driver = Some(peer), service = "MaterialDelivery"))
          Then("one Material is registered with a distinct reference and durable checkpoints")
          registered.getString("state") shouldBe Some("registered")
          val materialref = registered.getString("materialRef").get
          materialref should not be reference
          database.snapshot().rows should have size 1
          peer.observed should not be empty
          peer.observed.flatten.foreach(_.isDefined shouldBe true)
          Given("the registered delivery and its current provider effect count")
          val count = peer.delegate.effects.size
          When("the same delivery is finalized again")
          val repeated = record(session.query("finalize", Vector("deliveryRef" -> reference), driver = Some(peer), service = "MaterialDelivery"))
          Then("the same registration is returned without another provider effect")
          repeated shouldBe registered
          peer.delegate.effects.size shouldBe count
          (reference, materialref, count)
        }
        Given("a newly bootstrapped component without current delivery configuration on the same native datastore")
        withSession(database, parameters(None), prepare = prepare()) { session =>
          When("normal delivery and Material APIs inspect the retained registration")
          val status = record(session.query("get", Vector("deliveryRef" -> retained._1), driver = Some(peer), service = "MaterialDelivery"))
          val metadata = record(session.query("getMaterial", Vector("materialRef" -> retained._2), driver = Some(peer)))
          val original = take(session.query("readOriginal", Vector("materialRef" -> retained._2, "resourceId" -> "detail"), driver = Some(peer)))
          Then("the same registered reference and original bytes remain ordinarily accessible")
          status.getString("state") shouldBe Some("registered")
          status.getString("materialRef") shouldBe Some(retained._2)
          metadata.getString("title") shouldBe contribution.hcursor.get[String]("title").toOption
          val media = original match {
            case org.goldenport.protocol.operation.OperationResponse.Http(value) => value
            case _ => fail("Expected native binary original response")
          }
          media shouldBe a[HttpResponse.Binary]
          Using.resource(media.bag.openInputStream())(_.readAllBytes().toVector) shouldBe pngs(1)
          peer.delegate.confirmed.filterNot(_.collection).filter(_.media == "image/png").map(_.bytes) shouldBe pngs
        }
      }
    }

    "resume a confirmed partial upload with its retained destination after configuration changes" in {
      Given("one prepared delivery with its first original durably confirmed")
      withDatabase(native_container) { database =>
        prepareDatabase(database)
        val peer = new CheckpointPeer(database, new HttpPeer(database))
        val reference = withSession(database, parameters(), prepare = prepare()) { session =>
          val prepared = record(session.query("prepare", fields, driver = Some(peer), service = "MaterialDelivery"))
          val reference = prepared.getString("deliveryRef").get
          record(session.query("uploadOriginal", upload(reference, 0), driver = Some(peer), service = "MaterialDelivery"))
          reference
        }
        val before = peer.delegate.confirmed.filterNot(_.collection).count(_.media == "image/png")
        Given("a new runtime with a different current physical destination")
        withSession(database, parameters(Some(configuration("new-binding", "OtherSelectedParent"))), prepare = prepare()) { session =>
          When("the same request resumes, uploads its remaining original and finalizes")
          val duplicate = record(session.query("prepare", fields, driver = Some(peer), service = "MaterialDelivery"))
          duplicate.getString("deliveryRef") shouldBe Some(reference)
          duplicate.getString("destinationId") shouldBe Some(binding)
          record(session.query("uploadOriginal", upload(reference, 1), driver = Some(peer), service = "MaterialDelivery"))
          val registered = record(session.query("finalize", Vector("deliveryRef" -> reference), driver = Some(peer), service = "MaterialDelivery"))
          Then("the previous original is retained, one remaining original is added and no folder is silently retargeted")
          before shouldBe 1
          peer.delegate.confirmed.filterNot(_.collection).count(_.media == "image/png") shouldBe 2
          peer.delegate.confirmed.filter(_.collection).exists(_.parent == "OtherSelectedParent") shouldBe false
          registered.getString("destinationId") shouldBe Some(binding)
          registered.getString("state") shouldBe Some("registered")
        }
      }
    }

    "retain an uncertain provider write and forbid automatic duplicate creation" in {
      Given("a controlled provider that creates the first original but loses its PUT acknowledgement")
      withDatabase(native_container) { database =>
        prepareDatabase(database)
        val peer = new CheckpointPeer(database, new HttpPeer(database,
          fault = Some(Fault(3, MaterialReferenceFixture.jsonResponse(Json.obj(), 503), after = true))))
        withSession(database, parameters(), prepare = prepare()) { session =>
          val reference = record(session.query("prepare", fields, driver = Some(peer), service = "MaterialDelivery")).getString("deliveryRef").get
          When("the upload returns an uncertain failure and status is queried before another attempt")
          session.query("uploadOriginal", upload(reference, 0), driver = Some(peer), service = "MaterialDelivery").isSuccess shouldBe false
          val before = peer.delegate.effects.size
          val status = record(session.query("get", Vector("deliveryRef" -> reference), driver = Some(peer), service = "MaterialDelivery"))
          Then("reconciliationRequired preserves the remote bytes without a fabricated upload or Material receipt")
          status.getString("state") shouldBe Some("reconciliationRequired")
          status.getString("materialRef") shouldBe None
          peer.delegate.confirmed.filterNot(_.collection).map(_.bytes) shouldBe Vector(pngs.head)
          Given("the retained uncertain upload and its provider effect count before retry")
          When("the same original upload is attempted again")
          session.query("uploadOriginal", upload(reference, 0), driver = Some(peer), service = "MaterialDelivery") /* Then the uncertain retry fails. */ .isSuccess shouldBe false
          Then("no further provider effect or Material row is created")
          peer.delegate.effects.size shouldBe before
          database.snapshot().rows shouldBe empty
        }
      }
    }

    "isolate caller correlation and reject changed committed metadata" in {
      Given("two actual native fixture principals using the same client request ID")
      withDatabase(native_container) { database =>
        prepareDatabase(database)
        withSession(database, parameters(), prepare = prepare("absent")) { session =>
          val first = record(session.query("prepare", fields, service = "MaterialDelivery"))
          val reference = first.getString("deliveryRef").get
          When("the other caller searches its correlation and attempts a known foreign reference")
          record(session.query("findForRequest", Vector("requestId" -> requestId), session = Some("foreign-session"), service = "MaterialDelivery"))
            .getAsC[Boolean]("found").TAKE shouldBe Some(false)
          session.query("get", Vector("deliveryRef" -> reference), session = Some("foreign-session"), service = "MaterialDelivery").isSuccess shouldBe false
          val second = record(session.query("prepare", fields, session = Some("foreign-session"), service = "MaterialDelivery"))
          Then("native ownership yields distinct deliveries and conflicting committed metadata never overwrites the first")
          second.getString("deliveryRef") should not be Some(reference)
          val changed = fields.map { case (name, value) => name -> (if (name == "contribution")
            MaterialRegistrationOperationsFixture.replace(contribution, "title", Json.fromString("Changed")) else value) }
          session.query("prepare", changed, service = "MaterialDelivery").isSuccess shouldBe false
          documents(database) should have size 2
          database.snapshot().rows shouldBe empty
        }
      }
    }
  }
}
