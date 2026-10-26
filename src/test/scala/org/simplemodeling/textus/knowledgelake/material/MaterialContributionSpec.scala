package org.simplemodeling.textus.knowledgelake.material

import io.circe.{Json, parser}
import org.goldenport.{Conclusion, Consequence}
import org.goldenport.cncf.context.PrincipalId
import org.goldenport.observation.Taxonomy
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * Neutral contribution admission and TKL-owned pure package semantics.
 *
 * @since   Oct. 21, 2026
 * @version Oct. 21, 2026
 */
final class MaterialContributionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import MaterialContributionSpec.*

  "Material contribution" should {
    "retain admitted literal meaning" which {
      "retain complete opaque JSON, ordered resources and literal metadata without date interpretation" in {
        Given("a neutral rich image/audio contribution with histories, nulls, opaque receipts and a non-date createdAt")
        val raw = contributionJson()
        When("the complete external contribution is admitted")
        val admitted = take(MaterialContribution.parseC(raw))
        Then("all fields retain their exact selected spelling and JSON trees")
        admitted.json shouldBe raw
        admitted.title shouldBe "  literal 雪 title  "
        admitted.createdAt shouldBe "captured time retained literally"
        admitted.classifications shouldBe Vector(" raw ", "image", "audio", " raw ")
        admitted.provenance shouldBe raw.hcursor.downField("provenance").focus.get
        admitted.originals.map(_.resourceId) shouldBe Vector("image-1", "image-2", "audio-1")
        admitted.originals.map(_.role) shouldBe Vector(" image-front ", "unselected-image", "audio-original")
        admitted.originals.head.mediaType shouldBe "Image/PNG"
        admitted.originals.head.sourceReference shouldBe raw.hcursor.downField("originals").downArray.downField("sourceReference").focus.get
        admitted.documents.map(_.resourceId) shouldBe Vector("capture", "review")
        admitted.documents.head.body shouldBe raw.hcursor.downField("documents").downArray.downField("body").focus.get
        admitted.documents.head.derivedFrom shouldBe Vector("audio-1", "image-1", "image-2")
      }

      "permit empty resource vectors and arbitrary document JSON values" in {
        Given("the package minimum with empty resources, and document bodies spanning every JSON shape")
        val minimalraw = contributionJson(rich = false, documents = false)
        val bodies = Vector(Json.Null, Json.True, Json.fromInt(42), Json.fromString("literal"), Json.arr(), Json.obj())
        When("the empty contribution and each opaque document body are admitted")
        val minimal = take(MaterialContribution.parseC(minimalraw))
        val values = bodies.map(body => take(MaterialContribution.parseC(contributionJson(rich = false, opaque = body))))
        Then("empty originals/documents remain valid and no schema is imposed on document contents")
        minimal.originals shouldBe empty
        minimal.documents shouldBe empty
        values.map(_.documents.head.body) shouldBe bodies
      }

      "admit zero-length originals and the largest known nonnegative Long length" in {
        Given("originals whose known sizes are zero or Long.MaxValue")
        val raws = Vector(0L, Long.MaxValue).map(length => contributionJson(byteLength = length))
        When("the original declarations are admitted")
        val admitted = raws.map(raw => take(MaterialContribution.parseC(raw)))
        Then("the known byte lengths retain their exact values")
        admitted.map(_.originals.head.byteLength) shouldBe Vector(0L, Long.MaxValue)
      }
    }

    "reject malformed contributions" which {
      "reject unknown or incomplete envelopes and malformed required fields with a fixed structured failure" in {
        Given("closed-envelope violations and malformed metadata, resource arrays or provenance")
        val raw = contributionJson()
        val malformed = Vector(null, Json.Null, Json.arr(), replace(raw, "schema", Json.fromString("unknown")),
          raw.mapObject(_.remove("title")), raw.mapObject(_.add("destination", Json.fromString("private-source-marker"))),
          replace(raw, "title", Json.Null), replace(raw, "title", Json.fromString(" \t\n")),
          replace(raw, "createdAt", Json.fromString("")), replace(raw, "createdAt", Json.fromInt(1)),
          replace(raw, "classifications", Json.Null), replace(raw, "classifications", Json.arr(Json.fromString(" "))),
          replace(raw, "classifications", Json.arr(Json.fromInt(1))), replace(raw, "provenance", Json.arr()),
          replace(raw, "originals", Json.Null), replace(raw, "documents", Json.obj()))
        When("each malformed envelope reaches public admission")
        val results = malformed.map(MaterialContribution.parseC)
        Then("admission fails without reflecting private values or accepting a provider destination")
        results.foreach { result =>
          failure(result).observation.taxonomy shouldBe Taxonomy.argumentInvalid
          failure(result).displayMessage shouldBe "Invalid Material contribution"
          failure(result).display should not include "private-source-marker"
        }
      }

      "reject malformed original shape, IDs, MIME, lengths and non-object source references" in {
        Given("one original declaration changed at a time without altering the opaque source object's valid interior")
        val raw = contributionJson()
        val original = raw.hcursor.downField("originals").downArray.focus.get
        val invalidids = Vector("", "../escape", "with.dot", "a/b", "x" * 129, " space", "雪", "_first")
        val malformed = Vector(Json.Null, original.mapObject(_.remove("role")),
          original.mapObject(_.add("path", Json.fromString("private-source-marker"))),
          replace(original, "role", Json.fromString("\t ")), replace(original, "mediaType", Json.fromString("image/png; charset=utf-8")),
          replace(original, "mediaType", Json.fromString("image")), replace(original, "byteLength", Json.fromLong(-1)),
          replace(original, "byteLength", Json.fromDoubleOrNull(1.5)), replace(original, "byteLength", Json.fromString("4")),
          replace(original, "byteLength", parser.parse("9223372036854775808").toOption.get),
          replace(original, "sourceReference", Json.Null), replace(original, "sourceReference", Json.fromString("https://example.invalid"))) ++
          invalidids.map(id => replace(original, "resourceId", Json.fromString(id)))
        When("each malformed original is parsed as part of the complete contribution")
        val results = malformed.map(original => MaterialContribution.parseC(replace(raw, "originals", Json.arr(original))))
        Then("every invalid declaration is rejected before it can become an original capability request")
        results.foreach(result => failure(result).observation.taxonomy shouldBe Taxonomy.argumentInvalid)
      }

      "reject malformed documents, duplicate lineage and lineage outside this contribution's originals" in {
        Given("closed document declarations with invalid IDs, bodies, roles or derivedFrom values")
        val raw = contributionJson()
        val document = raw.hcursor.downField("documents").downArray.focus.get
        val malformed = Vector(Json.Null, document.mapObject(_.remove("body")),
          document.mapObject(_.add("mediaType", Json.fromString("text/plain"))),
          replace(document, "role", Json.fromString("")), replace(document, "resourceId", Json.fromString("a.json")),
          replace(document, "derivedFrom", Json.Null), replace(document, "derivedFrom", Json.arr(Json.fromInt(1))),
          replace(document, "derivedFrom", Json.arr(Json.fromString("image-1"), Json.fromString("image-1"))),
          replace(document, "derivedFrom", Json.arr(Json.fromString("missing"))),
          replace(document, "derivedFrom", Json.arr(Json.fromString("review"))))
        When("each changed document is admitted within its complete contribution")
        val results = malformed.map(document => MaterialContribution.parseC(replace(raw, "documents", Json.arr(document))))
        Then("no unknown source, document-to-document lineage or malformed declaration is accepted")
        results.foreach(result => failure(result).observation.taxonomy shouldBe Taxonomy.argumentInvalid)
      }

      "reject resource ID collisions across originals and documents" in {
        Given("duplicate original IDs, duplicate document IDs and an original/document collision")
        val raw = contributionJson()
        val originals = raw.hcursor.get[Vector[Json]]("originals").toOption.get
        val documents = raw.hcursor.get[Vector[Json]]("documents").toOption.get
        val duplicates = Vector(replace(raw, "originals", Json.fromValues(originals :+ originals.head)),
          replace(raw, "documents", Json.fromValues(documents :+ documents.head)),
          replace(raw, "documents", Json.arr(replace(documents.head, "resourceId", Json.fromString("image-1")))))
        When("the complete resource namespace is admitted")
        val results = duplicates.map(MaterialContribution.parseC)
        Then("one contribution cannot contain two declarations for the same resource ID")
        results.foreach(result => failure(result).observation.taxonomy shouldBe Taxonomy.argumentInvalid)
      }

      "redact contribution and resource displays without changing retained content" in {
        Given("an admitted contribution containing opaque private source, provenance and document markers")
        val value = take(MaterialContribution.parseC(contributionJson()))
        When("the admitted values are rendered for diagnostics")
        val displays = Vector(value.toString, value.originals.toString, value.documents.toString)
        Then("displays reveal no opaque content while the caller still retains the exact complete JSON")
        displays.foreach(display => display should not include "private-source-marker")
        displays.foreach(display => display should not include "private-provenance-marker")
        displays.foreach(display => display should not include "private-body-marker")
        value.json shouldBe contributionJson()
      }
    }
  }

  "TKL package planning" should {
    "own exact location and format" which {
      "retain safe lake/path spelling at grammar boundaries and construct only the established URN" in {
        Given("explicit safe lakes and logical paths including maximum lengths")
        val selections = Vector(("Lake.Name-1", "2026/Raw_1/item.v1"), ("a" * 128, "b" * 1024))
        When("TKL admits the selected locations")
        val locations = selections.map { case (lake, path) => take(MaterialLocation.createC(lake, path)) }
        Then("URN construction preserves exactly the selected logical spelling")
        locations.zip(selections).foreach { case (location, (lake, path)) =>
          location.lake shouldBe lake
          location.logicalPath shouldBe path
          location.urn shouldBe s"urn:textus:klake:$lake:material:$path"
        }
      }

      "reject unsafe locations without trimming, escaping or default inference" in {
        Given("missing, escaped, whitespace, dot and nested-empty location components")
        val lakes = Vector(null, "", " lake", "lake ", "a" * 129, "a/b", "雪")
        val paths = Vector(null, "", "/a", "a/", "a//b", ".", "..", "a/../b", "a/./b", "a\\b", "a%2fb",
          "a:b", "a?b", "a#b", "a b", "a\nb", "a" * 1025)
        When("each literal location is validated")
        val results = lakes.map(lake => MaterialLocation.createC(lake, "item")) ++ paths.map(path => MaterialLocation.createC("lake", path))
        Then("every unsafe location returns the fixed structured argument failure")
        results.foreach { result =>
          failure(result).observation.taxonomy shouldBe Taxonomy.argumentInvalid
          failure(result).displayMessage shouldBe "Invalid TKL material location"
        }
      }

      "plan exact generic metadata, ordered layout and lineage without exposing source receipts or inferring producer paths" in {
        Given("a neutral rich contribution and explicit location and server principals")
        val contribution = take(MaterialContribution.parseC(contributionJson()))
        val location = take(MaterialLocation.createC("Explicit-Lake", "2026/10/item-1"))
        When("TKL plans the complete package without any provider")
        val plan = take(MaterialPackage.planC(contribution, location, owner, creator))
        val assets = plan.manifest.hcursor.get[Vector[Json]]("assets").toOption.get
        Then("the manifest has exactly the generic format and originals precede complete JSON documents")
        plan.manifest shouldBe Json.obj(
          "schema" -> Json.fromString("textus.material/1"), "id" -> Json.fromString(location.urn),
          "title" -> Json.fromString(contribution.title), "createdAt" -> Json.fromString(contribution.createdAt),
          "classifications" -> Json.fromValues(contribution.classifications.map(Json.fromString)),
          "provenance" -> contribution.provenance, "owner" -> Json.fromString(owner.value), "createdBy" -> Json.fromString(creator.value),
          "assets" -> Json.arr(asset("assets/image-1", " image-front ", "Image/PNG"),
            asset("assets/image-2", "unselected-image", "image/jpeg"), asset("assets/audio-1", "audio-original", "audio/wav"),
            derivedAsset("derived/capture.json", "capture-metadata", Vector("assets/audio-1", "assets/image-1", "assets/image-2")),
            derivedAsset("derived/review.json", "transcript-history", Vector("assets/audio-1"))))
        plan.contribution should be theSameInstanceAs contribution
        plan.originals.map(_.path) shouldBe Vector("assets/image-1", "assets/image-2", "assets/audio-1")
        plan.documents.map(_.path) shouldBe Vector("derived/capture.json", "derived/review.json")
        plan.documents.map(document => parser.parse(document.body).toOption.get) shouldBe contribution.documents.map(_.body)
        assets.forall(_.asObject.get.keys.toSet.subsetOf(Set("path", "role", "mediaType", "derivedFrom"))) shouldBe true
        plan.manifest.hcursor.downField("component").focus shouldBe None
        plan.manifest.hcursor.downField("sourceMaterialEntityId").focus shouldBe None
        plan.manifest.hcursor.downField("provenance").focus shouldBe Some(contribution.provenance)
        plan.toString shouldBe "MaterialPackagePlan(<planned>)"
      }

      "plan the media-free minimum with an empty assets array and no fabricated identity" in {
        Given("a complete contribution with neither originals nor documents")
        val contribution = take(MaterialContribution.parseC(contributionJson(rich = false, documents = false)))
        val location = take(MaterialLocation.createC("lake", "one-leaf"))
        When("the minimum package is planned")
        val plan = take(MaterialPackage.planC(contribution, location, owner, creator))
        Then("an empty package still has required metadata and no source or Entity ID is invented")
        plan.originals shouldBe empty
        plan.documents shouldBe empty
        plan.manifest.hcursor.get[Vector[Json]]("assets").toOption shouldBe Some(Vector.empty)
        plan.manifest.hcursor.get[String]("id").toOption shouldBe Some(location.urn)
      }

      "reject absent inputs and malformed server principals before planning" in {
        Given("an admitted contribution/location and absent or control-bearing server principals")
        val contribution = take(MaterialContribution.parseC(contributionJson()))
        val location = take(MaterialLocation.createC("lake", "item"))
        When("invalid planning inputs are submitted")
        val results = Vector(MaterialPackage.planC(null, location, owner, creator),
          MaterialPackage.planC(contribution, null, owner, creator)) ++
          Vector(null, PrincipalId(null), PrincipalId(" "), PrincipalId("private\nprincipal")).flatMap(principal =>
            Vector(MaterialPackage.planC(contribution, location, principal, creator), MaterialPackage.planC(contribution, location, owner, principal)))
        Then("the fixed argument failure contains no principal data")
        results.foreach(result => failure(result).displayMessage shouldBe "Invalid Material package request")
      }

      "retain at least one hundred arbitrary UTF16 JSON samples with no discarded cases" in {
        Given("bounded arbitrary UTF16 code-unit sequences including unpaired surrogates")
        val strings = Gen.choose(0, 32).flatMap(length => Gen.listOfN(length, Gen.choose(0, 65535).map(_.toChar))).map(_.mkString)
        When("ScalaCheck admits and plans each complete literal contribution")
        val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), Prop.forAll(strings) { literal =>
          val raw = contributionJson(opaque = Json.obj("literal" -> Json.fromString(literal)), title = "title" + literal)
          val contribution = take(MaterialContribution.parseC(raw))
          val plan = take(MaterialPackage.planC(contribution, take(MaterialLocation.createC("lake", "item")), owner, creator))
          contribution.json == raw && parser.parse(plan.documents.head.body).toOption.contains(contribution.documents.head.body) &&
            plan.manifest.hcursor.get[String]("title").toOption.contains("title" + literal)
        })
        Then("all one hundred successful undiscarded cases preserve their exact string code units")
        checked.passed shouldBe true
        checked.succeeded should be >= 100
        checked.discarded shouldBe 0
      }
    }
  }
}

