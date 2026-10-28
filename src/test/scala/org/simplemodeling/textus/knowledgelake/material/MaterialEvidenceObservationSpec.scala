package org.simplemodeling.textus.knowledgelake.material

import java.io.{ByteArrayInputStream, IOException, InputStream, OutputStream}
import cats.syntax.all.*
import org.goldenport.{Consequence, ConsequenceT}
import org.goldenport.bag.{Bag, BagBackend, BagMetadata, BinaryBag}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.filestore.*
import org.goldenport.cncf.unitofwork.{ExecUowM, UnitOfWork, UnitOfWorkInterpreter}
import org.goldenport.datatype.MimeType
import org.goldenport.observation.Taxonomy
import org.goldenport.record.Record
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Complete byte consumption and honest source-version observations, without cloud IO.
  * @since Oct. 28, 2026
  * @version Oct. 28, 2026
  */
final class MaterialEvidenceObservationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import MaterialEvidenceObservationSpec.*
  import MaterialReferenceFixture.failure

  "Explicit Material evidence selection" should {
    "preserve empty and ordered exact selections but reject duplicates and invalid bounds" in {
      Given("opaque resource identities and invalid collections")
      val valid = Vector(Vector.empty, Vector("原本:α", "image/1"))
      val invalid = Vector(Vector("same", "same"), Vector(""), Vector("bad\nresource"),
        Vector("x" * 257), (0 to 128).map(index => s"resource-$index").toVector)
      When("TKL admits the caller's explicit logical selection")
      val accepted = valid.map(MaterialEvidenceObservation.selectionC)
      val rejected = invalid.map(MaterialEvidenceObservation.selectionC)
      Then("valid spelling and order remain intact without normalization")
      accepted.map(_.TAKE) shouldBe valid
      rejected.foreach(value => failure(value).observation.taxonomy shouldBe Taxonomy.argumentInvalid)
    }
  }

  "Resource-scoped version observation" should {
    "attest only a complete read surrounded by equal known source versions" in {
      Given("materialized nonUTF8 original bytes and an opaque source version")
      val store = new Store()
      When("the native UOW interprets the source observation")
      val result = evaluate(store).TAKE
      Then("the exact logical pair and version are returned after stream consumption and close")
      result.getString("materialRef") shouldBe Some(reference)
      result.getString("resourceId") shouldBe Some(resource.resourceid)
      result.getString("revision") shouldBe Some("source-17")
      result.getString("retrievalState") shouldBe Some("available")
      result.getString("limitation").get should include("no version-pinned historical access or retention")
      result.fields.map(_.key).toSet shouldBe Set("evidenceRef", "materialRef", "resourceId", "revision", "retrievalState", "limitation")
      result.getString("evidenceRef").get should fullyMatch regex "urn:textus:klake:evidence:[0-9a-f]{64}"
      store.events shouldBe Vector("stat", "read", "consume", "close", "stat")
      store.bag.opens shouldBe 1
      store.bag.closes shouldBe 1
      Vector(resource.storage.resourcekey, resource.storage.parentkey, resource.storage.connectionid,
        store.name, "private-stream-error").foreach(marker => result.print should not include marker)
    }

    "preserve generated binary content and stable resource-incarnation identities" in {
      Given("arbitrary complete binary originals and distinct exact resource selectors")
      val samples = for {
        bytes <- Gen.listOf(Gen.choose(-128, 127)).map(_.take(300).map(_.toByte).toVector)
        leaf <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.take(30).mkString)
      } yield bytes -> leaf
      When("generated observations read complete bytes and project their logical pair")
      val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), Prop.forAll(samples) { case (bytes, leaf) =>
        val selected = resource.copy(resourceid = "原本:/?#%:" + leaf, bytelength = Some(bytes.size.toLong))
        val material = reference + ":?#/" + leaf
        val store = new Store(selected = selected, bytes = bytes)
        val first = evaluate(store, material, selected).toOption
        val repeated = evaluate(new Store(selected = selected, bytes = bytes), material, selected).toOption
        val rebound = selected.copy(storage = selected.storage.copy(resourcekey = selected.storage.resourcekey + "-other"))
        val other = evaluate(new Store(selected = rebound, bytes = bytes), material, rebound).toOption
        first.exists { result => repeated.exists { repeat => other.exists { changed =>
          result.getString("materialRef").contains(material) && result.getString("resourceId").contains(selected.resourceid) &&
            result.getString("evidenceRef") == repeat.getString("evidenceRef") &&
            result.getString("evidenceRef") != changed.getString("evidenceRef") &&
            Vector(result, repeat, changed).forall(_.getString("revision").contains("source-17")) &&
            store.bag.count == bytes.size.toLong && store.events == Vector("stat", "read", "consume", "close", "stat")
        } } }
      })
      Then("identical bindings are stable while remapped resources retain distinct identities even at the same source version")
      checked.passed shouldBe true
      checked.succeeded should be >= 100
      checked.discarded shouldBe 0
    }

    "keep retained Unicode and every binding identity component distinct without using a content hash as revision" in {
      Given("the same logical selection with individually changed connection parent or resource binding")
      val variants = Vector(resource,
        resource.copy(storage = resource.storage.copy(connectionid = "private-other-connection")),
        resource.copy(storage = resource.storage.copy(parentkey = "private-other-parent")),
        resource.copy(storage = resource.storage.copy(resourcekey = "private-other-original")))
      When("each binding supplies the same exact source version and bytes")
      val observed = variants.map(selected => evaluate(new Store(selected = selected), selected = selected).TAKE)
      val unicode = Vector("原本", "原本:" + '\uD800', "原本:" + '\uD801', "原本:?")
        .map(id => resource.copy(resourceid = id))
        .map(selected => evaluate(new Store(selected = selected), selected = selected).TAKE)
      Then("the opaque evidence identity separates incarnations while the unchanged provider version remains exact")
      observed.map(_.getString("evidenceRef").get).distinct should have size variants.size
      unicode.map(_.getString("evidenceRef").get).distinct should have size 4
      (observed ++ unicode).foreach(_.getString("revision") shouldBe Some("source-17"))
    }

    Vector((None, None), (Some("source-17"), None), (None, Some("source-17"))).foreach { case (before, after) =>
      s"retain unverified state when source versions are $before then $after" in {
        Given("a readable original with at least one missing source version")
        val store = new Store()
        store.before = store.before.copy(version = before)
        store.after = store.after.copy(version = after)
        When("the original is consumed completely and checked again")
        val result = evaluate(store).TAKE
        Then("no Candidate/registration revision hash timestamp or snapshot fills the gap")
        result.getString("retrievalState") shouldBe Some("unverified")
        result.getString("revision") shouldBe None
        result.getString("snapshotRef") shouldBe None
        result.getString("limitation").get should include("unchanged source version could not be established")
        store.events shouldBe Vector("stat", "read", "consume", "close", "stat")
      }
    }

    "reject a source that changed during the completed read without returning old readiness" in {
      Given("equal sized bytes whose source version changes while being read")
      val store = new Store()
      store.after = store.after.copy(version = Some("source-18"))
      When("TKL confirms the post-read source")
      val result = evaluate(store)
      Then("the observation fails with no usable partial version or automatic reread")
      failure(result).observation.taxonomy shouldBe Taxonomy.resourceInvalid
      store.events shouldBe Vector("stat", "read", "consume", "close", "stat")
    }

    Vector("reference", "parent", "media", "kind", "declared-length", "invalid-version").foreach { defect =>
      Vector(false, true).foreach { postread =>
        s"reject $defect at ${if (postread) "post-read" else "pre-read"} confirmation" in {
          Given("an original whose physical resource no longer matches the admitted logical binding")
          val store = new Store()
          val broken = defect match {
            case "reference" => store.before.copy(reference = FileStoreRef(store.name, "private-other"))
            case "parent" => store.before.copy(parents = Vector(FileStoreRef(store.name, "private-other")))
            case "media" => store.before.copy(mediaType = MimeType("text/plain"))
            case "kind" => store.before.copy(kind = FileStoreKind.Collection)
            case "declared-length" => store.before.copy(byteLength = Some(9L))
            case "invalid-version" => store.before.copy(version = Some("bad\nversion"))
          }
          if (postread) store.after = broken else store.before = broken
          When("the source observation reaches the changed binding")
          val result = evaluate(store)
          Then("a fixed resource failure prevents stale evidence without fallback or retries")
          failure(result).observation.taxonomy shouldBe Taxonomy.resourceInvalid
          store.events shouldBe (if (postread) Vector("stat", "read", "consume", "close", "stat") else Vector("stat"))
          result.display should not include "private-other"
        }
      }
    }

    "compare actual byte count with both stat sizes even when no length is declared" in {
      Given("an unknown declared length and two equal source versions with inconsistent observed sizes")
      val selected = resource.copy(bytelength = None)
      val store = new Store(selected = selected)
      store.after = store.after.copy(byteLength = Some(9L))
      When("all original bytes have been consumed")
      val result = evaluate(store, selected = selected)
      Then("metadata alone cannot claim a confirmed original version")
      failure(result).observation.taxonomy shouldBe Taxonomy.resourceInvalid
      store.bag.count shouldBe 4L
    }

    Vector("nonbinary", "url", "bag-length", "content-media").foreach { defect =>
      s"withhold evidence for $defect content without a second network route" in {
        Given("an admitted source whose materialized content does not satisfy the original contract")
        val store = new Store()
        val bytes = Array[Byte](0, -1, -128, 42)
        val content = defect match {
          case "nonbinary" => Bag.fromBytes(bytes)
          case "url" => BinaryBag.Instance(Bag.Instance(BagBackend.Url(java.net.URI.create("https://example.invalid/private-stream-error").toURL)))
          case "bag-length" => BinaryBag.Instance(Bag.Instance(BagBackend.InMemory(bytes), BagMetadata(size = Some(9L))))
          case _ => store.bag
        }
        store.content = FileStoreContent(MimeType(if (defect == "content-media") "text/plain" else "image/png"), content)
        When("TKL attempts the read observation under the admitted FileStore only")
        val result = evaluate(store)
        Then("no version is returned or post-read stat attempted and URL bytes are never opened")
        failure(result).observation.taxonomy shouldBe Taxonomy.resourceInvalid
        store.events.count(_ == "stat") shouldBe 1
        result.display should not include "private-stream-error"
      }
    }

    Vector("read", "close").foreach { defect =>
      s"close the original and withhold observation after a stream $defect failure" in {
        Given("a materialized source Bag with a controlled stream failure")
        val store = new Store(failRead = defect == "read", failClose = defect == "close")
        When("TKL attempts the complete original read")
        val result = evaluate(store)
        Then("the stream closes exactly once and its private error text stays inside the boundary")
        failure(result).observation.taxonomy shouldBe Taxonomy.resourceInvalid
        store.bag.opens shouldBe 1
        store.bag.closes shouldBe 1
        store.events.count(_ == "stat") shouldBe 1
        result.display should not include "private-stream-error"
      }
    }

    "propagate native provider failures without retry or false unverified success" in {
      Given("a provider refusing the actual original read after successful metadata admission")
      val store = new Store()
      store.readfailure = Some(Consequence.securityPermissionDenied("Controlled provider refusal"))
      When("the native program attempts original access")
      val result = evaluate(store)
      Then("the exact structured provider Conclusion remains the failed outcome")
      failure(result) shouldBe failure(store.readfailure.get)
      store.events shouldBe Vector("stat", "read")
      store.bag.opens shouldBe 0
    }
  }
}

