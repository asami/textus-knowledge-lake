package org.simplemodeling.textus.knowledgelake.material

import org.goldenport.cncf.filestore.*
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class MaterialDeliveryReconciliationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
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
