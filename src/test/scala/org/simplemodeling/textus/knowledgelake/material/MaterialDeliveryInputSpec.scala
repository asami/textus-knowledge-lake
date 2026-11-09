package org.simplemodeling.textus.knowledgelake.material

import io.circe.Json
import scala.util.Using
import org.goldenport.bag.{Bag, BinaryBag}
import org.goldenport.datatype.{ContentType, MimeBody}
import org.goldenport.protocol.{Argument, Property, Request, Switch}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.knowledgelake.KnowledgeLakeComponent.MaterialDeliveryService.*

/*
 * Generated input admission only; no deployment, persistence or Google access.
 *
 * @since   Oct. 24, 2026
 * @version Nov.  6, 2026
 */
final class MaterialDeliveryInputSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Generated Material delivery input" should {
    "admit destination discovery without storage settings or arguments" in {
      Given("the generated zero-argument destination query")
      val request = _request("getDestination", Vector.empty)
      When("the generated operation admits normal discovery input")
      val result = GetDestinationOperation.createOperationRequest(request)
      Then("no physical Google destination or credentials are required as arguments")
      result.isSuccess shouldBe true
      result.TAKE.request.properties shouldBe empty
    }

    "retain one structured contribution including explicit JSON null" in {
      Given("a committed request with nested JSON and literal Unicode")
      val contribution = Json.obj("schema" -> Json.fromString("textus.material-contribution/1"),
        "title" -> Json.fromString("窯変源氏物語 7"), "createdAt" -> Json.fromString("2026-10-24T00:00:00Z"),
        "classifications" -> Json.arr(Json.fromString("book")), "provenance" -> Json.obj("context" -> Json.Null),
        "originals" -> Json.arr(), "documents" -> Json.arr(Json.obj("resourceId" -> Json.fromString("book-metadata"),
          "role" -> Json.fromString("book"), "body" -> Json.Null, "derivedFrom" -> Json.arr())))
      val request = _request("prepare", Vector("requestId" -> "book-commit-1", "contribution" -> contribution))
      When("the generated prepare operation admits its named JSON input")
      val result = MaterialDeliveryPrepareCommand.create(request)
      Then("the request identity and complete JSON tree survive without string conversion")
      result.isSuccess shouldBe true
      result.TAKE.requestId shouldBe "book-commit-1"
      result.TAKE.contribution shouldBe contribution
    }

    "admit only the reconciliation identity and optimistic revision" in {
      Given("a delivery reference and current revision")
      val request = _request("reconcile", Vector("deliveryRef" -> "delivery-1", "revision" -> 3L))
      When("the generated command and closed business-field admission inspect the input")
      val command = MaterialDeliveryReconcileCommand.create(request)
      val admitted = MaterialDeliveryInput.recordC(request, Set("deliveryRef", "revision"))
      val extra = _request("reconcile", Vector("deliveryRef" -> "delivery-1", "revision" -> 3L, "proof" -> "absent"))
      Then("no caller-provided observation or provider reference is accepted")
      command.isSuccess shouldBe true
      admitted.isSuccess shouldBe true
      MaterialDeliveryInput.recordC(extra, Set("deliveryRef", "revision")).isSuccess shouldBe false
    }

    "accept an already binary original without interpreting it as text" in {
      Given("an exact logical original selector and non-text binary bytes")
      val bytes = Array[Byte](0, -1, -128, 10, 0)
      val request = _request("uploadOriginal", Vector("deliveryRef" -> "delivery-1", "resourceId" -> "cover",
        "content" -> Bag.binary(bytes)))
      When("the generated upload operation admits a native BinaryBag")
      val result = MaterialDeliveryUploadCommand.create(request)
      Then("the declared input contains the unchanged bytes")
      result.isSuccess shouldBe true
      val retained = result.TAKE.record.getAny("content").get
      retained shouldBe a[BinaryBag]
      Using.resource(retained.asInstanceOf[BinaryBag].openInputStream())(_.readAllBytes().toVector) shouldBe bytes.toVector
    }

    "retain non-HTTP MimeBody and filename property compatibility" in {
      Given("a non-HTTP caller supplying binary content and filename as named properties")
      val bytes = Array[Byte](0, -1, -128, 10, 0)
      val body = MimeBody(ContentType.parse("image/png"), Bag.binary(bytes))
      val request = _request("uploadOriginal", Vector("deliveryRef" -> "delivery-1", "resourceId" -> "cover",
        "content" -> body, "content.filename" -> "cover.png"))
      When("the generated upload operation admits that native multipart value")
      val result = MaterialDeliveryUploadCommand.create(request)
      Then("the binary input reaches the owning operation without a client-specific decoder")
      result.isSuccess shouldBe true
      val retained = MaterialDeliveryOriginalInput.parseC(result.TAKE.request)
      retained.isSuccess shouldBe true
      retained.TAKE.deliveryRef shouldBe "delivery-1"
      retained.TAKE.resourceId shouldBe "cover"
      retained.TAKE.content.contentType shouldBe ContentType.IMAGE_PNG
      Using.resource(retained.TAKE.content.value.openInputStream())(_.readAllBytes().toVector) shouldBe bytes.toVector
    }
    "keep non-HTTP argument and switch admission strict" in {
      Given("a non-HTTP original with valid binary properties and metadata")
      val body = MimeBody(ContentType.IMAGE_PNG, Bag.binary(Array[Byte](0, -1)))
      val request = _request("uploadOriginal", Vector("deliveryRef" -> "delivery-1", "resourceId" -> "cover",
        "content" -> body, "content.filename" -> "cover.png"))
      val variants = Vector(
        request.copy(arguments = List(Argument("content.filename", "cover.png"))),
        request.copy(properties = request.properties.filterNot(_.name == "content.filename"),
          arguments = List(Argument("content.filename", "cover.png"))),
        request.copy(arguments = List(Argument("param1", "cover.png"))),
        request.copy(arguments = List(Argument("unexpected", "extra"))),
        request.copy(switches = List(Switch("extra", true, None))))
      variants.foreach { candidate =>
        When("a non-HTTP caller supplies arguments or switches")
        val result = MaterialDeliveryOriginalInput.parseC(candidate)
        Then("the HTTP metadata exception grants no non-HTTP argument admission")
        result.isSuccess shouldBe false
      }
    }
    "validate optional filename properties at the established length and control boundaries" in {
      Given("non-HTTP original properties with optional untrusted filename metadata")
      val body = MimeBody(ContentType.IMAGE_PNG, Bag.binary(Array[Byte](0, -1)))
      val fields = Vector[(String, Any)]("deliveryRef" -> "delivery-1", "resourceId" -> "cover", "content" -> body)
      val accepted = Vector("x", "x" * 2048, "表紙.png", "../../untrusted-name.png")
      val rejected = Vector[Any]("", "x" * 2049, "nul\u0000.png", "line\nfeed.png", "del\u007f.png", "c1\u009f.png", 42)
      When("the owning decoder checks the exact filename value without interpreting it as a path")
      val valid = accepted.map(value => MaterialDeliveryOriginalInput.parseC(_request("uploadOriginal",
        fields :+ ("content.filename" -> value))))
      val invalid = rejected.map(value => MaterialDeliveryOriginalInput.parseC(_request("uploadOriginal",
        fields :+ ("content.filename" -> value))))
      Then("safe metadata retains spelling and malformed metadata fails admission")
      valid.foreach(_.isSuccess shouldBe true)
      invalid.foreach(_.isSuccess shouldBe false)
    }
    "reject ambiguous or extra multipart fields before resource access" in {
      Given("native multipart content and invalid selector or metadata variants")
      val body = MimeBody(ContentType.IMAGE_PNG, Bag.binary(Array[Byte](0, -1)))
      val fields = Vector[(String, Any)]("deliveryRef" -> "delivery-1", "resourceId" -> "cover", "content" -> body)
      val variants = Vector(fields :+ ("resourceId" -> "other"), fields :+ ("destinationParentKey" -> "other"),
        fields.filterNot(_._1 == "content") :+ ("content" -> "/private/local-photo.png"),
        fields :+ ("content.filename" -> 42),
        fields ++ Vector("content.filename" -> "cover.png", "content.filename" -> "other.png"),
        fields.filterNot(_._1 == "deliveryRef"), fields.filterNot(_._1 == "resourceId"),
        fields :+ ("authorization" -> "untrusted"))
      variants.foreach { values =>
        When("the owning TKL input admits duplicate, unknown or non-binary properties")
        val result = MaterialDeliveryOriginalInput.parseC(_request("uploadOriginal", values))
        Then("admission fails without interpreting a filename or text as original bytes")
        result.isSuccess shouldBe false
      }
    }
  }

  private def _request(operation: String, fields: Vector[(String, Any)]): Request =
    Request.of(component = "KnowledgeLake", service = "MaterialDelivery", operation = operation,
      properties = fields.map { case (name, value) => Property(name, value, None) }.toList)
}
