package org.simplemodeling.textus.knowledgelake.material

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.nio.file.attribute.PosixFilePermissions
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}
import cats.~>
import io.circe.Json
import org.goldenport.{Conclusion, Consequence}
import org.goldenport.bag.Bag
import org.goldenport.cncf.action.Action
import org.goldenport.cncf.bootstrap.{BootstrapConfig, CncfBootstrap}
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentDescriptor, ComponentInstanceId, ComponentInstanceMetadata, ComponentOrigin}
import org.goldenport.cncf.context.{ExecutionContext, GlobalContext, GlobalRuntimeContext, PrincipalId, RuntimeContext}
import org.goldenport.cncf.http.HttpDriver
import org.goldenport.cncf.log.LogBackendHolder
import org.goldenport.cncf.oauth.OAuthConnectionRuntime
import org.goldenport.cncf.security.{AuthenticationProvider, AuthenticationRequest, AuthenticationResult, IngressSecurityResolver}
import org.goldenport.cncf.subsystem.{GenericSubsystemAuthenticationBinding, GenericSubsystemAuthenticationProviderBinding, GenericSubsystemComponentBinding, GenericSubsystemDescriptor, GenericSubsystemSecurityBinding, Subsystem}
import org.goldenport.cncf.unitofwork.{UnitOfWork, UnitOfWorkInterpreter, UnitOfWorkOp}
import org.goldenport.datatype.{ContentType, MimeType}
import org.goldenport.http.{HttpResponse, HttpStatus}
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.knowledgelake.KnowledgeLakeComponent
import org.simplemodeling.textus.knowledgelake.impl.{KnowledgeLakeParticipantFactoryBase, KnowledgeLakePrimaryComponent}

/**
 * Test-owned synthetic provider, private grant files and public native lifecycle only.
 *
 * @since   Oct. 21, 2026
 * @version Oct. 21, 2026
 */
