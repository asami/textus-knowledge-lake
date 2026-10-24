package org.simplemodeling.textus.knowledgelake.material

import java.nio.file.{Files, Path}
import java.nio.file.attribute.PosixFilePermissions
import java.security.KeyPairGenerator
import java.util.Base64
import io.circe.Json
import org.goldenport.cncf.filestore.GoogleDriveConnectionRuntime
import org.goldenport.protocol.operation.OperationResponse
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 24, 2026
 * @version Oct. 24, 2026
 */
final class MaterialServiceAccountSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import MaterialReferenceFixture.*
  import MaterialServiceAccountSpec.*

  "TKL service-account original access" should {
    "use the actual component factory and native HTTP token exchange without user OAuth configuration" in {
      Given("a TKL-owned service-account connection, native ingress authentication and an authorized logical Material")
      withSession(connection = "service-account") { session =>
        val token = tokenResponse
        val driver = new Driver(Vector(token, jsonResponse(metadata), mediaResponse(), jsonResponse(metadata), mediaResponse()), Some(token))
        When("the same logical original is read twice through generated public TKL Operations")
        val first = session.execute("readOriginal", Vector("materialRef" -> materialRef, "resourceId" -> resourceId), driver = driver)
        val second = session.execute("readOriginal", Vector("materialRef" -> materialRef, "resourceId" -> resourceId), driver = driver)
        Then("the owning runtime acquires once, preserves the original bytes and reuses its private token")
        Vector(first, second).foreach { outcome => outcome.TAKE match {
          case OperationResponse.Http(response) =>
            response.code shouldBe 200
            val input = response.bag.openInputStream()
            try input.readAllBytes().toVector shouldBe bytes finally input.close()
          case _ => fail("Expected the native original HTTP response")
        }}
        driver.calls.map(_.method) shouldBe Vector("POST", "GET", "GET", "GET", "GET")
        driver.calls.tail.foreach(_.headers("Authorization") shouldBe "Bearer synthetic-service-account-token")
        session.custody.keySet should not contain "grant.json"
        session.component.hasGoogleDriveConfiguration shouldBe true
      }
    }

    "preserve Material authorization before touching the shared server connection" in {
      Given("a shared server identity but a Material readable only by its admitted user")
      withSession(connection = "service-account") { session =>
        val driver = new Driver(Vector.empty, Some(tokenResponse))
        When("another provider-authenticated user requests the known original")
        val result = session.execute("readOriginal", Vector("materialRef" -> materialRef, "resourceId" -> resourceId),
          sessionId = Some("material-other-session"), driver = driver)
        Then("the existing Material denial precedes token exchange and provider access")
        result.isSuccess shouldBe false
        driver.calls shouldBe empty
      }
    }

    "not treat Material permission as service-account connection permission" in {
      Given("a Material visible to another user who is not in the connection's allowedSubjects")
      withSession(catalogText = Some(catalog(Vector(entry(subjects = Vector("other-subject")))).noSpaces), connection = "service-account") { session =>
        val driver = new Driver(Vector.empty, Some(tokenResponse))
        When("that user reads metadata and then the original")
        val metadata = session.execute("getMaterial", Vector("materialRef" -> materialRef), sessionId = Some("material-other-session"), driver = driver)
        val result = session.execute("readOriginal", Vector("materialRef" -> materialRef, "resourceId" -> resourceId),
          sessionId = Some("material-other-session"), driver = driver)
        Then("outbound connection admission refuses without using the server identity")
        metadata.isSuccess shouldBe true
        result.isSuccess shouldBe false
        driver.calls shouldBe empty
      }
    }

    "retain token-provider denial without a user-OAuth fallback or original read" in {
      Given("a selected server connection whose token exchange is denied")
      withSession(connection = "service-account") { session =>
        val denied = jsonResponse(Json.obj("error" -> Json.fromString("synthetic-provider-secret")), 403)
        val driver = new Driver(Vector(denied), Some(denied))
        When("the authorized user reads an original")
        val result = session.execute("readOriginal", Vector("materialRef" -> materialRef, "resourceId" -> resourceId), driver = driver)
        Then("one token POST fails safely, with no read, fallback or credential write")
        result.isSuccess shouldBe false
        result.toString should not include "synthetic-provider-secret"
        driver.calls.map(_.method) shouldBe Vector("POST")
        session.custody.keySet should not contain "grant.json"
      }
    }
  }
}

private[material] object MaterialServiceAccountSpec {
  def tokenResponse = MaterialReferenceFixture.jsonResponse(Json.obj("access_token" -> Json.fromString("synthetic-service-account-token"),
    "token_type" -> Json.fromString("Bearer"), "expires_in" -> Json.fromInt(3600)))

  def connectionSettings(root: Path): Map[String, String] = {
    val generator = KeyPairGenerator.getInstance("RSA")
    generator.initialize(2048)
    val pair = generator.generateKeyPair()
    val key = Json.obj("type" -> Json.fromString("service_account"), "private_key_id" -> Json.fromString("synthetic-key"),
      "client_email" -> Json.fromString("synthetic@fixture.iam.gserviceaccount.com"), "token_uri" -> Json.fromString("https://oauth2.googleapis.com/token"),
      "private_key" -> Json.fromString("-----BEGIN PRIVATE KEY-----\n" + Base64.getEncoder.encodeToString(pair.getPrivate.getEncoded) + "\n-----END PRIVATE KEY-----\n"))
    val file = root.resolve("service-key.json")
    _write(file, key.noSpaces)
    val config = Json.obj("schema" -> Json.fromString("cncf.google-drive-service-account/1"),
      "connectionId" -> Json.fromString(MaterialReferenceFixture.connectionId),
      "allowedSubjects" -> Json.arr(Json.fromString(MaterialReferenceFixture.owner)),
      "scopes" -> Json.arr(Json.fromString("https://www.googleapis.com/auth/drive.readonly")),
      "credentialFile" -> Json.fromString(file.toString))
    val source = root.resolve("service-connection.json")
    _write(source, config.noSpaces)
    Map(GoogleDriveConnectionRuntime.configurationKey.name -> source.toUri.toString)
  }

  private def _write(path: Path, text: String): Unit = {
    Files.writeString(path, text)
    Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
  }
}
