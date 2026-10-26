package org.simplemodeling.textus.knowledgelake.material

import org.goldenport.cncf.action.QueryAction
import org.goldenport.observation.Taxonomy
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 21, 2026
 * @version Oct. 22, 2026
 */
final class MaterialReferenceOperationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import MaterialReferenceFixture.*

  "Native Material query factory" should {
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
