package org.simplemodeling.textus.knowledgelake.material

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.util.Using
import io.circe.{Json, parser}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * Local evidence-order regression using native commands and independent PostgreSQL reads.
 * These observations do not establish live Drive acceptance or Android success.
 *
 * @since   Oct. 25, 2026
 * @version Nov.  6, 2026
 */
final class Esa63RegistrationEvidenceSpec extends AnyWordSpec with Matchers with GivenWhenThen
    with MaterialRegistrationPostgresqlSpec {
  import MaterialRegistrationPersistenceFixture.*

  "Independent native registration evidence" should {
    "retain committed SQL identity and domain data before client completion" which {
      "remain readable after the owning schema closes without an Android success artifact" in {
        Given("a private local work directory, native PostgreSQL registration and no Android completion file")
        _with_work { work =>
          val expected = withDatabase(native_container) { database =>
            withSession(database) { session =>
              val contribution = take(MaterialContribution.parseC(MaterialContributionSpec.contributionJson()))
              val destination = new Destination(Files.createDirectory(session.root.resolve("destination")), database)
              val source = new Originals(Files.createDirectory(session.root.resolve("originals")), contribution)
              val prepared = take(session.create(contribution))
              val registered = take(session.materialize(prepared.entity.id, destination, source))
              val committed = database.snapshot().rows
              Files.exists(work.resolve("android.done")) shouldBe false

              When("the observer reads the committed registration through a separate JDBC connection")
              val observed = Esa63RegistrationEvidence.awaitRegistered(work, () => database.snapshot().rows,
                System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5))

              Then("the exact native row is saved before any client completion or final success")
              observed shouldBe committed
              observed should have size 1
              observed.head("id") shouldBe Some(registered.entity.id.value)
              observed.head("materialRef") shouldBe Some(registered.entity.document.location.urn)
              observed.head("state") shouldBe Some("registered")
              observed.head("owner_id") shouldBe Some(principal)
              val row = Json.obj("id" -> Json.fromString(registered.entity.id.value),
                "materialRef" -> Json.fromString(registered.entity.document.location.urn),
                "state" -> Json.fromString("registered"), "owner_id" -> Json.fromString(principal),
                "document" -> registered.entity.document.json)
              val evidence = Json.obj("source" -> Json.fromString("independent-postgresql"),
                "registrationRows" -> Json.fromInt(1), "registeredRows" -> Json.fromInt(1),
                "clientCompletionObserved" -> Json.False, "rows" -> Json.arr(row))
              parser.parse(Files.readString(work.resolve("native-registration-observation.json"), UTF_8)).toOption shouldBe Some(evidence)
              Files.exists(work.resolve("android.done")) shouldBe false
              Files.exists(work.resolve("native-registration-evidence.json")) shouldBe false
              evidence
            }
          }

          When("the native session and owning SQL schema have closed")
          val retained = parser.parse(Files.readString(work.resolve("native-registration-observation.json"), UTF_8))

          Then("the independent observation remains readable with its original domain document and incomplete client fact")
          retained.toOption shouldBe Some(expected)
          Files.exists(work.resolve("android.done")) shouldBe false
          Files.exists(work.resolve("native-registration-evidence.json")) shouldBe false
        }
      }
    }

    "refuse to invent registration when observation must stop" which {
      Vector(false, true).foreach { prepared =>
        val state = if (prepared) "a native Prepared row" else "empty native SQL state"
        s"fail for $state and an existing stop request without emitting evidence" in {
          Given(s"$state, a private local work directory and an existing stop request")
          _with_work { work => withDatabase(native_container) { database => withSession(database) { session =>
            if (prepared) take(session.create(take(MaterialContribution.parseC(MaterialContributionSpec.contributionJson()))))
            Files.writeString(work.resolve("stop.request"), "stop", UTF_8)

            When("the observer independently reads SQL while awaiting registration")
            val failed = intercept[IllegalStateException] {
              Esa63RegistrationEvidence.awaitRegistered(work, () => database.snapshot().rows,
                System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5))
            }

            Then("the explicit stop fails without a registered observation or final success artifact")
            failed.getMessage shouldBe "Native registration observation stopped before registration"
            database.snapshot().rows.map(_("state")) shouldBe (if (prepared) Vector(Some("prepared")) else Vector.empty)
            Files.exists(work.resolve("native-registration-observation.json")) shouldBe false
            Files.exists(work.resolve("native-registration-evidence.json")) shouldBe false
          } } }
        }
      }

      "fail at an elapsed monotonic deadline without fabricating registration" in {
        Given("empty native SQL state, a private local work directory and an already elapsed monotonic deadline")
        _with_work { work => withDatabase(native_container) { database =>
          val deadline = System.nanoTime() - java.util.concurrent.TimeUnit.SECONDS.toNanos(1)

          When("the observer independently reads SQL with no registered rows")
          val failed = intercept[IllegalStateException] {
            Esa63RegistrationEvidence.awaitRegistered(work, () => database.snapshot().rows, deadline)
          }

          Then("the deadline fails without a registered observation or final success artifact")
          failed.getMessage shouldBe "Native registration observation deadline elapsed"
          Files.exists(work.resolve("native-registration-observation.json")) shouldBe false
          Files.exists(work.resolve("native-registration-evidence.json")) shouldBe false
        } }
      }
    }
  }

  private def _with_work[A](run: Path => A): A = {
    val parent = Files.createDirectories(Path.of("target/cncf/work/esa63-registration-evidence").toAbsolutePath.normalize)
    val work = Files.createTempDirectory(parent, "observation-")
    try run(work)
    finally Using.resource(Files.walk(work)) { paths =>
      paths.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(path => Files.deleteIfExists(path))
    }
  }
}
