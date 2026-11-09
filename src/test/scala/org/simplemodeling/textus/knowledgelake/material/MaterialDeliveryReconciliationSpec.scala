package org.simplemodeling.textus.knowledgelake.material

import org.goldenport.cncf.filestore.*
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since Nov.  9, 2026
 * @version Nov.  9, 2026
 */
final class MaterialDeliveryReconciliationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "A trusted legacy decision" should {
    "round trip only the explicit host adjudication across source references" in {
      Given("the complete contribution and destination with arbitrary retained source references")
      val contribution = MaterialRegistrationOperationsFixture.contribution
      val configuration = MaterialDeliveryOperationsFixture.configuration()
      val references = Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
      When("the host control is parsed")
      val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), Prop.forAll(references) { source =>
        val json = io.circe.Json.obj(
          "schema" -> io.circe.Json.fromString("textus.material-delivery-legacy-decision/1"),
          "adjudication" -> io.circe.Json.fromString("hostAdjudicatedOriginalRefusalBeforeDispatch"),
          "owner" -> io.circe.Json.fromString("owner"), "delivery" -> io.circe.Json.fromString("delivery"),
          "request" -> io.circe.Json.fromString("request"), "revision" -> io.circe.Json.fromLong(4),
          "contribution" -> contribution, "configuration" -> configuration, "source" -> io.circe.Json.fromString(source))
        val parsed = MaterialDeliveryLegacyRecovery.parseC(json.noSpaces)
        val raw = json.noSpaces
        val duplicates = Vector(
          raw.replace("\"owner\":\"owner\"", "\"owner\":\"foreign\",\"owner\":\"owner\""),
          raw.replace("\"delivery\":\"delivery\"", "\"delivery\":\"delivery\",\"delivery\":\"foreign\""),
          raw.replace("\"adjudication\":", "\"adjudication\":\"unknown\",\"adjudication\":"),
          raw.replace("\"title\":", "\"title\":\"forged\",\"title\":"),
          raw.replace("\"parentKey\":", "\"parentKey\":\"foreign\",\"parentKey\":")
        )
        parsed.isSuccess && parsed.TAKE.source == source && duplicates.forall(_ != raw) &&
          duplicates.forall(value => !MaterialDeliveryLegacyRecovery.parseC(value).isSuccess) &&
          !MaterialDeliveryLegacyRecovery.parseC(json.mapObject(_.add("adjudication", io.circe.Json.fromString("unknown"))).noSpaces).isSuccess &&
          !MaterialDeliveryLegacyRecovery.parseC(json.mapObject(_.remove("source")).noSpaces).isSuccess
      })
      Then("only a complete, explicitly adjudicated decision is admitted")
      checked.passed shouldBe true
    }
  }

  "A retained collection reservation" should {
    "round trip its exact binding and observation over many opaque identities" in {
      Given("provider-allocated identities and every durable observation state")
      val keys = Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
      val states = Gen.oneOf("reserved", "notDispatched", "possiblyDispatched", "confirmed", "unknown")
      When("the private checkpoint is serialized and restored")
      val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), Prop.forAll(keys, states) { (key, state) =>
        val binding = FileStoreWriteBinding("delivery:collection:root", "owner", FileStoreRef("store", "parent"), "name")
        val value = MaterialDeliveryCollectionCheckpoint("root", FileStoreCollectionReservation(binding,
          FileStoreRef("store", "key-" + key), None), state)
        MaterialDeliveryCollectionCheckpoint.parse(value.json).contains(value)
      })
      Then("the complete identity, owner, parent and observation survive")
      checked.passed shouldBe true
    }

    "bind observations to the exact reservation even when display names repeat" in {
      Given("a reserved collection and another identity sharing its parent and display name")
      val binding = FileStoreWriteBinding("delivery:collection:root", "owner", FileStoreRef("store", "parent"), "same-name")
      val reservation = FileStoreCollectionReservation(binding, FileStoreRef("store", "reserved"), None)
      val duplicate = reservation.copy(reference = FileStoreRef("store", "other"))
      val wrongowner = reservation.copy(binding = binding.copy(caller = "foreign"))
      When("native observations are matched against the retained checkpoint")
      val exact = FileStoreWriteObservation(reservation, FileStoreWriteState.Unknown)
      val other = FileStoreWriteObservation(duplicate, FileStoreWriteState.Unknown)
      val foreign = FileStoreWriteObservation(wrongowner, FileStoreWriteState.Unknown)
      Then("matching uses the complete reservation rather than the shared label")
      exact.matches(reservation) shouldBe true
      other.matches(reservation) shouldBe false
      foreign.matches(reservation) shouldBe false
    }

    "reject altered or incomplete private evidence" in {
      Given("one well-formed reservation")
      val binding = FileStoreWriteBinding("delivery:collection:root", "owner", FileStoreRef("store", "parent"), "name")
      val checkpoint = MaterialDeliveryCollectionCheckpoint("root", FileStoreCollectionReservation(binding,
        FileStoreRef("store", "reserved"), None), "unknown")
      When("its fields are removed or its observation is replaced with a caller assertion")
      val missing = checkpoint.json.mapObject(_.remove("caller"))
      val forged = checkpoint.json.mapObject(_.add("observation", io.circe.Json.fromString("absent")))
      Then("neither value is admitted as durable native evidence")
      MaterialDeliveryCollectionCheckpoint.parse(missing) shouldBe None
      MaterialDeliveryCollectionCheckpoint.parse(forged) shouldBe None
    }
  }
}
