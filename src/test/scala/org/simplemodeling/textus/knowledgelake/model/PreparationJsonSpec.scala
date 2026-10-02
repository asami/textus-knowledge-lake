package org.simplemodeling.textus.knowledgelake.model

import io.circe.Json
import io.circe.parser.parse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.io.Source

/** Executable canonical wire contract and offline projection.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
class PreparationJsonSpec extends AnyWordSpec with GivenWhenThen with Matchers:
  "Unicode nonblank values at public JSON boundaries" should {
    val blankvalues = Vector(
      9, 10, 11, 12, 13, 32, 160, 5760, 8192, 8193, 8194, 8195, 8196,
      8197, 8198, 8199, 8200, 8201, 8202, 8232, 8233, 8239, 8287, 12288, 65279
    ).map(codepoint => f"U+$codepoint%04X" -> codepoint.toChar.toString) :+
      ("mixed ECMAScript whitespace" -> "\u2003\u00a0\t\u3000")
    blankvalues.foreach { (label, value) =>
      s"reject $label alone in required metadata and identity on encode and raw decode" in {
        Given(s"a model and raw JSON fixture with $label in a Resource title and Context ID")
        val text = _fixture_text()
        val original = PreparationJson.decode(text).toOption.get
        val bundle = original.copy(
          resources = original.resources.updated(0, original.resources.head.copy(title = value)),
          contexts = original.contexts.updated(0, original.contexts.head.copy(ref = original.contexts.head.ref.copy(id = value)))
        )
        val titlechanged = _edit(parse(text).toOption.get, Vector("resources", "0", "title"), _ => Json.fromString(value))
        val raw = _edit(titlechanged, Vector("contexts", "0", "ref", "id"), _ => Json.fromString(value))
        When("the public encoder and decoder receive the invalid values independently")
        val results = Vector(PreparationJson.encode(bundle).map(_ => ()), PreparationJson.decode(raw.noSpaces).map(_ => ()))
        Then("each boundary reports blank errors for both exact fields")
        all(results.map(_.isLeft)) shouldBe true
        results.foreach { result =>
          Vector("$.resources[0].title", "$.contexts[0].ref.id").foreach { path =>
            result.swap.toOption.get should contain(ModelError(path, "blank", "A nonblank value is required"))
          }
        }
      }
    }

    val validvalues = Vector(
      "padded Japanese and emoji" -> "\u2003日本語 🌱\u00a0",
      "NEL" -> "\u0085",
      "zero-width space" -> "\u200b",
      "file separator" -> "\u001c"
    )
    validvalues.foreach { (label, value) =>
      s"roundtrip $label exactly in required metadata and an opaque identity" in {
        Given(s"raw JSON with schema non-whitespace $label in a title and Context ID")
        val original = PreparationJson.decode(_fixture_text()).toOption.get
        val expected = original.copy(
          resources = original.resources.updated(0, original.resources.head.copy(title = value)),
          contexts = original.contexts.updated(0, original.contexts.head.copy(ref = original.contexts.head.ref.copy(id = value)))
        )
        val titlechanged = _edit(parse(_fixture_text()).toOption.get, Vector("resources", "0", "title"), _ => Json.fromString(value))
        val raw = _edit(titlechanged, Vector("contexts", "0", "ref", "id"), _ => Json.fromString(value))
        When("raw input is decoded, encoded and decoded again")
        val decoded = PreparationJson.decode(raw.noSpaces)
        val encoded = decoded.flatMap(PreparationJson.encode)
        val roundtrip = encoded.flatMap(PreparationJson.decode)
        Then("model values, padding and literal content are preserved exactly")
        decoded shouldBe Right(expected)
        roundtrip shouldBe Right(expected)
        parse(encoded.toOption.get).toOption.get shouldBe raw
        roundtrip.toOption.get.resources.head.title shouldBe value
        roundtrip.toOption.get.contexts.head.ref.id shouldBe value
        roundtrip.toOption.get.evidence.head.snapshot.content shouldBe original.evidence.head.snapshot.content
        roundtrip.toOption.get.materials.head.sections.head.text shouldBe original.materials.head.sections.head.text
      }
    }
  }

  "Canonical preparation JSON v1" should {
    "decode and encode the fixture losslessly with Unicode, nulls, six root kinds and declaration ordering" in {
      Given("the human-readable local fixture containing all six root model kinds")
      val text = _fixture_text()
      val fixture = parse(text).toOption.get
      When("the fixture is decoded, encoded twice and decoded again")
      val decoded = PreparationJson.decode(text)
      val encoded = decoded.flatMap(PreparationJson.encode)
      val repeated = decoded.flatMap(PreparationJson.encode)
      val roundtrip = encoded.flatMap(PreparationJson.decode)
      val json = parse(encoded.toOption.get).toOption.get
      Then("semantic values, explicit nulls, vector order and deterministic declaration order are preserved")
      decoded.isRight shouldBe true
      roundtrip shouldBe decoded
      encoded shouldBe repeated
      json shouldBe fixture
      json.asObject.get.keys.toVector shouldBe Vector("schema", "resources", "evidence", "contexts", "runs", "materials", "proposals")
      json.hcursor.downField("resources").downArray.focus.get.asObject.get.keys.toVector shouldBe Vector("ref", "kind", "title", "location")
      json.hcursor.downField("evidence").downArray.focus.get.asObject.get.keys.toVector shouldBe Vector("ref", "resourceRef", "providerRevision", "snapshot")
      val bundle = decoded.toOption.get
      bundle.resources.map(_.ref.revision) shouldBe Vector(1L, 2L)
      bundle.evidence.map(_.ref.revision) shouldBe Vector(1L, 2L)
      bundle.evidence.head.snapshot.content shouldBe Some("日本語の資料 🌱\n第二行\n")
      bundle.evidence.last.providerRevision shouldBe None
      bundle.evidence.last.snapshot.content shouldBe None
      json.hcursor.downField("evidence").downArray.right.downField("providerRevision").focus shouldBe Some(Json.Null)
      json.hcursor.downField("evidence").downArray.right.downField("snapshot").downField("provenance").downField("actor").focus shouldBe Some(Json.Null)
      bundle.contexts should have size 1
      bundle.runs should have size 1
      bundle.materials should have size 1
      bundle.proposals should have size 1
      bundle.proposals.head.producerId shouldBe "textus-knowledge-lake"
      bundle.proposals.head.proposalId shouldBe "proposal-local-001"
    }

    "accept JSON whitespace and object order without changing semantic identity" in {
      Given("the fixture with root key order reversed and compact whitespace")
      val fixture = parse(_fixture_text()).toOption.get
      val reordered = Json.fromFields(fixture.asObject.get.toVector.reverse).noSpaces
      When("both representations are decoded and canonically encoded")
      val original = PreparationJson.decode(fixture.spaces2).flatMap(PreparationJson.encode)
      val result = PreparationJson.decode(reordered).flatMap(PreparationJson.encode)
      Then("canonical output is identical")
      result shouldBe original
    }

    val objectpaths = Vector(
      Vector.empty[String],
      Vector("resources", "0"),
      Vector("resources", "0", "ref"),
      Vector("resources", "0", "location"),
      Vector("evidence", "0"),
      Vector("evidence", "0", "snapshot"),
      Vector("evidence", "0", "snapshot", "provenance"),
      Vector("contexts", "0"),
      Vector("contexts", "0", "bindings", "0"),
      Vector("contexts", "0", "existingInformation", "0"),
      Vector("runs", "0"),
      Vector("runs", "0", "input"),
      Vector("runs", "0", "input", "context"),
      Vector("runs", "0", "input", "resources", "0"),
      Vector("runs", "0", "input", "evidence", "0"),
      Vector("materials", "0"),
      Vector("materials", "0", "sections", "0"),
      Vector("proposals", "0")
    )
    objectpaths.foreach { path =>
      s"reject unknown fields at ${path.mkString("$.", ".", "")}" in {
        Given("a root or nested DTO with an undeclared field")
        val fixture = parse(_fixture_text()).toOption.get
        val invalid = _edit(fixture, path, json => json.mapObject(_.add("futureField", Json.fromString("unsupported"))))
        When("the strict v1 decoder receives it")
        val result = PreparationJson.decode(invalid.noSpaces)
        Then("a structured decoding error rejects the extra field")
        result.isLeft shouldBe true
        result.swap.toOption.get.head.code shouldBe "json-decode"
        result.swap.toOption.get.head.message should include("futureField")
      }
    }

    "reject an unknown nested failure descriptor field" in {
      Given("a partial Run with an undeclared failure-descriptor field")
      val fixture = parse(_fixture_text()).toOption.get
      val partial = _edit(fixture, Vector("runs", "0", "status"), _ => Json.fromString("partial"))
      val failure = Json.obj("code" -> Json.fromString("input"), "message" -> Json.fromString("Unavailable"), "evidenceRef" -> Json.Null, "futureField" -> Json.True)
      val invalid = _edit(partial, Vector("runs", "0", "failures"), _ => Json.arr(failure))
      When("the strict decoder receives the Run")
      val result = PreparationJson.decode(invalid.noSpaces)
      Then("the undeclared descriptor field is rejected")
      result.swap.toOption.get.head.code shouldBe "json-decode"
      result.swap.toOption.get.head.message should include("futureField")
    }

    val missingpaths = Vector(
      Vector.empty[String] -> "schema",
      Vector.empty[String] -> "proposals",
      Vector("resources", "1", "location") -> "uri",
      Vector("evidence", "1") -> "providerRevision",
      Vector("evidence", "1", "snapshot") -> "content",
      Vector("evidence", "1", "snapshot", "provenance") -> "actor",
      Vector("runs", "0") -> "failures",
      Vector("runs", "0", "input") -> "context",
      Vector("materials", "0") -> "sections",
      Vector("proposals", "0") -> "materialRef"
    )
    missingpaths.foreach { (path, field) =>
      s"reject absent required ${path.mkString(".")}.$field" in {
        Given(s"a DTO with its required $field key removed, even when nullable")
        val fixture = parse(_fixture_text()).toOption.get
        val invalid = _edit(fixture, path, json => json.mapObject(_.remove(field)))
        When("the decoder receives the incomplete object")
        val result = PreparationJson.decode(invalid.noSpaces)
        Then("the absent key produces a structured decoding error")
        result.swap.toOption.get.head.code shouldBe "json-decode"
      }
    }

    val invalidwire = Vector(
      ("unsupported schema", Vector("schema"), Json.fromString("tkl.preparation-bundle.v2"), "schema"),
      ("unknown provenance tag", Vector("evidence", "0", "snapshot", "provenance", "status"), Json.fromString("inferred"), "json-decode"),
      ("unknown retention tag", Vector("evidence", "0", "snapshot", "retention"), Json.fromString("cached"), "json-decode"),
      ("unknown Run tag", Vector("runs", "0", "status"), Json.fromString("pending"), "json-decode"),
      ("wrong root array type", Vector("resources"), Json.fromString("array required"), "json-decode"),
      ("wrong nested object type", Vector("runs", "0", "input"), Json.Null, "json-decode"),
      ("wrong optional value type", Vector("evidence", "1", "providerRevision"), Json.fromInt(2), "json-decode"),
      ("fractional revision", Vector("resources", "0", "ref", "revision"), Json.fromBigDecimal(BigDecimal("1.5")), "json-decode"),
      ("nonpositive revision", Vector("resources", "0", "ref", "revision"), Json.fromInt(0), "revision"),
      ("invalid retention", Vector("evidence", "0", "snapshot", "content"), Json.Null, "retention"),
      ("invalid provenance", Vector("runs", "0", "processor", "actor"), Json.Null, "provenance"),
      ("orphan citation", Vector("materials", "0", "sections", "0", "evidenceRefs", "0", "id"), Json.fromString("unresolved"), "unresolved")
    )
    invalidwire.foreach { (label, path, value, code) =>
      s"reject $label with structured errors" in {
        Given(s"a canonical object changed to contain $label")
        val fixture = parse(_fixture_text()).toOption.get
        val invalid = _edit(fixture, path, _ => value)
        When("the public decoder receives the invalid wire input")
        val result = PreparationJson.decode(invalid.noSpaces)
        Then(s"the decoder returns Left with a $code error")
        result.isLeft shouldBe true
        result.swap.toOption.get.map(_.code) should contain(code)
        all(result.swap.toOption.get.map(_.message)) should not be empty
      }
    }

    "return structured errors for malformed JSON and non-object roots" in {
      Given("truncated JSON, invalid syntax and roots with the wrong type")
      val inputs = Vector("{", "{bad}", "null", "[]", "\"text\"", "")
      When("each input is passed to the public decoder")
      val results = inputs.map(PreparationJson.decode)
      Then("every input returns structured Left without a parser exception")
      all(results.map(_.isLeft)) shouldBe true
      results.foreach(result => result.swap.toOption.get should not be empty)
    }
  }

  "Offline Markdown" should {
    "show exact material lineage and citations with unknown and reference-only status" in {
      Given("decoded canonical data with two material revisions and unknown processor provenance")
      val original = PreparationJson.decode(_fixture_text()).toOption.get
      val material = original.materials.head.copy(ref = original.materials.head.ref.copy(revision = 2), title = "Exact second material revision")
      val run = original.runs.head.copy(processor = Provenance(ProvenanceStatus.Unknown, None, "Processor attribution was not retained"))
      val bundle = original.copy(materials = original.materials :+ material, runs = Vector(run))
      When("the exact second revision is rendered using only the decoded bundle")
      val result = PreparedMaterialMarkdown.render(bundle, material.ref)
      Then("the projection includes literal section text and explicit frozen lineage without claiming unretained analysis")
      result.isRight shouldBe true
      val markdown = result.toOption.get
      markdown should include("# Exact second material revision")
      markdown should include(material.summary)
      markdown should include(material.sections.head.text)
      markdown should include("Material: material:準備 @ revision 2")
      markdown should include("Run: run\\-local\\-001")
      markdown should include("Context: context:調査 @ revision 1")
      markdown should include("Run input snapshot: run\\-input\\-local\\-001")
      markdown should include("Processor provenance: unknown actor")
      markdown should include("resource resource:資料/α @ revision 1")
      markdown should include("resource resource:資料/α @ revision 2")
      markdown should include("provider revision drive\\-revision\\-a")
      markdown should include("provider revision unknown")
      markdown should include("input snapshot snapshot:資料/α\\-1")
      markdown should include("input snapshot snapshot:資料/α\\-2")
      markdown should include("retained; provenance known actor fixture operator")
      markdown should include("reference-only (content not retained; no claim of content analysis)")
      markdown should include("provenance unknown actor")
      markdown should include("Existing information: information:baseline @ revision 3")
      markdown should include("External binding: binding\\-project\\-001")
    }

    "return a structured error when the requested material revision is missing" in {
      Given("a valid bundle and an absent material revision")
      val bundle = PreparationJson.decode(_fixture_text()).toOption.get
      val missing = bundle.materials.head.ref.copy(revision = 99)
      When("the missing exact revision is requested")
      val result = PreparedMaterialMarkdown.render(bundle, missing)
      Then("the projection boundary returns a structured unresolved error")
      result shouldBe Left(Vector(ModelError("$.materialRef", "unresolved", "Requested material revision does not exist")))
    }

    "escape metadata while preserving literal material text" in {
      Given("opaque IDs and provenance descriptions containing Markdown and HTML punctuation")
      val original = PreparationJson.decode(_fixture_text()).toOption.get
      val run = original.runs.head.copy(processor = Provenance(ProvenanceStatus.Known, Some("<actor> [manual]"), "A & B\nmetadata"))
      val bundle = original.copy(runs = Vector(run))
      When("the material is projected")
      val result = PreparedMaterialMarkdown.render(bundle, bundle.materials.head.ref)
      Then("metadata is escaped and section text is preserved literally")
      result.toOption.get should include("&lt;actor&gt; \\[manual\\]")
      result.toOption.get should include("A &amp; B&#10;metadata")
      result.toOption.get should include(bundle.materials.head.sections.head.text)
    }
  }

  private def _fixture_text(): String =
    val source = Source.fromResource("tkl/preparation-bundle-v1.json", getClass.getClassLoader)
    try source.mkString finally source.close()

  private def _edit(json: Json, path: Vector[String], change: Json => Json): Json =
    if path.isEmpty then change(json)
    else json.asObject match
      case Some(obj) => json.mapObject(_.add(path.head, _edit(obj(path.head).get, path.tail, change)))
      case None => json.mapArray(values => values.updated(path.head.toInt, _edit(values(path.head.toInt), path.tail, change)))
