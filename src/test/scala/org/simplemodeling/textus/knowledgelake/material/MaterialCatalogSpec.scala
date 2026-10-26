package org.simplemodeling.textus.knowledgelake.material

import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.observation.Taxonomy
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 21, 2026
 * @version Oct. 21, 2026
 */
final class MaterialCatalogSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import MaterialReferenceFixture.*

  "Trusted Material catalog" should {
    "logical identity and private mapping" which {
      "retain opaque references and expose only logical resource and provenance fields" in {
        Given("an exact bootstrap entry with a private connection, provider file and authorized owner")
        val document = catalog(Vector(entry()))
        When("the actual catalog parser admits the entry and resolves its exact reference")
        val parsed = MaterialCatalog.parseC(document.noSpaces).TAKE
        val selected = parsed.resolve(owner, materialRef).TAKE
        Then("logical metadata retains the opaque identity and no private binding appears in a projection or model display")
        selected.metadata.getString("materialRef") shouldBe Some(materialRef)
        selected.resources.head.logical.getString("resourceId") shouldBe Some(resourceId)
        Vector(selected.metadata.print, selected.summary.print, parsed.toString, selected.toString,
          selected.resources.head.toString, selected.resources.head.storage.toString).foreach { output =>
          privateMarkers.foreach(marker => output should not include marker)
        }
      }
    }
    "strict admission" which {
      "distinguish an explicitly empty catalog from malformed configuration" in {
        Given("a trusted schema with an explicitly empty collection")
        val document = catalog(Vector.empty)
        When("the actual parser admits the collection")
        val result = MaterialCatalog.parseC(document.noSpaces)
        Then("the empty catalog is valid and its authorized page is genuinely empty")
        result.TAKE.materials shouldBe empty
        result.TAKE.search(owner, None, 0, 20).getAsC[Vector[org.goldenport.record.Record]]("items").TAKE shouldBe Some(Vector.empty)
      }

      "reject unknown fields, duplicate identities, unsupported providers and malformed values without disclosure" in {
        Given("exact entries varied at each strict configuration boundary")
        val valid = entry()
        val resource = valid.hcursor.downField("resources").focus.get.asArray.get.head
        val storage = resource.hcursor.downField("storage").focus.get
        val variants = Vector(
          Json.obj(), catalog(Vector(valid, valid)),
          replace(catalog(Vector(valid)), "unknown", Json.True),
          catalog(Vector(replace(valid, "unknown", Json.True))),
          catalog(Vector(replace(valid, "authorizedSubjects", Json.arr()))),
          catalog(Vector(replace(valid, "authorizedSubjects", Json.arr(Json.fromString(owner), Json.fromString(owner))))),
          catalog(Vector(replace(valid, "materialRef", Json.fromString("bad\nreference")))),
          catalog(Vector(replace(valid, "title", Json.fromString("x" * 4097)))),
          catalog(Vector(replace(valid, "provenance", Json.arr(Json.obj("kind" -> Json.fromString("source")))))),
          catalog(Vector(replace(valid, "resources", Json.arr(resource, resource))))) ++
          Vector("role" -> Json.fromString("preview"), "mediaType" -> Json.fromString("image/png\r\nsecret"),
            "resourceId" -> Json.fromString(""), "byteLength" -> Json.fromLong(-1),
            "byteLength" -> Json.fromBigDecimal(BigDecimal("1.5")), "extra" -> Json.True)
            .map { case (key, value) => catalog(Vector(replace(valid, "resources", Json.arr(replace(resource, key, value))))) } ++
          Vector("provider" -> Json.fromString("s3"), "connectionId" -> Json.fromString("../private"),
            "resourceKey" -> Json.fromString("/private/path"), "parentKey" -> Json.fromString(""), "extra" -> Json.True)
            .map { case (key, value) => catalog(Vector(replace(valid, "resources", Json.arr(
              replace(resource, "storage", replace(storage, key, value)))))) }
        val texts = variants.map(_.noSpaces) ++ Vector("{private-marker", "x" * 131073)
        When("all invalid documents are interpreted by the actual parser")
        val outcomes = texts.map(MaterialCatalog.parseC)
        Then("every rejection is the same safe structured configuration failure")
        outcomes.foreach { result =>
          failure(result).observation.taxonomy shouldBe Taxonomy(Taxonomy.Category.Configuration, Taxonomy.Symptom.Invalid)
          failure(result).displayMessage shouldBe "Material reference catalog configuration is invalid"
          privateMarkers.foreach(marker => result.display should not include marker)
        }
      }
    }
    "authorized search" which {
      "preserve stable filtering, paging and unrelated opaque domains for at least one hundred randomized catalogs" in {
        Given("randomized catalog order, authorized subjects, opaque reference domains, queries and valid pages")
        val scenarios = for {
          count <- Gen.choose(1, 45)
          flags <- Gen.listOfN(count, Gen.oneOf(true, false))
          order <- Gen.pick(count, (0 until count).toVector)
          text <- Gen.oneOf("", "TITLE", "urn:OTHER:", "no-match", "7")
          offset <- Gen.choose(0, 50)
          limit <- Gen.choose(1, 20)
        } yield (flags.toVector, order.toVector, text, offset, limit)
        When("ScalaCheck parses and searches each generated catalog under its actual authorization rules")
        val result = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), Prop.forAll(scenarios) {
          case (flags, order, text, offset, limit) =>
            val references = flags.indices.map(index => s"urn:${if (index % 2 == 0) "other" else "knowledge-lake"}:$index").toVector
            val entries = order.map(index => entry(references(index), s"Title $index", if (flags(index)) Vector(owner) else Vector("unrelated-owner")))
            val admitted = MaterialCatalog.parseC(catalog(entries).noSpaces).TAKE
            val expected = flags.indices.filter(flags(_)).filter(index =>
              references(index).toLowerCase(java.util.Locale.ROOT).contains(text.toLowerCase(java.util.Locale.ROOT)) ||
                s"Title $index".toLowerCase(java.util.Locale.ROOT).contains(text.toLowerCase(java.util.Locale.ROOT)))
              .map(references(_)).sorted.toVector
            val page = admitted.search(owner, Some(text), offset, limit)
            page.getAsC[Vector[org.goldenport.record.Record]]("items").TAKE.get.map(_.getString("materialRef").get) ==
              expected.slice(offset, offset + limit) &&
              page.getAsC[Boolean]("hasMore").TAKE.contains(offset + limit < expected.size)
        })
        Then("all generated authorized pages match a separate stable model without discards")
        result.passed shouldBe true
        result.succeeded should be >= 100
        result.discarded shouldBe 0
      }
    }
  }
}
