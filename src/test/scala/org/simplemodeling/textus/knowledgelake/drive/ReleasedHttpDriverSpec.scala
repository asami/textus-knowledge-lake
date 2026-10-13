package org.simplemodeling.textus.knowledgelake.drive

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import org.goldenport.cncf.http.UrlConnectionHttpDriver
import org.goldenport.protocol.Property
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

/** Characterizes released CNCF/core HTTP transport on controlled loopback fixtures.
 * Status collapse, narrow success classification and replacement decoding are
 * limitations, not evidence of Google authentication, CAS, storage or native
 * integration fitness.
 * @since Oct. 3, 2026
 * @version Oct. 3, 2026
 * @author ASAMI, Tomoharu
 */
class ReleasedHttpDriverSpec extends AnyWordSpec with GivenWhenThen with Matchers:
  "Released GET responses" should {
    "HTTP-B1-01 preserve Unicode, line endings, query and headers" in {
      Given("a UTF-8 loopback export with Japanese, emoji, LF, CRLF, blank lines and a trailing newline")
      val text = "日本語 😀\nLF\r\nCRLF\n\n末尾\n"
      _with_server { fixture =>
        fixture.addRoute("/export") { exchange =>
          _respond(exchange, 200, _utf8(text), "text/plain;charset=UTF-8", Map("X-MiXeD-Probe" -> "retained"))
        }
        When("the released driver GETs the exact export query with a fake probe header")
        val response = fixture.driver.get("/export?alt=media", Map("X-TKL-Probe" -> "loopback-only"), _controls())
        Then("the exact text, status, mixed-case header and observed request survive")
        response.code shouldBe 200
        response.getString shouldBe Some(text)
        response.headerValue("x-MIXED-probe") shouldBe Some("retained")
        fixture.requests.map(_.rawUri) shouldBe Vector("/export?alt=media")
        fixture.requests.head.method shouldBe "GET"
        fixture.requests.head.headers.get("x-tkl-probe") shouldBe Some(Vector("loopback-only"))
      }
    }

    "HTTP-B1-02 distinguish a successful empty text from a failed read at a zero byte bound" in {
      Given("an HTTP 200 text response with zero bytes and a zero response byte limit")
      _with_server { fixture =>
        fixture.addRoute("/empty") { exchange => _respond(exchange, 200, Array.emptyByteArray) }
        When("the released driver GETs the empty response")
        val response = fixture.driver.get("/empty", Map.empty, _controls(0))
        Then("the response is 200 with a defined empty string and no exception")
        response.code shouldBe 200
        response.getString shouldBe Some("")
      }
    }

    "HTTP-B1-03 retain octet-stream bytes without text decoding" in {
      Given("an octet-stream containing NUL, non-ASCII bytes and malformed UTF-8")
      val bytes = Array[Byte](0, 0x7f, 0x80.toByte, 0xc3.toByte, 0x28, 0xff.toByte)
      _with_server { fixture =>
        fixture.addRoute("/binary") { exchange => _respond(exchange, 200, bytes, "application/octet-stream") }
        When("the released driver GETs the response and its Bag input stream is read")
        val response = fixture.driver.get("/binary", Map.empty, _controls())
        val stream = response.bag.openInputStream()
        val actual = try stream.readAllBytes() finally stream.close()
        Then("the bytes are exact, getBinary is defined and getString is absent")
        actual.toVector shouldBe bytes.toVector
        response.getBinary.isDefined shouldBe true
        response.getString shouldBe None
      }
    }
  }

  "Status and failure boundaries" should {
    Vector(401, 403, 404).foreach { status =>
      s"HTTP-B1-04 retain recognized $status and its Unicode error body" in {
        Given(s"a controlled $status response with a nonempty Unicode body and Retry-After")
        val text = s"認識されたエラー $status 😀\n"
        _with_server { fixture =>
          fixture.addRoute("/recognized") { exchange =>
            _respond(exchange, status, _utf8(text), headers = Map("Retry-After" -> "7"))
          }
          When("the released driver GETs this controlled error")
          val response = fixture.driver.get("/recognized", Map.empty, _controls())
          Then("status, exact body and case-insensitive Retry-After remain, with isSuccess false")
          response.code shouldBe status
          response.getString shouldBe Some(text)
          response.headerValue("rEtRy-AfTeR") shouldBe Some("7")
          response.isSuccess shouldBe false
        }
      }
    }

    Vector(409, 429, 503).foreach { status =>
      s"HTTP-B1-05 characterize $status collapsing to 500 while retaining its body" in {
        Given(s"an unrecognized $status response with a distinguishing nonempty body")
        val text = s"失われるステータス $status\n"
        _with_server { fixture =>
          fixture.addRoute("/unrecognized") { exchange => _respond(exchange, status, _utf8(text)) }
          When("the released driver GETs the unrecognized status")
          val response = fixture.driver.get("/unrecognized", Map.empty, _controls())
          Then("status information is lost to 500, the body remains and no conflict/retry capability is established")
          response.code shouldBe 500
          response.getString shouldBe Some(text)
          response.isSuccess shouldBe false
        }
      }
    }

    "HTTP-B1-06 characterize a bodyless 204 collapsing to 500" in {
      Given("a controlled HTTP 204 without a response body")
      _with_server { fixture =>
        fixture.addRoute("/no-content") { exchange => _respond(exchange, 204, Array.emptyByteArray) }
        When("the released driver GETs the bodyless response")
        val response = fixture.driver.get("/no-content", Map.empty, _controls())
        Then("204 is lost to 500, without establishing TKL acceptance")
        response.code shouldBe 500
      }
    }
  }

  "Outgoing requests and controls" should {
    "HTTP-B1-07 send exact UTF-8 POST bytes and map headers with narrow 201 success classification" in {
      Given("a loopback capture route returning 201 and an exact Japanese JSON request with fake map headers")
      val body = "{\"title\":\"日本語 😀\",\"text\":\"行\\n次\"}"
      val text = "作成された応答\n"
      val headers = Map("Content-Type" -> "application/json;charset=UTF-8", "Authorization" -> "Bearer tkl-test-only")
      _with_server { fixture =>
        fixture.addRoute("/capture") { exchange => _respond(exchange, 201, _utf8(text)) }
        When("the released driver POSTs Some(body) to the relative URL with bounded framework Property controls")
        val response = fixture.driver.post("/capture?mode=probe", Some(body), headers, _controls())
        Then("the server receives exact POST bytes and map headers, while 201 is retained but isSuccess is false")
        fixture.requests.map(_.method) shouldBe Vector("POST")
        fixture.requests.head.rawUri shouldBe "/capture?mode=probe"
        fixture.requests.head.body shouldBe _utf8(body).toVector
        fixture.requests.head.headers.get("content-type") shouldBe Some(Vector("application/json;charset=UTF-8"))
        fixture.requests.head.headers.get("authorization") shouldBe Some(Vector("Bearer tkl-test-only"))
        response.code shouldBe 201
        response.isSuccess shouldBe false
        response.getString shouldBe Some(text)
      }
    }

    "HTTP-B1-10 retain 303 and Location without following or forwarding fake authorization" in {
      Given("a 303 to a second loopback route, fake Authorization and a redirected-route hit counter")
      val hits = new AtomicInteger(0)
      _with_server { fixture =>
        val location = s"${fixture.baseUrl}/redirected"
        fixture.addRoute("/redirect") { exchange =>
          _respond(exchange, 303, _utf8("移動\n"), headers = Map("Location" -> location))
        }
        fixture.addRoute("/redirected") { exchange =>
          hits.incrementAndGet()
          _respond(exchange, 200, _utf8("unexpected destination"))
        }
        When("the released driver GETs with automatic redirect following disabled")
        val response = fixture.driver.get("/redirect", Map("Authorization" -> "Bearer tkl-test-only"), _controls())
        Then("303 and Location remain and the destination receives no request or forwarded credential")
        response.code shouldBe 303
        response.headerValue("lOcAtIoN") shouldBe Some(location)
        hits.get() shouldBe 0
        fixture.requests.map(_.rawUri) shouldBe Vector("/redirect")
        fixture.requests.head.headers.get("authorization") shouldBe Some(Vector("Bearer tkl-test-only"))
      }
    }

    "HTTP-B1-11 reject numeric loopback under public-network-only before any request" in {
      Given("a numeric 127.0.0.1 loopback server and public-network-only enabled")
      _with_server { fixture =>
        fixture.addRoute("/policy") { exchange => _respond(exchange, 200, _utf8("unexpected request")) }
        When("the released driver attempts a GET under the public-network policy")
        val failure = intercept[IOException] {
          fixture.driver.get("/policy", Map.empty, _controls(publiconly = true))
        }
        Then("the policy exception occurs before the server observes a request")
        failure.getMessage should include("public-network policy")
        fixture.requests shouldBe empty
      }
    }
  }

  "Bounded response reads" should {
    "HTTP-B1-08 accept multibyte UTF-8 at the exact encoded byte bound" in {
      Given("multibyte UTF-8 text and a byte limit equal to its encoded length")
      val text = "日本語 😀\n"
      val bytes = _utf8(text)
      _with_server { fixture =>
        fixture.addRoute("/bounded") { exchange => _respond(exchange, 200, bytes) }
        When("the released driver GETs at the exact byte bound")
        val response = fixture.driver.get("/bounded", Map.empty, _controls(bytes.length))
        Then("the complete exact text succeeds at equality")
        response.code shouldBe 200
        response.getString shouldBe Some(text)
      }
    }

    "HTTP-B1-09 reject multibyte UTF-8 one byte beyond the configured limit" in {
      Given("the same multibyte UTF-8 text with a byte limit one byte below its encoded length")
      val text = "日本語 😀\n"
      val bytes = _utf8(text)
      _with_server { fixture =>
        fixture.addRoute("/bounded") { exchange => _respond(exchange, 200, bytes) }
        When("the released driver GETs above the configured byte limit")
        val failure = intercept[IOException] {
          fixture.driver.get("/bounded", Map.empty, _controls(bytes.length - 1))
        }
        Then("an explicit byte-limit exception replaces any partial or empty successful response")
        failure.getMessage should include("configured byte limit")
      }
    }
  }

  "Decoding and incomplete responses" should {
    "HTTP-B1-12 characterize malformed UTF-8 replacement without strict validation" in {
      Given("text/plain;charset=UTF-8 bytes C3 28, which are malformed UTF-8")
      val bytes = Array[Byte](0xc3.toByte, 0x28)
      _with_server { fixture =>
        fixture.addRoute("/malformed") { exchange => _respond(exchange, 200, bytes) }
        When("the released driver GETs through its text decoding path")
        val response = fixture.driver.get("/malformed", Map.empty, _controls())
        Then("decoding replaces the malformed byte with U+FFFD and retains the opening parenthesis")
        response.getString shouldBe Some("\ufffd(")
      }
    }

    "HTTP-B1-13 throw on every incomplete response without claiming a physical retry count" in {
      Given("a loopback handler that closes every exchange without sending response headers")
      val hits = new AtomicInteger(0)
      _with_server { fixture =>
        fixture.addRoute("/incomplete") { exchange =>
          hits.incrementAndGet()
          exchange.close()
        }
        When("the released driver GETs the incomplete response")
        val failure = intercept[IOException] {
          fixture.driver.get("/incomplete", Map.empty, _controls())
        }
        Then("transport failure is an exception, never a successful empty response or evidence of provider state")
        failure shouldBe a[IOException]
        hits.get() should be > 0
      }
    }
  }

  private def _utf8(text: String): Array[Byte] = text.getBytes(StandardCharsets.UTF_8)

  // Properties control the framework; outbound request headers use the driver's Map.
  private def _controls(maxbytes: Long = 8192L, publiconly: Boolean = false): Vector[Property] = Vector(
    Property("http.connect-timeout-ms", "1000", None),
    Property("http.read-timeout-ms", "1000", None),
    Property("http.max-response-bytes", maxbytes.toString, None),
    Property("http.follow-redirects", "false", None),
    Property("http.public-network-only", publiconly.toString, None)
  )

  private def _respond(
    exchange: HttpExchange,
    status: Int,
    bytes: Array[Byte],
    contenttype: String = "text/plain;charset=UTF-8",
    headers: Map[String, String] = Map.empty
  ): Unit =
    exchange.getResponseHeaders.set("Content-Type", contenttype)
    headers.foreach { (name, value) => exchange.getResponseHeaders.set(name, value) }
    exchange.sendResponseHeaders(status, if bytes.isEmpty then -1L else bytes.length.toLong)
    val stream = exchange.getResponseBody
    try
      if bytes.nonEmpty then stream.write(bytes)
    finally stream.close()

  private def _with_server(action: LoopbackFixture => Unit): Unit =
    val fixture = new LoopbackFixture
    var primary: Option[Throwable] = None
    try
      fixture.start()
      action(fixture)
    catch
      case NonFatal(failure) =>
        primary = Some(failure)
        throw failure
    finally
      fixture.stop()
      fixture.checkFailure(primary)

  /** Bounded request observation without logging fake authorization or payloads.
   * @since Oct. 3, 2026
   * @version Oct. 3, 2026
   * @author ASAMI, Tomoharu
   */
  private case class ObservedRequest(method: String, rawUri: String, headers: Map[String, Vector[String]], body: Vector[Byte])

  /** Default-executor ephemeral loopback fixture for released transport limitations.
   * Callback failures propagate to the test after deterministic stop; this helper
   * cannot establish Google authentication, CAS, storage or native integration.
   * @since Oct. 3, 2026
   * @version Oct. 3, 2026
   * @author ASAMI, Tomoharu
   */
  private class LoopbackFixture:
    private val _server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
    private val _requests = new ConcurrentLinkedQueue[ObservedRequest]()
    private val _failure = new AtomicReference[Throwable]()
    val baseUrl: String = s"http://127.0.0.1:${_server.getAddress.getPort}"
    val driver = new UrlConnectionHttpDriver(baseUrl)

    def requests: Vector[ObservedRequest] = _requests.iterator().asScala.toVector
    def start(): Unit = _server.start()
    def stop(): Unit = _server.stop(0)

    def addRoute(path: String)(handler: HttpExchange => Unit): Unit =
      _server.createContext(path, (exchange: HttpExchange) =>
        try
          val stream = exchange.getRequestBody
          val bytes = try stream.readNBytes(8193) finally stream.close()
          if bytes.length > 8192 then throw new IOException("Loopback request exceeds fixture byte bound")
          val headers = exchange.getRequestHeaders.asScala.iterator.map { (name, values) =>
            name.toLowerCase(Locale.ROOT) -> values.asScala.toVector
          }.toMap
          _requests.add(ObservedRequest(exchange.getRequestMethod, exchange.getRequestURI.toASCIIString, headers, bytes.toVector))
          handler(exchange)
        catch
          case NonFatal(failure) => _failure.compareAndSet(null, failure)
        finally
          try exchange.close()
          catch
            case NonFatal(failure) => _failure.compareAndSet(null, failure)
      )
      ()

    def checkFailure(primary: Option[Throwable]): Unit =
      Option(_failure.get()).foreach { failure =>
        val error = new AssertionError("Loopback fixture callback failed", failure)
        primary match
          case Some(original) => original.addSuppressed(error)
          case None => throw error
      }
