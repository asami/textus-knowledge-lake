package org.simplemodeling.textus.knowledgelake.material

import io.circe.{Json, Printer, parser}
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.util.Using

/* Native generated Operations, PostgreSQL and controlled native provider IO.
 * Synthetic byte integrity only; no real Google or device acceptance.
 * @since Oct. 25, 2026
 * @version Nov.  6, 2026
 */
@org.scalatest.tags.Slow
final class MaterialDeliveryIntegrityOperationsSpec extends AnyWordSpec with Matchers with GivenWhenThen with MaterialRegistrationPostgresqlSpec {
  import MaterialRegistrationPersistenceFixture.*
  import MaterialDeliveryOperationsFixture.*
  import MaterialRegistrationOperationsFixture.{HttpPeer, MetadataFault, Fault, prepare, pngs, contribution, integrityContribution, sha256, statUrl}

  private def _config(direct: Boolean): Json = configuration().mapObject(_.add("preferDirect", Json.fromBoolean(direct)))
  private def _prepare(session: Session, peer: org.goldenport.cncf.http.HttpDriver, raw: Json = integrityContribution): Record =
    record(session.query("prepare", Vector("requestId" -> requestId, "contribution" -> raw), driver = Some(peer), service = "MaterialDelivery"))
  private def _instruction(session: Session, peer: org.goldenport.cncf.http.HttpDriver, reference: String, resource: String): Record =
    record(session.query("prepareOriginalTransfer", Vector("deliveryRef" -> reference, "resourceId" -> resource),
      driver = Some(peer), service = "MaterialDelivery")).getAsC[Record]("instruction").TAKE.get
  private def _notification(reference: String, instruction: Record, length: Long): Vector[(String, Any)] = Vector(
    "deliveryRef" -> reference, "resourceId" -> instruction.getString("resourceId").get,
    "transferRef" -> instruction.getString("transferRef").get,
    "result" -> Json.obj("outcome" -> Json.fromString("uploaded"), "byteLength" -> Json.fromLong(length)))
  private def _confirm(session: Session, peer: org.goldenport.cncf.http.HttpDriver, reference: String, instruction: Record, length: Long): Record =
    record(session.query("confirmOriginalTransfer", _notification(reference, instruction, length), driver = Some(peer), service = "MaterialDelivery"))
  private def _finalize(session: Session, peer: org.goldenport.cncf.http.HttpDriver, reference: String): Record =
    record(session.query("finalize", Vector("deliveryRef" -> reference), driver = Some(peer), service = "MaterialDelivery"))
  private def _wire(status: Record): Json =
    parser.parse(org.goldenport.record.io.RecordEncoder.jsonC(status).TAKE)
      .fold(error => fail("Native delivery JSON did not parse: " + error.message), value => value)
  private def _resource(status: Record, index: Int = 0): Json =
    _wire(status).hcursor.get[Vector[Json]]("resources")
      .fold(error => fail("Native delivery resources did not decode: " + error.message), values => values(index))
  private def _replace_document(database: Database, document: Json): Unit = database.connection { connection =>
    Using.resource(connection.prepareStatement("UPDATE " + quote(deliveryTable) + " SET document = ?")) { statement =>
      statement.setString(1, Printer.noSpaces.copy(escapeNonAscii = true).print(document))
      statement.executeUpdate() shouldBe 1
    }
  }
  private def _single: Json = integrityContribution.mapObject(fields => fields.add("originals", Json.arr(fields("originals").get.asArray.get.head))
    .add("documents", Json.arr()))

  "Material delivery integrity" should {
    "admission authorization and immutable expectation" which {
      "reject missing and partial digests on the direct route before durable or provider effects" in {
        Given("direct-preferred native TKL and legacy or partially enriched two-original declarations")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          val peer = new HttpPeer(database)
          val partial = integrityContribution.mapObject(fields => fields.add("originals", Json.fromValues(
            fields("originals").get.asArray.get.zipWithIndex.map { case (value, index) =>
              if (index == 0) value else value.mapObject(_.remove("contentDigest")) })))
          withSession(database, parameters(Some(_config(true))), prepare = prepare()) { session =>
            When("new direct requests omit all or one expected digest")
            val results = Vector(contribution, partial).map(raw => session.query("prepare",
              Vector("requestId" -> requestId, "contribution" -> raw), driver = Some(peer), service = "MaterialDelivery"))
            Then("both requests fail with fixed argument failure and no claim or provider request")
            results.foreach(result => failure(result).displayMessage shouldBe "Material delivery requires complete original content digests")
            documents(database) shouldBe empty
            peer.effects shouldBe empty
          }
        }
      }

      "reject partial digests on the mediated route and retain the configured size limit" in {
        Given("explicit mediated delivery and a partially enriched contribution")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          val peer = new HttpPeer(database)
          val partial = integrityContribution.mapObject(fields => fields.add("originals", Json.fromValues(
            fields("originals").get.asArray.get.zipWithIndex.map { case (value, index) =>
              if (index == 0) value else value.mapObject(_.remove("contentDigest")) })))
          withSession(database, parameters(), prepare = prepare()) { session =>
            When("partial expectations or a declared original over the configured byte limit are prepared")
            val oversized = _single.mapObject(fields => fields.add("originals", Json.fromValues(
              fields("originals").get.asArray.get.map(_.mapObject(_.add("byteLength", Json.fromLong(1000001)))))))
            val results = Vector(partial, oversized).map(raw => session.query("prepare",
              Vector("requestId" -> requestId, "contribution" -> raw), driver = Some(peer), service = "MaterialDelivery"))
            val destination = record(session.query("getDestination", driver = Some(peer), service = "MaterialDelivery"))
            Then("integrity enrichment cannot bypass complete expectations or the existing limit")
            results.foreach(_.isSuccess shouldBe false)
            destination.getString("contentIntegrity") shouldBe Some("sha256")
            _wire(destination).hcursor.get[Long]("maxResourceBytes").toOption shouldBe Some(1000000L)
            documents(database) shouldBe empty
            peer.effects shouldBe empty
          }
        }
      }

