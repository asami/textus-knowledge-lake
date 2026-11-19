package org.simplemodeling.textus.knowledgelake.material

import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/* @since   Nov. 12, 2026
 * @version Nov. 12, 2026
 */
final class MaterialReferencedOriginalSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import MaterialContentIntegrity.*

  "Referenced original content integrity" should {
    "compare actual bytes while retaining the original baseline" in {
      Given("a complete original and a same-length changed original")
      val original = Vector[Byte](1, 2, 3)
      val expected = baseline(original)
      val changed = Vector[Byte](1, 2, 4)
      When("the later complete bytes are compared")
      val result = compare(Some(expected), Some(changed))
      Then("the mismatch is changed and the baseline still matches the original")
      result shouldBe Comparison.Changed
      compare(Some(expected), Some(original)) shouldBe Comparison.Matched
      restore(expected.sha256, expected.byteLength) shouldBe Some(expected)
    }

    "distinguish absent bytes from an absent baseline" in {
      Given("a baseline and empty complete bytes")
      val expected = baseline(Vector.empty)
      When("availability and baseline evidence differ")
      Then("only a complete read with its baseline can match")
      compare(Some(expected), None) shouldBe Comparison.Unavailable
      compare(None, Some(Vector.empty)) shouldBe Comparison.Unverified
      compare(Some(expected), Some(Vector.empty)) shouldBe Comparison.Matched
      expected.sha256 shouldBe "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
      restore("invalid", 0) shouldBe None
    }

    "recognize arbitrary complete byte sequences" in {
      Given("generated binary originals")
      val samples = Gen.listOf(Gen.choose(-128, 127)).map(_.take(300).map(_.toByte).toVector)
      When("their actual bytes are baselined and compared")
      val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), Prop.forAll(samples) { bytes =>
        compare(Some(baseline(bytes)), Some(bytes)) == Comparison.Matched
      })
      Then("every complete read matches its own baseline")
      checked.passed shouldBe true
      checked.succeeded should be >= 100
    }

    "detect same-length changes across generated content" in {
      Given("generated nonempty complete byte sequences")
      val samples = Gen.nonEmptyListOf(Gen.choose(-128, 127)).map(_.take(300).map(_.toByte).toVector)
      When("one byte changes without changing the length")
      val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), Prop.forAll(samples) { bytes =>
        val changed = bytes.updated(0, (bytes.head ^ 1).toByte)
        compare(Some(baseline(bytes)), Some(changed)) == Comparison.Changed
      })
      Then("every observation is changed")
      checked.passed shouldBe true
      checked.succeeded should be >= 100
    }
  }
}
