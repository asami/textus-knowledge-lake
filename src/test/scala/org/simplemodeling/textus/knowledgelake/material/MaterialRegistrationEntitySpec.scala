package org.simplemodeling.textus.knowledgelake.material

import io.circe.{Json, parser}
import java.util.Locale
import org.goldenport.cncf.context.IdGenerationContext
import org.goldenport.cncf.datastore.DataStoreTextSearchField
import org.goldenport.cncf.directive.Query
import org.goldenport.cncf.entity.{EntityRevisionRepresentation, EntitySearchScope, EntityVisibilityScope}
import org.goldenport.cncf.filestore.FileStoreRef
import org.goldenport.observation.Taxonomy
import org.goldenport.record.Record
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.model.datatype.{EntityCollectionId, EntityRevision}

/*
 * Lossless native lifecycle and Entity admission, independent of physical IO.
 *
 * @since   Oct. 21, 2026
 * @version Oct. 21, 2026
 */
final class MaterialRegistrationEntitySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import MaterialContributionSpec.{contributionJson, creator, failure, owner, replace, take}
  import MaterialRegistrationEntitySpec.*

  "Native Material registration document" should {
    "admit only the forward lifecycle" which {
      "roundtrip Prepared Materializing and confirmed Registered with exact generic mappings" in {
        Given("a validated contribution, explicit location and a confirmed package receipt")
        val prepared = document()
        val receipt = stored(prepared)
        When("the document begins and completes once and every saved state is decoded")
        val materializing = take(prepared.beginC)
        val registered = take(materializing.completeC(receipt))
        val states = Vector(prepared, materializing, registered)
        val decoded = states.map(value => take(MaterialRegistrationDocument.parseC(value.json)))
        val issuer = IdGenerationContext.deterministic(IdGenerationContext.DEFAULT_NAMESPACE, "registration-states")
        val native = states.map(value => MaterialRegistrationRecord(issuer.entityId(MaterialRegistrationEntity.collectionId),
          EntityRevision.INITIAL, value, Record.empty))
        val restored = native.map(value => take(MaterialRegistrationEntity.persistent.fromStoreRecord(
          MaterialRegistrationEntity.persistent.toStoreRecord(value))))
        Then("all literal source and mappings survive and the completed document retains exact parents")
        decoded.map(_.status.value) shouldBe Vector("prepared", "materializing", "registered")
        decoded.map(_.json) shouldBe states.map(_.json)
        restored.map(_.document.json) shouldBe states.map(_.json)
        states.foreach(value => value.contribution.json shouldBe contributionJson())
        prepared.json.asObject.get.keys.toSet shouldBe Set("schema", "contribution", "lake", "logicalPath", "storageBinding", "owner", "createdBy", "status")
        registered.stored.get.root shouldBe receipt._root
        registered.stored.get.manifest shouldBe receipt._manifest
        registered.stored.get.files.map(_.path) shouldBe receipt._files.map(_.path)
        registered.stored.get.files.map(_.parent) shouldBe receipt._files.map(_._parent)
        registered.stored.get.files.map(_.reference) shouldBe receipt._files.map(_._reference)
      }

      "refuse backward repeated and premature transitions and mismatched confirmed plans" in {
        Given("all three states and a receipt for a different contribution")
        val prepared = document()
        val materializing = take(prepared.beginC)
        val registered = take(materializing.completeC(stored(prepared)))
        val foreign = document(contributionJson(title = "other"))
        When("invalid transitions and a mismatched completion are requested")
        val conflicts = Vector(prepared.completeC(stored(prepared)), materializing.beginC,
          registered.beginC, registered.completeC(stored(prepared)))
        val mismatch = materializing.completeC(stored(foreign))
        Then("state conflicts are structured and a foreign plan cannot complete this document")
        conflicts.foreach(value => failure(value).observation.taxonomy shouldBe Taxonomy(Taxonomy.Category.Operation, Taxonomy.Symptom.Conflict))
        failure(mismatch).observation.taxonomy shouldBe Taxonomy.resourceInvalid
        failure(mismatch).displayMessage shouldBe "Invalid Material registration storage receipt"
        materializing.status.value shouldBe "materializing"
      }
    }

    "reject malformed saved data" which {
      "reject unknown incomplete invalid principal location binding and state envelopes without submitted text" in {
        Given("a complete Prepared document and malformed closed-schema variants")
        val raw = document().json
        val invalid = Vector(null, Json.Null, raw.mapObject(_.remove("owner")),
          raw.mapObject(_.add("unknown", Json.fromString("private-marker"))),
          replace(raw, "schema", Json.fromString("other")), replace(raw, "contribution", Json.obj()),
          replace(raw, "lake", Json.fromString("bad lake")), replace(raw, "logicalPath", Json.fromString("a/../b")),
          replace(raw, "owner", Json.fromString("")), replace(raw, "createdBy", Json.fromString("\n")),
          replace(raw, "storageBinding", Json.fromString("https://private-marker")),
          replace(raw, "storageBinding", Json.fromString("x" * 129)), replace(raw, "status", Json.fromString("ready")),
          raw.mapObject(_.add("stored", Json.obj())), replace(raw, "status", Json.fromString("registered")))
        When("each saved document is decoded without physical discovery")
        val results = invalid.map(MaterialRegistrationDocument.parseC)
        Then("every malformed shape has the same nonsecret argument failure")
        results.foreach { value =>
          failure(value).observation.taxonomy shouldBe Taxonomy.argumentInvalid
          failure(value).displayMessage shouldBe "Invalid Material registration document"
          failure(value).display should not include "private-marker"
        }
      }

      "reject inconsistent stores aliases paths order and parent mappings" in {
        Given("one fully registered saved document and exact ordered file mappings")
        val prepared = document()
        val raw = take(take(prepared.beginC).completeC(stored(prepared))).json
        val mapping = raw.hcursor.downField("stored").focus.get
        val files = mapping.hcursor.get[Vector[Json]]("files").toOption.get
        val root = mapping.hcursor.downField("root").focus.get
        val file = files.head
        val reference = file.hcursor.downField("reference").focus.get
        val badfiles = Vector(files.reverse, files.drop(1), files.updated(0, replace(file, "path", Json.fromString("assets/foreign"))),
          files.updated(0, replace(file, "reference", root)),
          files.updated(0, replace(file, "reference", replace(reference, "store", Json.fromString("other")))),
          files.updated(0, replace(file, "reference", replace(reference, "key", Json.fromString("")))),
          files.updated(0, replace(file, "reference", replace(reference, "key", Json.fromString("x" * 4097)))),
          files.updated(0, replace(file, "parent", root)),
          files.updated(0, replace(file, "parent", replace(reference, "key", Json.fromString("bad\nkey")))),
          files.updated(1, replace(files(1), "parent", Json.obj("store" -> Json.fromString("test-store"), "key" -> Json.fromString("other-assets")))))
        val invalid = badfiles.map(value => replace(raw, "stored", replace(mapping, "files", Json.fromValues(value)))) ++
          Vector(replace(raw, "stored", replace(mapping, "manifest", root)),
            replace(raw, "stored", mapping.mapObject(_.add("extra", Json.Null))))
        When("the saved mappings cross the pure decoder")
        val outcomes = invalid.map(MaterialRegistrationDocument.parseC)
        Then("no malformed mapping can be admitted or repaired through discovery")
        outcomes.foreach(value => failure(value).observation.taxonomy shouldBe Taxonomy.argumentInvalid)
      }
    }
  }

  "Native Material registration Entity codec" should {
    "preserve authoritative native metadata" which {
      "retain unrelated attributes while preventing managed-field collisions and deriving safe columns" in {
        Given("an issued native identity, embedded revision and colliding plus unrelated attributes")
        val issuer = IdGenerationContext.deterministic(IdGenerationContext.DEFAULT_NAMESPACE, "registration-codec")
        val attributes = Record.dataAuto("owner_id" -> "actual_owner", "audit" -> "retained", "unknown" -> Json.arr(Json.Null))
        val entity = MaterialRegistrationRecord(issuer.entityId(MaterialRegistrationEntity.collectionId), EntityRevision.INITIAL,
          document(), attributes ++ Record.dataAuto("id" -> "spoof", "revision" -> 99L, "document" -> Json.Null,
            "materialRef" -> "spoof", "state" -> "spoof", "titleSearchV1" -> "spoof", "referenceSearchV1" -> "spoof"))
        When("the normal logical and stored codecs encode and decode the complete entity")
        val logical = MaterialRegistrationEntity.persistent.toRecord(entity)
        val stored = MaterialRegistrationEntity.persistent.toStoreRecord(entity)
        val restored = take(MaterialRegistrationEntity.persistent.fromStoreRecord(stored))
        val draft = MaterialRegistrationDraft(entity.document)
        val created = MaterialRegistrationEntity.createPersistent.toStoreRecord(draft)
        Then("managed fields have one authoritative value and the create codec supplies no native defaults")
        MaterialRegistrationEntity.collectionId shouldBe EntityCollectionId("textus", "knowledge_lake", "MaterialPackageRegistration")
        MaterialRegistrationEntity.ownerName shouldBe "MaterialPackageRegistration"
        MaterialRegistrationEntity.revisionBinding.representation shouldBe EntityRevisionRepresentation.Embedded
        logical.getAny("id") shouldBe Some(entity.id)
        restored.id shouldBe entity.id
        restored.revision shouldBe entity.revision
        restored.document.json shouldBe entity.document.json
        restored.attributes.fields shouldBe attributes.fields
        Set("id", "revision", "document", "materialRef", "state", "titleSearchV1", "referenceSearchV1")
          .foreach(key => logical.fields.count(_.key == key) shouldBe 1)
        stored.getString("materialRef") shouldBe Some(entity.document.location.urn)
        stored.getString("state") shouldBe Some("prepared")
        stored.getString("titleSearchV1") shouldBe Some(DataStoreTextSearchField("titleSearchV1", caseInsensitive = true)
          .toStoreValue(entity.document.contribution.title))
        stored.getString("referenceSearchV1") shouldBe Some(DataStoreTextSearchField("referenceSearchV1", caseInsensitive = true)
          .toStoreValue(entity.document.location.urn))
        stored.getAny("title") shouldBe None
        created.fields.map(_.key).toSet shouldBe Set("document", "materialRef", "state", "titleSearchV1", "referenceSearchV1")
        Vector("titleSearchV1", "referenceSearchV1").foreach(key => created.getAny(key) shouldBe stored.getAny(key))
        MaterialRegistrationEntity.createPersistent.id(draft) shouldBe None
        MaterialRegistrationEntity.createPersistent.collection(draft) shouldBe MaterialRegistrationEntity.collectionId
      }

      "reject noncanonical foreign missing duplicate and contradictory native fields" in {
        Given("an issued registration identity and one valid logical record")
        val issuer = IdGenerationContext.deterministic(IdGenerationContext.DEFAULT_NAMESPACE, "registration-invalid")
        val entity = MaterialRegistrationRecord(issuer.entityId(MaterialRegistrationEntity.collectionId), EntityRevision.INITIAL, document(), Record.empty)
        val logical = MaterialRegistrationEntity.persistent.toRecord(entity)
        val foreign = issuer.entityId(EntityCollectionId("textus", "knowledge_lake", "Other"))
        val invalid = Vector(logical.upsertSingle("id", foreign), logical.upsertSingle("id", "receipt-1"),
          logical.upsertSingle("id", " " + entity.id.value), logical.upsertSingle("revision", 0L),
          logical.upsertSingle("revision", "invalid"), logical.upsertSingle("state", "registered"),
          logical.upsertSingle("materialRef", "urn:other"), logical.upsertSingle("document", Json.obj()),
          logical ++ Record.dataAuto("cncf_revision" -> 1L)) ++
          logical.fields.map(field => Record(logical.fields.filterNot(_.key == field.key))) ++
          logical.fields.map(field => Record(logical.fields :+ field))
        val store = MaterialRegistrationEntity.persistent.toStoreRecord(entity)
        val projections = Vector("titleSearchV1" -> entity.document.contribution.title, "referenceSearchV1" -> entity.document.location.urn)
        def _malformed_projections_(record: Record): Vector[Record] = projections.flatMap { case (key, original) =>
          val encoded = record.getString(key).get
          Vector(Record(record.fields.filterNot(_.key == key)), record ++ Record.dataAuto(key -> encoded),
            record.upsertSingle(key, 1L), record.upsertSingle(key, Json.fromString(encoded)),
            record.upsertSingle(key, encoded.replace("u16x1:", "u16x2:")),
            record.upsertSingle(key, encoded.toUpperCase(Locale.ROOT)),
            record.upsertSingle(key, DataStoreTextSearchField(key).toStoreValue(original.toUpperCase(Locale.ROOT))),
            record.upsertSingle(key, DataStoreTextSearchField(key, caseInsensitive = true).toStoreValue(original + "stale")))
        }
        When("the canonical ID string and malformed logical and stored records are decoded")
        val canonical = take(MaterialRegistrationEntity.persistent.fromRecord(logical.upsertSingle("id", entity.id.value)))
        val results = (invalid ++ _malformed_projections_(logical)).map(MaterialRegistrationEntity.persistent.fromRecord) ++
          _malformed_projections_(store).map(MaterialRegistrationEntity.persistent.fromStoreRecord) ++ Vector(
          MaterialRegistrationEntity.persistent.fromStoreRecord(store.upsertSingle("document", "{")),
          MaterialRegistrationEntity.persistent.fromStoreRecord(store ++ Record.dataAuto("document" -> "{}")))
        Then("only canonical consistent fields decode and errors never repeat opaque documents")
        canonical.id shouldBe entity.id
        results.foreach { result =>
          failure(result).observation.taxonomy shouldBe Taxonomy.argumentInvalid
          failure(result).displayMessage shouldBe "Invalid Material registration entity"
        }
      }

      "redact every native document record draft and receipt display" in {
        Given("a registration carrying private opaque contribution and physical keys")
        val prepared = document()
        val receipt = stored(prepared)
        val registered = take(take(prepared.beginC).completeC(receipt))
        val issuer = IdGenerationContext.deterministic(IdGenerationContext.DEFAULT_NAMESPACE, "registration-display")
        val entity = MaterialRegistrationRecord(issuer.entityId(MaterialRegistrationEntity.collectionId), EntityRevision.INITIAL, registered,
          Record.dataAuto("secret" -> "private-marker"))
        When("diagnostic strings are requested")
        val displays = Vector(prepared.toString, registered.toString, registered.stored.get.toString,
          registered.stored.get.files.toString, entity.toString, MaterialRegistrationDraft(prepared).toString,
          receipt.toString, receipt._files.toString)
        Then("diagnostics retain no private content or keys")
        displays.foreach { value =>
          value should not include "private"
          value should not include "test-store"
          value should not include prepared.location.urn
        }
      }
    }

    "retain arbitrary UTF16 and JSON" which {
      "roundtrip at least one hundred undiscarded contributions through ASCII SQL TEXT" in {
        Given("a native issuer and arbitrary UTF16 titles and recursive opaque JSON values")
        val issuer = IdGenerationContext.deterministic(IdGenerationContext.DEFAULT_NAMESPACE, "registration-property")
        val unit = Gen.choose(0, 65535).map(_.toChar)
        val text = Gen.choose(0, 40).flatMap(size => Gen.listOfN(size, unit).map(_.mkString))
        def _json_(depth: Int): Gen[Json] = if (depth == 0) Gen.oneOf(Gen.const(Json.Null), text.map(Json.fromString), Gen.choose(-100, 100).map(Json.fromInt))
          else Gen.oneOf(_json_(0), Gen.listOfN(2, _json_(depth - 1)).map(Json.fromValues),
            _json_(depth - 1).map(value => Json.obj("opaque" -> value)))
        When("the lossless public native codec roundtrip is checked for generated complete contributions")
        val result = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), Prop.forAll(text, _json_(2)) { (literal, opaque) =>
          val raw = contributionJson(title = "title\u0000\uD800\uDC00\uD800" + literal, opaque = opaque)
          val prepared = document(raw)
          val entity = MaterialRegistrationRecord(issuer.entityId(MaterialRegistrationEntity.collectionId), EntityRevision.INITIAL, prepared, Record.empty)
          val logical = MaterialRegistrationEntity.persistent.toRecord(entity)
          val stored = MaterialRegistrationEntity.persistent.toStoreRecord(entity)
          val restored = take(MaterialRegistrationEntity.persistent.fromStoreRecord(stored))
          val fromlogical = take(MaterialRegistrationEntity.persistent.fromRecord(logical))
          val encoded = stored.getString("document").get
          encoded.forall(_ <= 127) && parser.parse(encoded).toOption.contains(prepared.json) &&
            restored.document.contribution.json == raw && fromlogical.document.contribution.json == raw &&
            Vector("titleSearchV1", "referenceSearchV1").forall(key => stored.getString(key).exists(_.forall(_ <= 127))) &&
            stored.getString("titleSearchV1").contains(DataStoreTextSearchField("titleSearchV1", caseInsensitive = true)
              .toStoreValue(prepared.contribution.title)) &&
            stored.getString("referenceSearchV1").contains(DataStoreTextSearchField("referenceSearchV1", caseInsensitive = true)
              .toStoreValue(prepared.location.urn)) &&
            stored.getAny("title").isEmpty
        })
        Then("all samples preserve NUL unpaired units and complete JSON without a title SQL column")
        result.passed shouldBe true
        result.succeeded should be >= 100
        result.discarded shouldBe 0
      }
    }
  }

  "Native Material registration search query" should {
    "declare the internal Owner view with paired projections and no query paging" which {
      "match only Registered using literal symbols ROOT context and complete UTF16 source Strings" in {
        Given("complete source titles, all native lifecycle states and independently chosen admitted needles")
        val sources = Vector("literal % _ ! \\ ' symbols", "literal abc X decoy", "ΟΣ ΣοΣ İIıi",
          "e\u0301 \u00e9 \uD83D\uDE00 \uD800A\uDC00", "control\u0000title\nretained", "u16x1:x0025")
        val needles = Vector(None, Some(""), Some("%"), Some("_"), Some("!"), Some("\\"), Some("'"),
          Some("Σ"), Some("ΟΣ"), Some("İ"), Some("i"), Some("\u0301"), Some("\uDE00"), Some("\uD800"),
          Some("title"), Some("NATIVE-LAKE"), Some("2026/ITEM-1"), Some("absent"))
        val issuer = IdGenerationContext.deterministic(IdGenerationContext.DEFAULT_NAMESPACE, "registration-query")
        sources.foreach { source =>
          val prepared = document(contributionJson(title = source))
          val materializing = take(prepared.beginC)
          val registered = take(materializing.completeC(stored(prepared)))
          Vector(prepared, materializing, registered).foreach { document =>
            val record = MaterialRegistrationEntity.persistent.toStoreRecord(MaterialRegistrationRecord(
              issuer.entityId(MaterialRegistrationEntity.collectionId), EntityRevision.INITIAL, document, Record.empty))
            needles.foreach { text =>
              When("the product query predicate interprets the stored projection of that lifecycle state")
              val query = MaterialRegistrationEntity.searchQuery(text)
              val actual = Query.eval(query.query.where, record)
              val needle = text.getOrElse("").toLowerCase(Locale.ROOT)
              val expected = document.status.value == "registered" && (source.toLowerCase(Locale.ROOT).contains(needle) ||
                document.location.urn.toLowerCase(Locale.ROOT).contains(needle))
              Then("the query agrees with independent original String semantics and carries exact paired predicates")
              actual shouldBe expected
              query.collection shouldBe MaterialRegistrationEntity.collectionId
              query.scope shouldBe EntitySearchScope.Store
              query.visibilityScope shouldBe Some(EntityVisibilityScope.Owner)
              query.query.limit shouldBe None
              query.query.offset shouldBe None
              query.query.includeTotal shouldBe false
              query.query.where shouldBe Query.And(Vector(Query.Eq("state", "registered"), Query.Or(Vector(
                DataStoreTextSearchField("titleSearchV1", caseInsensitive = true).contains(text.getOrElse("")),
                DataStoreTextSearchField("referenceSearchV1", caseInsensitive = true).contains(text.getOrElse(""))))))
            }
          }
        }
      }
    }
  }
}

private[material] object MaterialRegistrationEntitySpec {
  import MaterialContributionSpec.{contributionJson, creator, owner, take}
  def document(json: Json = contributionJson()): MaterialRegistrationDocument = take(MaterialRegistrationDocument.preparedC(
    take(MaterialContribution.parseC(json)), take(MaterialLocation.createC("test-lake", "2026/item-1")), "host-binding", owner, creator))
  def stored(document: MaterialRegistrationDocument): StoredMaterialPackage = {
    val plan = take(MaterialPackage.planC(document.contribution, document.location, document.owner, document.createdBy))
    val root = FileStoreRef("test-store", "private-root")
    val originals = plan.originals.zipWithIndex.map { case (value, index) =>
      new StoredMaterialPackageFile(value.path, FileStoreRef("test-store", "private-original-" + index), FileStoreRef("test-store", "private-assets")) }
    val documents = plan.documents.zipWithIndex.map { case (value, index) =>
      new StoredMaterialPackageFile(value.path, FileStoreRef("test-store", "private-document-" + index), FileStoreRef("test-store", "private-derived")) }
    new StoredMaterialPackage(plan, root, FileStoreRef("test-store", "private-manifest"), originals ++ documents)
  }
}