      "conflict on a changed expected digest without replacing the original authenticated checkpoint" in {
        Given("one authenticated request with complete expected digests durably claimed")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          val peer = new HttpPeer(database)
          withSession(database, parameters(Some(_config(true))), prepare = prepare()) { session =>
            val original = _prepare(session, peer)
            val before = documents(database)
            val changed = integrityContribution.mapObject(fields => fields.add("originals", Json.fromValues(
              fields("originals").get.asArray.get.map(_.mapObject(_.add("contentDigest", Json.obj(
                "algorithm" -> Json.fromString("sha256"), "value" -> Json.fromString("0" * 64))))))))
            When("the same caller and request ID replay a changed expectation")
            val result = session.query("prepare", Vector("requestId" -> requestId, "contribution" -> changed), driver = Some(peer), service = "MaterialDelivery")
            Then("the native request conflicts before provider IO and preserves the original contribution and reference")
            result.isSuccess shouldBe false
            documents(database) shouldBe before
            _prepare(session, peer).getString("deliveryRef") shouldBe original.getString("deliveryRef")
            peer.effects shouldBe empty
          }
        }
      }

      "reject foreign actors unknown transfers resources and extra completion claims before effects" in {
        Given("a prepared direct session owned by the authenticated caller")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          val peer = new HttpPeer(database)
          withSession(database, parameters(Some(_config(true))), prepare = prepare()) { session =>
            val reference = _prepare(session, peer).getString("deliveryRef").get
            val instruction = _instruction(session, peer, reference, "cover")
            val fields = _notification(reference, instruction, pngs.head.size)
            val before = peer.effects.size
            val stored = documents(database)
            When("a foreign actor or forged completion supplies authority, an unknown selection or extra metadata")
            val foreignresult = session.query("confirmOriginalTransfer", fields, session = Some("foreign-session"), driver = Some(peer), service = "MaterialDelivery")
            val malformed = Vector(
              fields.map { case (key, value) => key -> (if (key == "transferRef") "unknown-transfer" else value) },
              fields.map { case (key, value) => key -> (if (key == "resourceId") "unknown-resource" else value) },
              fields.map { case (key, value) => key -> (if (key == "result") Json.Null else value) },
              fields.map { case (key, value) => key -> (if (key == "result")
                value.asInstanceOf[Json].mapObject(_.add("byteLength", Json.Null)) else value) }) ++
              Vector("fileId", "contentDigest", "storageRoot").map(extra => fields.map { case (key, value) => key ->
                (if (key == "result") value.asInstanceOf[Json].mapObject(_.add(extra, Json.Null)) else value) })
            val results = malformed.map(value => session.query("confirmOriginalTransfer", value, driver = Some(peer), service = "MaterialDelivery"))
            Then("normal native authorization and closed completion admission prevent all provider and checkpoint effects")
            foreignresult.isSuccess shouldBe false
            results.foreach(_.isSuccess shouldBe false)
            peer.effects.size shouldBe before
            documents(database) shouldBe stored
          }
        }
      }
    }

    "placement and independent verification" which {
      "verify actual stored bytes and expose additive native serializer fields before registration" in {
        Given("a single digest-bearing original and independently observed shared-drive placement")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          val peer = new CheckpointPeer(database, new HttpPeer(database, storageRoot = Some("ObservedSharedDrive")))
          withSession(database, parameters(Some(_config(true))), prepare = prepare()) { session =>
            val reference = _prepare(session, peer, _single).getString("deliveryRef").get
            val instruction = _instruction(session, peer, reference, "cover")
            peer.delegate.uploadFromMobile(instruction.getString("uploadSessionUri").get, pngs.head)
            When("the owner reports uploaded length and TKL inspects the reserved identity independently")
            val status = _confirm(session, peer, reference, instruction, pngs.head.size)
            Then("placement and verified proof are separate retained facts with exact native RecordEncoder field names")
            val resource = _resource(status)
            resource.asObject.get.keys.toSet shouldBe Set("resourceId", "mediaType", "byteLength", "state",
              "contentDigest", "verificationState", "verificationAttempts")
            resource.hcursor.get[String]("resourceId").toOption shouldBe Some("cover")
            resource.hcursor.get[String]("mediaType").toOption shouldBe Some("image/png")
            resource.hcursor.get[Long]("byteLength").toOption shouldBe Some(pngs.head.size.toLong)
            resource.hcursor.get[String]("state").toOption shouldBe Some("uploaded")
            resource.hcursor.get[String]("verificationState").toOption shouldBe Some("verified")
            resource.hcursor.get[Int]("verificationAttempts").toOption shouldBe Some(1)
            resource.hcursor.downField("contentDigest").focus shouldBe Some(Json.obj(
              "algorithm" -> Json.fromString("sha256"), "value" -> Json.fromString(sha256(pngs.head))))
            val doc = documents(database).head
            val key = doc.hcursor.downField("transfers").downArray.downField("fileReference").get[String]("key").toOption.get
            peer.delegate.effects.exists(value => value.method == "GET" && value.path == statUrl(key)) shouldBe true
            peer.checkpoints.head.head.hcursor.downField("registration").downField("contribution").focus shouldBe Some(_single)
            peer.checkpoints.exists(_.exists(value => value.hcursor.downField("transfers").downArray.downField("fileReference").focus.isDefined)) shouldBe true
            status.print should not include key
            status.print should not include "ObservedSharedDrive"
            status.print should not include "upload_id="
            When("finalization consumes the retained verified proof")
            val registered = _finalize(session, peer, reference)
            Then("only the verified package acquires one native registration")
            registered.getString("state") shouldBe Some("registered")
            database.snapshot().rows should have size 1
          }
        }
      }

      "retain a same-size altered original as a failed candidate without registering" in {
        Given("an immutable expected digest and a mobile upload with one altered byte at the same length")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          val peer = new HttpPeer(database)
          withSession(database, parameters(Some(_config(true))), prepare = prepare()) { session =>
            val reference = _prepare(session, peer, _single).getString("deliveryRef").get
            val instruction = _instruction(session, peer, reference, "cover")
            val altered = pngs.head.updated(0, (pngs.head.head ^ 1).toByte)
            peer.uploadFromMobile(instruction.getString("uploadSessionUri").get, altered)
            When("independent metadata observes the actual altered bytes")
            val status = _confirm(session, peer, reference, instruction, altered.size)
            val finalized = _finalize(session, peer, reference)
            Then("the retained candidate is uploaded but failed verification cannot produce a manifest or registration")
            status.getString("state") shouldBe Some("verificationFailed")
            status.getString("actionRequired") shouldBe Some("integrityMismatch")
            _resource(status).hcursor.get[String]("state").toOption shouldBe Some("uploaded")
            finalized.getString("materialRef") shouldBe None
            peer.confirmed.filterNot(_.collection).map(_.bytes) shouldBe Vector(altered)
            peer.confirmed.exists(_.label == "material.json") shouldBe false
            database.snapshot().rows shouldBe empty
          }
        }
      }

      "refuse a completion hint before bytes exist without manufacturing placement or verification" in {
        Given("a pending reserved upload and a matching client length hint")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          val peer = new CheckpointPeer(database, new HttpPeer(database))
          withSession(database, parameters(Some(_config(true))), prepare = prepare()) { session =>
            val reference = _prepare(session, peer, _single).getString("deliveryRef").get
            val instruction = _instruction(session, peer, reference, "cover")
            val before = peer.checkpoints.size
            When("the client reports uploaded before sending original bytes")
            val result = session.query("confirmOriginalTransfer", _notification(reference, instruction, pngs.head.size), driver = Some(peer), service = "MaterialDelivery")
            val status = record(session.query("get", Vector("deliveryRef" -> reference), driver = Some(peer), service = "MaterialDelivery"))
            Then("pending offsets leave the resource pending with zero verification attempts")
            result.isSuccess shouldBe false
            val checkpoint = peer.checkpoints.drop(before).head.head
            checkpoint.hcursor.get[Option[String]]("busy").toOption.flatten shouldBe Some("verification:cover")
            checkpoint.hcursor.downField("verifications").downArray.get[Int]("attempts").toOption shouldBe Some(0)
            documents(database).head.hcursor.get[Option[String]]("busy").toOption.flatten shouldBe None
            documents(database).head.hcursor.downField("verifications").downArray.get[String]("state").toOption shouldBe Some("pending")
            documents(database).head.hcursor.downField("verifications").downArray.downField("observedDigest").focus shouldBe Some(Json.Null)
            _resource(status).hcursor.get[String]("state").toOption shouldBe Some("pending")
            _resource(status).hcursor.get[Int]("verificationAttempts").toOption shouldBe Some(0)
            documents(database).head.hcursor.get[Vector[Json]]("files").toOption.get shouldBe empty
            database.snapshot().rows shouldBe empty
            When("the owner asks for the same pending transfer instruction again")
            val repeated = _instruction(session, peer, reference, "cover")
            Then("the known Pending observation retains the same instruction and zero offset without spending SHA checks")
            repeated shouldBe instruction
            _wire(repeated).hcursor.get[Long]("nextOffset").toOption shouldBe Some(0L)
            documents(database).head.hcursor.get[Option[String]]("busy").toOption.flatten shouldBe None
            documents(database).head.hcursor.downField("verifications").downArray.get[Int]("attempts").toOption shouldBe Some(0)
            peer.delegate.metadataChecks("cover") shouldBe 0
          }
        }
      }

      "reject independent identity parent root MIME and length mismatches despite a valid session response" in {
        Given("reserved uploads with valid session metadata and one hostile independent metadata field each")
        val faults = Vector(MetadataFault("cover", 1, identity = Some("ForeignIdentity")),
          MetadataFault("cover", 1, parent = Some("ForeignParent")), MetadataFault("cover", 1, root = Some(Some("ForeignDrive"))),
          MetadataFault("cover", 1, length = Some(999)), MetadataFault("cover", 1, media = Some("image/jpeg")))
        faults.foreach { fault => withDatabase(native_container) { database =>
          prepareDatabase(database)
          val peer = new HttpPeer(database)
          peer.setMetadataFaults(Vector(fault))
          withSession(database, parameters(Some(_config(true))), prepare = prepare()) { session =>
            val reference = _prepare(session, peer, _single).getString("deliveryRef").get
            val instruction = _instruction(session, peer, reference, "cover")
            peer.uploadFromMobile(instruction.getString("uploadSessionUri").get, pngs.head)
            When("the reserved inspection receives metadata outside its retained binding")
            val result = session.query("confirmOriginalTransfer", _notification(reference, instruction, pngs.head.size), driver = Some(peer), service = "MaterialDelivery")
            Then("the native metadata failure prevents placement and registration without fallback")
            result.isSuccess shouldBe false
            documents(database).head.hcursor.get[Vector[Json]]("files").toOption.get shouldBe empty
            peer.confirmed.exists(_.label == "material.json") shouldBe false
            database.snapshot().rows shouldBe empty
          }
        }}
      }

      "use independent SHA instead of the completed session checksum when they disagree" in {
        Given("correct actual session bytes but a different checksum from the independent known-file GET")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          val peer = new HttpPeer(database)
          peer.setMetadataFaults(Vector(MetadataFault("cover", 1, digest = Some("0" * 64))))
          withSession(database, parameters(Some(_config(true))), prepare = prepare()) { session =>
            val reference = _prepare(session, peer, _single).getString("deliveryRef").get
            val instruction = _instruction(session, peer, reference, "cover")
            peer.uploadFromMobile(instruction.getString("uploadSessionUri").get, pngs.head)
            When("completion requires a new independent observation")
            val status = _confirm(session, peer, reference, instruction, pngs.head.size)
            Then("session SHA cannot override an independent mismatch")
            status.getString("actionRequired") shouldBe Some("integrityMismatch")
            _resource(status).hcursor.get[String]("verificationState").toOption shouldBe Some("failed")
            database.snapshot().rows shouldBe empty
          }
        }
      }
    }

    "bounded rechecks and restart" which {
      "retain verification ownership after the third GET response is lost and refuse a fourth check after restart" in {
        Given("a verified cover and a placed detail whose first two independent observations omit SHA")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          val delegate = new HttpPeer(database)
          delegate.setMetadataFaults((1 to 2).map(index => MetadataFault("detail", index, omitDigest = true)).toVector)
          val retained = withSession(database, parameters(Some(_config(true))), prepare = prepare()) { session =>
            val reference = _prepare(session, delegate).getString("deliveryRef").get
            val cover = _instruction(session, delegate, reference, "cover")
            delegate.uploadFromMobile(cover.getString("uploadSessionUri").get, pngs.head)
            _confirm(session, delegate, reference, cover, pngs.head.size)
            val detail = _instruction(session, delegate, reference, "detail")
            delegate.uploadFromMobile(detail.getString("uploadSessionUri").get, pngs(1))
            _confirm(session, delegate, reference, detail, pngs(1).size)
            _finalize(session, delegate, reference)
            val before = documents(database).head
            val file = before.hcursor.get[Vector[Json]]("files").toOption.get
              .find(_.hcursor.get[String]("path").toOption.contains("assets/detail")).get
            val path = statUrl(file.hcursor.downField("reference").get[String]("key").toOption.get)
            var observed = Vector.empty[Json]
            var lostresponse = false
            val peer = new CheckpointPeer(database, delegate,
              beforeGet = value => if (value == path) observed ++= documents(database),
              afterGet = value => if (value == path && !lostresponse) {
                lostresponse = true
                throw new IllegalStateException("Test-owned third verification response was lost")
              })
            When("the third independent metadata GET completes but its response is lost before the proof save")
            val result = scala.util.Try(session.query("finalize", Vector("deliveryRef" -> reference),
              driver = Some(peer), service = "MaterialDelivery"))
            Then("independent JDBC retains the owned busy checkpoint and the previous two attempts without inventing proof")
            result.map(_.isSuccess).getOrElse(false) shouldBe false
            lostresponse shouldBe true
            delegate.metadataChecks("detail") shouldBe 3
            observed should have size 1
            observed.head.hcursor.get[Option[String]]("busy").toOption.flatten shouldBe Some("verification:detail")
            val unknown = documents(database).head
            unknown shouldBe observed.head
            unknown.hcursor.get[Vector[Json]]("verifications").toOption.get(1).hcursor.get[Int]("attempts").toOption shouldBe Some(2)
            unknown.hcursor.get[Vector[Json]]("verifications").toOption.get(1).hcursor.downField("observedDigest").focus shouldBe Some(Json.Null)
            unknown.hcursor.get[Vector[Json]]("verifications").toOption.get.head shouldBe before.hcursor.get[Vector[Json]]("verifications").toOption.get.head
            unknown.hcursor.downField("registration").focus shouldBe before.hcursor.downField("registration").focus
            unknown.hcursor.downField("transfers").focus shouldBe before.hcursor.downField("transfers").focus
            unknown.hcursor.downField("files").focus shouldBe before.hcursor.downField("files").focus
            database.snapshot().rows shouldBe empty
            (reference, detail, unknown, delegate.effects.size, delegate.confirmed, delegate.mobileUploads)
          }
          Given("the same delivery reopened with its unresolved third observation and original stored bytes")
          withSession(database, parameters(), prepare = prepare()) { session =>
            When("passive status and repeated finalize, confirm and prepare inspect the retained native checkpoint")
            val status = record(session.query("get", Vector("deliveryRef" -> retained._1), driver = Some(delegate), service = "MaterialDelivery"))
            val finalized = session.query("finalize", Vector("deliveryRef" -> retained._1), driver = Some(delegate), service = "MaterialDelivery")
            val confirmed = record(session.query("confirmOriginalTransfer", _notification(retained._1, retained._2, pngs(1).size),
              driver = Some(delegate), service = "MaterialDelivery"))
            val prepared = record(session.query("prepareOriginalTransfer", Vector("deliveryRef" -> retained._1, "resourceId" -> "detail"),
              driver = Some(delegate), service = "MaterialDelivery"))
            Then("reconciliation prevents a fourth GET, another session, object or registration and preserves the verified sibling")
            status.getString("state") shouldBe Some("reconciliationRequired")
            status.getString("actionRequired") shouldBe Some("operatorReconciliationRequired")
            _resource(status, 0).hcursor.get[String]("verificationState").toOption shouldBe Some("verified")
            _resource(status, 1).hcursor.get[Int]("verificationAttempts").toOption shouldBe Some(2)
            finalized.isSuccess shouldBe false
            confirmed.getString("state") shouldBe Some("reconciliationRequired")
            prepared.getAsC[Record]("instruction").TAKE shouldBe None
            prepared.getAsC[Record]("delivery").TAKE.get.getString("state") shouldBe Some("reconciliationRequired")
            delegate.metadataChecks("detail") shouldBe 3
            delegate.metadataChecks("cover") shouldBe 1
            delegate.effects.size shouldBe retained._4
            delegate.confirmed shouldBe retained._5
            delegate.mobileUploads shouldBe retained._6
            delegate.confirmed.filter(_.media == "image/png").map(_.bytes) shouldBe pngs
            documents(database).head shouldBe retained._3
            database.snapshot().rows shouldBe empty
          }
        }
      }

      "reject a competing native finalize while the admitted observation owns the durable checkpoint" in {
        Given("one placed direct candidate awaiting a second SHA observation in the same native session")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          val delegate = new HttpPeer(database)
          delegate.setMetadataFaults(Vector(MetadataFault("cover", 1, omitDigest = true)))
          withSession(database, parameters(Some(_config(true))), prepare = prepare()) { session =>
            val reference = _prepare(session, delegate, _single).getString("deliveryRef").get
            val instruction = _instruction(session, delegate, reference, "cover")
            delegate.uploadFromMobile(instruction.getString("uploadSessionUri").get, pngs.head)
            _confirm(session, delegate, reference, instruction, pngs.head.size)
            val before = documents(database).head
            val path = statUrl(before.hcursor.downField("files").downArray.downField("reference").get[String]("key").toOption.get)
            var entered = false
            var rejected = false
            var observed = Vector.empty[Json]
            var aftercompeting = Vector.empty[Json]
            var effectsbefore = -1
            var effectsafter = -1
            lazy val peer: CheckpointPeer = new CheckpointPeer(database, delegate, beforeGet = value => {
              if (value == path && !entered) {
                entered = true
                observed = documents(database)
                effectsbefore = delegate.effects.size
                When("a competing generated finalize is admitted from the one-shot hook while verification is busy")
                rejected = !session.query("finalize", Vector("deliveryRef" -> reference), driver = Some(peer), service = "MaterialDelivery").isSuccess
                effectsafter = delegate.effects.size
                aftercompeting = documents(database)
                Then("the competing operation has no provider effect and leaves the independently committed ownership unchanged")
                rejected shouldBe true
                effectsafter shouldBe effectsbefore
                aftercompeting shouldBe observed
              }
            })
            When("the owner finalizes the retained candidate through a new independent observation")
            val status = _finalize(session, peer, reference)
            Then("the competing normal operation fails before provider access or checkpoint replacement and the owner saves one proof")
            entered shouldBe true
            rejected shouldBe true
            observed should have size 1
            observed.head.hcursor.get[Option[String]]("busy").toOption.flatten shouldBe Some("verification:cover")
            observed.head.hcursor.downField("verifications").downArray.get[Int]("attempts").toOption shouldBe Some(1)
            aftercompeting shouldBe observed
            effectsafter shouldBe effectsbefore
            status.getString("state") shouldBe Some("registered")
            _resource(status).hcursor.get[String]("verificationState").toOption shouldBe Some("verified")
            _resource(status).hcursor.get[Int]("verificationAttempts").toOption shouldBe Some(2)
            delegate.metadataChecks("cover") shouldBe 2
            delegate.mobileUploads should have size 1
            delegate.confirmed.count(_.media == "image/png") shouldBe 1
            documents(database).head.hcursor.get[Option[String]]("busy").toOption.flatten shouldBe None
            documents(database).head.hcursor.downField("transfers").focus shouldBe before.hcursor.downField("transfers").focus
            database.snapshot().rows should have size 1
          }
        }
      }

      "retain a checksum-less candidate through restart and stop at exactly three independent checks" in {
        Given("a provider whose independent metadata omits SHA on three observations although session SHA exists")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          val peer = new HttpPeer(database)
          peer.setMetadataFaults((1 to 3).map(index => MetadataFault("cover", index, omitDigest = true)).toVector)
          val retained = withSession(database, parameters(Some(_config(true))), prepare = prepare()) { session =>
            val reference = _prepare(session, peer, _single).getString("deliveryRef").get
            val instruction = _instruction(session, peer, reference, "cover")
            peer.uploadFromMobile(instruction.getString("uploadSessionUri").get, pngs.head)
            val status = _confirm(session, peer, reference, instruction, pngs.head.size)
            status.getString("state") shouldBe Some("verifying")
            (reference, peer.confirmed.size, peer.effects.count(_.method == "POST"), documents(database).head)
          }
          withSession(database, parameters(), prepare = prepare()) { session =>
            When("the retained delivery is queried passively and finalize is requested repeatedly after reopening")
            val before = peer.effects.size
            record(session.query("get", Vector("deliveryRef" -> retained._1), driver = Some(peer), service = "MaterialDelivery")).getString("state") shouldBe Some("verifying")
            record(session.query("findForRequest", Vector("requestId" -> requestId), driver = Some(peer), service = "MaterialDelivery"))
            record(session.query("getDestination", driver = Some(peer), service = "MaterialDelivery"))
            peer.effects.size shouldBe before
            val second = _finalize(session, peer, retained._1)
            val third = _finalize(session, peer, retained._1)
            val terminal = peer.effects.size
            val repeated = _finalize(session, peer, retained._1)
            Then("each eligible command checks once, terminal failure retains the object and no upload or session is recreated")
            second.getString("state") shouldBe Some("verifying")
            third.getString("state") shouldBe Some("verificationFailed")
            repeated.getString("actionRequired") shouldBe Some("verificationUnavailable")
            _resource(repeated).hcursor.get[Int]("verificationAttempts").toOption shouldBe Some(3)
            peer.metadataChecks("cover") shouldBe 3
            peer.effects.size shouldBe terminal
            peer.effects.count(_.method == "POST") shouldBe retained._3
            peer.confirmed.size shouldBe retained._2
            peer.effects.filter(_.method == "PUT").forall(_.bytes.isEmpty) shouldBe true
            documents(database).head.hcursor.downField("transfers").focus shouldBe retained._4.hcursor.downField("transfers").focus
            documents(database).head.hcursor.downField("registration").focus shouldBe retained._4.hcursor.downField("registration").focus
            database.snapshot().rows shouldBe empty
          }
        }
      }

      "verify on the second independent observation and then register without relaying original bytes" in {
        Given("one retained candidate whose checksum becomes available on its second metadata check")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          val peer = new HttpPeer(database)
          peer.setMetadataFaults(Vector(MetadataFault("cover", 1, omitDigest = true)))
          withSession(database, parameters(Some(_config(true))), prepare = prepare()) { session =>
            val reference = _prepare(session, peer, _single).getString("deliveryRef").get
            val instruction = _instruction(session, peer, reference, "cover")
            peer.uploadFromMobile(instruction.getString("uploadSessionUri").get, pngs.head)
            _confirm(session, peer, reference, instruction, pngs.head.size).getString("state") shouldBe Some("verifying")
            When("finalize independently rechecks the retained candidate")
            val status = _finalize(session, peer, reference)
            Then("the second matching checksum permits actual registration with one original object")
            status.getString("state") shouldBe Some("registered")
            _resource(status).hcursor.get[Int]("verificationAttempts").toOption shouldBe Some(2)
            peer.metadataChecks("cover") shouldBe 2
            peer.confirmed.count(_.media == "image/png") shouldBe 1
            peer.mobileUploads should have size 1
          }
        }
      }

      "preserve a verified sibling when another original fails integrity" in {
        Given("two declared originals with distinct expected hashes")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          val peer = new HttpPeer(database)
          withSession(database, parameters(Some(_config(true))), prepare = prepare()) { session =>
            val reference = _prepare(session, peer).getString("deliveryRef").get
            val cover = _instruction(session, peer, reference, "cover")
            peer.uploadFromMobile(cover.getString("uploadSessionUri").get, pngs.head)
            _confirm(session, peer, reference, cover, pngs.head.size)
            val detail = _instruction(session, peer, reference, "detail")
            peer.uploadFromMobile(detail.getString("uploadSessionUri").get, pngs(1).updated(0, 0.toByte))
            When("the second original mismatches and finalize inspects the retained state")
            _confirm(session, peer, reference, detail, pngs(1).size)
            val status = _finalize(session, peer, reference)
            Then("the first verified proof survives unchanged while the failed sibling prevents registration")
            _resource(status, 0).hcursor.get[String]("verificationState").toOption shouldBe Some("verified")
            _resource(status, 0).hcursor.get[Int]("verificationAttempts").toOption shouldBe Some(1)
            _resource(status, 1).hcursor.get[String]("verificationState").toOption shouldBe Some("failed")
            peer.metadataChecks("cover") shouldBe 1
            database.snapshot().rows shouldBe empty
          }
        }
      }

      "reject contradictory restored proofs and bindings while reading old transfer and file shapes explicitly" in {
        Given("one native independently verified candidate and its retained private JSON checkpoint")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          val peer = new HttpPeer(database)
          withSession(database, parameters(Some(_config(true))), prepare = prepare()) { session =>
            val reference = _prepare(session, peer, _single).getString("deliveryRef").get
            val instruction = _instruction(session, peer, reference, "cover")
            peer.uploadFromMobile(instruction.getString("uploadSessionUri").get, pngs.head)
            _confirm(session, peer, reference, instruction, pngs.head.size)
            val original = documents(database).head
            val proof = original.hcursor.get[Vector[Json]]("verifications").toOption.get.head
            val transfer = original.hcursor.get[Vector[Json]]("transfers").toOption.get.head
            def _proof_(key: String, value: Json): Json = original.mapObject(_.add("verifications", Json.arr(proof.mapObject(_.add(key, value)))))
            val malformed = Vector(_proof_("attempts", Json.fromInt(0)), _proof_("attempts", Json.fromInt(4)),
              _proof_("observedDigest", Json.Null), _proof_("reason", Json.fromString("integrityMismatch")),
              _proof_("state", Json.fromString("verifying")), _proof_("resourceId", Json.fromString("unknown")),
              _proof_("fileReference", Json.Null),
              _proof_("fileReference", Json.obj("store" -> Json.fromString("foreign"), "key" -> Json.fromString("foreign"))),
              _proof_("storageRoot", Json.obj("store" -> Json.fromString("foreign"), "key" -> Json.fromString("foreign"))),
              original.mapObject(_.add("verifications", Json.arr(proof, proof))),
              original.mapObject(_.add("transfers", Json.arr(transfer.mapObject(_.add("fileReference", Json.Null))))),
              original.mapObject(_.add("transfers", Json.arr(transfer.mapObject(_.add("byteLength", Json.fromInt(999)))))))
            val legacy = original.mapObject(fields => fields.remove("verifications")
              .add("transfers", Json.arr(transfer.mapObject(_.remove("fileReference").remove("storageRoot"))))
              .add("files", Json.fromValues(fields("files").get.asArray.get.map(_.mapObject(_.remove("storageRoot"))))))
            When("the actual delivery restore parser admits the retained, malformed and old-shape checkpoints")
            val valid = MaterialDeliveryDocument.parseC(original)
            val invalid = malformed.map(MaterialDeliveryDocument.parseC)
            val old = MaterialDeliveryDocument.parseC(legacy)
            Then("only consistent proofs restore and legacy shapes remain readable with an explicit upgrade disposition")
            valid.isSuccess shouldBe true
            invalid.foreach(_.isSuccess shouldBe false)
            old.isSuccess shouldBe true
            old.TAKE.integrityUpgradeRequired shouldBe true
            old.TAKE.originalsComplete shouldBe false
            old.TAKE.verifications shouldBe empty
          }
        }
      }

      "retain busy after unknown reserved preparation without generating another ID or session" in {
        Given("a reserved initiation that succeeds remotely but returns a controlled service failure")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          // Each collection reserves through parent and ID GET before its POST; then direct initiation does the same.
          val peer = new HttpPeer(database, fault = Some(Fault(8, MaterialReferenceFixture.jsonResponse(Json.obj(), 503), after = true)))
          withSession(database, parameters(Some(_config(true))), prepare = prepare()) { session =>
            val reference = _prepare(session, peer, _single).getString("deliveryRef").get
            When("preparation loses its acknowledgement and the owner retries the same command")
            val first = session.query("prepareOriginalTransfer", Vector("deliveryRef" -> reference, "resourceId" -> "cover"), driver = Some(peer), service = "MaterialDelivery")
            val count = peer.effects.size
            val second = session.query("prepareOriginalTransfer", Vector("deliveryRef" -> reference, "resourceId" -> "cover"), driver = Some(peer), service = "MaterialDelivery")
            val status = record(session.query("get", Vector("deliveryRef" -> reference), driver = Some(peer), service = "MaterialDelivery"))
            Then("unknown outcome stays busy and repeat preparation causes no further provider effect")
            first.isSuccess shouldBe false
            second.isSuccess shouldBe false
            status.getString("state") shouldBe Some("reconciliationRequired")
            peer.effects.size shouldBe count
            documents(database).head.hcursor.get[Option[String]]("busy").toOption.flatten shouldBe Some("directPrepare:cover")
          }
        }
      }
    }

    "mediated and legacy compatibility" which {
      "checkpoint a digest-bearing mediated candidate before stat and avoid duplicate creation during verification" in {
        Given("explicit mediated delivery whose independent SHA is initially unavailable")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          val peer = new CheckpointPeer(database, new HttpPeer(database, storageRoot = Some("ObservedSharedDrive")))
          peer.delegate.setMetadataFaults(Vector(MetadataFault("cover", 1, omitDigest = true)))
          withSession(database, parameters(), prepare = prepare()) { session =>
            val reference = _prepare(session, peer, _single).getString("deliveryRef").get
            When("native binary ingress creates one original and independently checks its metadata")
            val first = record(session.query("uploadOriginal", upload(reference, 0), driver = Some(peer), service = "MaterialDelivery"))
            val creates = peer.delegate.effects.count(_.method == "POST")
            val repeated = record(session.query("uploadOriginal", upload(reference, 0), driver = Some(peer), service = "MaterialDelivery"))
            Then("the placed candidate survives the missing checksum without a second create or relayed payload")
            first.getString("state") shouldBe Some("verifying")
            repeated.getString("state") shouldBe Some("verifying")
            peer.delegate.effects.count(_.method == "POST") shouldBe creates
            val checkpoint = peer.checkpoints.last.head
            checkpoint.hcursor.get[Vector[Json]]("files").toOption.get should have size 1
            checkpoint.hcursor.downField("verifications").downArray.get[Int]("attempts").toOption shouldBe Some(0)
            checkpoint.hcursor.downField("files").downArray.downField("storageRoot").get[String]("key").toOption shouldBe Some("ObservedSharedDrive")
            When("finalize rechecks bytes-free after the matching independent SHA appears")
            val finalized = _finalize(session, peer, reference)
            Then("the retained candidate registers without creating or relaying another original")
            finalized.getString("state") shouldBe Some("registered")
            peer.delegate.confirmed.count(_.media == "image/png") shouldBe 1
            peer.delegate.effects.filter(_.method == "PUT").count(_.bytes == pngs.head) shouldBe 1
          }
        }
      }

      "retain old no-digest mediated semantics without inventing verification fields" in {
        Given("an old no-digest contribution explicitly selecting mediated delivery")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          val peer = new HttpPeer(database)
          withSession(database, parameters(), prepare = prepare()) { session =>
            val reference = _prepare(session, peer, contribution).getString("deliveryRef").get
            When("both ordinary original inputs and finalize use the legacy route")
            (0 to 1).foreach(index => record(session.query("uploadOriginal", upload(reference, index), driver = Some(peer), service = "MaterialDelivery")))
            val status = _finalize(session, peer, reference)
            Then("registration remains available and legacy resources expose only their original four fields")
            status.getString("state") shouldBe Some("registered")
            _resource(status).asObject.get.keys.toSet shouldBe Set("resourceId", "mediaType", "byteLength", "state")
            peer.metadataChecks("cover") shouldBe 0
          }
        }
      }

      "block unfinished old direct checkpoints without provider IO or silent upgrade" in {
        Given("an old direct delivery retained before the digest and reservation contract")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          val peer = new HttpPeer(database)
          val reference = withSession(database, parameters(), prepare = prepare()) { session =>
            _prepare(session, peer, contribution).getString("deliveryRef").get
          }
          val legacy = documents(database).head.mapObject(_.add("transferMethod", Json.fromString("googleDriveResumable")).remove("verifications"))
          _replace_document(database, legacy)
          withSession(database, parameters(Some(_config(true))), prepare = prepare()) { session =>
            When("the legacy owner requests preparation status and finalization after reopening")
            val prepared = record(session.query("prepareOriginalTransfer", Vector("deliveryRef" -> reference, "resourceId" -> "cover"), driver = Some(peer), service = "MaterialDelivery"))
            val status = _finalize(session, peer, reference)
            Then("integrityUpgradeRequired preserves the old checkpoint and issues no session or registration")
            prepared.getAsC[Record]("instruction").TAKE shouldBe None
            status.getString("state") shouldBe Some("reconciliationRequired")
            status.getString("actionRequired") shouldBe Some("integrityUpgradeRequired")
            peer.effects shouldBe empty
            database.snapshot().rows shouldBe empty
          }
        }
      }

      "read already registered old direct deliveries without retroactive verification or metadata rechecks" in {
        Given("a native registered no-digest package with an old direct-route checkpoint")
        withDatabase(native_container) { database =>
          prepareDatabase(database)
          val peer = new HttpPeer(database)
          val retained = withSession(database, parameters(), prepare = prepare()) { session =>
            val reference = _prepare(session, peer, contribution).getString("deliveryRef").get
            When("the existing no-digest originals are uploaded and the native package is registered for legacy setup")
            (0 to 1).foreach(index => record(session.query("uploadOriginal", upload(reference, index), driver = Some(peer), service = "MaterialDelivery")))
            val status = _finalize(session, peer, reference)
            Then("the setup has an actual native Registered package and Material reference")
            status.getString("state") shouldBe Some("registered")
            status.getString("materialRef") should not be empty
            When("the retained delivery is parsed and its expected registration is independently reconstructed")
            val document = MaterialDeliveryDocument.parseC(documents(database).head).TAKE
            val begun = document.registration.beginC.TAKE
            val expected = begun.completeC(document.packageC.TAKE).TAKE
            val physical = database.snapshot()
            Then("the unmodified setup registration exactly equals the existing independent JDBC registration document")
            physical.rows should have size 1
            val actual = parser.parse(physical.rows.head("document").get)
              .fold(error => fail("Native registration JSON did not parse: " + error.message), value => value)
            expected.json shouldBe actual
            (reference, status.getString("materialRef"))
          }
          Given("the same registered package with only the old direct route and omitted verification vector")
          val legacy = documents(database).head.mapObject(_.add("transferMethod", Json.fromString("googleDriveResumable")).remove("verifications"))
          When("the old-shape private delivery is parsed and its expected native registration is reconstructed before writing")
          val document = MaterialDeliveryDocument.parseC(legacy).TAKE
          val begun = document.registration.beginC.TAKE
          val expected = begun.completeC(document.packageC.TAKE).TAKE
          val physical = database.snapshot()
          Then("the modified setup still exactly equals the independent existing JDBC registration document")
          physical.rows should have size 1
          val actual = parser.parse(physical.rows.head("document").get)
            .fold(error => fail("Native registration JSON did not parse: " + error.message), value => value)
          expected.json shouldBe actual
          When("the established old-shape checkpoint is retained before reopening")
          _replace_document(database, legacy)
          Then("independent delivery JDBC contains exactly the old-shape setup")
          documents(database).head shouldBe legacy
          documents(database).head.hcursor.downField("registration").downField("contribution").focus shouldBe Some(contribution)
          Given("the same legacy registered checkpoint reopened without current delivery configuration")
          withSession(database, parameters(None), prepare = prepare()) { session =>
            val before = peer.effects.size
            When("passive status and idempotent finalize inspect the existing native registration")
            val status = record(session.query("get", Vector("deliveryRef" -> retained._1), driver = Some(peer), service = "MaterialDelivery"))
            val finalized = _finalize(session, peer, retained._1)
            Then("the retained Registered reference remains accessible without a new SHA claim or provider effect")
            status.getString("state") shouldBe Some("registered")
            finalized.getString("materialRef") shouldBe retained._2
            _resource(status).hcursor.downField("verificationState").focus shouldBe None
            peer.effects.size shouldBe before
          }
        }
      }
    }
  }
}
