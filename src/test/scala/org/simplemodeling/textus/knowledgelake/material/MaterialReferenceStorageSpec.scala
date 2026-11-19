package org.simplemodeling.textus.knowledgelake.material

import io.circe.Json
import org.goldenport.cncf.context.PrincipalId
import org.goldenport.cncf.workspace.{FileObservedReadResult, FileSourceStability, FileTarget, WorkspaceFilePath, WorkspaceId}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/* @since   Nov. 12, 2026
 * @version Nov. 12, 2026
 */
final class MaterialReferenceStorageSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  private val _config = Json.obj(
    "schema" -> Json.fromString("textus.material-local-reference/1"), "id" -> Json.fromString("local"),
    "allowedSubjects" -> Json.arr(Json.fromString("owner")), "lake" -> Json.fromString("heritage"),
    "logicalParent" -> Json.fromString("reports"), "destinationWorkspace" -> Json.fromString("destination"),
    "maximumBytes" -> Json.fromInt(40000000), "bindings" -> Json.arr(Json.obj(
      "id" -> Json.fromString("report"), "workspace" -> Json.fromString("source"), "path" -> Json.fromString("414/report.pdf"))))
  private val _contribution = Json.obj(
    "schema" -> Json.fromString("textus.material-contribution/1"), "title" -> Json.fromString("Report"),
    "createdAt" -> Json.fromString("2026-11-12"), "classifications" -> Json.arr(), "provenance" -> Json.obj(),
    "originals" -> Json.arr(Json.obj("resourceId" -> Json.fromString("report"), "role" -> Json.fromString("original"),
      "mediaType" -> Json.fromString("application/pdf"), "byteLength" -> Json.fromInt(3),
      "sourceReference" -> Json.obj("schema" -> Json.fromString("textus.material-source-binding/1"),
        "bindingId" -> Json.fromString("report"), "workspace" -> Json.fromString("source"),
        "path" -> Json.fromString("414/report.pdf")))), "documents" -> Json.arr())

  "Local reference registration" should {
    "retain the exact private binding and actual-byte baseline across document reconstruction" in {
      Given("an admitted binding and a complete observed read with unknown stability")
      val config = MaterialReferenceStorage.parseC(_config.noSpaces).TAKE
      val contribution = MaterialContribution.parseC(_contribution).TAKE
      val binding = config.resolve("report").get
      val observed = FileObservedReadResult(FileTarget(WorkspaceId.parseC("source").TAKE),
        WorkspaceFilePath.parseC("414/report.pdf").TAKE, Some(Vector[Byte](1, 2, 3)), None, None, FileSourceStability.Unknown)
      val entry = MaterialReferencedOriginal.observed("report", binding, observed).get
      val prepared = MaterialRegistrationDocument.preparedC(contribution,
        MaterialLocation.createC("heritage", "reports/issued").TAKE, "local", PrincipalId("owner"), PrincipalId("owner")).TAKE
      When("the native document transitions and is reconstructed from its JSON")
      val completed = prepared.beginC.TAKE.completeReferencesC(Vector(entry)).TAKE
      val restored = MaterialRegistrationDocument.parseC(completed.json).TAKE
      Then("the same exact source binding and immutable digest survive without a copied original")
      restored.references shouldBe Some(Vector(entry))
      restored.stored shouldBe None
      restored.status shouldBe MaterialRegistrationDocument.Status.Registered
      restored.references.get.head.stability shouldBe "unknown"
      MaterialContentIntegrity.compare(Some(restored.references.get.head.baseline), Some(Vector[Byte](1, 2, 4))) shouldBe
        MaterialContentIntegrity.Comparison.Changed
      val rebound = binding.copy(path = "other/report.pdf")
      MaterialReferenceStorage.matches(contribution.originals.head, rebound) shouldBe false
    }

    "reject malformed paths and duplicate logical bindings" in {
      Given("a host configuration containing traversal or duplicate identifiers")
      val binding = _config.hcursor.downField("bindings").focus.get.asArray.get.head
      val invalid = _config.mapObject(_.add("bindings", Json.arr(binding, binding)))
      val traversal = _config.mapObject(_.add("bindings", Json.arr(binding.mapObject(_.add("path", Json.fromString("../other"))))))
      When("the configuration is admitted")
      Then("neither can select a source")
      MaterialReferenceStorage.parseC(invalid.noSpaces).isSuccess shouldBe false
      MaterialReferenceStorage.parseC(traversal.noSpaces).isSuccess shouldBe false
    }

    "retain an observed change separately from matching byte content" in {
      Given("the same complete bytes and a provider-reported changed observation")
      val config = MaterialReferenceStorage.parseC(_config.noSpaces).TAKE
      val binding = config.resolve("report").get
      val observed = FileObservedReadResult(binding.target, binding.filePath, Some(Vector[Byte](1, 2, 3)),
        None, None, FileSourceStability.Changed)
      When("the byte baseline and source observation are retained")
      val entry = MaterialReferencedOriginal.observed("report", binding, observed).get
      Then("a content match does not become a stable source observation")
      MaterialContentIntegrity.compare(Some(entry.baseline), observed.bytes) shouldBe MaterialContentIntegrity.Comparison.Matched
      entry.stability shouldBe "changed"
    }
  }
}
