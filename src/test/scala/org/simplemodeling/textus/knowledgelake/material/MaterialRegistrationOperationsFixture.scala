package org.simplemodeling.textus.knowledgelake.material

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.nio.file.attribute.PosixFilePermissions
import java.util.Base64
import scala.util.Using
import io.circe.{Json, parser}
import org.goldenport.cncf.http.HttpDriver
import org.goldenport.cncf.oauth.OAuthConnectionRuntime
import org.goldenport.http.HttpResponse
import org.goldenport.protocol.Property
import org.goldenport.record.Record
import org.simplemodeling.textus.knowledgelake.KnowledgeLakeComponent

/*
 * Controlled owning-TKL custody and a native HTTP peer, with independent committed SQL observations.
 * No real account, credentials or network is used by this fixture.
 *
 * @since   Oct. 22, 2026
 * @version Oct. 22, 2026
 */
private[material] object MaterialRegistrationOperationsFixture {
  import MaterialRegistrationPersistenceFixture.{Database, PhysicalState, principal, foreign, lake, logicalParent, binding}
  val token: String = "registration-operations-synthetic-private-token"
  val connectionId: String = "registration-operations-drive"
  val storeName: String = "registration-operations-files"
  val sourceParent: String = "SelectedSourceParent"
  val destinationParent: String = "SelectedDestinationParent"
  val sourceKeys: Vector[String] = Vector("SelectedCover", "SelectedDetail")
  val pngs: Vector[Vector[Byte]] = Vector(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aP1sAAAAASUVORK5CYII=",
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
  ).map(value => Base64.getDecoder.decode(value).toVector)
  val sources: Map[String, Vector[Byte]] = sourceKeys.zip(pngs).toMap
  private val _scopes = Vector("https://www.googleapis.com/auth/drive")
  private val _redirect = s"http://127.0.0.1:8123/web/knowledge-lake/connections/$connectionId/callback"
  private val _folder_mime = "application/vnd.google-apps.folder"

  def replace(document: Json, key: String, value: Json): Json = document.mapObject(_.add(key, value))
  def configuration: Json = Json.obj(
    "schema" -> Json.fromString("textus.knowledge-lake.material-registration/1"), "id" -> Json.fromString(binding),
    "allowedSubjects" -> Json.arr(Json.fromString(principal)), "sourceFormat" -> Json.fromString("nict-book-material-transfer/1"),
    "connectionId" -> Json.fromString(connectionId), "storeName" -> Json.fromString(storeName),
    "sourceParentKey" -> Json.fromString(sourceParent), "destinationParentKey" -> Json.fromString(destinationParent),
    "lake" -> Json.fromString(lake), "logicalParent" -> Json.fromString(logicalParent))
  def parameters(config: Option[String] = Some(configuration.noSpaces), readBindings: Boolean = true): Map[String, String] =
    Map(MaterialReferenceRuntime.modeKey.name -> "registered") ++
      config.map(MaterialRegistrationConfiguration.configurationKey.name -> _) ++
      Option.when(readBindings)(MaterialReferenceRuntime.registeredBindingsKey.name -> Json.obj(
        "schema" -> Json.fromString("textus.knowledge-lake.material-read-bindings/1"), "bindings" -> Json.arr(Json.obj(
          "id" -> Json.fromString(binding), "provider" -> Json.fromString("google-drive"),
          "connectionId" -> Json.fromString(connectionId), "storeName" -> Json.fromString(storeName)))).noSpaces)
  def contribution: Json = Json.obj(
    "schema" -> Json.fromString("textus.material-contribution/1"), "title" -> Json.fromString("Complete PNG originals\u0000\uD800\uDC00\uD800"),
    "createdAt" -> Json.fromString("retained original source time"), "classifications" -> Json.arr(Json.fromString("raw")),
    "provenance" -> Json.obj("clientowner" -> Json.fromString("untrusted-source-owner"),
      "history" -> Json.arr(Json.Null, Json.fromString("opaque\u0000\uDC00"), Json.arr(Json.fromInt(2), Json.fromInt(1)))),
    "originals" -> Json.fromValues(Vector("cover", "detail").zipWithIndex.map { case (id, index) => Json.obj(
      "resourceId" -> Json.fromString(id), "role" -> Json.fromString("original"), "mediaType" -> Json.fromString("image/png"),
      "byteLength" -> Json.fromLong(pngs(index).size.toLong), "sourceReference" -> Json.obj(
        "original" -> Json.obj("id" -> Json.fromString(id), "mediaType" -> Json.fromString("image/png"),
          "byteLength" -> Json.fromLong(pngs(index).size.toLong), "complete" -> Json.arr(Json.Null, Json.fromString("\uD800"))),
        "receipt" -> Json.obj("providerId" -> Json.fromString("google-drive"), "remoteId" -> Json.fromString(sourceKeys(index)),
          "accessConnectionId" -> Json.fromString("untrusted-studio-connection"), "url" -> Json.fromString("https://untrusted.invalid/never-fetch")),
        "clientowner" -> Json.fromString("untrusted-source-owner"), "extra" -> Json.obj("retained" -> Json.True))) }),
    "documents" -> Json.arr(Json.obj("resourceId" -> Json.fromString("review"), "role" -> Json.fromString("review-history"),
      "body" -> Json.obj("history" -> Json.arr(Json.Null, Json.fromString("\u0000\uD800\uDC00\uDC00")),
        "complete" -> Json.arr(Json.fromString("second"), Json.fromString("first"))),
      "derivedFrom" -> Json.arr(Json.fromString("detail"), Json.fromString("cover"))),
      Json.obj("resourceId" -> Json.fromString("empty"), "role" -> Json.fromString("retained-null"),
        "body" -> Json.Null, "derivedFrom" -> Json.arr())))
  def changedSource(section: String, key: String, value: Json): Json = contribution.mapObject { fields =>
    fields.add("originals", Json.fromValues(fields("originals").get.asArray.get.zipWithIndex.map { case (original, index) =>
      if (index != 0) original else original.mapObject { originalfields =>
        originalfields.add("sourceReference", originalfields("sourceReference").get.mapObject { sourcefields =>
          sourcefields.add(section, replace(sourcefields(section).get, key, value))
        })
      }
    }))
  }
  def selector(identity: String): Vector[(String, Any)] = Vector("registrationId" -> identity)
  def createFields(document: Json = contribution): Vector[(String, Any)] = Vector("contribution" -> document)
  def statUrl(key: String): String =
    s"https://www.googleapis.com/drive/v3/files/$key?supportsAllDrives=true&fields=id,name,mimeType,size,parents,trashed,version"
  def mediaUrl(key: String): String = s"https://www.googleapis.com/drive/v3/files/$key?alt=media&supportsAllDrives=true"
  def metadata(key: String, parent: String, label: String, media: String, length: Option[Long]): Json =
    Json.obj("id" -> Json.fromString(key), "name" -> Json.fromString(label), "mimeType" -> Json.fromString(media),
      "parents" -> Json.arr(Json.fromString(parent)), "trashed" -> Json.False).mapObject { fields =>
      length.fold(fields)(value => fields.add("size", Json.fromString(value.toString)))
    }
  def sourceMetadata(key: String): Json = metadata(key, sourceParent, "source-private-display", "image/png", Some(sources(key).size.toLong))
  def custody(root: Path): Map[String, Vector[Byte]] = Vector("operator.json", "grant.json").flatMap { name =>
    val file = root.resolve(name)
    Option.when(Files.exists(file))(name -> Files.readAllBytes(file).toVector)
  }.toMap
  def prepare(state: String = "connected"): Path => Map[String, String] = root => {
    val configfile = root.resolve(".textus/config.json")
    val current = parser.parse(Files.readString(configfile, StandardCharsets.UTF_8)).toOption.get
    val plan = Json.obj("schema" -> Json.fromString("cncf.runtime-http-execution.v1"),
      "baseUrl" -> Json.fromString("https://www.googleapis.com/"), "limits" -> Json.obj(
        "connectTimeoutMillis" -> Json.fromInt(1000), "readTimeoutMillis" -> Json.fromInt(2000), "responseBytes" -> Json.fromInt(65536)),
      "grants" -> Json.arr(Json.obj("componentId" -> Json.fromString(KnowledgeLakeComponent.name),
        "origins" -> Json.arr(Json.fromString("https://www.googleapis.com")),
        "methods" -> Json.arr(Json.fromString("GET"), Json.fromString("POST"), Json.fromString("PUT")))))
    Files.writeString(configfile, replace(current, "textus.subsystem.http-execution.plan", Json.fromString(plan.noSpaces)).noSpaces,
      StandardCharsets.UTF_8)
    if (state == "absent") Map.empty
    else {
      val owner = if (state == "wrong-owner") foreign else principal
      val client = Json.obj("providerId" -> Json.fromString("google-drive"),
        "authorizationEndpoint" -> Json.fromString("https://accounts.google.com/o/oauth2/v2/auth"),
        "tokenEndpoint" -> Json.fromString("https://oauth2.googleapis.com/token"),
        "clientId" -> Json.fromString("operations-private-client"), "redirectUri" -> Json.fromString(_redirect),
        "requestedScopes" -> Json.fromValues(_scopes.map(Json.fromString)))
      val operator = root.resolve("operator.json")
      _write_private(operator, Json.obj("schema" -> Json.fromString("cncf.oauth-connection/1"),
        "connectionId" -> Json.fromString(connectionId), "ownerId" -> Json.fromString(owner), "provider" -> Json.fromString("google-drive"),
        "clientId" -> Json.fromString("operations-private-client"), "clientSecret" -> Json.fromString("operations-private-secret"),
        "redirectUri" -> Json.fromString(_redirect), "scopes" -> Json.fromValues(_scopes.map(Json.fromString)),
        "credentialFile" -> Json.fromString(root.resolve("grant.json").toString), "returnPath" -> Json.fromString("/web/nict-editing-studio")))
      if (state != "disconnected") _write_private(root.resolve("grant.json"), Json.obj(
        "schema" -> Json.fromString("cncf.oauth-credential/1"), "connectionId" -> Json.fromString(connectionId),
        "ownerId" -> Json.fromString(owner), "client" -> client, "accessToken" -> Json.fromString(token),
        "grantedScopes" -> Json.fromValues(_scopes.map(Json.fromString)), "expiresAt" -> Json.fromString("2099-10-22T00:00:00Z")))
      Map(OAuthConnectionRuntime.configurationKey.name -> operator.toUri.toString)
    }
  }
  private def _write_private(file: Path, document: Json): Unit = {
    Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
    Files.writeString(file, document.noSpaces, StandardCharsets.UTF_8)
    ()
  }

  final case class Effect(method: String, path: String, headers: Map[String, String], request: Option[Json],
    bytes: Vector[Byte], before: PhysicalState) {
    override def toString: String = "RegistrationEffect(<test-owned>)"
  }
  final case class Confirmed(key: String, parent: String, label: String, media: String, bytes: Vector[Byte], collection: Boolean) {
    override def toString: String = "RegistrationConfirmed(<test-owned>)"
  }
  final case class Fault(index: Int, response: HttpResponse, after: Boolean = false)

  /** Records actual request bytes and a fresh JDBC snapshot before every synthetic provider effect. */
  final class HttpPeer(database: Database, fault: Option[Fault] = None,
    sourceStat: Option[Json] = None, sourceContent: Option[HttpResponse] = None) extends HttpDriver {
    private var _effects = Vector.empty[Effect]
    private var _confirmed = Vector.empty[Confirmed]
    private var _pending = Map.empty[String, (String, String, String)]
    private var _serial = 0
    private val _fault = fault
    private val _source_stat = sourceStat
    private val _source_content = sourceContent
    def effects: Vector[Effect] = _effects
    def confirmed: Vector[Confirmed] = _confirmed
    def releaseObservations(): Unit = { _effects = _effects.map(_.copy(before = PhysicalState(Vector.empty, Vector.empty))) }

    def get(path: String, headers: Map[String, String], properties: Vector[Property]): HttpResponse = {
      val key = new URI(path).getPath.split('/').last
      val media = path == mediaUrl(key)
      val bytes = sources.get(key).orElse(_confirmed.find(_.key == key).map(_.bytes))
        .getOrElse(throw new AssertionError("Unexpected source or persisted key"))
      _effect("GET", path, headers, None, if (media) bytes else Vector.empty) {
        if (sources.contains(key)) {
          if (media) _source_content.getOrElse(MaterialReferenceFixture.mediaResponse(content = bytes))
          else if (path == statUrl(key)) MaterialReferenceFixture.jsonResponse(_source_stat.getOrElse(sourceMetadata(key)))
          else throw new AssertionError("Unexpected source GET")
        } else {
          val file = _confirmed.find(_.key == key).get
          if (media) MaterialReferenceFixture.mediaResponse(mediaType = file.media, content = file.bytes)
          else if (path == statUrl(key)) MaterialReferenceFixture.jsonResponse(
            metadata(key, file.parent, file.label, file.media, Some(file.bytes.size.toLong)))
          else throw new AssertionError("Unexpected stored GET")
        }
      }
    }
    def post(path: String, body: Option[String], headers: Map[String, String], properties: Vector[Property]): HttpResponse = {
      val document = body.flatMap(text => parser.parse(text).toOption).getOrElse(throw new AssertionError("Actual creation JSON is required"))
      _effect("POST", path, headers, Some(document), Vector.empty) {
        val fields = document.asObject.get
        val label = fields("name").flatMap(_.asString).get
        val parent = fields("parents").flatMap(_.asArray).get.head.asString.get
        val media = fields("mimeType").flatMap(_.asString).get
        _serial += 1
        val key = "ConfirmedResource_" + _serial
        if (path.startsWith("https://www.googleapis.com/drive/v3/files?") && media == _folder_mime) {
          _confirmed :+= Confirmed(key, parent, label, media, Vector.empty, true)
          MaterialReferenceFixture.jsonResponse(metadata(key, parent, label, media, None), 201)
        } else if (path.startsWith("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&")) {
          val location = "https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&upload_id=" + key
          _pending += location -> ((parent, label, media))
          MaterialReferenceFixture.jsonResponse(Json.obj()).withHeader(Record.data("Location" -> location))
        } else throw new AssertionError("Unexpected creation POST")
      }
    }
    def put(path: String, body: Option[String], headers: Map[String, String], properties: Vector[Property]): HttpResponse =
      throw new AssertionError("Original bytes must use native putBag")
    override def putBag(path: String, body: Option[org.goldenport.bag.Bag], headers: Map[String, String],
      properties: Vector[Property]): HttpResponse = {
      val bytes = Using.resource(body.get.openInputStream())(_.readAllBytes().toVector)
      _effect("PUT", path, headers, None, bytes) {
        val (parent, label, media) = _pending.getOrElse(path, throw new AssertionError("Unknown upload session"))
        val key = path.split("upload_id=", 2).last
        _pending -= path
        _confirmed :+= Confirmed(key, parent, label, media, bytes, false)
        MaterialReferenceFixture.jsonResponse(metadata(key, parent, label, media, Some(bytes.size.toLong)), 201)
      }
    }
    private def _effect(method: String, path: String, headers: Map[String, String], document: Option[Json],
      bytes: Vector[Byte])(response: => HttpResponse): HttpResponse = {
      if (new URI(path).getHost != "www.googleapis.com") throw new AssertionError("Unexpected provider host")
      val index = _effects.size
      _effects :+= Effect(method, path, headers, document, bytes, database.snapshot())
      _fault.filter(_.index == index) match {
        case Some(selected) if !selected.after => selected.response
        case Some(selected) => val completed = response; selected.response
        case None => response
      }
    }
  }
}