private[material] object MaterialEvidenceObservationSpec {
  val reference = "urn:textus:klake:fixture:material:book-1"
  val resource = MaterialResourceBinding("cover", "image/png", Some(4L),
    MaterialStorage("private-connection", "private-original", "private-parent"))

  def evaluate(store: Store, materialRef: String = reference,
      selected: MaterialResourceBinding = resource): Consequence[Record] =
    new UnitOfWorkInterpreter(new UnitOfWork(ExecutionContext.create()))
      .run(MaterialEvidenceObservation.observe(materialRef, selected, store))

  final class TrackedBag(bytes: Vector[Byte], event: String => Unit, failRead: Boolean, failClose: Boolean) extends BinaryBag {
    val bag: Bag = Bag.binary(bytes.toArray)
    private var _opens = 0
    private var _closes = 0
    private var _count = 0L
    def opens: Int = _opens
    def closes: Int = _closes
    def count: Long = _count
    override def openInputStream(): InputStream = {
      _opens += 1
      new ByteArrayInputStream(bytes.toArray) {
        override def transferTo(output: OutputStream): Long = {
          event("consume")
          if (failRead) throw new IOException("private-stream-error")
          val count = super.transferTo(output)
          _count = count
          count
        }
        override def close(): Unit = {
          event("close")
          _closes += 1
          if (failClose) throw new IOException("private-stream-error")
          super.close()
        }
      }
    }
  }
  final class Store(selected: MaterialResourceBinding = resource, bytes: Vector[Byte] = Vector(0, -1, -128, 42),
      failRead: Boolean = false, failClose: Boolean = false) extends FileStore {
    val name = "private-store"
    private var _events = Vector.empty[String]
    def events: Vector[String] = _events
    val bag = new TrackedBag(bytes, event => _events = _events :+ event, failRead, failClose)
    var content = FileStoreContent(MimeType(selected.mediatype), bag)
    var before = FileStoreMetadata(FileStoreRef(name, selected.storage.resourcekey), "private-provider-name",
      MimeType(selected.mediatype), Some(bytes.size.toLong), Vector(FileStoreRef(name, selected.storage.parentkey)),
      FileStoreKind.File, Some("source-17"))
    var after = before
    var readfailure: Option[Consequence[FileStoreContent]] = None
    def stat(reference: FileStoreRef): ExecUowM[FileStoreMetadata] = {
      val metadata = if (_events.contains("stat")) after else before
      _events = _events :+ "stat"
      ConsequenceT.fromConsequence(Consequence.success(metadata))
    }
    def read(reference: FileStoreRef): ExecUowM[FileStoreContent] = {
      _events = _events :+ "read"
      ConsequenceT.fromConsequence(readfailure.getOrElse(Consequence.success(content)))
    }
    def createCollection(parent: FileStoreRef, name: String): ExecUowM[FileStoreMetadata] =
      ConsequenceT.fromConsequence(Consequence.resourceInvalid("Read observation must not create"))
    def create(parent: FileStoreRef, name: String, mediaType: MimeType, content: Bag): ExecUowM[FileStoreMetadata] =
      ConsequenceT.fromConsequence(Consequence.resourceInvalid("Read observation must not create"))
  }
}
