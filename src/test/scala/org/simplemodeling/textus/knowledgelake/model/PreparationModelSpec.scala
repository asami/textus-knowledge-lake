package org.simplemodeling.textus.knowledgelake.model

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.io.Source

/** Executable preparation lineage and retention contract.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
class PreparationModelSpec extends AnyWordSpec with GivenWhenThen with Matchers:
  "Required Unicode metadata and opaque identities" should {
    val blankvalues = Vector(
      9, 10, 11, 12, 13, 32, 160, 5760, 8192, 8193, 8194, 8195, 8196,
      8197, 8198, 8199, 8200, 8201, 8202, 8232, 8233, 8239, 8287, 12288, 65279
    ).map(codepoint => f"U+$codepoint%04X" -> codepoint.toChar.toString) :+
      ("mixed ECMAScript whitespace" -> "\u2003\u00a0\t\u3000")
    blankvalues.foreach { (label, value) =>
      s"reject $label alone in a Resource title and Context identity" in {
        Given(s"a fixture with $label as required metadata and an opaque VersionedRef ID")
        val original = _fixture()
        val bundle = original.copy(
          resources = original.resources.updated(0, original.resources.head.copy(title = value)),
          contexts = original.contexts.updated(0, original.contexts.head.copy(ref = original.contexts.head.ref.copy(id = value)))
        )
        When("the public model validator receives the bundle")
        val result = PreparationModel.validate(bundle)
        Then("both exact fields report structured blank errors")
        result.isLeft shouldBe true
        Vector("$.resources[0].title", "$.contexts[0].ref.id").foreach { path =>
          result.swap.toOption.get should contain(ModelError(path, "blank", "A nonblank value is required"))
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
      s"preserve $label exactly in metadata and an opaque identity" in {
        Given(s"a fixture with schema non-whitespace $label in its title and Context ID")
        val original = _fixture()
        val bundle = original.copy(
          resources = original.resources.updated(0, original.resources.head.copy(title = value)),
          contexts = original.contexts.updated(0, original.contexts.head.copy(ref = original.contexts.head.ref.copy(id = value)))
        )
        When("the model validates the values without normalization")
        val result = PreparationModel.validate(bundle)
        Then("the complete bundle and literal content remain exact")
        result shouldBe Right(bundle)
        result.toOption.get.resources.head.title shouldBe value
        result.toOption.get.contexts.head.ref.id shouldBe value
        result.toOption.get.evidence.head.snapshot.content shouldBe original.evidence.head.snapshot.content
        result.toOption.get.materials.head.sections.head.text shouldBe original.materials.head.sections.head.text
      }
    }
  }

  "Frozen Run input" should {
    "survive changes to the current Context and resource version after roundtrip" in {
      Given("a historical Run and a changed current Context, binding, baseline and resource")
      val original = _fixture()
      val context = original.contexts.head
      val changed = original.copy(
        resources = original.resources.map(resource => resource.copy(title = "Current title changed")) :+
          original.resources.head.copy(ref = VersionedRef(original.resources.head.ref.id, 3)),
        contexts = Vector(context.copy(
          ref = VersionedRef(context.ref.id, 2),
          evidenceRefs = Vector(original.evidence.last.ref),
          bindings = Vector(context.bindings.head.copy(externalId = "current-project-002")),
          existingInformation = Vector(context.existingInformation.head.copy(
            ref = VersionedRef("information:baseline", 4),
            snapshot = context.existingInformation.head.snapshot.copy(content = Some("Current baseline"))
          ))
        ))
      )
      When("the changed canonical bundle is encoded and decoded")
      val result = PreparationJson.encode(changed).flatMap(PreparationJson.decode)
      Then("the embedded historical content, binding, baseline and Context revision remain exact")
      result shouldBe Right(changed)
      val input = result.toOption.get.runs.head.input
      input shouldBe original.runs.head.input
      input.evidence.head.snapshot.content shouldBe Some("日本語の資料 🌱\n第二行\n")
      input.context.bindings.head.externalId shouldBe "drive-project-001"
      input.context.existingInformation.head.ref.revision shouldBe 3L
      input.context.existingInformation.head.snapshot.content shouldBe Some("既存情報 baseline\n")
      input.context.ref.revision shouldBe 1L
    }

    "resolve historical sources independently of current top-level selections" in {
      Given("a Run whose historical Evidence and resources are absent from the current top-level snapshot")
      val original = _fixture()
      val historical = original.copy(resources = Vector.empty, evidence = Vector.empty, contexts = Vector.empty)
      When("the bundle is validated")
      val result = PreparationModel.validate(historical)
      Then("the internally complete historical Run still supports its material and proposal")
      result shouldBe Right(historical)
    }
  }

  "Retention and provenance" should {
    "preserve retained empty text and reference-only unknown metadata" in {
      Given("retained evidence with empty content and reference-only evidence with unknown actor and revision")
      val original = _fixture()
      val empty = original.evidence.head.copy(snapshot = original.evidence.head.snapshot.copy(content = Some("")))
      val run = original.runs.head
      val bundle = original.copy(evidence = Vector(empty, original.evidence.last), runs = Vector(run.copy(input = run.input.copy(evidence = Vector(empty, original.evidence.last)))))
      When("the bundle is validated and roundtripped")
      val result = PreparationJson.encode(bundle).flatMap(PreparationJson.decode)
      Then("empty retained content remains present while unknown and unretained fields remain absent")
      result shouldBe Right(bundle)
      val evidence = result.toOption.get.runs.head.input.evidence
      evidence.head.snapshot.retention shouldBe Retention.Retained
      evidence.head.snapshot.content shouldBe Some("")
      evidence.last.snapshot.retention shouldBe Retention.ReferenceOnly
      evidence.last.snapshot.content shouldBe None
      evidence.last.snapshot.provenance.actor shouldBe None
      evidence.last.providerRevision shouldBe None
    }

    val invalid: Vector[(String, PreparationBundle => PreparationBundle, String)] = Vector(
      ("retained content absence", bundle => bundle.copy(evidence = bundle.evidence.updated(0, bundle.evidence.head.copy(snapshot = bundle.evidence.head.snapshot.copy(content = None)))), "retention"),
      ("reference-only content presence", bundle => bundle.copy(evidence = bundle.evidence.updated(1, bundle.evidence.last.copy(snapshot = bundle.evidence.last.snapshot.copy(content = Some("not retained"))))), "retention"),
      ("known provenance without actor", bundle => bundle.copy(runs = Vector(bundle.runs.head.copy(processor = Provenance(ProvenanceStatus.Known, None, "Manual fixture")))), "provenance"),
      ("unknown provenance with actor", bundle => bundle.copy(runs = Vector(bundle.runs.head.copy(processor = Provenance(ProvenanceStatus.Unknown, Some("invented"), "Unknown origin")))), "provenance"),
      ("nonpositive revision", bundle => bundle.copy(resources = bundle.resources.updated(0, bundle.resources.head.copy(ref = VersionedRef("resource:資料/α", 0)))), "revision"),
      ("blank opaque identity", bundle => bundle.copy(runs = Vector(bundle.runs.head.copy(id = " \n"))), "blank"),
      ("duplicate exact resource revision", bundle => bundle.copy(resources = bundle.resources :+ bundle.resources.head), "duplicate"),
      ("duplicate exact evidence revision", bundle => bundle.copy(evidence = bundle.evidence :+ bundle.evidence.head), "duplicate"),
      ("duplicate exact Context revision", bundle => bundle.copy(contexts = bundle.contexts :+ bundle.contexts.head), "duplicate"),
      ("duplicate Run ID", bundle => bundle.copy(runs = bundle.runs :+ bundle.runs.head), "duplicate"),
      ("duplicate material revision", bundle => bundle.copy(materials = bundle.materials :+ bundle.materials.head), "duplicate"),
      ("duplicate producer/proposal identity", bundle => bundle.copy(proposals = bundle.proposals :+ bundle.proposals.head), "duplicate"),
      ("duplicate binding ID", bundle => bundle.copy(contexts = Vector(bundle.contexts.head.copy(bindings = bundle.contexts.head.bindings ++ bundle.contexts.head.bindings))), "duplicate"),
      ("duplicate baseline revision", bundle => bundle.copy(contexts = Vector(bundle.contexts.head.copy(existingInformation = bundle.contexts.head.existingInformation ++ bundle.contexts.head.existingInformation))), "duplicate"),
      ("duplicate Context selection", bundle => bundle.copy(contexts = Vector(bundle.contexts.head.copy(evidenceRefs = bundle.contexts.head.evidenceRefs :+ bundle.contexts.head.evidenceRefs.head))), "duplicate"),
      ("duplicate Run snapshot identity", bundle => {
        val run = bundle.runs.head
        val evidence = run.input.evidence.last.copy(snapshot = run.input.evidence.last.snapshot.copy(id = run.input.evidence.head.snapshot.id))
        bundle.copy(runs = Vector(run.copy(input = run.input.copy(evidence = run.input.evidence.updated(1, evidence)))))
      }, "duplicate"),
      ("baseline and evidence snapshot collision", bundle => {
        val run = bundle.runs.head
        val context = run.input.context
        val information = context.existingInformation.head
        bundle.copy(runs = Vector(run.copy(input = run.input.copy(context = context.copy(existingInformation = Vector(information.copy(snapshot = information.snapshot.copy(id = run.input.evidence.head.snapshot.id))))))))
      }, "duplicate"),
      ("duplicate section citation", bundle => {
        val material = bundle.materials.head
        val section = material.sections.head
        bundle.copy(materials = Vector(material.copy(sections = material.sections.updated(0, section.copy(evidenceRefs = section.evidenceRefs ++ section.evidenceRefs)))))
      }, "duplicate"),
      ("duplicate proposal citation", bundle => bundle.copy(proposals = Vector(bundle.proposals.head.copy(evidenceRefs = bundle.proposals.head.evidenceRefs :+ bundle.proposals.head.evidenceRefs.head))), "duplicate")
    )
    invalid.foreach { (label, change, code) =>
      s"reject $label" in {
        Given(s"a bundle containing $label")
        val bundle = change(_fixture())
        When("the pure model validator receives it")
        val result = PreparationModel.validate(bundle)
        Then(s"a structured $code error identifies the defect")
        result.isLeft shouldBe true
        result.swap.toOption.get.map(_.code) should contain(code)
        all(result.swap.toOption.get.map(_.path)) should startWith("$")
      }
    }
  }

  "Lineage validation" should {
    val invalid: Vector[(String, PreparationBundle => PreparationBundle, String)] = Vector(
      ("orphan top-level Evidence", bundle => bundle.copy(resources = Vector.empty), "unresolved"),
      ("orphan current Context selection", bundle => bundle.copy(contexts = Vector(bundle.contexts.head.copy(evidenceRefs = Vector(VersionedRef("missing", 1))))), "unresolved"),
      ("missing embedded Resource", bundle => bundle.copy(runs = Vector(bundle.runs.head.copy(input = bundle.runs.head.input.copy(resources = Vector.empty)))), "unresolved"),
      ("extra embedded Resource", bundle => {
        val run = bundle.runs.head
        val resource = run.input.resources.head.copy(ref = VersionedRef("unselected", 1))
        bundle.copy(runs = Vector(run.copy(input = run.input.copy(resources = run.input.resources :+ resource))))
      }, "selection"),
      ("mismatched frozen Evidence selection", bundle => {
        val run = bundle.runs.head
        bundle.copy(runs = Vector(run.copy(input = run.input.copy(context = run.input.context.copy(evidenceRefs = Vector.empty)))))
      }, "selection"),
      ("orphan material Run", bundle => bundle.copy(materials = Vector(bundle.materials.head.copy(producingRunId = "missing"))), "unresolved"),
      ("empty material sections", bundle => bundle.copy(materials = Vector(bundle.materials.head.copy(sections = Vector.empty))), "empty"),
      ("empty material citations", bundle => {
        val material = bundle.materials.head
        bundle.copy(materials = Vector(material.copy(sections = Vector(material.sections.head.copy(evidenceRefs = Vector.empty)))))
      }, "empty"),
      ("orphan material citation", bundle => {
        val material = bundle.materials.head
        bundle.copy(materials = Vector(material.copy(sections = Vector(material.sections.head.copy(evidenceRefs = Vector(VersionedRef("missing", 1)))))))
      }, "unresolved"),
      ("orphan proposal material", bundle => bundle.copy(proposals = Vector(bundle.proposals.head.copy(materialRef = VersionedRef("missing", 1)))), "unresolved"),
      ("wrong material revision", bundle => bundle.copy(proposals = Vector(bundle.proposals.head.copy(materialRef = bundle.materials.head.ref.copy(revision = 2)))), "unresolved"),
      ("mismatched proposal Run", bundle => {
        val run = bundle.runs.head.copy(id = "other-run")
        bundle.copy(runs = bundle.runs :+ run, proposals = Vector(bundle.proposals.head.copy(producingRunId = run.id)))
      }, "lineage"),
      ("orphan proposal Run", bundle => bundle.copy(proposals = Vector(bundle.proposals.head.copy(producingRunId = "missing"))), "unresolved"),
      ("orphan proposal citation", bundle => bundle.copy(proposals = Vector(bundle.proposals.head.copy(evidenceRefs = bundle.proposals.head.evidenceRefs :+ VersionedRef("missing", 1)))), "unresolved"),
      ("incomplete proposal coverage", bundle => bundle.copy(proposals = Vector(bundle.proposals.head.copy(evidenceRefs = Vector(bundle.evidence.head.ref)))), "coverage"),
      ("empty proposal citations", bundle => bundle.copy(proposals = Vector(bundle.proposals.head.copy(evidenceRefs = Vector.empty))), "empty"),
      ("orphan failure citation", bundle => bundle.copy(runs = Vector(bundle.runs.head.copy(status = RunStatus.Partial, failures = Vector(ProcessingFailure("input", "Unavailable input", Some(VersionedRef("missing", 1))))))), "unresolved")
    )
    invalid.foreach { (label, change, code) =>
      s"reject $label without source access" in {
        Given(s"a self-contained fixture altered to have $label")
        val bundle = change(_fixture())
        When("the model, encoder and Markdown boundaries receive the invalid bundle")
        val results = Vector(
          PreparationModel.validate(bundle).map(_ => ()),
          PreparationJson.encode(bundle).map(_ => ()),
          PreparedMaterialMarkdown.render(bundle, bundle.materials.head.ref).map(_ => ())
        )
        Then("each boundary reports the lineage defect as a structured error")
        all(results.map(_.isLeft)) shouldBe true
        results.foreach(result => result.swap.toOption.get.map(_.code) should contain(code))
      }
    }
  }

  "Run status" should {
    "preserve observable partial and failed descriptors while blocking failed material production" in {
      Given("partial and failed Runs with explicit failures tied to their fixed Evidence")
      val original = _fixture()
      val failure = ProcessingFailure("manual-input-unavailable", "Reference-only content was not available", Some(original.evidence.last.ref))
      val partial = original.runs.head.copy(status = RunStatus.Partial, failures = Vector(failure))
      val failed = partial.copy(id = "run-failed", status = RunStatus.Failed)
      val bundle = original.copy(runs = Vector(partial, failed))
      val invalid = bundle.copy(materials = Vector(bundle.materials.head.copy(producingRunId = failed.id)))
      When("the observable Runs are roundtripped and failed production is validated")
      val result = PreparationJson.encode(bundle).flatMap(PreparationJson.decode)
      val rejected = PreparationModel.validate(invalid)
      Then("the statuses and failure descriptors survive and failed material has a specific error")
      result shouldBe Right(bundle)
      result.toOption.get.runs.map(_.failures) shouldBe Vector(Vector(failure), Vector(failure))
      rejected.swap.toOption.get.map(_.code) should contain("failed-run")
    }

    "require descriptors for partial and failed Runs and no descriptors for completed Runs" in {
      Given("completed-with-failure, partial-without-failure and failed-without-failure Runs")
      val original = _fixture()
      val run = original.runs.head
      val runs = Vector(
        run.copy(failures = Vector(ProcessingFailure("input", "Missing input", None))),
        run.copy(status = RunStatus.Partial),
        run.copy(status = RunStatus.Failed)
      )
      When("each status/descriptor combination is validated")
      val results = runs.map(run => PreparationModel.validate(original.copy(runs = Vector(run))))
      Then("each inconsistent combination reports a status error")
      results.foreach(result => result.swap.toOption.get.map(_.code) should contain("status"))
    }
  }

  private def _fixture(): PreparationBundle =
    val source = Source.fromResource("tkl/preparation-bundle-v1.json", getClass.getClassLoader)
    val text = try source.mkString finally source.close()
    PreparationJson.decode(text).fold(errors => fail(errors.mkString("; ")), identity)