object MaterialContributionSpec {
  val owner: PrincipalId = PrincipalId("server-selected-owner")
  val creator: PrincipalId = PrincipalId("server-selected-creator")
  val opaque: Json = Json.obj("marker" -> Json.fromString("private-body-marker"),
    "history" -> Json.arr(Json.obj("text" -> Json.fromString("machine")), Json.obj("text" -> Json.fromString("human"))),
    "empty" -> Json.arr(), "single" -> Json.arr(Json.Null), "nullable" -> Json.Null)

  def contributionJson(rich: Boolean = true, documents: Boolean = true, byteLength: Long = 4,
    opaque: Json = MaterialContributionSpec.opaque, title: String = "  literal 雪 title  "): Json = {
    val originals = if (rich) Vector(("image-1", " image-front ", "Image/PNG"),
      ("image-2", "unselected-image", "image/jpeg"), ("audio-1", "audio-original", "audio/wav")).map {
      case (id, role, media) => Json.obj("resourceId" -> Json.fromString(id), "role" -> Json.fromString(role),
        "mediaType" -> Json.fromString(media), "byteLength" -> Json.fromLong(byteLength),
        "sourceReference" -> Json.obj("selector" -> Json.fromString(id), "opaque" -> opaque,
          "receipt" -> Json.obj("private" -> Json.fromString("private-source-marker"), "unknown" -> Json.arr(Json.Null))))
    } else Vector.empty
    val bodies = if (!documents) Vector.empty else Vector(
      Json.obj("resourceId" -> Json.fromString("capture"), "role" -> Json.fromString("capture-metadata"), "body" -> opaque,
        "derivedFrom" -> Json.fromValues((if (rich) Vector("audio-1", "image-1", "image-2") else Vector.empty).map(Json.fromString))),
      Json.obj("resourceId" -> Json.fromString("review"), "role" -> Json.fromString("transcript-history"),
        "body" -> Json.obj("version" -> Json.fromString("retained"), "review" -> opaque, "revisions" -> Json.arr(opaque, Json.Null)),
        "derivedFrom" -> Json.fromValues((if (rich) Vector("audio-1") else Vector.empty).map(Json.fromString))))
    Json.obj("schema" -> Json.fromString("textus.material-contribution/1"), "title" -> Json.fromString(title),
      "createdAt" -> Json.fromString("captured time retained literally"),
      "classifications" -> Json.arr(Json.fromString(" raw "), Json.fromString("image"), Json.fromString("audio"), Json.fromString(" raw ")),
      "provenance" -> Json.obj("marker" -> Json.fromString("private-provenance-marker"), "producerPath" -> Json.fromString("opaque/producer-path"),
        "requestId" -> Json.fromString("legacy-app-request"), "subjectId" -> Json.fromString("legacy-app-subject"), "complete" -> opaque),
      "originals" -> Json.fromValues(originals), "documents" -> Json.fromValues(bodies))
  }

  def replace(json: Json, key: String, value: Json): Json = json.mapObject(_.add(key, value))
  def take[A](outcome: Consequence[A]): A = outcome.getOrElse(throw new AssertionError("Material specification admission failed"))
  def failure(outcome: Consequence[?]): Conclusion = outcome match {
    case Consequence.Failure(conclusion) => conclusion
    case _ => throw new AssertionError("Expected a structured Material failure")
  }
  def asset(path: String, role: String, mediaType: String): Json = Json.obj(
    "path" -> Json.fromString(path), "role" -> Json.fromString(role), "mediaType" -> Json.fromString(mediaType))
  def derivedAsset(path: String, role: String, sources: Vector[String]): Json =
    asset(path, role, "application/json").mapObject(_.add("derivedFrom", Json.fromValues(sources.map(Json.fromString))))
}
