package org.simplemodeling.textus.knowledgelake.material

import io.circe.Json
import org.goldenport.http.{HttpPath, HttpRequest}
import org.goldenport.protocol.{Property, Request}
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Native HTTP business input admission, independent of transport headers.
  *
  * @since Oct. 25, 2026
  * @version Nov.  6, 2026
  */
final class MaterialDeliveryHttpInputSpec extends AnyWordSpec with Matchers with GivenWhenThen {
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
  private def _request(method: HttpRequest.Method, query: Record, form: Record, properties: List[Property]): Request = {
    val http = HttpRequest(HttpPath.parse("/rest/v1/knowledge-lake/material-delivery/test"), method, query, form,
      Record.data("Authorization" -> "Bearer explicit-test-token"))
    Request.ofHttpRequest(http, "KnowledgeLake", "MaterialDelivery", "test",
      properties = properties :+ Property("authorization", "Bearer explicit-test-token", None))
  }
}
