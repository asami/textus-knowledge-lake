package org.simplemodeling.textus.knowledgelake.material

import org.goldenport.bag.BinaryBag
import org.goldenport.observation.Taxonomy
import org.goldenport.protocol.operation.OperationResponse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.circe.Json

/**
 * Controlled native provider evidence; this does not assert real Google acceptance.
 *
 * @since   Oct. 21, 2026
 * @version Oct. 21, 2026
 */
final class MaterialOriginalReadSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import MaterialReferenceFixture.*
  private val _selection: Vector[(String, Any)] = Vector("materialRef" -> materialRef, "resourceId" -> resourceId)

  "Native managed Material original" should {
    "exact read and binary transport" which {
      "stat the selected original before one media GET and preserve its Bag, bytes and private headers" in {
        Given("a provider-admitted owner, a private managed grant and matching native metadata/media responses")
        withSession() { session =>
          val media = mediaResponse()
          val driver = new Driver(Vector(jsonResponse(metadata), media))
          val before = session.custody
          When("the normal generated readOriginal action interprets the native FileStore program")
          val result = session.execute("readOriginal", _selection, driver = driver)
          val response = result.TAKE match {
            case OperationResponse.Http(value) => value
            case _ => fail("Expected the native private binary original response")
          }
          val returned = UsingBag.read(response.bag.asInstanceOf[BinaryBag])
          Then("exact metadata precedes media once and the same bytes travel in the safe private binary response")
          response shouldBe a[org.goldenport.http.HttpResponse.Binary]
          response.bag should be theSameInstanceAs media.bag
          returned shouldBe bytes
          response.mime.value shouldBe "image/png"
          response.headerValue("Cache-Control") shouldBe Some("private, no-store")
          response.headerValue("X-Content-Type-Options") shouldBe Some("nosniff")
          response.headerValue("Content-Security-Policy") shouldBe Some("default-src 'none'; sandbox")
          response.headerValue("Content-Disposition") shouldBe Some("attachment; filename=\"original\"")
          driver.calls.map(_.method) shouldBe Vector("GET", "GET")
          driver.calls.map(_.path) shouldBe Vector(metadataUrl, mediaUrl)
          driver.calls.map(_.headers("Authorization")) shouldBe Vector.fill(2)(s"Bearer $token")
          session.acquisitions shouldBe 2
          session.custody shouldBe before
          privateMarkers.foreach(marker => response.header.print should not include marker)
        }
      }
    }

    "authorization before managed connection lookup" which {
      "deny a different Material subject before observing a missing managed connection" in {
        Given("a Material owned by another subject and an absent OAuth declaration")
        withSession(connection = "absent") { session =>
          val driver = new Driver(Vector.empty)
          val before = session.custody
          When("a provider-authenticated but unauthorized subject requests its known logical original")
          val result = session.execute("readOriginal", _selection, sessionId = Some("material-other-session"), driver = driver)
          Then("Material denial wins over missing connection and there is no credential or outbound side effect")
          failure(result).observation.taxonomy shouldBe Taxonomy.securityPermissionDenied
          session.acquisitions shouldBe 0
          driver.calls shouldBe empty
          session.custody shouldBe before
        }
      }

      "distinguish a missing logical Material or resource without consulting credentials" in {
        Given("an authorized caller, absent OAuth configuration and missing logical selectors")
        withSession(connection = "absent") { session =>
          Vector(Vector("materialRef" -> "urn:missing:material", "resourceId" -> resourceId),
            Vector("materialRef" -> materialRef, "resourceId" -> "missing-logical-resource")).foreach { fields =>
            val driver = new Driver(Vector.empty)
            When("the native read query resolves the exact logical selection")
            val result = session.execute("readOriginal", fields, driver = driver)
            Then("the missing logical selection is a structured not-found before credential lookup")
            failure(result).observation.taxonomy shouldBe Taxonomy.resourceNotFound
            driver.calls shouldBe empty
            session.acquisitions shouldBe 0
          }
        }
      }

      "distinguish absent connections from required grant authorization without HTTP or metadata loss" in {
        Vector("absent", "disconnected").foreach { state =>
          Given(s"an authorized Material whose managed connection is $state")
          withSession(connection = state) { session =>
            val driver = new Driver(Vector.empty)
            val before = session.custody
            When("the owner reads the original and independently gets its logical metadata")
            val read = session.execute("readOriginal", _selection, driver = driver)
            val selected = session.execute("getMaterial", Vector("materialRef" -> materialRef), driver = driver)
            Then("the read distinguishes unavailable configuration from required authorization while metadata succeeds without provider calls or grant changes")
            state match {
              case "absent" => failure(read).observation.taxonomy shouldBe Taxonomy.serviceUnavailable
              case _ =>
                failure(read).observation.taxonomy shouldBe Taxonomy.securityPermissionDenied
                failure(read).displayMessage shouldBe "OAuth connection requires authorization"
            }
            record(selected).getString("materialRef") shouldBe Some(materialRef)
            driver.calls shouldBe empty
            session.acquisitions shouldBe 0
            session.custody shouldBe before
            privateMarkers.foreach(marker => read.display should not include marker)
          }
        }
      }

      "retain the native not-configured conclusion for a selected unknown managed connection" in {
        Given("a valid native grant and a trusted resource selecting another well-formed connection ID")
        val original = entry()
        val resource = original.hcursor.downField("resources").focus.get.asArray.get.head
        val storage = resource.hcursor.downField("storage").focus.get
        val configured = replace(original, "resources", Json.arr(replace(resource, "storage",
          replace(storage, "connectionId", Json.fromString("another-connection")))))
        withSession(catalogText = Some(catalog(Vector(configured)).noSpaces)) { session =>
          val driver = new Driver(Vector.empty)
          When("the native managed binding resolves the selected connection through its actual owning component")
          val result = session.execute("readOriginal", _selection, driver = driver)
          Then("the framework's original not-found conclusion survives without a fallback or private projection")
          failure(result).observation.taxonomy shouldBe Taxonomy.resourceNotFound
          failure(result).displayMessage shouldBe "OAuth connection is not configured"
          privateMarkers.foreach(marker => result.display should not include marker)
          driver.calls shouldBe empty
          session.acquisitions shouldBe 0
        }
      }

      "retain managed grant ownership even when Material authorization includes another subject" in {
        Given("two authorized Material subjects but a managed grant owned by only the first")
        withSession(catalogText = Some(catalog(Vector(entry(subjects = Vector(owner, "other-subject")))).noSpaces)) { session =>
          val driver = new Driver(Vector.empty)
          When("the second provider-authenticated Material reader requests the owner's configured grant")
          val result = session.execute("readOriginal", _selection, sessionId = Some("material-other-session"), driver = driver)
          Then("native grant ownership denies access without provider calls or identity substitution")
          failure(result).observation.taxonomy shouldBe Taxonomy.securityPermissionDenied
          driver.calls shouldBe empty
          session.acquisitions shouldBe 0
        }
      }
    }

    "native admission and provider failures" which {
      Vector("absent", "wrong-owner", "wrong-origin").foreach { grant =>
        s"preserve $grant native HTTP admission failure without acquiring a driver" in {
          Given("an authorized Material and connected grant without the owning component's exact outbound authority")
          withSession(grant = grant) { session =>
            val driver = new Driver(Vector.empty)
            val before = session.custody
            When("the native FileStore stat reaches CNCF HTTP admission")
            val result = session.execute("readOriginal", _selection, driver = driver)
            Then("the structured native admission failure prevents all provider calls and leaves custody unchanged")
            result.isSuccess shouldBe false
            driver.calls shouldBe empty
            session.acquisitions shouldBe 0
            session.custody shouldBe before
            privateMarkers.foreach(marker => result.display should not include marker)
          }
        }
      }

      Vector(401 -> Taxonomy.securityPermissionDenied, 403 -> Taxonomy.securityPermissionDenied,
        404 -> Taxonomy.resourceNotFound, 429 -> Taxonomy.serviceUnavailable, 503 -> Taxonomy.serviceUnavailable).foreach { case (status, taxonomy) =>
        s"preserve safe native provider status $status after exactly one stat GET" in {
          Given("an admitted native stat whose provider responds with a private failure body")
          withSession() { session =>
            val body = Json.obj("private" -> Json.fromString(privateMarkers.mkString(" ")))
            val driver = new Driver(Vector(jsonResponse(body, status)))
            val before = session.custody
            When("the original query interprets the selected provider failure")
            val result = session.execute("readOriginal", _selection, driver = driver)
            Then("the category survives, no media/fallback occurs and private provider content is absent")
            failure(result).observation.taxonomy shouldBe taxonomy
            driver.calls.map(_.path) shouldBe Vector(metadataUrl)
            session.acquisitions shouldBe 1
            session.custody shouldBe before
            privateMarkers.foreach(marker => result.display should not include marker)
          }
        }
      }
    }

    "exact metadata and content confirmation" which {
      Vector("parent", "mime", "length", "kind", "id", "malformed").foreach { mismatch =>
        s"reject $mismatch metadata without issuing a media GET" in {
          Given("an authorized original whose provider metadata does not confirm the trusted binding")
          val document = mismatch match {
            case "parent" => replace(metadata, "parents", Json.arr(Json.fromString("OtherParent")))
            case "mime" => replace(metadata, "mimeType", Json.fromString("text/html"))
            case "length" => replace(metadata, "size", Json.fromString("5"))
            case "kind" => replace(metadata, "mimeType", Json.fromString("application/vnd.google-apps.folder"))
            case "id" => replace(metadata, "id", Json.fromString("OtherFile"))
            case _ => Json.obj("private" -> Json.fromString(token))
          }
          withSession() { session =>
            val driver = new Driver(Vector(jsonResponse(document)))
            val before = session.custody
            When("the actual original query confirms the provider's metadata")
            val result = session.execute("readOriginal", _selection, driver = driver)
            Then("the resource is invalid and there is no media call, fallback or mutation")
            failure(result).observation.taxonomy shouldBe Taxonomy.resourceInvalid
            driver.calls.map(_.path) shouldBe Vector(metadataUrl)
            session.custody shouldBe before
            privateMarkers.foreach(marker => result.display should not include marker)
          }
        }
      }

      Vector("mime", "length").foreach { mismatch =>
        s"reject changed $mismatch in returned content without rereading or fallback" in {
          Given("matching metadata followed by media that differs from the trusted declaration")
          withSession() { session =>
            val media = if (mismatch == "mime") mediaResponse(mediaType = "text/html") else mediaResponse(content = Vector[Byte](1))
            val driver = new Driver(Vector(jsonResponse(metadata), media))
            val before = session.custody
            When("the original query confirms the native media response")
            val result = session.execute("readOriginal", _selection, driver = driver)
            Then("the invalid response never becomes a binary original and exactly two GETs occurred")
            failure(result).observation.taxonomy shouldBe Taxonomy.resourceInvalid
            driver.calls.map(_.path) shouldBe Vector(metadataUrl, mediaUrl)
            session.custody shouldBe before
          }
        }
      }
    }
  }
}

private object UsingBag {
  def read(bag: BinaryBag): Vector[Byte] = {
    val stream = bag.openInputStream()
    try stream.readAllBytes().toVector finally stream.close()
  }
}
