package org.simplemodeling.textus.knowledgelake.material

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Real native PostgreSQL and synthetic HTTP provider/Drive fixtures only.
  * Real composed UserAccount/Studio and cloud delivery require later evidence.
  * @since   Nov.  6, 2026
  * @version Nov.  6, 2026
  */
@org.scalatest.tags.Slow
final class MaterialParticipantOperationsSpec extends AnyWordSpec with Matchers with GivenWhenThen
    with MaterialRegistrationPostgresqlSpec {
  import MaterialRegistrationPersistenceFixture.*
  import MaterialDeliveryOperationsFixture.*
  import MaterialRegistrationOperationsFixture.{HttpPeer, prepare}

  "Opt-in participant Material operations" should {
    "preserve two server-owned users and refuse foreign delivery/read/finalization before provider effects" in {
      withDatabase(native_container) { database =>
        prepareDatabase(database)
        val settings = parameters() + (MaterialParticipantConfiguration.configurationKey.name -> MaterialParticipantFixture.configuration())
        withSession(database, settings, prepare = prepare()) { session =>
          Given("an explicit participant profile, actual explicit multi-user HTTP ingress and two independently authenticated synthetic users")
          session.withParticipantIngress() { ingress =>
          val ownersecurity = ingress.context(principal)
          val foreignsecurity = ingress.context(foreign)
          val peer = new CheckpointPeer(database, new HttpPeer(database))
          When("each user prepares the same local request identifier through the native public delivery operation")
          val owned = record(session.query("prepare", fields, driver = Some(peer), service = "MaterialDelivery", security = Some(ownersecurity)))
          val other = record(session.query("prepare", fields, driver = Some(peer), service = "MaterialDelivery", security = Some(foreignsecurity)))
          val reference = owned.getString("deliveryRef").get
          Then("correlation and owner binding are server-derived and distinct, without provider writes")
          reference should not be other.getString("deliveryRef").get
          documents(database) should have size 2
          peer.delegate.effects shouldBe empty
          val before = documents(database)
          val effects = peer.delegate.effects
          val queries = Vector("get" -> Vector[(String, Any)]("deliveryRef" -> reference),
            "finalize" -> Vector[(String, Any)]("deliveryRef" -> reference),
            "prepareOriginalTransfer" -> Vector[(String, Any)]("deliveryRef" -> reference, "resourceId" -> "cover"))
          When("the other admitted user substitutes the owner's retained delivery reference")
          queries.foreach { case (operation, values) =>
            session.query(operation, values, driver = Some(peer), service = "MaterialDelivery", security = Some(foreignsecurity)).isSuccess shouldBe false
          }
          Then("the reservation and provider effects remain unchanged")
          documents(database) shouldBe before
          peer.delegate.effects shouldBe effects

          When("the legitimate owner uses the retained mediated route and finalizes its own originals")
          (0 to 1).foreach(index => session.query("uploadOriginal", upload(reference, index), driver = Some(peer),
            service = "MaterialDelivery", security = Some(ownersecurity)).isSuccess shouldBe true)
          val registered = record(session.query("finalize", Vector("deliveryRef" -> reference), driver = Some(peer),
            service = "MaterialDelivery", security = Some(ownersecurity)))
          val material = registered.getString("materialRef").get
          Then("the owner's registered Material remains readable through the ordinary logical contract")
          registered.getString("state") shouldBe Some("registered")
          session.query("getMaterial", Vector("materialRef" -> material), driver = Some(peer), security = Some(ownersecurity)).isSuccess shouldBe true
          session.query("readOriginal", Vector("materialRef" -> material, "resourceId" -> "detail"), driver = Some(peer), security = Some(ownersecurity)).isSuccess shouldBe true
          val completed = documents(database)
          val providerstate = peer.delegate.effects
          When("the other user requests the same private Material/original and finalization")
          session.query("getMaterial", Vector("materialRef" -> material), driver = Some(peer), security = Some(foreignsecurity)).isSuccess shouldBe false
          session.query("readOriginal", Vector("materialRef" -> material, "resourceId" -> "detail"), driver = Some(peer), security = Some(foreignsecurity)).isSuccess shouldBe false
          session.query("finalize", Vector("deliveryRef" -> reference), driver = Some(peer), service = "MaterialDelivery", security = Some(foreignsecurity)).isSuccess shouldBe false
          Then("native owner/Entity guards disclose no original and change no provider or delivery state")
          documents(database) shouldBe completed
          peer.delegate.effects shouldBe providerstate
          }
        }
      }
    }

    "reject mixed transports and public copies before destination, preparation, discovery or registration datastore access" in {
      withDatabase(native_container) { database =>
        prepareDatabase(database)
        val settings = parameters() + (MaterialParticipantConfiguration.configurationKey.name -> MaterialParticipantFixture.configuration())
        withSession(database, settings, prepare = prepare()) { session =>
          Given("a configured profile and genuine synthetic ingress contexts with cookie/session ambiguity or lost private proof")
          session.withParticipantIngress() { ingress =>
          val ownersecurity = ingress.context(principal)
          val denied = Vector(ownersecurity.copy(), ingress.context(principal, Vector("Cookie" -> "cncf-session=synthetic")),
            ingress.context(principal, Vector("X-Textus-Session" -> "synthetic")))
          val peer = new CheckpointPeer(database, new HttpPeer(database))
          val before = database.snapshot()
          val rows = documents(database)
          val searches = session.store.searches.size
          When("each context tries both delivery and ordinary Material entry points")
          denied.foreach { security =>
            session.query("getDestination", service = "MaterialDelivery", driver = Some(peer), security = Some(security)).isSuccess shouldBe false
            session.query("prepare", fields, service = "MaterialDelivery", driver = Some(peer), security = Some(security)).isSuccess shouldBe false
            session.query("findForRequest", Vector("requestId" -> requestId), service = "MaterialDelivery", driver = Some(peer), security = Some(security)).isSuccess shouldBe false
            session.query("searchMaterials", driver = Some(peer), security = Some(security)).isSuccess shouldBe false
            session.query("createMaterialRegistration", Vector("contribution" -> MaterialRegistrationOperationsFixture.contribution),
              driver = Some(peer), security = Some(security)).isSuccess shouldBe false
          }
          Then("the participant gate precedes the existing datastore and stronger provider credentials")
          session.store.searches.size shouldBe searches
          database.snapshot() shouldBe before
          documents(database) shouldBe rows
          peer.delegate.effects shouldBe empty
          }
        }
      }
    }
  }
}