private[knowledgelake] object MaterialReferenceFixture {
  val owner = "fixture-material-owner"
  val materialRef = "urn:knowledge-lake:opaque:original-001"
  val resourceId = "logical-original"
  val connectionId = "fixture-drive"
  val resourceKey = "PrivateDriveFile_1"
  val parentKey = "PrivateDriveParent_1"
  val token = "synthetic-access-token-private-marker"
  val bytes = Vector[Byte](0, 1, -1, 42)
  val privateMarkers = Vector(owner, connectionId, resourceKey, parentKey, token, "private-client-marker", "private-secret-marker")
  val metadataUrl = s"https://www.googleapis.com/drive/v3/files/$resourceKey?supportsAllDrives=true&fields=id,name,mimeType,size,parents,trashed,version"
  val mediaUrl = s"https://www.googleapis.com/drive/v3/files/$resourceKey?alt=media&supportsAllDrives=true"

  def replace(document: Json, key: String, value: Json): Json = Json.fromJsonObject(document.asObject.get.add(key, value))
  def entry(reference: String = materialRef, title: String = "Original evidence", subjects: Vector[String] = Vector(owner)): Json =
    Json.obj("materialRef" -> Json.fromString(reference), "title" -> Json.fromString(title),
      "authorizedSubjects" -> Json.fromValues(subjects.map(Json.fromString)), "resources" -> Json.arr(Json.obj(
        "resourceId" -> Json.fromString(resourceId), "role" -> Json.fromString("original"), "mediaType" -> Json.fromString("image/png"),
        "byteLength" -> Json.fromInt(bytes.size), "storage" -> Json.obj("provider" -> Json.fromString("google-drive"),
          "connectionId" -> Json.fromString(connectionId), "resourceKey" -> Json.fromString(resourceKey), "parentKey" -> Json.fromString(parentKey)))),
      "provenance" -> Json.arr(Json.obj("kind" -> Json.fromString("citation"), "reference" -> Json.fromString("urn:evidence:source-001"))))
  def catalog(entries: Vector[Json] = Vector(entry())): Json = Json.obj(
    "schema" -> Json.fromString("textus.knowledge-lake.material-catalog/1"), "materials" -> Json.fromValues(entries))
  def metadata: Json = Json.obj("id" -> Json.fromString(resourceKey), "name" -> Json.fromString("private-provider-name"),
    "mimeType" -> Json.fromString("image/png"), "size" -> Json.fromString(bytes.size.toString),
    "parents" -> Json.arr(Json.fromString(parentKey)), "trashed" -> Json.False)
  def jsonResponse(document: Json, code: Int = 200): HttpResponse = HttpResponse.Text(
    HttpStatus.fromInt(code).getOrElse(HttpStatus.InternalServerError), ContentType(MimeType("application/json"), Some(StandardCharsets.UTF_8)),
    Bag.text(document.noSpaces, StandardCharsets.UTF_8))
  def mediaResponse(mediaType: String = "image/png", content: Vector[Byte] = bytes): HttpResponse =
    HttpResponse.Binary(HttpStatus.Ok, ContentType(MimeType(mediaType), None), Bag.binary(content.toArray))
  def failure(result: Consequence[?]): Conclusion = result match {
    case Consequence.Failure(conclusion) => conclusion
    case _ => throw new AssertionError("Expected a structured Material failure")
  }
  def record(result: Consequence[OperationResponse]): Record = result.TAKE match {
    case OperationResponse.RecordResponse(value) => value
    case _ => throw new AssertionError("Expected logical Material metadata")
  }

  final case class Call(method: String, path: String, headers: Map[String, String], properties: Vector[Property]) {
    override def toString: String = "Call(<redacted>)"
  }
  final class Driver(responses: Vector[HttpResponse]) extends HttpDriver {
    private var _calls = Vector.empty[Call]
    private var _acquisitions = 0
    def calls: Vector[Call] = _calls
    def acquisitions: Int = _acquisitions
    def acquire(): HttpDriver = { _acquisitions += 1; this }
    def get(path: String, headers: Map[String, String], properties: Vector[Property]): HttpResponse = {
      val index = _calls.size
      _calls = _calls :+ Call("GET", path, headers, properties)
      responses.lift(index).getOrElse(throw new AssertionError("Unexpected native HTTP request"))
    }
    def post(path: String, body: Option[String], headers: Map[String, String], properties: Vector[Property]): HttpResponse =
      throw new AssertionError("Material reference must not POST")
    def put(path: String, body: Option[String], headers: Map[String, String], properties: Vector[Property]): HttpResponse =
      throw new AssertionError("Material reference must not PUT")
  }
  final class CountedUow(context: ExecutionContext, driver: Driver) extends UnitOfWork(context) {
    def acquisitions: Int = driver.acquisitions
    override def withContext(ctx: ExecutionContext): UnitOfWork = new CountedUow(ctx, driver)
    override def httpDriver: HttpDriver = driver.acquire()
  }
  private object FixtureProvider extends AuthenticationProvider {
    def name: String = "material-reference-synthetic-provider"
    def authenticate(request: AuthenticationRequest)(using ExecutionContext): Consequence[Option[AuthenticationResult]] =
      Consequence.success(request.sessionId.flatMap {
        case "material-owner-session" => Some(AuthenticationResult(PrincipalId(owner)))
        case "material-other-session" => Some(AuthenticationResult(PrincipalId("other-subject")))
        case _ => None
      })
  }
  private final class FixtureComponent extends KnowledgeLakePrimaryComponent {
    override def authenticationProviders: Vector[AuthenticationProvider] = Vector(FixtureProvider)
  }
  private object FixtureFactory extends KnowledgeLakeParticipantFactoryBase with Component.PrimaryComponentFactory {
    override protected def create_Component(params: ComponentCreate): Component = new FixtureComponent
    override protected def create_Core(params: ComponentCreate, comp: Component): Component.Core =
      component_core(KnowledgeLakeComponent.name, KnowledgeLakeComponent.componentId)
  }

  final class Session(val component: KnowledgeLakePrimaryComponent, val subsystem: Subsystem, val root: Path) {
    private var _last_uow: Option[CountedUow] = None
    def acquisitions: Int = _last_uow.fold(0)(_.acquisitions)
    def request(operation: String, fields: Vector[(String, Any)] = Vector.empty): Request =
      Request.of(component = component.name, service = "Material", operation = operation,
        properties = fields.map { case (name, value) => Property(name, value, None) }.toList)
    def execute(operation: String, fields: Vector[(String, Any)] = Vector.empty,
        sessionId: Option[String] = Some("material-owner-session"), driver: Driver = new Driver(Vector.empty)): Consequence[OperationResponse] = {
      val base = ExecutionContext.create().withScope(component.scopeContext)
      IngressSecurityResolver.resolve(base, sessionId.map(value => Map("x-textus-session" -> value)).getOrElse(Map.empty[String, String]))
        .flatMap { admitted =>
          val selected = admitted.executionContext
          val uow = new CountedUow(selected, driver)
          _last_uow = Some(uow)
          val interpreter = new UnitOfWorkInterpreter(uow)
          val transform = new (UnitOfWorkOp ~> Consequence) {
            def apply[A](operation: UnitOfWorkOp[A]): Consequence[A] = interpreter.interpret(operation)
          }
          val runtime = new RuntimeContext(selected.scope.core, () => uow, transform,
            work => { work.commit(); () }, work => { work.abort(); () }, _ => (), "material-reference-fixture")
          val context = ExecutionContext.withRuntimeContext(selected, runtime)
          component.logic.makeOperationRequest(request(operation, fields)).flatMap {
            case action: Action =>
              val call = component.logic.createActionCall(action, context)
              call.authorize()(using context).flatMap(_ => call.execute())
            case _ => Consequence.argumentInvalid("Expected a native Material query")
          }
        }
    }
    def custody: Map[String, String] = Using.resource(Files.list(root)) { stream =>
      stream.iterator().asScala.filter(Files.isRegularFile(_)).map(path => path.getFileName.toString -> Files.readString(path)).toMap
    }
  }

  def withSession[A](catalogText: Option[String] = Some(catalog().noSpaces), connection: String = "connected",
      grant: String = "admitted")(body: Session => A): A = {
    val previousruntime = GlobalRuntimeContext.current
    val previousglobal = Try(GlobalContext.globalContext).toOption
    val previouslog = LogBackendHolder.backend
    val root = Files.createTempDirectory("tkl-material-reference-", PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))).toRealPath()
    try {
      val httpplan = Json.obj("schema" -> Json.fromString("cncf.runtime-http-execution.v1"),
        "baseUrl" -> Json.fromString("https://www.googleapis.com/"),
        "limits" -> Json.obj("connectTimeoutMillis" -> Json.fromInt(1000), "readTimeoutMillis" -> Json.fromInt(2000), "responseBytes" -> Json.fromInt(4096)),
        "grants" -> Json.arr(Json.obj("componentId" -> Json.fromString(if (grant == "wrong-owner") "org.example.Unrelated" else KnowledgeLakeComponent.name),
          "origins" -> Json.arr(Json.fromString(if (grant == "wrong-origin") "https://example.invalid" else "https://www.googleapis.com")),
          "methods" -> Json.arr(Json.fromString("GET")))))
      val config = if (grant == "absent") Json.obj() else Json.obj("textus.subsystem.http-execution.plan" -> Json.fromString(httpplan.noSpaces))
      Files.writeString(Files.createDirectories(root.resolve(".textus")).resolve("config.json"), config.noSpaces)
      val descriptor = root.resolve("test.yaml")
      Files.writeString(descriptor, """kind: test-descriptor
        |execution:
        |  profile: controlled
        |  key: tkl-material-reference
        |  time:
        |    mode: manual
        |    start-at: '2026-10-21T00:00:00Z'
        |  random:
        |    mode: seeded
        |    seed: tkl-material-reference
        |  ids:
        |    mode: deterministic
        |  scheduler:
        |    mode: manual
        |  ordering:
        |    mode: deterministic
        |""".stripMargin)
      val handle = CncfBootstrap.initialize(BootstrapConfig(cwd = root, args = Array(s"--textus.test.descriptor=$descriptor"))).TAKE
      try {
        val settings = catalogText.map(MaterialReferenceRuntime.catalogKey.name -> _).toMap ++
          (if (connection == "absent") Map.empty[String, String] else {
            val source = root.resolve("operator.json")
            _write_private(source, _connection_document(root).noSpaces)
            if (connection == "connected") _write_private(root.resolve("grant.json"), _grant_document.noSpaces)
            Map(OAuthConnectionRuntime.configurationKey.name -> source.toUri.toString)
          })
        val id = KnowledgeLakeComponent.componentId
        val descriptor = ComponentDescriptor(name = Some(id.name), componentName = Some(id.name), schemaVersion = Some(3), componentId = Some(id))
        val component = FixtureFactory.createPrimaryC(ComponentCreate(handle.subsystem, ComponentOrigin.Builtin, Vector(descriptor))
          .withInstanceMetadata(ComponentInstanceMetadata(id.name, ComponentInstanceId.default(id).instance, settings, componentId = Some(id)))).TAKE
          .asInstanceOf[KnowledgeLakePrimaryComponent]
        handle.subsystem.add(component)
        handle.subsystem.withDescriptor(GenericSubsystemDescriptor(path = root.resolve("fixture-subsystem.yaml"), subsystemName = handle.subsystem.name,
          componentBindings = Vector(GenericSubsystemComponentBinding(component.name)),
          security = Some(GenericSubsystemSecurityBinding(authentication = Some(GenericSubsystemAuthenticationBinding(
            convention = Some("enabled"), fallbackPrivilege = Some("disabled"), providers = Vector(
              GenericSubsystemAuthenticationProviderBinding(name = FixtureProvider.name, component = component.name, enabled = Some(true)))))))))
        body(new Session(component, handle.subsystem, root))
      } finally handle.close()
    } finally {
      GlobalRuntimeContext.current = previousruntime
      previousglobal.foreach(GlobalContext.set)
      previouslog.foreach(LogBackendHolder.install)
      Using.resource(Files.walk(root))(_.iterator().asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.deleteIfExists(_)))
    }
  }

  private val _scopes = Vector("https://www.googleapis.com/auth/drive.readonly")
  private val _redirect = s"http://127.0.0.1:8123/web/knowledge-lake/connections/$connectionId/callback"
  private def _connection_document(root: Path): Json = Json.obj("schema" -> Json.fromString("cncf.oauth-connection/1"),
    "connectionId" -> Json.fromString(connectionId), "ownerId" -> Json.fromString(owner), "provider" -> Json.fromString("google-drive"),
    "clientId" -> Json.fromString("private-client-marker"), "clientSecret" -> Json.fromString("private-secret-marker"),
    "redirectUri" -> Json.fromString(_redirect), "scopes" -> Json.fromValues(_scopes.map(Json.fromString)),
    "credentialFile" -> Json.fromString(root.resolve("grant.json").toString), "returnPath" -> Json.fromString("/web/nict-editing-studio"))
  private def _grant_document: Json = Json.obj("schema" -> Json.fromString("cncf.oauth-credential/1"),
    "connectionId" -> Json.fromString(connectionId), "ownerId" -> Json.fromString(owner),
    "client" -> Json.obj("providerId" -> Json.fromString("google-drive"),
      "authorizationEndpoint" -> Json.fromString("https://accounts.google.com/o/oauth2/v2/auth"),
      "tokenEndpoint" -> Json.fromString("https://oauth2.googleapis.com/token"), "clientId" -> Json.fromString("private-client-marker"),
      "redirectUri" -> Json.fromString(_redirect), "requestedScopes" -> Json.fromValues(_scopes.map(Json.fromString))),
    "accessToken" -> Json.fromString(token), "grantedScopes" -> Json.fromValues(_scopes.map(Json.fromString)),
    "expiresAt" -> Json.fromString("2099-10-21T00:00:00Z"))
  private def _write_private(file: Path, text: String): Unit = {
    Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
    Files.writeString(file, text, StandardCharsets.UTF_8)
    ()
  }
}
