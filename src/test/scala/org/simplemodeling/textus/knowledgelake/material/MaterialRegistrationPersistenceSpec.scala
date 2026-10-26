package org.simplemodeling.textus.knowledgelake.material

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Locale
import io.circe.{Json, parser}
import cats.syntax.all.*
import org.goldenport.{Consequence, ConsequenceException}
import org.goldenport.cncf.context.IdGenerationContext
import org.goldenport.cncf.datastore.{DataStore, DataStoreTextSearchField, Query as StoreQuery, QueryLimit, QueryProjection}
import org.goldenport.cncf.directive.Query
import org.goldenport.cncf.entity.EntitySnapshot
import org.goldenport.convert.ValueReader
import org.goldenport.observation.{Descriptor, Taxonomy}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.model.datatype.{EntityCollectionId, EntityId}
import org.simplemodeling.model.statemachine.PostStatus

/*
 * Real PostgreSQL durability and native authorization before external effects.
 * The separate SQL and physical file oracles do not write registration state.
 *
 * @since   Oct. 21, 2026
 * @version Oct. 21, 2026
 */
final class MaterialRegistrationPersistenceSpec extends AnyWordSpec with Matchers with GivenWhenThen
    with MaterialRegistrationPostgresqlSpec {
  import MaterialRegistrationPersistenceFixture.*
  import MaterialRegistrationPersistenceSpec.*

  "Native PostgreSQL Material registration" should {
    "commit the forward lifecycle before acknowledging physical completion" which {
      "create fresh identities query without writes materialize after revision two and reload from a new runtime" in {
        Given("required durable PostgreSQL, a provider-authenticated owning component and real test-owned source/destination files")
        withDatabase(native_container) { database =>
          database.durability shouldBe ("on" -> "on")
          val contribution = _contribution()
          val persisted = withSession(database) { session =>
            val destination = new Destination(Files.createDirectory(session.root.resolve("destination")), database)
            val source = new Originals(Files.createDirectory(session.root.resolve("originals")), contribution)
            database.snapshot().rows shouldBe empty
            When("ordinary native commands create query and then materialize the returned registration identity")
            val prepared = take(session.create(contribution))
            val beforequery = database.snapshot()
            val queried = take(session.get(prepared.entity.id))
            val afterquery = database.snapshot()
            val queryeffects = (destination.events, source.calls)
            val registered = take(session.materialize(prepared.entity.id, destination, source))
            val physical = database.snapshot()
            Then("native creation supplies revision one and actual principal metadata while the query performs no writes or provider access")
            prepared.revision.value shouldBe 1L
            prepared.entity.id.collection shouldBe MaterialRegistrationEntity.collectionId
            prepared.entity.id.value should not be prepared.entity.document.location.urn
            prepared.entity.document.owner.value shouldBe principal
            prepared.entity.document.createdBy.value shouldBe principal
            prepared.entity.document.contribution.json shouldBe contribution.json
            beforequery.rows should have size 1
            beforequery.writes shouldBe Vector(prepared.entity.id.value -> "INSERT")
            queried.entity.document.json shouldBe prepared.entity.document.json
            afterquery shouldBe beforequery
            queryeffects shouldBe ((Vector.empty, Vector.empty))
            source.calls shouldBe contribution.originals
            beforequery.rows.head("owner_id") shouldBe Some(principal)
            beforequery.rows.head("created_by") shouldBe Some(principal)
            beforequery.rows.head("materialRef") shouldBe Some(prepared.entity.document.location.urn)
            _post_status(beforequery.rows.head) shouldBe PostStatus.Published
            parser.parse(beforequery.rows.head("permission").get).toOption shouldBe
              Some(parser.parse(ownerPermission).toOption.get)
            _document(beforequery.rows.head) shouldBe prepared.entity.document.json
            _projections(beforequery.rows.head, prepared.entity.document)
            And("every external call sees separately committed Materializing revision two and exact INSERT UPDATE UPDATE audit history")
            destination.observed should not be empty
            destination.observed.foreach { state =>
              state.rows.head("state") shouldBe Some("materializing")
              state.rows.head(revisionName) shouldBe Some("2")
              state.writes shouldBe Vector(prepared.entity.id.value -> "INSERT", prepared.entity.id.value -> "UPDATE")
              _projections(state.rows.head, prepared.entity.document)
            }
            registered.revision.value shouldBe 3L
            registered.entity.revision.value shouldBe 3L
            registered.entity.document.status.value shouldBe "registered"
            physical.writes shouldBe Vector(prepared.entity.id.value -> "INSERT", prepared.entity.id.value -> "UPDATE", prepared.entity.id.value -> "UPDATE")
            _document(physical.rows.head) shouldBe registered.entity.document.json
            _projections(physical.rows.head, registered.entity.document)
            physical.rows.head("state") shouldBe Some("registered")
            physical.rows.head(revisionName) shouldBe Some("3")
            _post_status(physical.rows.head) shouldBe PostStatus.Published
            val changed = Set("document", "state", revisionName, "updated_at", "updated_by", "trace_id", "correlation_id")
            physical.rows.head.filterNot(entry => changed(entry._1)) shouldBe beforequery.rows.head.filterNot(entry => changed(entry._1))
            val mapping = registered.entity.document.stored.get
            val plan = take(MaterialPackage.planC(contribution, prepared.entity.document.location,
              prepared.entity.document.owner, prepared.entity.document.createdBy))
            val leaf = prepared.entity.document.location.logicalPath.split('/').last
            destination.events shouldBe Vector("collection:" + leaf, "collection:assets") ++
              contribution.originals.map(original => "file:" + original.resourceId) ++ Vector("collection:derived") ++
              contribution.documents.map(document => "file:" + document.resourceId + ".json") ++ Vector("file:material.json")
            mapping.root shouldBe destination.entries.find(_.label == leaf).get.reference
            mapping.manifest shouldBe destination.entries.find(_.label == "material.json").get.reference
            parser.parse(new String(destination.bytes(mapping.manifest).toArray, StandardCharsets.UTF_8)).toOption shouldBe Some(plan.manifest)
            mapping.files.map(_.path) shouldBe plan.originals.map(_.path) ++ plan.documents.map(_.path)
            mapping.files.foreach { file =>
              val entry = destination.entries.find(_.reference == file.reference).get
              entry.parent shouldBe file.parent
              destination.entries.find(_.reference == file.parent).get.label shouldBe
                (if (file.path.startsWith("assets/")) "assets" else "derived")
            }
            mapping.files.take(contribution.originals.size).zip(contribution.originals).foreach { case (file, original) =>
              destination.bytes(file.reference) shouldBe source.expected(original)
            }
            mapping.files.drop(contribution.originals.size).zip(contribution.documents).foreach { case (file, document) =>
              parser.parse(new String(destination.bytes(file.reference).toArray, StandardCharsets.UTF_8)).toOption shouldBe Some(document.body)
            }
            val calls = destination.events
            val reads = source.calls
            When("the same owning command explicitly attempts materialization again")
            val repeated = session.materialize(prepared.entity.id, destination, source)
            Then("Registered refuses another attempt before any provider IO or native mutation")
            _not_prepared(repeated)
            destination.events shouldBe calls
            source.calls shouldBe reads
            database.snapshot() shouldBe physical
            (prepared.entity.id, registered.entity.document.json, physical)
          }
          Given("a new bootstrap component SQL datastore and interpreter for the same durable registered snapshot")
          withSession(database) { reopened =>
            val destination = new Destination(Files.createDirectory(reopened.root.resolve("destination")), database)
            val source = new Originals(Files.createDirectory(reopened.root.resolve("originals")), contribution)
            When("get reloads native state and an explicit repeated materialization is requested")
            val loaded = take(reopened.get(persisted._1))
            val repeated = reopened.materialize(persisted._1, destination, source)
            Then("the actual saved mapping is authoritative and neither query nor repeat performs another write")
            loaded.entity.document.json shouldBe persisted._2
            loaded.revision.value shouldBe 3L
            _projections(persisted._3.rows.head, loaded.entity.document)
            _not_prepared(repeated)
            destination.events shouldBe empty
            source.calls shouldBe empty
            database.snapshot() shouldBe persisted._3
          }
        }
      }

      "give each separate create its own native identity and leaf without another insert on get" in {
        Given("one authenticated native runtime and a complete identical contribution")
        withDatabase(native_container) { database => withSession(database) { session =>
          val contribution = _contribution()
          When("two explicit create commands and subsequent get commands run")
          val first = take(session.create(contribution))
          val second = take(session.create(contribution))
          val beforeget = database.snapshot()
          val loaded = Vector(first, second).map(value => take(session.get(value.entity.id)))
          Then("two native INSERTs and independent TKL leaves remain with no query mutations")
          first.entity.id should not be second.entity.id
          first.entity.document.location.urn should not be second.entity.document.location.urn
          loaded.map(_.revision.value) shouldBe Vector(1L, 1L)
          loaded.map(_.entity.document.contribution.json) shouldBe Vector.fill(2)(contribution.json)
          beforeget.writes shouldBe Vector(first.entity.id.value -> "INSERT", second.entity.id.value -> "INSERT")
          database.snapshot() shouldBe beforeget
        } }
      }
    }

    "search Registered through the ordinary authorized native SQL route" which {
      "retain literal and Unicode meaning page after authorization and reopen without query side effects" in {
        Given("durable PostgreSQL, no managed grant, original titles and normal owner and foreign registration programs")
        withDatabase(native_container) { database =>
          database.durability shouldBe ("on" -> "on")
          val retained = withSession(database) { session =>
            val titles = Vector("literal % marker", "literal _ marker", "literal ! marker", "literal \\ marker",
              "literal ' marker", "literal X decoy", "literal abc decoy", "ΟΣ ΣοΣ İIıi marker",
              "e\u0301 \u00e9 \uD83D\uDE00 \uD800A\uDC00 marker", "control\u0000title\nmarker")
            val registered = titles.map(title => _seed_registered(session, title))
            val hidden = _seed_registered(session, "literal % _ ! \\ ' ΟΣ İ marker", "foreign-session")
            val prepared = take(session.create(_search_contribution("literal % marker prepared")))
            val failingcontribution = _search_contribution("literal % marker materializing")
            val failing = take(session.create(failingcontribution))
            val failingroot = Files.createTempDirectory(session.root, "search-failure-")
            val failingdestination = new Destination(Files.createDirectory(failingroot.resolve("destination")), database,
              Some("collection:assets"))
            val failingsource = new Originals(Files.createDirectory(failingroot.resolve("originals")), failingcontribution)
            When("normal materialization fails after the durable Materializing transition")
            val failed = session.materialize(failing.entity.id, failingdestination, failingsource)
            val materializing = take(session.get(failing.entity.id))
            Then("that failed native registration remains Materializing with its original contribution and exact provider failure")
            failure(failed) shouldBe failure(failingdestination.error)
            materializing.entity.document.status.value shouldBe "materializing"
            materializing.entity.document.contribution.json shouldBe failingcontribution.json

            Given("Registered, Prepared, failed Materializing and matching foreign-owner rows in the same native collection")
            val seeds = registered :+ hidden
            val documents = seeds.map(_.snapshot.entity.document) ++ Vector(prepared.entity.document, materializing.entity.document)
            val before = database.snapshot()
            val effects = seeds.map(value => value.destination.events -> value.source.calls)
            val failureeffects = failingdestination.events -> failingsource.calls
            before.rows.foreach { row =>
              val document = take(MaterialRegistrationDocument.parseC(_document(row)))
              _projections(row, document)
              _post_status(row) shouldBe PostStatus.Published
              _document(row).hcursor.downField("contribution").focus shouldBe Some(document.contribution.json)
            }
            val reference = registered.head.snapshot.entity.document.location.urn
            val texts = Vector(None, Some(""), Some("%"), Some("_"), Some("!"), Some("\\"), Some("'"),
              Some("Σ"), Some("ΟΣ"), Some("İ"), Some("i"), Some("\u0301"), Some("\uDE00"), Some("\uD800"),
              Some("title"), Some("MARKER"), Some(reference.toUpperCase(Locale.ROOT)), Some("no-match"))
            val pages = Vector(0 -> 1, 1 -> 2, 0 -> 100, Int.MaxValue -> 20)
            texts.foreach { text => pages.foreach { case (offset, limit) =>
              val start = session.store.searches.size
              When("an owner query uses the normal Entity search before exact URN ordering and result paging")
              val page = take(session.search(text, offset, limit))
              val after = database.snapshot()
              Then("only authorized Registered original Strings determine items and hasMore and all durable and provider state is unchanged")
              _expect_page(page, documents, principal, text, offset, limit)
              if (text.isEmpty && offset == 0) take(page.getAsC[Vector[Record]]("items")).get should not be empty
              _delegated_search(session, start, text)
              after shouldBe before
              seeds.map(value => value.destination.events -> value.source.calls) shouldBe effects
              (failingdestination.events -> failingsource.calls) shouldBe failureeffects
            } }
            Vector(None, Some("%"), Some("MARKER")).foreach { text =>
              val start = session.store.searches.size
              When("the actual foreign provider subject searches with its own native permissions")
              val page = take(session.search(text, limit = 1, session = Some("foreign-session")))
              val after = database.snapshot()
              Then("only that authenticated subject's visible Registered row affects its page")
              _expect_page(page, documents, foreign, text, 0, 1)
              take(page.getAsC[Vector[Record]]("items")).get should have size 1
              _delegated_search(session, start, text)
              after shouldBe before
              seeds.map(value => value.destination.events -> value.source.calls) shouldBe effects
              (failingdestination.events -> failingsource.calls) shouldBe failureeffects
            }
            (documents, before)
          }
          Given("a fresh bootstrap component and native datastore reopening exactly those stored rows")
          withSession(database) { reopened =>
            Vector(None, Some("%"), Some("ΟΣ"), Some("\uD800"), Some("MARKER")).foreach { text =>
              val start = reopened.store.searches.size
              When("the reopened ordinary native search reads the same Registered projections")
              val page = take(reopened.search(text, offset = 1, limit = 2))
              val after = database.snapshot()
              Then("the same authorized page is reconstructed solely from unchanged durable native rows")
              _expect_page(page, retained._1, principal, text, 1, 2)
              _delegated_search(reopened, start, text)
              after shouldBe retained._2
              after.rows.foreach(row => _projections(row, take(MaterialRegistrationDocument.parseC(_document(row)))))
            }
          }
        }
      }

      "reject missing authentication and invalid parameters before querying and filter owner read denial without effects" in {
        Given("a real Registered row with counted physical source and destination and no managed grant")
        withDatabase(native_container) { database => withSession(database) { session =>
          val seeded = _seed_registered(session, "query admission marker")
          val before = database.snapshot()
          val effects = seeded.destination.events -> seeded.source.calls
          val searches = session.store.searches
          When("an unauthenticated caller supplies otherwise valid and invalid search parameters")
          val anonymous = Vector(session.search(None, session = None),
            session.search(Some("bad\ntext"), offset = -1, limit = 0, session = None))
          Then("provider authentication fails first with no datastore query or physical effect")
          anonymous.foreach(result => failure(result).observation.taxonomy shouldBe Taxonomy.securityAuthenticationRequired)
          session.store.searches shouldBe searches
          database.snapshot() shouldBe before
          (seeded.destination.events -> seeded.source.calls) shouldBe effects
          Given("an authenticated owner with null oversized control-bearing or out-of-range query parameters")
          val invalid = Vector((Some(null: String), 0, 20), (Some("x" * 257), 0, 20), (Some("x\u0000y"), 0, 20),
            (Some("x\ny"), 0, 20), (Some("\u007F"), 0, 20), (Some("\u0085"), 0, 20),
            (None, -1, 20), (None, 0, 0), (None, 0, 101), (None, 0, Int.MinValue))
          invalid.foreach { case (text, offset, limit) =>
            When("the normal command interprets invalid search vocabulary")
            val result = session.search(text, offset, limit)
            Then("one fixed structured argument failure occurs before any delegated query or side effect")
            failure(result).observation.taxonomy shouldBe Taxonomy.argumentInvalid
            failure(result).displayMessage shouldBe "Material search parameters are invalid"
            session.store.searches shouldBe searches
            database.snapshot() shouldBe before
            (seeded.destination.events -> seeded.source.calls) shouldBe effects
          }
          When("the owner supplies empty text and the maximum admitted 256-unit text")
          val admitted = Vector(Some(""), Some("x" * 256)).map { text =>
            val start = session.store.searches.size
            val page = take(session.search(text))
            (text, start, page)
          }
          Then("both requests reach the ordinary predicate and preserve all rows and provider call vectors")
          admitted.foreach { case (text, start, page) =>
            _expect_page(page, Vector(seeded.snapshot.entity.document), principal, text, 0, 20)
            val captured = session.store.searches(start)
            _expect_directive(captured, text)
          }
          session.store.searches.size shouldBe searches.size + 2
          database.snapshot() shouldBe before
          (seeded.destination.events -> seeded.source.calls) shouldBe effects
          Given("the same Published Registered row retains its actual owner with private permissions but grants no owner group or other permission")
          database.security(seeded.snapshot.entity.id, permission = readDeniedPermission)
          val denied = database.snapshot()
          val start = session.store.searches.size
          denied.rows.head("owner_id") shouldBe Some(principal)
          denied.rows.head("permission") shouldBe Some(readDeniedPermission)
          _post_status(denied.rows.head) shouldBe PostStatus.Published
          When("the provider-authenticated owner searches through ordinary native UserPermission authorization")
          val page = take(session.search(None))
          val after = database.snapshot()
          Then("Owner visibility grants no read access and the empty page changes neither SQL audit nor provider calls")
          take(page.getAsC[Vector[Record]]("items")) shouldBe Some(Vector.empty[Record])
          take(page.getAsC[Boolean]("hasMore")) shouldBe Some(false)
          _delegated_search(session, start, None)
          after shouldBe denied
          (seeded.destination.events -> seeded.source.calls) shouldBe effects
        } }
      }

      "match at least one hundred undiscarded arbitrary UTF16 sources using independent original String expectations" in {
        Given("one owned PostgreSQL database and session, full UTF16 sources and independently generated admitted needles")
        withDatabase(native_container) { database => withSession(database) { session =>
          val unit = Gen.choose(0, 65535).map(_.toChar)
          val source = Gen.choose(0, 32).flatMap(size => Gen.listOfN(size, unit).map(_.mkString))
          val admittedunit = Gen.oneOf(Gen.choose(32, 126), Gen.choose(160, 65535)).map(_.toChar)
          val needle = Gen.choose(0, 24).flatMap(size => Gen.listOfN(size, admittedunit).map(_.mkString))
          var corpus = Vector.empty[MaterialRegistrationDocument]
          var seeds = Vector.empty[SearchSeed]
          val property = Prop.forAll(source, needle, Gen.choose(0, 3), Gen.choose(1, 4)) { (literal, text, offset, limit) =>
            Given("fresh normal native registrations with arbitrary source code units, a foreign matching decoy and a Prepared matching decoy")
            val title = "nonblank \u0000\uD800\uDC00\uD800 " + literal + " " + text + " tail"
            val registered = _seed_registered(session, title)
            val hidden = _seed_registered(session, title, "foreign-session")
            val prepared = take(session.create(_search_contribution(title)))
            seeds ++= Vector(registered, hidden)
            corpus ++= Vector(registered.snapshot.entity.document, hidden.snapshot.entity.document, prepared.entity.document)
            val before = database.snapshot()
            val effects = seeds.map(value => value.destination.events -> value.source.calls)
            val start = session.store.searches.size
            When("the authenticated normal native search queries the independent needle then orders and pages authorized matches")
            val page = take(session.search(Some(text), offset, limit))
            val after = database.snapshot()
            Then("the page equals the original String corpus oracle and the query leaves SQL audit and physical call vectors unchanged")
            _expect_page(page, corpus, principal, Some(text), offset, limit)
            _delegated_search(session, start, Some(text))
            after shouldBe before
            seeds.map(value => value.destination.events -> value.source.calls) shouldBe effects
            val documents = Map(registered.snapshot.entity.id.value -> registered.snapshot.entity.document,
              hidden.snapshot.entity.id.value -> hidden.snapshot.entity.document, prepared.entity.id.value -> prepared.entity.document)
            before.rows.filter(row => documents.contains(row("id").get)).foreach { row =>
              val document = documents(row("id").get)
              _document(row) shouldBe document.json
              _projections(row, document)
              _post_status(row) shouldBe PostStatus.Published
            }
            true
          }
          When("the native PostgreSQL search correspondence property runs at least one hundred samples")
          val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100).withWorkers(1), property)
          Then("all generated queries retain source meaning without discarded inputs")
          withClue(checked.status.toString) {
            checked.passed shouldBe true
          }
          checked.succeeded should be >= 100
          checked.discarded shouldBe 0
        } }
      }
    }

    "expose an explicitly selected registered public reference view" which {
      "search and get logical metadata through generated queries and reopen without catalog grant or provider access" in {
        Given("required PostgreSQL and an explicitly registered component with no bootstrap catalog or OAuth grant")
        withDatabase(native_container) { database =>
          val retained = withSession(database, _registered_parameters) { session =>
            val title = "Public % ΟΣ literal\u0000\uD800"
            val first = _seed_registered(session, title)
            val second = _seed_registered(session, "Public % second")
            val hidden = _seed_registered(session, "Public % ΟΣ hidden", "foreign-session")
            val prepared = take(session.create(_search_contribution("Public % ΟΣ prepared")))
            val failedcontribution = _search_contribution("Public % ΟΣ materializing")
            val failed = take(session.create(failedcontribution))
            val failedroot = Files.createTempDirectory(session.root, "public-materializing-")
            val destination = new Destination(Files.createDirectory(failedroot.resolve("destination")), database, Some("collection:assets"))
            val source = new Originals(Files.createDirectory(failedroot.resolve("originals")), failedcontribution)
            When("normal materialization of the matching decoy fails after its durable state transition")
            val failedresult = session.materialize(failed.entity.id, destination, source)
            val materializing = take(session.get(failed.entity.id))
            Then("the decoy remains unavailable Materializing rather than becoming a public Material")
            failure(failedresult) shouldBe failure(destination.error)
            materializing.entity.document.status.value shouldBe "materializing"

            Given("two owned Registered rows and matching foreign Prepared and Materializing decoys")
            val seeds = Vector(first, second, hidden)
            val documents = seeds.map(_.snapshot.entity.document) ++ Vector(prepared.entity.document, materializing.entity.document)
            val reference = first.snapshot.entity.document.location.urn
            val before = database.snapshot()
            val effects = seeds.map(seed => seed.destination.events -> seed.source.calls)
            val failedeffects = destination.events -> source.calls
            val start = session.store.searches.size
            When("the actual generated Material Operations interpret literal paging ROOT matching and exact selection")
            val page = _public_record(session.query("searchMaterials", Vector("text" -> "%", "offset" -> 1, "limit" -> 1)))
            val folded = _public_record(session.query("searchMaterials", Vector("text" -> "ος")))
            val metadata = _public_record(session.query("getMaterial", Vector("materialRef" -> reference)))
            Then("only authorized Registered original Strings determine the page and logical metadata")
            _expect_page(page, documents, principal, Some("%"), 1, 1)
            _expect_page(folded, documents, principal, Some("ος"), 0, 20)
            _expect_directive(session.store.searches(start), Some("%"))
            _expect_directive(session.store.searches(start + 1), Some("ος"))
            _expect_reference_directive(session.store.searches(start + 2), reference)
            session.store.searches.size shouldBe start + 3
            metadata.fields.map(_.key).toSet shouldBe Set("materialRef", "title", "resources", "provenance")
            metadata.getString("materialRef") shouldBe Some(reference)
            metadata.getString("title") shouldBe Some(title)
            val resources = take(metadata.getAsC[Vector[Record]]("resources")).get
            resources.map(_.getString("resourceId")) shouldBe Vector(Some("image-1"), Some("image-2"), Some("audio-1"))
            resources.map(_.getString("role")) shouldBe Vector(Some(" image-front "), Some("unselected-image"), Some("audio-original"))
            resources.map(_.getString("mediaType")) shouldBe Vector(Some("Image/PNG"), Some("image/jpeg"), Some("audio/wav"))
            resources.map(value => take(value.getAsC[Long]("byteLength"))) shouldBe Vector.fill(3)(Some(4L))
            resources.foreach(value => value.fields.map(_.key).toSet shouldBe Set("resourceId", "role", "mediaType", "byteLength"))
            val provenance = take(metadata.getAsC[Vector[Record]]("provenance")).get
            provenance.map(_.fields) shouldBe Vector(Record.data("kind" -> "registration", "reference" -> first.snapshot.entity.id.value).fields)
            Vector("private-source-marker", "private-provenance-marker", "sourceReference", "storageBinding", "physical-").foreach(marker =>
              metadata.print should not include marker)
            first.snapshot.entity.id.value should not be reference
            session.component.materialCatalog shouldBe None
            database.snapshot() shouldBe before
            seeds.map(seed => seed.destination.events -> seed.source.calls) shouldBe effects
            (destination.events -> source.calls) shouldBe failedeffects
            (documents, reference, metadata, before)
          }
          Given("a fresh explicitly registered component and interpreter reading the same durable rows without a grant")
          withSession(database, _registered_parameters) { reopened =>
            val start = reopened.store.searches.size
            When("generated search and get reload the public logical projection from PostgreSQL")
            val page = _public_record(reopened.query("searchMaterials", Vector("text" -> "%", "offset" -> 1, "limit" -> 1)))
            val metadata = _public_record(reopened.query("getMaterial", Vector("materialRef" -> retained._2)))
            Then("the exact result survives reinitialization with no catalog fallback or durable changes")
            _expect_page(page, retained._1, principal, Some("%"), 1, 1)
            metadata.fields shouldBe retained._3.fields
            _expect_directive(reopened.store.searches(start), Some("%"))
            _expect_reference_directive(reopened.store.searches(start + 1), retained._2)
            reopened.store.searches.size shouldBe start + 2
            reopened.component.materialCatalog shouldBe None
            database.snapshot() shouldBe retained._4
          }
        }
      }

      "withhold unavailable and unauthorized exact references without a provider lookup or bootstrap fallback" in {
        Given("registered mode with an owned row a foreign row a Prepared row and a failed Materializing row")
        withDatabase(native_container) { database => withSession(database, _registered_parameters) { session =>
          val own = _seed_registered(session, "available original")
          val hidden = _seed_registered(session, "foreign original", "foreign-session")
          val prepared = take(session.create(_search_contribution("prepared original")))
          val contribution = _search_contribution("materializing original")
          val pending = take(session.create(contribution))
          val root = Files.createTempDirectory(session.root, "public-pending-")
          val destination = new Destination(Files.createDirectory(root.resolve("destination")), database, Some("collection:assets"))
          val source = new Originals(Files.createDirectory(root.resolve("originals")), contribution)
          When("a physical failure leaves the pending native row Materializing")
          val failed = session.materialize(pending.entity.id, destination, source)
          Then("the normal writer reports its exact provider failure")
          failure(failed) shouldBe failure(destination.error)
          val reference = own.snapshot.entity.document.location.urn
          val before = database.snapshot()
          val effects = Vector(own.destination.events -> own.source.calls, hidden.destination.events -> hidden.source.calls,
            destination.events -> source.calls)
          Given("exact foreign Prepared Materializing missing case-changed and space-padded logical references")
          val unavailable = Vector(hidden.snapshot.entity.document.location.urn, prepared.entity.document.location.urn,
            pending.entity.document.location.urn, "urn:textus:klake:native-lake:material:missing", reference.toUpperCase(Locale.ROOT),
            " " + reference, reference + " ")
          unavailable.foreach { selection =>
            When("the owner requests that exact reference through the generated get Operation")
            val start = session.store.searches.size
            val result = session.query("getMaterial", Vector("materialRef" -> selection))
            Then("ordinary native visibility yields no available Material and does not substitute another identity")
            failure(result).observation.taxonomy shouldBe Taxonomy.resourceNotFound
            _expect_reference_directive(session.store.searches(start), selection)
            session.store.searches.size shouldBe start + 1
            database.snapshot() shouldBe before
          }
          When("an anonymous request attempts each public reference Operation")
          val start = session.store.searches.size
          val anonymous = Vector(session.query("searchMaterials", session = None),
            session.query("getMaterial", Vector("materialRef" -> reference), session = None),
            session.query("readOriginal", Vector("materialRef" -> reference, "resourceId" -> "image-1"), session = None))
          Then("authentication fails before native queries or provider access")
          anonymous.foreach(result => failure(result).observation.taxonomy shouldBe Taxonomy.securityAuthenticationRequired)
          session.store.searches.size shouldBe start
          When("the owner selects a known original and a missing or derived resource through the registered read route")
          val known = session.query("readOriginal", Vector("materialRef" -> reference, "resourceId" -> "image-1"))
          val missing = Vector("missing", "capture").map(resource =>
            session.query("readOriginal", Vector("materialRef" -> reference, "resourceId" -> resource)))
          Then("physical integration remains explicitly unavailable and no derived document becomes an original")
          failure(known).observation.taxonomy shouldBe Taxonomy.serviceUnavailable
          failure(known).displayMessage shouldBe "Registered Material original access is unavailable"
          missing.foreach(result => failure(result).observation.taxonomy shouldBe Taxonomy.resourceNotFound)
          database.snapshot() shouldBe before
          Vector(own.destination.events -> own.source.calls, hidden.destination.events -> hidden.source.calls,
            destination.events -> source.calls) shouldBe effects
          Given("the same owned row with native owner read permission explicitly refused")
          database.security(own.snapshot.entity.id, permission = readDeniedPermission)
          val denied = database.snapshot()
          When("the authenticated owner searches and selects the refused registration")
          val page = _public_record(session.query("searchMaterials"))
          val result = session.query("getMaterial", Vector("materialRef" -> reference))
          Then("Owner mode does not grant read access or disclose an available exact reference")
          take(page.getAsC[Vector[Record]]("items")) shouldBe Some(Vector.empty[Record])
          take(page.getAsC[Boolean]("hasMore")) shouldBe Some(false)
          failure(result).observation.taxonomy shouldBe Taxonomy.resourceNotFound
          database.snapshot() shouldBe denied
          Vector(own.destination.events -> own.source.calls, hidden.destination.events -> hidden.source.calls,
            destination.events -> source.calls) shouldBe effects
        } }
      }

      "reject ambiguous logical references arranged by real authorized native updates" in {
        Given("two independently materialized owned registrations and their confirmed physical receipts")
        withDatabase(native_container) { database => withSession(database, _registered_parameters) { session =>
          val first = _seed_registered(session, "first registration")
          val second = _seed_registered(session, "second registration")
          val effects = Vector(first.destination.events -> first.source.calls, second.destination.events -> second.source.calls)
          When("an ordinary observed native update assigns the first complete document to the second issued identity")
          val duplicate = take(session.execute[EntitySnapshot[MaterialRegistrationRecord]]("duplicateReference")(
            _.replaceDocument(second.snapshot, first.snapshot.entity.document)))
          Then("the separate native identity and revision remain real while the selected logical reference is ambiguous")
          duplicate.entity.id shouldBe second.snapshot.entity.id
          duplicate.revision.value shouldBe second.snapshot.revision.value + 1L
          duplicate.entity.document.json shouldBe first.snapshot.entity.document.json
          val before = database.snapshot()
          before.rows should have size 2
          before.rows.map(_("materialRef")) shouldBe Vector.fill(2)(Some(first.snapshot.entity.document.location.urn))
          val start = session.store.searches.size
          When("the generated get Operation resolves the exact duplicated logical reference")
          val result = session.query("getMaterial", Vector("materialRef" -> first.snapshot.entity.document.location.urn))
          Then("a structured ambiguity conflict is returned rather than selecting the first row or consulting a provider")
          failure(result).observation.taxonomy shouldBe Taxonomy(Taxonomy.Category.Operation, Taxonomy.Symptom.Conflict)
          failure(result).observation.cause.descriptor.facets should contain(Descriptor.Facet.Reason("material-reference-ambiguous"))
          _expect_reference_directive(session.store.searches(start), first.snapshot.entity.document.location.urn)
          session.store.searches.size shouldBe start + 1
          database.snapshot() shouldBe before
          Vector(first.destination.events -> first.source.calls, second.destination.events -> second.source.calls) shouldBe effects
        } }
      }

      "reject unknown or padded modes and any registered bootstrap catalog before admitting a session" in {
        Given("a real empty database and exact invalid mode or mixed-catalog component parameters")
        withDatabase(native_container) { database =>
          val catalog = Json.obj("schema" -> Json.fromString("textus.knowledge-lake.material-catalog/1"), "materials" -> Json.arr()).noSpaces
          val variants = Vector("unknown", " registered ", "").map(mode => Map(MaterialReferenceRuntime.modeKey.name -> mode)) ++
            Vector(_registered_parameters + (MaterialReferenceRuntime.catalogKey.name -> catalog))
          val before = database.snapshot()
          variants.foreach { parameters =>
            var entered = false
            When("normal component bootstrap admits the explicit reference configuration")
            val rejected = intercept[ConsequenceException] {
              withSession(database, parameters) { _ => entered = true }
            }
            Then("configuration admission fails without running the body or changing the durable corpus")
            failure(rejected.consequence).observation.taxonomy shouldBe Taxonomy(Taxonomy.Category.Configuration, Taxonomy.Symptom.Invalid)
            entered shouldBe false
            database.snapshot() shouldBe before
          }
        }
      }
    }

    "retain native authorization before provider effects" which {
      "reject unauthenticated foreign-owner read-only and wrong-collection requests without provider calls or domain mutation" in {
        Given("a real Prepared registration, counted provider and ordinary native owner-only permissions")
        withDatabase(native_container) { database => withSession(database) { session =>
          val contribution = _contribution()
          val prepared = take(session.create(contribution))
          val identity = prepared.entity.id
          val destination = new Destination(Files.createDirectory(session.root.resolve("destination")), database)
          val source = new Originals(Files.createDirectory(session.root.resolve("originals")), contribution)
          val issuer = IdGenerationContext.deterministic(IdGenerationContext.DEFAULT_NAMESPACE, "foreign-registration")
          val foreignid = issuer.entityId(EntityCollectionId("textus", "knowledge_lake", "Other"))
          val before = database.snapshot()
          When("missing authentication a foreign provider subject and a wrong native collection reach the protected helpers")
          val unauthenticated = Vector(session.get(identity, None), session.materialize(identity, destination, source, None),
            session.execute[EntitySnapshot[MaterialRegistrationRecord]]("create", None)(_.create(contribution)))
          val foreignresults = Vector(session.get(identity, Some("foreign-session")),
            session.materialize(identity, destination, source, Some("foreign-session")))
          val wrongcollection = Vector(session.get(foreignid), session.materialize(foreignid, destination, source))
          Then("authentication and normal native read authorization reject before changes or external IO")
          unauthenticated.foreach(result => failure(result).observation.taxonomy shouldBe Taxonomy.securityAuthenticationRequired)
          foreignresults.foreach(result => failure(result).observation.taxonomy shouldBe Taxonomy.securityPermissionDenied)
          wrongcollection.foreach(result => failure(result).observation.taxonomy shouldBe Taxonomy.argumentInvalid)
          database.snapshot() shouldBe before
          destination.events shouldBe empty
          source.calls shouldBe empty
          Given("the test row grants the actual owner read-only permission while retaining the full Prepared document")
          database.security(identity, permission = readOnlyPermission)
          val readonly = database.snapshot()
          When("the owner loads and attempts the ordinary observed update")
          val loaded = take(session.get(identity))
          val refused = session.materialize(identity, destination, source)
          Then("native write authorization fails unchanged before the provider while owner read remains available")
          loaded.entity.document.json shouldBe prepared.entity.document.json
          failure(refused).observation.taxonomy shouldBe Taxonomy.securityPermissionDenied
          database.snapshot() shouldBe readonly
          destination.events shouldBe empty
          source.calls shouldBe empty
        } }
      }
    }

    "keep durable state across outer aborts" which {
      "retain an already committed Prepared create after a later native program failure aborts its outer action" in {
        Given("a normal authenticated create program followed by an explicit structured failure")
        withDatabase(native_container) { database => withSession(database) { session =>
          val contribution = _contribution()
          val error = Consequence.operationConflict[EntitySnapshot[MaterialRegistrationRecord]]("nativeFixture", Vector(Descriptor.Facet.Reason("after-create")))
          val beforeabort = session.aborts
          When("the ordinary ActionEngine executes create then fails and performs outer abort")
          val outcome = session.execute("createThenFail")(driver => driver.create(contribution).flatMap(_ => lift(error)))
          val physical = database.snapshot()
          val identity = take(EntityId.parse(physical.rows.head("id").get))
          val loaded = take(session.get(identity))
          Then("the exact failure is returned but one durable native Prepared revision remains authoritative")
          failure(outcome) shouldBe failure(error)
          session.aborts shouldBe beforeabort + 1
          physical.writes shouldBe Vector(identity.value -> "INSERT")
          physical.rows.head("state") shouldBe Some("prepared")
          physical.rows.head(revisionName) shouldBe Some("1")
          loaded.entity.document.contribution.json shouldBe contribution.json
          database.snapshot() shouldBe physical
        } }
      }

      Vector(("collection:assets", false), ("file:material.json", true)).foreach { case (stage, afterwrite) =>
        s"retain Materializing and prevent replay after provider failure at $stage with write-before-error=$afterwrite" in {
          Given("a real Prepared registration and a deterministic physical provider failing at the selected stage")
          withDatabase(native_container) { database =>
            val contribution = _contribution()
            val retained = withSession(database) { session =>
              val prepared = take(session.create(contribution))
              val destination = new Destination(Files.createDirectory(session.root.resolve("destination")), database, Some(stage), afterwrite)
              val source = new Originals(Files.createDirectory(session.root.resolve("originals")), contribution)
              val beforeabort = session.aborts
              When("materialize commits Materializing then the physical provider reports its exact native failure")
              val outcome = session.materialize(prepared.entity.id, destination, source)
              val physical = database.snapshot()
              val calls = destination.events
              val reads = source.calls
              val repeated = session.materialize(prepared.entity.id, destination, source)
              Then("outer abort leaves durable revision two and explicit repeat cannot replay any provider action")
              failure(outcome) shouldBe failure(destination.error)
              session.aborts shouldBe beforeabort + 2
              _materializing(physical, prepared.entity.id)
              calls.last shouldBe stage
              calls.count(_ == stage) shouldBe 1
              _not_prepared(repeated)
              destination.events shouldBe calls
              source.calls shouldBe reads
              destination.observed.foreach(state => state.rows.head("state") shouldBe Some("materializing"))
              if (afterwrite) {
                val manifest = destination.entries.find(_.label == "material.json").get
                val expected = take(MaterialPackage.planC(contribution, prepared.entity.document.location,
                  prepared.entity.document.owner, prepared.entity.document.createdBy)).manifest
                parser.parse(new String(destination.bytes(manifest.reference).toArray, StandardCharsets.UTF_8)).toOption shouldBe Some(expected)
                physical.rows.head("state") should not be Some("registered")
              }
              database.snapshot() shouldBe physical
              (prepared.entity.id, physical)
            }
            Given("a new native runtime has no remembered in-process attempt")
            withSession(database) { reopened =>
              val destination = new Destination(Files.createDirectory(reopened.root.resolve("destination")), database)
              val source = new Originals(Files.createDirectory(reopened.root.resolve("originals")), contribution)
              When("the saved identity is loaded and explicitly materialized again")
              val loaded = take(reopened.get(retained._1))
              val outcome = reopened.materialize(retained._1, destination, source)
              Then("the durable state alone rejects replay with no physical call or native mutation")
              loaded.entity.document.status.value shouldBe "materializing"
              loaded.revision.value shouldBe 2L
              _not_prepared(outcome)
              destination.events shouldBe empty
              source.calls shouldBe empty
              database.snapshot() shouldBe retained._2
            }
          }
        }
      }

      "leave Materializing after a refused final native save without repeating confirmed provider writes" in {
        Given("a real Prepared record and the public SqlDataStore commit hook refusing the final update before commit")
        withDatabase(native_container) { database => withSession(database) { session =>
          val contribution = _contribution()
          val prepared = take(session.create(contribution))
          val destination = new Destination(Files.createDirectory(session.root.resolve("destination")), database)
          val source = new Originals(Files.createDirectory(session.root.resolve("originals")), contribution)
          session.store.failAfter(2, afterCommit = false)
          When("all physical bytes are confirmed but the final native transaction fails")
          val result = session.materialize(prepared.entity.id, destination, source)
          val physical = database.snapshot()
          val events = destination.events
          val repeat = session.materialize(prepared.entity.id, destination, source)
          Then("the exact native failure survives outer abort and committed state never claims Registered")
          failure(result) shouldBe failure(session.store.rejected)
          _materializing(physical, prepared.entity.id)
          destination.entries.map(_.label) should contain("material.json")
          _not_prepared(repeat)
          destination.events shouldBe events
          database.snapshot() shouldBe physical
        } }
      }
    }

    "treat native commit acknowledgement uncertainty as authoritative failure without replay" which {
      Vector(1, 2).foreach { update =>
        s"retain actual durable state after update $update commits and its acknowledgement becomes indeterminate" in {
          Given("a real Prepared record and a public SQL commit hook that actually commits before returning transactionIndeterminate")
          withDatabase(native_container) { database =>
            val contribution = _contribution()
            val retained = withSession(database) { session =>
              val prepared = take(session.create(contribution))
              val destination = new Destination(Files.createDirectory(session.root.resolve("destination")), database)
              val source = new Originals(Files.createDirectory(session.root.resolve("originals")), contribution)
              session.store.failAfter(update, afterCommit = true)
              val beforeabort = session.aborts
              When("materialization reaches the selected committed-but-unacknowledged native update")
              val result = session.materialize(prepared.entity.id, destination, source)
              val physical = database.snapshot()
              val events = destination.events
              val reads = source.calls
              val loaded = take(session.get(prepared.entity.id))
              val repeated = session.materialize(prepared.entity.id, destination, source)
              Then("public failure remains exact while a separate committed SQL connection and native get reveal actual persistence")
              failure(result) shouldBe failure(session.store.unknown)
              session.aborts shouldBe beforeabort + 2
              loaded.entity.document.status.value shouldBe (if (update == 1) "materializing" else "registered")
              loaded.revision.value shouldBe (if (update == 1) 2L else 3L)
              physical.rows.head("state") shouldBe Some(loaded.entity.document.status.value)
              physical.rows.head(revisionName) shouldBe Some(loaded.revision.value.toString)
              physical.writes.map(_._2) shouldBe (if (update == 1) Vector("INSERT", "UPDATE") else Vector("INSERT", "UPDATE", "UPDATE"))
              _not_prepared(repeated)
              destination.events shouldBe events
              source.calls shouldBe reads
              if (update == 1) {
                events shouldBe empty
                reads shouldBe empty
                loaded.entity.document.stored shouldBe None
              } else {
                events.last shouldBe "file:material.json"
                loaded.entity.document.stored should not be empty
                val mapping = loaded.entity.document.stored.get
                parser.parse(new String(destination.bytes(mapping.manifest).toArray, StandardCharsets.UTF_8)).toOption.get
                  .hcursor.get[String]("id").toOption shouldBe Some(loaded.entity.document.location.urn)
              }
              database.snapshot() shouldBe physical
              (prepared.entity.id, loaded.entity.document.json, physical)
            }
            Given("a fully reinitialized native runtime relying on the database rather than the uncertain caller outcome")
            withSession(database) { reopened =>
              val destination = new Destination(Files.createDirectory(reopened.root.resolve("destination")), database)
              val source = new Originals(Files.createDirectory(reopened.root.resolve("originals")), contribution)
              When("the identity is reloaded and an explicit repeated attempt is requested")
              val loaded = take(reopened.get(retained._1))
              val result = reopened.materialize(retained._1, destination, source)
              Then("the real committed status survives and no retry emits a native or provider write")
              loaded.entity.document.json shouldBe retained._2
              _not_prepared(result)
              destination.events shouldBe empty
              source.calls shouldBe empty
              database.snapshot() shouldBe retained._3
            }
          }
        }
      }
    }
  }

  private val _registered_parameters = Map(MaterialReferenceRuntime.modeKey.name -> "registered")
  private def _public_record(result: Consequence[OperationResponse]): Record = take(result) match {
    case OperationResponse.RecordResponse(value) => value
    case _ => fail("The public Material query must return logical metadata")
  }
  private def _expect_reference_directive(captured: (DataStore.CollectionId, org.goldenport.cncf.datastore.QueryDirective),
    reference: String): Unit = {
    captured._1 shouldBe DataStore.CollectionId.EntityStore(MaterialRegistrationEntity.collectionId)
    val directive = captured._2
    directive.query match {
      case StoreQuery.Expr(Query.And(items)) =>
        items should contain(Query.Eq("state", "registered"))
        items should contain(Query.Eq("materialRef", reference))
        items should not contain Query.True
      case _ => fail("The delegated SQL query must retain Registered AND the exact logical reference")
    }
    directive.limit shouldBe QueryLimit.Unbounded
    directive.offset shouldBe 0
    directive.projection shouldBe QueryProjection.All
  }
  private def _contribution(): MaterialContribution = take(MaterialContribution.parseC(MaterialContributionSpec.contributionJson(
    title = " Native SQL literal\u0000雪\uD800 title ", opaque = Json.obj("nested" -> Json.arr(Json.Null, Json.fromString("\u0000\uDC00"))))))
  private def _search_contribution(title: String): MaterialContribution = take(MaterialContribution.parseC(
    MaterialContributionSpec.contributionJson(title = title, opaque = Json.obj("complete" -> Json.arr(
      Json.fromString(title), Json.Null, Json.fromString("opaque\u0000\uDC00"))))))
  private def _seed_registered(session: Session, title: String, sessionid: String = "owner-session"): SearchSeed = {
    Given("a normally prepared search registration and its test-owned physical destination and original sources")
    val contribution = _search_contribution(title)
    val prepared = take(session.execute[EntitySnapshot[MaterialRegistrationRecord]]("create", Some(sessionid))(_.create(contribution)))
    val root = Files.createTempDirectory(session.root, "search-seed-")
    val destination = new Destination(Files.createDirectory(root.resolve("destination")), session.database)
    val source = new Originals(Files.createDirectory(root.resolve("originals")), contribution)
    When("the prepared search registration is normally materialized")
    val registered = take(session.materialize(prepared.entity.id, destination, source, Some(sessionid)))
    Then("the completed seed retains its owner and all original-source and manifest effects")
    registered.entity.document.status.value shouldBe "registered"
    registered.entity.document.owner.value shouldBe (if (sessionid == "owner-session") principal else foreign)
    source.calls shouldBe contribution.originals
    destination.events.last shouldBe "file:material.json"
    And("every physical write observed the complete separately committed Materializing revision two document and INSERT UPDATE audit")
    val materializing = take(prepared.entity.document.beginC)
    destination.observed should not be empty
    destination.observed.foreach { state =>
      val rows = state.rows.filter(row => row("id").contains(prepared.entity.id.value))
      rows should have size 1
      val row = rows.head
      row("state") shouldBe Some("materializing")
      row(revisionName) shouldBe Some("2")
      _document(row) shouldBe materializing.json
      _projections(row, materializing)
      state.writes.filter(_._1 == prepared.entity.id.value).map(_._2) shouldBe Vector("INSERT", "UPDATE")
    }
    // Release only asserted fixture history before later seeds grow the native corpus.
    destination.releaseObservations()
    destination.observed shouldBe empty
    SearchSeed(registered, destination, source)
  }
  private def _expect_page(page: Record, documents: Vector[MaterialRegistrationDocument], subject: String,
    text: Option[String], offset: Int, limit: Int): Unit = {
    val needle = text.getOrElse("").toLowerCase(Locale.ROOT)
    val matches = documents.filter(value => value.status.value == "registered" && value.owner.value == subject)
      .filter(value => value.contribution.title.toLowerCase(Locale.ROOT).contains(needle) ||
        value.location.urn.toLowerCase(Locale.ROOT).contains(needle)).sortBy(_.location.urn)
    val end = offset.toLong + limit
    val expected = matches.slice(offset, end.min(Int.MaxValue).toInt).map(value =>
      Record.data("materialRef" -> value.location.urn, "title" -> value.contribution.title))
    page.fields.map(_.key).toSet shouldBe Set("items", "offset", "limit", "hasMore")
    val items = take(page.getAsC[Vector[Record]]("items")).get
    items.map(_.fields) shouldBe expected.map(_.fields)
    items.foreach(value => value.fields.map(_.key).toSet shouldBe Set("materialRef", "title"))
    take(page.getAsC[Int]("offset")) shouldBe Some(offset)
    take(page.getAsC[Int]("limit")) shouldBe Some(limit)
    take(page.getAsC[Boolean]("hasMore")) shouldBe Some(end < matches.size.toLong)
  }
  private def _delegated_search(session: Session, start: Int, text: Option[String]): Unit = {
    val searches = session.store.searches.drop(start)
    searches should have size 1
    _expect_directive(searches.head, text)
  }
  private def _expect_directive(captured: (DataStore.CollectionId, org.goldenport.cncf.datastore.QueryDirective),
    text: Option[String]): Unit = {
    captured._1 shouldBe DataStore.CollectionId.EntityStore(MaterialRegistrationEntity.collectionId)
    val directive = captured._2
    val or = Query.Or(Vector(DataStoreTextSearchField("titleSearchV1", caseInsensitive = true).contains(text.getOrElse("")),
      DataStoreTextSearchField("referenceSearchV1", caseInsensitive = true).contains(text.getOrElse(""))))
    directive.query match {
      case StoreQuery.Expr(Query.And(items)) =>
        items should contain(Query.Eq("state", "registered"))
        items should contain(or)
        items should not contain Query.True
      case _ => fail("The delegated SQL query must retain Registered AND the paired literal projection predicates")
    }
    directive.limit shouldBe QueryLimit.Unbounded
    directive.offset shouldBe 0
    directive.projection shouldBe QueryProjection.All
  }
  private def _projections(row: Map[String, Option[String]], document: MaterialRegistrationDocument): Unit = {
    row("titleSearchV1") shouldBe Some(DataStoreTextSearchField("titleSearchV1", caseInsensitive = true)
      .toStoreValue(document.contribution.title))
    row("referenceSearchV1") shouldBe Some(DataStoreTextSearchField("referenceSearchV1", caseInsensitive = true)
      .toStoreValue(document.location.urn))
    Vector("document", "titleSearchV1", "referenceSearchV1").foreach(key => row(key).get.forall(_ <= 127) shouldBe true)
    _document(row).hcursor.downField("contribution").focus shouldBe Some(document.contribution.json)
    row.contains("title") shouldBe false
  }
  private def _document(row: Map[String, Option[String]]): Json = parser.parse(row("document").get).toOption.get
  private def _post_status(row: Map[String, Option[String]]): PostStatus =
    take(summon[ValueReader[PostStatus]].readC(row("post_status").get))
  private def _not_prepared(result: Consequence[?]): Unit = {
    failure(result).observation.taxonomy shouldBe Taxonomy(Taxonomy.Category.Operation, Taxonomy.Symptom.Conflict)
    failure(result).observation.cause.descriptor.facets should contain(Descriptor.Facet.Reason("registration-not-prepared"))
  }
  private def _materializing(state: PhysicalState, identity: EntityId): Unit = {
    state.rows should have size 1
    state.rows.head("state") shouldBe Some("materializing")
    state.rows.head(revisionName) shouldBe Some("2")
    state.writes shouldBe Vector(identity.value -> "INSERT", identity.value -> "UPDATE")
    _document(state.rows.head).hcursor.downField("stored").focus shouldBe None
  }
}

private[material] object MaterialRegistrationPersistenceSpec {
  import MaterialRegistrationPersistenceFixture.{Destination, Originals}
  final case class SearchSeed(snapshot: EntitySnapshot[MaterialRegistrationRecord], destination: Destination, source: Originals)
}
