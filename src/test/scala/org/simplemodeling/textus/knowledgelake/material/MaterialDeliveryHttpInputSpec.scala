package org.simplemodeling.textus.knowledgelake.material

import io.circe.Json
import scala.util.Using
import org.goldenport.bag.Bag
import org.goldenport.datatype.{ContentType, MimeBody}
import org.goldenport.http.{HttpPath, HttpRequest}
import org.goldenport.protocol.{Argument, Property, Request, Switch}
import org.goldenport.protocol.handler.ingress.Ingress
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.simplemodeling.textus.knowledgelake.KnowledgeLakeComponent.MaterialDeliveryService.*
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Native HTTP business input admission, independent of transport headers.
  *
  * @since Oct. 25, 2026
  * @version Nov.  6, 2026
  */
final class MaterialDeliveryHttpInputSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  "Material delivery native HTTP input" should {
    "admit header-authenticated discovery without treating the bearer as business input" in {
      Given("a genuine HTTP source and CNCF's header-derived authorization property")
      val request = _request(HttpRequest.GET, Record.empty, Record.empty, Nil)
      When("the owning operation checks its zero business fields")
      val result = MaterialDeliveryInput.recordC(request, Set.empty)
      Then("the projection is empty and contains no credential")
      result.isSuccess shouldBe true
      result.TAKE.isEmpty shouldBe true
    }
    "retain structured JSON and explicit null while excluding transport headers" in {
      Given("a JSON command and the matching native generated properties")
      val contribution = Json.obj("opaque" -> Json.Null, "arms" -> Json.arr(Json.fromInt(7)))
      val fields = List(Property("requestId", "live-book", None), Property("contribution", contribution, None))
      val body = Record.create(fields.map(property => property.name -> property.value))
      val request = _request(HttpRequest.POST, Record.empty, body, fields)
      When("the command input is projected")
      val result = MaterialDeliveryInput.recordC(request, Set("requestId", "contribution"))
      Then("the exact body survives without Authorization becoming a DTO field")
      result.isSuccess shouldBe true
      result.TAKE shouldBe body
    }
    "reject extra business fields including authorization even when native metadata overwrote them" in {
      Given("an otherwise empty authenticated discovery source")
      for (key <- Vector("unexpected", "authorization", "cookie")) {
        val request = _request(HttpRequest.GET, Record.data(key -> Json.Null), Record.empty, Nil)
        When("the original query contains an extra field")
        val result = MaterialDeliveryInput.recordC(request, Set.empty)
        Then("transport projection cannot hide the extra wire field")
        result.isSuccess shouldBe false
      }
    }
    "admit the public ingress filename argument with exact bytes and MIME" in {
      Given("Http4s multipart form values and the generated upload operation definition")
      forAll(Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString + ".png")) { filename =>
        val bytes = Array[Byte](0, -1, -128, 10, 0)
        val body = MimeBody(ContentType.IMAGE_PNG, Bag.binary(bytes))
        val request = _multipart(_original_fields(body) :+ ("content.filename" -> filename))
        When("public ingress classifies the form and the generated operation retains its Request")
        val generated = UploadOriginalOperation.createOperationRequest(request)
        Then("filename is a named argument and the owning decoder admits the correlated binary")
        request.arguments.map(argument => argument.name -> argument.value) shouldBe List("content.filename" -> filename)
        request.properties.map(_.name) should not contain "content.filename"
        generated.isSuccess shouldBe true
        val admitted = MaterialDeliveryOriginalInput.parseC(generated.TAKE.request)
        admitted.isSuccess shouldBe true
        admitted.TAKE.deliveryRef shouldBe "delivery-1"
        admitted.TAKE.resourceId shouldBe "cover"
        admitted.TAKE.content.contentType shouldBe ContentType.IMAGE_PNG
        Using.resource(admitted.TAKE.content.value.openInputStream())(_.readAllBytes().toVector) shouldBe bytes.toVector
      }
    }
    "admit an original with no optional filename" in {
      Given("a native form whose binary part has no filename metadata")
      val request = _multipart(_original_fields(MimeBody(ContentType.IMAGE_PNG, Bag.binary(Array[Byte](0, -1)))))
      When("the owning operation projects native ingress input")
      val result = MaterialDeliveryOriginalInput.parseC(request)
      Then("the optional metadata does not become a required business field")
      result.isSuccess shouldBe true
    }
    "limit filename argument projection to the optional original POST field" in {
      Given("matching raw filename metadata and declared binary properties")
      val fields = _original_fields(MimeBody(ContentType.IMAGE_PNG, Bag.binary(Array[Byte](0, -1))))
      val request = _multipart(fields :+ ("content.filename" -> "cover.png"))
      val get = _request(HttpRequest.GET, Record.create(fields :+ ("content.filename" -> "cover.png")),
        Record.empty, fields.map { case (name, value) => Property(name, value, None) }.toList)
        .copy(arguments = request.arguments)
      When("an operation omits the optional field or a caller uses GET")
      val unrelated = MaterialDeliveryInput.propertiesC(request, Set("deliveryRef", "resourceId", "content"))
      val wrongmethod = MaterialDeliveryOriginalInput.parseC(get)
      Then("the multipart metadata exception cannot promote other operation inputs")
      unrelated.isSuccess shouldBe false
      wrongmethod.isSuccess shouldBe false
    }
    "reject duplicate, mismatched, positional and unrelated metadata arguments" in {
      Given("a public-ingress Request with one exact filename argument")
      val fields = _original_fields(MimeBody(ContentType.IMAGE_PNG, Bag.binary(Array[Byte](0, -1))))
      val request = _multipart(fields :+ ("content.filename" -> "cover.png"))
      val variants = Vector(
        request.copy(arguments = request.arguments ++ request.arguments),
        request.copy(properties = request.properties :+ Property("content.filename", "cover.png", None)),
        request.copy(arguments = List(Argument("content.filename", "different.png"))),
        request.copy(arguments = List(Argument("param1", "cover.png"))),
        request.copy(arguments = request.arguments :+ Argument("other", "extra")),
        request.copy(switches = List(Switch("extra", true, None))),
        _multipart(fields).copy(arguments = List(Argument("content.filename", "cover.png"))),
        _multipart(fields :+ ("content.filename" -> Vector("cover.png", "other.png"))),
        _multipart(fields :+ ("resourceId" -> "other")),
        _multipart(fields :+ ("content.filename" -> "cover.png")).copy(arguments = Nil),
        _multipart(fields).copy(properties = request.properties :+ Property("content.filename", "cover.png", None)),
        _multipart(fields.filterNot(_._1 == "content")),
        _multipart(fields.filterNot(_._1 == "content") :+ ("content" -> "text")))
      variants.foreach { candidate =>
        When("the owning boundary correlates all original fields and parameter carriers")
        val result = MaterialDeliveryOriginalInput.parseC(candidate)
        Then("only unique and identical native filename metadata is admitted")
        result.isSuccess shouldBe false
      }
    }
    "reject every C0 and C1 control in native filename arguments" in {
      Given("a native binary form with a filename containing an ISO control character")
      val fields = _original_fields(MimeBody(ContentType.IMAGE_PNG, Bag.binary(Array[Byte](0, -1))))
      forAll(Gen.oneOf((0 to 31) ++ (127 to 159))) { code =>
        val request = _multipart(fields :+ ("content.filename" -> ("cover" + code.toChar + ".png")))
        When("the owning decoder checks the correlated filename")
        val result = MaterialDeliveryOriginalInput.parseC(request)
        Then("control characters remain invalid after native argument projection")
        result.isSuccess shouldBe false
      }
    }
    "reject malformed filename values and body or query authorization injection" in {
      Given("a binary original with malformed metadata or extra wire fields")
      val fields = _original_fields(MimeBody(ContentType.IMAGE_PNG, Bag.binary(Array[Byte](0, -1))))
      val malformed = Vector[Any]("", "x" * 2049, "line\nfeed.png", "nul\u0000.png", "c1\u0085.png", 42)
      val injected = Vector("authorization", "Authorization", "cookie", "unexpected").map { key =>
        _multipart(fields :+ (key -> "untrusted"))
      }
      val query = _multipart(fields, Record.data("authorization" -> "untrusted"))
      val variants = malformed.map(value => _multipart(fields :+ ("content.filename" -> value))) ++ injected :+ query
      variants.foreach { candidate =>
        When("the original form and exact metadata value are admitted")
        val result = MaterialDeliveryOriginalInput.parseC(candidate)
        Then("invalid metadata and extra authorization fields cannot hide behind transport properties")
        result.isSuccess shouldBe false
      }
    }
    "reject a mismatch between wire and generated input and retain strict non-HTTP admission" in {
      Given("a source query that differs from the generated operation property")
      val request = _request(HttpRequest.GET, Record.data("requestId" -> "source-book"), Record.empty,
        List(Property("requestId", "different-book", None)))
      When("the query input is admitted")
      val mismatch = MaterialDeliveryInput.recordC(request, Set("requestId"))
      val untrusted = MaterialDeliveryInput.recordC(Request.ofOperation("getDestination")
        .copy(properties = List(Property("authorization", "not-http-metadata", None))), Set.empty)
      Then("neither inconsistency nor a CLI credential-looking business field is ignored")
      mismatch.isSuccess shouldBe false
      untrusted.isSuccess shouldBe false
    }
  }
  private def _original_fields(body: MimeBody): Vector[(String, Any)] =
    Vector("deliveryRef" -> "delivery-1", "resourceId" -> "cover", "content" -> body)

  private def _multipart(fields: Vector[(String, Any)], query: Record = Record.empty): Request = {
    val form = Record.create(fields)
    val http = HttpRequest(HttpPath.parse("/rest/v1/knowledge-lake/material-delivery/uploadOriginal"),
      HttpRequest.POST, query, form, Record.data("Authorization" -> "Bearer explicit-test-token"))
    val (arguments, switches, properties) = Ingress.recordToParameters(UploadOriginalOperation, form)
    Request.ofHttpRequest(http, "KnowledgeLake", "MaterialDelivery", "uploadOriginal",
      arguments = arguments, switches = switches,
      properties = properties :+ Property("authorization", "Bearer explicit-test-token", None))
  }

  private def _request(method: HttpRequest.Method, query: Record, form: Record, properties: List[Property]): Request = {
    val http = HttpRequest(HttpPath.parse("/rest/v1/knowledge-lake/material-delivery/test"), method, query, form,
      Record.data("Authorization" -> "Bearer explicit-test-token"))
    Request.ofHttpRequest(http, "KnowledgeLake", "MaterialDelivery", "test",
      properties = properties :+ Property("authorization", "Bearer explicit-test-token", None))
  }
}
