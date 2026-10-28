package org.simplemodeling.textus.knowledgelake.material

import org.goldenport.cncf.action.QueryAction
import io.circe.Json
import org.goldenport.observation.Taxonomy
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 21, 2026
 * @version Oct. 28, 2026
 */
final class MaterialReferenceOperationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import MaterialReferenceFixture.*

  "Native Material query factory" should {
    "explicit source evidence through existing getMaterial" which {
      "observe the exact selected original version without another public Operation or byte disclosure" in {
        Given("a caller-authorized Material and managed source reporting the same version around a complete binary read")
        withSession() { session =>
          val versioned = replace(metadata, "version", Json.fromString("17"))
          val driver = new Driver(Vector(jsonResponse(versioned), mediaResponse(), jsonResponse(versioned)))
          val before = session.custody
          When("the caller explicitly requests one logical original's evidence through generated getMaterial")
          val result = record(session.execute("getMaterial", Vector("materialRef" -> materialRef,
            "evidenceResourceIds" -> Vector(resourceId)), driver = driver))
          Then("metadata adds one typed logical version observation and retains provider/private custody boundaries")
          val observation = result.getAsC[Vector[Record]]("observedEvidence").TAKE.get.head
          observation.getString("materialRef") shouldBe Some(materialRef)
          observation.getString("resourceId") shouldBe Some(resourceId)
          observation.getString("revision") shouldBe Some("17")
          observation.getString("retrievalState") shouldBe Some("available")
          org.simplemodeling.textus.knowledgelake.value.MaterialMetadata.createC(result).TAKE
            .observedEvidence.head.revision shouldBe Some("17")
          driver.calls.map(_.path) shouldBe Vector(metadataUrl, mediaUrl, metadataUrl)
          driver.calls.map(_.method).distinct shouldBe Vector("GET")
          privateMarkers.foreach(marker => result.print should not include marker)
          result.getString("content") shouldBe None
          session.custody shouldBe before
        }
      }

      "retain exact metadata-only compatibility for omitted and explicit empty evidence selections" in {
        Given("an initialized Material catalog without credentials or any provider grant")
        withSession(connection = "absent") { session =>
          val driver = new Driver(Vector.empty)
          When("a legacy caller omits evidence and another supplies an empty collection")
          val omitted = record(session.execute("getMaterial", Vector("materialRef" -> materialRef), driver = driver))
          val explicit = record(session.execute("getMaterial", Vector("materialRef" -> materialRef,
            "evidenceResourceIds" -> Vector.empty[String]), driver = driver))
          Then("both results have the old four fields and require no original access")
          explicit shouldBe omitted
          explicit.fields.map(_.key).toSet shouldBe Set("materialRef", "title", "resources", "provenance")
          org.simplemodeling.textus.knowledgelake.value.MaterialMetadata.createC(omitted).TAKE.observedEvidence shouldBe empty
          driver.calls shouldBe Vector.empty
          session.acquisitions shouldBe 0
        }
      }

      "return unverified evidence when readable originals have no source version" in {
        Given("an admitted original whose provider metadata has no source version")
        withSession() { session =>
          val driver = new Driver(Vector(jsonResponse(metadata), mediaResponse(), jsonResponse(metadata)))
          When("getMaterial explicitly observes that original")
          val result = record(session.execute("getMaterial", Vector("materialRef" -> materialRef,
            "evidenceResourceIds" -> Vector(resourceId)), driver = driver))
          Then("readable bytes alone do not become versioned or review-ready evidence")
          val observation = result.getAsC[Vector[Record]]("observedEvidence").TAKE.get.head
          observation.getString("retrievalState") shouldBe Some("unverified")
          observation.getString("revision") shouldBe None
          observation.getString("snapshotRef") shouldBe None
          driver.calls should have size 3
        }
      }

      "reject all missing or invalid selected originals before custody or any original provider access" in {
        Given("a valid logical Material with no connection configuration")
        withSession(connection = "absent") { session =>
          val invalid = Vector(Vector(resourceId, resourceId), Vector(""), Vector("bad\nresource"),
            Vector("x" * 257), (0 to 128).map(index => s"resource-$index").toVector)
          val missing = Vector(Vector("missing"), Vector(resourceId, "missing"))
          (invalid.map(_ -> Taxonomy.argumentInvalid) ++ missing.map(_ -> Taxonomy.resourceNotFound)).foreach { case (ids, taxonomy) =>
            val driver = new Driver(Vector.empty)
            When("getMaterial resolves the complete explicit logical selection")
            val result = session.execute("getMaterial", Vector("materialRef" -> materialRef,
              "evidenceResourceIds" -> ids), driver = driver)
            Then("selection failure cannot partly observe an earlier valid resource or open credentials")
            failure(result).observation.taxonomy shouldBe taxonomy
            driver.calls shouldBe empty
            session.acquisitions shouldBe 0
          }
        }
      }

      "keep Material and anonymous caller admission ahead of evidence lookup" in {
        Given("an unavailable provider connection and one owner's known logical Material")
        withSession(connection = "absent") { session =>
          Vector(Some("material-other-session") -> Taxonomy.securityPermissionDenied,
            None -> Taxonomy.securityAuthenticationRequired).foreach { case (caller, taxonomy) =>
            val driver = new Driver(Vector.empty)
            When("a foreign or anonymous caller requests source observations")
            val result = session.execute("getMaterial", Vector("materialRef" -> materialRef,
              "evidenceResourceIds" -> Vector(resourceId)), sessionId = caller, driver = driver)
            Then("source versions and connection availability remain undisclosed")
            failure(result).observation.taxonomy shouldBe taxonomy
            driver.calls shouldBe empty
          }
        }
      }

      "withhold the complete response after a version change instead of rereading automatically" in {
        Given("the exact same sized original changing its source version during read")
        withSession() { session =>
          val first = replace(metadata, "version", Json.fromString("17"))
          val last = replace(metadata, "version", Json.fromString("18"))
          val driver = new Driver(Vector(jsonResponse(first), mediaResponse(), jsonResponse(last)))
          When("getMaterial observes the changed post-read version")
          val result = session.execute("getMaterial", Vector("materialRef" -> materialRef,
            "evidenceResourceIds" -> Vector(resourceId)), driver = driver)
          Then("no successful old or partial evidence is available and only one read occurred")
          failure(result).observation.taxonomy shouldBe Taxonomy.resourceInvalid
          driver.calls.map(_.path) shouldBe Vector(metadataUrl, mediaUrl, metadataUrl)
        }
      }
    }

    "logical search and metadata" which {
      "dispatch all three generated queries through the real owning factory" in {
        Given("a normal initialized participant with the synthetic ingress provider and a trusted catalog")
        withSession(connection = "absent") { session =>
          val requests = Vector(session.request("searchMaterials"), session.request("getMaterial", Vector("materialRef" -> materialRef)),
            session.request("readOriginal", Vector("materialRef" -> materialRef, "resourceId" -> resourceId)))
          When("the normal generated operation definitions interpret the public requests")
          val actions = requests.map(session.component.logic.makeOperationRequest)
          Then("every operation is a generated native query bound to the real participant factory")
          actions.foreach(_.TAKE shouldBe a[QueryAction])
          session.component.operationDefinitions.map(_.name).toSet shouldBe Set("searchMaterials", "getMaterial", "readOriginal",
            "createMaterialRegistration", "getMaterialRegistration", "materializeMaterial")
          session.component.factoryOption should not be empty
          session.component.materialCatalog should not be empty
        }
      }

      "search and resolve exact opaque metadata without a grant, outbound access or business writes" in {
        Given("a normally initialized catalog with no OAuth configuration or grant")
        withSession(connection = "absent") { session =>
          val before = session.custody
          val driver = new Driver(Vector.empty)
          When("the provider-admitted caller searches and gets through generated query dispatch")
          val search = session.execute("searchMaterials", Vector("text" -> "EVIDENCE"), driver = driver)
          val selected = session.execute("getMaterial", Vector("materialRef" -> materialRef), driver = driver)
          Then("metadata contains only logical contract fields while private custody and HTTP remain untouched")
          val page = record(search)
          page.getAsC[Vector[Record]]("items").TAKE.get.map(_.getString("materialRef")) shouldBe Vector(Some(materialRef))
          page.getAsC[Int]("offset").TAKE shouldBe Some(0)
          page.getAsC[Int]("limit").TAKE shouldBe Some(20)
          page.getAsC[Boolean]("hasMore").TAKE shouldBe Some(false)
          val metadata = record(selected)
          metadata.getString("materialRef") shouldBe Some(materialRef)
          metadata.getString("title") shouldBe Some("Original evidence")
          val resource = metadata.getAsC[Vector[Record]]("resources").TAKE.get.head
          resource.getString("resourceId") shouldBe Some(resourceId)
          resource.getString("role") shouldBe Some("original")
          resource.getString("mediaType") shouldBe Some("image/png")
          resource.getAsC[Long]("byteLength").TAKE shouldBe Some(4L)
          metadata.getAsC[Vector[Record]]("provenance").TAKE.get.head.getString("reference") shouldBe Some("urn:evidence:source-001")
          Vector(page.print, metadata.print).foreach(output => privateMarkers.foreach(marker => output should not include marker))
          driver.calls shouldBe empty
          session.acquisitions shouldBe 0
          session.custody shouldBe before
        }
      }

      "distinguish absent capability from an explicitly empty authorized page" in {
        Given("normal initialized instances with absent and explicitly empty catalog settings")
        val variants = Vector(None, Some(catalog(Vector.empty).noSpaces))
        variants.foreach { setting =>
          withSession(catalogText = setting, connection = "absent") { session =>
            When("the admitted caller requests its first search page")
            val result = session.execute("searchMaterials")
            Then("absence is unavailable and explicit emptiness is a successful page with items=[]")
            setting match {
              case None => failure(result).observation.taxonomy shouldBe Taxonomy.serviceUnavailable
              case Some(_) =>
                record(result).getAsC[Vector[Record]]("items").TAKE shouldBe Some(Vector.empty)
                record(result).getAsC[Boolean]("hasMore").TAKE shouldBe Some(false)
            }
            session.acquisitions shouldBe 0
          }
        }
      }

      "page only authorized entries while retaining unrelated logical identity domains" in {
        Given("three authorized entries in reverse order and one unauthorized title match")
        val entries = Vector(entry("urn:other:z", "match Z"), entry("urn:private:hidden", "match hidden", Vector("other-subject")),
          entry("urn:knowledge-lake:m", "match M"), entry("urn:other:a", "match A"))
        withSession(catalogText = Some(catalog(entries).noSpaces), connection = "absent") { session =>
          When("the owner searches the second page with a case-insensitive query")
          val result = session.execute("searchMaterials", Vector("text" -> "MATCH", "offset" -> 1, "limit" -> 2))
          Then("the page and hasMore are computed solely from authorized matches in stable exact-reference order")
          record(result).getAsC[Vector[Record]]("items").TAKE.get.map(_.getString("materialRef").get) shouldBe Vector("urn:other:a", "urn:other:z")
          record(result).getAsC[Boolean]("hasMore").TAKE shouldBe Some(false)
          record(result).print should not include "hidden"
          session.acquisitions shouldBe 0
        }
      }
    }

    "caller and argument admission" which {
      "deny unauthenticated callers before catalog or credential access" in {
        Given("an absent catalog and OAuth configuration, with no provider-admitted caller")
        withSession(catalogText = None, connection = "absent") { session =>
          Vector("searchMaterials" -> Vector.empty[(String, Any)], "getMaterial" -> Vector("materialRef" -> materialRef),
            "readOriginal" -> Vector("materialRef" -> materialRef, "resourceId" -> resourceId)).foreach { case (operation, fields) =>
            val driver = new Driver(Vector.empty)
            When("an anonymous request attempts each normal query route")
            val result = session.execute(operation, fields, sessionId = None, driver = driver)
            Then("the route denies admission without exposing whether the catalog or grant exists")
            result.isSuccess shouldBe false
            failure(result).observation.taxonomy shouldBe Taxonomy.securityAuthenticationRequired
            driver.calls shouldBe empty
            session.acquisitions shouldBe 0
          }
        }
      }

      "hide another subject's search entries and deny its exact known Material reference" in {
        Given("a Material authorized for one exact subject and no available connection")
        withSession(connection = "absent") { session =>
          When("another provider-authenticated subject searches and requests the known reference")
          val page = session.execute("searchMaterials", sessionId = Some("material-other-session"))
          val metadata = session.execute("getMaterial", Vector("materialRef" -> materialRef), sessionId = Some("material-other-session"))
          Then("search is empty and get fails with permission denied ahead of any provider work")
          record(page).getAsC[Vector[Record]]("items").TAKE shouldBe Some(Vector.empty)
          record(page).getAsC[Boolean]("hasMore").TAKE shouldBe Some(false)
          failure(metadata).observation.taxonomy shouldBe Taxonomy.securityPermissionDenied
          session.acquisitions shouldBe 0
        }
      }

      "reject invalid bounded selectors and paging without a provider call" in {
        Given("the normal query factory and valid caller with invalid raw request fields")
        withSession(connection = "absent") { session =>
          val searches = Vector(Vector("text" -> ("x" * 257)), Vector("offset" -> -1), Vector("limit" -> 0),
            Vector("limit" -> 101), Vector("text" -> "line\nbreak"))
          val selections = Vector("", "x" * 2049, "bad\nreference")
          val requests = searches.map("searchMaterials" -> _) ++ selections.map(value => "getMaterial" -> Vector("materialRef" -> value)) ++
            Vector("", "x" * 257, "bad\rresource").map(value => "readOriginal" -> Vector("materialRef" -> materialRef, "resourceId" -> value))
          requests.foreach { case (operation, fields) =>
            When("generated DTO parsing and the actual action interpret the bounded arguments")
            val result = session.execute(operation, fields)
            Then("invalid arguments have a structured rejection and perform no outbound lookup")
            result.isSuccess shouldBe false
            failure(result).observation.taxonomy shouldBe Taxonomy.argumentInvalid
            session.acquisitions shouldBe 0
          }
        }
      }

      "preserve exact opaque selectors and distinguish missing Materials from denied Materials" in {
        Given("one admitted opaque Material reference")
        withSession(connection = "absent") { session =>
          Vector(" " + materialRef, materialRef + " ", "urn:another:missing").foreach { reference =>
            When("a caller passes a different exact reference, including surrounding spaces")
            val result = session.execute("getMaterial", Vector("materialRef" -> reference))
            Then("the runtime neither trims nor constructs a replacement reference")
            failure(result).observation.taxonomy shouldBe Taxonomy.resourceNotFound
            privateMarkers.foreach(marker => result.display should not include marker)
          }
        }
      }
    }
  }
}
