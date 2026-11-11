package org.simplemodeling.textus.knowledgelake.material

import java.net.{HttpURLConnection, Proxy, URI}
import java.nio.file.{Files, Path}
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.time.{Duration, Instant}
import scala.util.Using
import io.circe.{Json, parser}
import org.goldenport.Consequence
import org.goldenport.cncf.bootstrap.{BootstrapConfig, CncfBootstrap}
import org.goldenport.cncf.component.*
import org.goldenport.cncf.context.*
import org.goldenport.cncf.datastore.ComponentDataStore
import org.goldenport.cncf.filestore.*
import org.goldenport.cncf.http.{Http4sHttpServer, HttpExecutionEngine, WebDescriptorResolver, UrlConnectionHttpDriver}
import org.goldenport.cncf.security.*
import org.goldenport.cncf.subsystem.*
import org.goldenport.cncf.unitofwork.{UnitOfWork, UnitOfWorkInterpreter}
import org.goldenport.datatype.MimeType
import org.simplemodeling.textus.knowledgelake.KnowledgeLakeComponent
import org.simplemodeling.textus.knowledgelake.impl.{KnowledgeLakePrimaryComponent, KnowledgeLakeParticipantFactoryBase}

/** Opt-in live harness. Never discovered by ScalaTest; no synthetic Google driver or clock.
  * The supervisor owns the JVM/listener and once-only cloud budget. Receipts omit session URLs.
  *
  * @since   Oct. 25, 2026
  * @version Nov.  6, 2026
  */
object Esa63LiveDriveRuntime {
  private val _principal = "editing-studio-mobile-development"
  // Expected physical identifier for this fixed caller under CNCF's default
  // EntityCreateDefaultsPolicy. Domain/authentication subjects remain unchanged.
  private val _storage_owner = "editing_studio_mobile_development"
  private var _token = ""
  private object Provider extends AuthenticationProvider {
    def name: String = "esa63-explicit-development-bearer"
    def authenticate(request: AuthenticationRequest)(using ExecutionContext): Consequence[Option[AuthenticationResult]] = {
      val values = request.attributes.toVector.collect { case (key, value) if key.equalsIgnoreCase("Authorization") => value }
      val admitted = values match {
        case Vector(value) if value.startsWith("Bearer ") && !value.drop(7).exists(_.isWhitespace) =>
          _token.nonEmpty && MessageDigest.isEqual(_token.getBytes(UTF_8), value.drop(7).getBytes(UTF_8))
        case _ => false
      }
      Consequence.success(Option.when(admitted)(AuthenticationResult(PrincipalId(_principal), subjectKind = SubjectKind.Service)))
    }
  }
  private final class LiveComponent extends KnowledgeLakePrimaryComponent {
    override def authenticationProviders: Vector[AuthenticationProvider] = Vector(Provider)
  }
  private object Factory extends KnowledgeLakeParticipantFactoryBase with Component.PrimaryComponentFactory {
    override protected def create_Component(params: ComponentCreate): Component = new LiveComponent
    override protected def create_Core(params: ComponentCreate, comp: Component): Component.Core =
      component_core(KnowledgeLakeComponent.name, KnowledgeLakeComponent.componentId)
  }
  private def _write(path: Path, value: Json): Unit = { Files.writeString(path, value.spaces2, UTF_8); () }
  private def _text(value: Json, key: String): String = value.hcursor.get[String](key).toOption.get
  private def _string(value: String): Json = Json.fromString(value)

  def main(args: Array[String]): Unit = {
    require(args.length == 8, "work port connection-file connection-id selection image token-file stage")
    val work = Path.of(args(0)).toRealPath()
    val port = args(1).toInt
    val connection = Path.of(args(2)).toRealPath()
    val connectionid = args(3)
    val selection = parser.parse(Files.readString(Path.of(args(4)))).toOption.get
    val image = Files.readAllBytes(Path.of(args(5)))
    val decoded = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(image))
    require(decoded != null && decoded.getWidth == 1024 && decoded.getHeight == 1536, "Exact selected PNG dimensions required")
    _token = Files.readString(Path.of(args(6))).trim
    val stage = args(7)
    require(Set("prepare", "display", "saved-display", "readiness", "connection", "persistence", "existing")(stage) && _token.nonEmpty && image.nonEmpty && image.length <= 8388608)
    require(port > 1024 && port < 65536 && connectionid.matches("[A-Za-z0-9._-]+"))
    if (stage == "existing") {
      require(selection.asObject.exists(_.keys.toSet == Set("schema", "driveParentKey", "folderKey", "folderName",
        "materialRef", "packageLabel", "requestId")) && _text(selection, "schema") == "esa63.existing-app-package-read/1")
      require(Vector("folderKey", "materialRef", "packageLabel", "requestId").forall(key => _text(selection, key).nonEmpty))
    }
    val savedbook = "fa573c1e16d48cd28618c82989c9af30"
    // Given an explicit saved-photo selection and a parent-created Python content fixture,
    val savedoriginals: Vector[(String, Long, FileStoreContentDigest)] = if (stage == "saved-display") {
      require(selection.asObject.exists(_.keys.toSet == Set("schema", "driveParentKey", "folderKey", "folderName", "bookId", "contentFixture")) &&
        _text(selection, "schema") == "esa63.saved-photo-live-selection/1" && _text(selection, "bookId") == savedbook,
        "Exact saved Book selection required")
      val selectedfixture = Path.of(_text(selection, "contentFixture"))
      require(selectedfixture.isAbsolute && selectedfixture.normalize == selectedfixture &&
        selectedfixture == Path.of("/Users/asami/src/Project2026/nict-editing-studio-app/target/esa63-real-shared-drive/captured-originals-du03/fixture.json") &&
        Files.isRegularFile(selectedfixture, java.nio.file.LinkOption.NOFOLLOW_LINKS), "Explicit parent-created fixture required")
      // When parsing that exact fixture before creating an HTTP listener,
      val contents = parser.parse(Files.readString(selectedfixture, UTF_8)).toOption.get
      // Then require the selected schema, six distinct JPEG declarations and checked SHA expectations.
      require(_text(contents, "schema") == "esa63.captured-original-content-fixture/1")
      val originals = contents.hcursor.get[Vector[Json]]("originals").toOption.get
      require(originals.size == 6, "Exactly six saved originals required")
      val admitted = originals.map { original =>
        val reference = original.hcursor.downField("reference")
        val resource = reference.get[String]("id").toOption.get
        val length = reference.get[Long]("byteLength").toOption.get
        require(resource.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,127}") && length > 0 && length <= 8388608 &&
          reference.get[String]("mediaType").toOption.contains("image/jpeg"), "Saved original declaration invalid")
        val digest = MaterialDeliveryIntegrity.parseDigest(original.hcursor.downField("contentDigest").focus.get).get
        (resource, length, digest)
      }
      require(admitted.map(_._1).distinct.size == 6, "Saved resource identities must be distinct")
      admitted
    } else Vector.empty
    val parentkey = _text(selection, "driveParentKey")
    val foldername = if (Set("display", "saved-display", "existing")(stage)) _text(selection, "folderName") else _text(selection, "testFolderName")
    require(parentkey == "0AO4b96r9HmUOUk9PVA" && foldername.matches("studio-original-test-[0-9]{8}-[0-9]{3}"))
    val destination = if (Set("display", "saved-display", "existing")(stage)) _text(selection, "folderKey") else parentkey
    val fixture = Files.createDirectory(work.resolve("native"))
    val container = new MaterialRegistrationPersistenceFixture.NativeContainer
    var database: Option[MaterialRegistrationPersistenceFixture.Database] = None
    var handle: Option[org.goldenport.cncf.bootstrap.CncfHandle] = None
    var passed = false
    try {
      container.start()
      val db = new MaterialRegistrationPersistenceFixture.Database(container)
      database = Some(db)
      db.prepare(); MaterialDeliveryOperationsFixture.prepareDatabase(db)
      if (stage == "saved-display") {
        // Given a new native database, When observing both domain stores, Then no earlier delivery can satisfy this run.
        require(db.snapshot().rows.isEmpty && MaterialDeliveryOperationsFixture.documents(db).isEmpty,
          "Saved proof requires empty native registration and delivery stores")
      }
      val id = KnowledgeLakeComponent.componentId
      val selector = ComponentDataStore.Request.forComponent(id).canonicalComponentSelector.get
      val prefix = "textus.component." + selector + ".datastores.application"
      val httpplan = Json.obj("schema" -> _string("cncf.runtime-http-execution.v1"), "baseUrl" -> _string("https://www.googleapis.com/"),
        "limits" -> Json.obj("connectTimeoutMillis" -> Json.fromInt(10000), "readTimeoutMillis" -> Json.fromInt(30000), "responseBytes" -> Json.fromInt(8388608)),
        "grants" -> Json.arr(Json.obj("componentId" -> _string(id.name), "origins" -> Json.arr(_string("https://oauth2.googleapis.com"), _string("https://www.googleapis.com")),
          "methods" -> Json.arr(_string("GET"), _string("POST"), _string("PUT")))))
      val host = Map(prefix + ".policy" -> "external-required", prefix + ".kind" -> "postgresql",
        prefix + ".jdbc.url" -> db.jdbcUrl, prefix + ".jdbc.user" -> container.getUsername,
        prefix + ".jdbc.password" -> container.getPassword, prefix + ".jdbc.driver" -> "org.postgresql.Driver",
        "textus.subsystem.user-mode" -> "multi-user", "textus.local-data.root" -> fixture.resolve("data").toString,
        "textus.subsystem.http-execution.plan" -> httpplan.noSpaces)
      _write(Files.createDirectory(fixture.resolve(".textus")).resolve("config.json"), Json.fromFields(host.map((k, v) => k -> _string(v))))
      // Multi-user admission consumes deployment intent before components initialize.
      // Declare the exact test provider first; it becomes executable only after the
      // actual component is added. No local subject, fallback privilege or test clock.
      val assembly = fixture.resolve("assembly.json")
      _write(assembly, Json.obj("subsystem" -> _string("ESA63Live"), "components" -> Json.arr(Json.obj(
        "namespace" -> _string("org.simplemodeling.textus"), "id" -> _string("KnowledgeLake"), "version" -> _string("0.1.1-SNAPSHOT"))),
        "subsystemCapabilities" -> Json.obj("providers" -> Json.arr(Json.obj("name" -> _string("esa63-native-host"),
          "component" -> _string(id.name), "provides" -> Json.arr(_string("datastore.optimistic-concurrency@1"),
            _string("datastore.persistent@1"), _string("datastore.transactional@1"), _string("user-context.current@1"))))),
        "security" -> Json.obj("authentication" -> Json.obj("convention" -> _string("enabled"),
          "fallbackPrivilege" -> _string("disabled"), "providers" -> Json.arr(Json.obj(
            "name" -> _string(Provider.name), "component" -> _string(id.name), "enabled" -> Json.True))))))
      val initialization = CncfBootstrap.initialize(BootstrapConfig(cwd = fixture,
        args = Array("--no-default-components", s"--textus.assembly.descriptor=$assembly",
          "--component-dev-dir", Path.of(".").toRealPath().toString),
        modeHint = Some(org.goldenport.cncf.cli.RunMode.Server)))
      initialization match {
        case Consequence.Failure(conclusion) =>
          val observation = conclusion.observation
          val positions = observation.cause.descriptor.facets.collect {
            case org.goldenport.observation.Descriptor.Facet.SrcPos(pos) =>
              Json.obj("file" -> _string(pos.file.replace('\\', '/').split('/').lastOption.getOrElse("unknown")),
                "line" -> Json.fromInt(pos.line))
          }
          _write(work.resolve("bootstrap-diagnostic.json"), Json.obj("status" -> Json.fromInt(conclusion.status.webCode.code),
            "category" -> _string(observation.taxonomy.category.name), "symptom" -> _string(observation.taxonomy.symptom.name),
            "sourcePositions" -> Json.fromValues(positions),
            "causeFrames" -> Json.fromValues(conclusion.getException.toVector.flatMap(_.getStackTrace.take(12)).map(frame =>
              Json.obj("file" -> _string(Option(frame.getFileName).getOrElse("unknown")), "line" -> Json.fromInt(frame.getLineNumber))))))
        case _ => ()
      }
      val own = initialization.TAKE
      handle = Some(own)
      val delivery = MaterialDeliveryConfiguration("esa63-live", "ESA63 controlled Shared Drive test", Vector(_principal),
        connectionid, "real-drive-material-test", destination, "esa63-live", "2026/10", 8388608, preferDirect = true)
      val bindings = Json.obj("schema" -> _string("textus.knowledge-lake.material-read-bindings/1"), "bindings" -> Json.arr(Json.obj(
        "id" -> _string(delivery.id), "provider" -> _string("google-drive"), "connectionId" -> _string(connectionid), "storeName" -> _string(delivery.storeName))))
      val settings = Map("google-drive.service-account.configuration" -> connection.toUri.toString,
        "material.reference.mode" -> "registered", "material.reference.registered.bindings" -> bindings.noSpaces,
        "material.delivery.configuration" -> delivery.json.noSpaces)
      val descriptor = ComponentDescriptor(name = Some(id.name), componentName = Some(id.name), schemaVersion = Some(3), componentId = Some(id))
      val create = ComponentCreate(own.subsystem, ComponentOrigin.Builtin, Vector(descriptor)).withInstanceMetadata(
        ComponentInstanceMetadata(id.name, ComponentInstanceId.default(id).instance, settings, componentId = Some(id)))
      val component = Factory.createPrimaryC(create).TAKE.asInstanceOf[KnowledgeLakePrimaryComponent]
      new org.goldenport.cncf.component.ComponentFactory().bootstrapC(component).TAKE
      own.subsystem.upsert(Vector(component)); component.withScopeContext(own.subsystem.globalRuntimeContext)
      own.subsystem.withDescriptor(GenericSubsystemDescriptor(path = fixture.resolve("subsystem.yaml"), subsystemName = own.subsystem.name,
        componentBindings = Vector(GenericSubsystemComponentBinding(component.name)),
        security = Some(GenericSubsystemSecurityBinding(authentication = Some(GenericSubsystemAuthenticationBinding(
          convention = Some("enabled"), fallbackPrivilege = Some("disabled"), providers = Vector(
            GenericSubsystemAuthenticationProviderBinding(name = Provider.name, component = component.name, enabled = Some(true)))))))))
      val context = IngressSecurityResolver.resolve(own.subsystem.executionProfileC.TAKE, component.logic.executionContext(),
        Map("Authorization" -> ("Bearer " + _token))).TAKE.executionContext
      require(SecuritySubject.from(context.security).isProviderAuthenticated && SecuritySubject.from(context.security).subjectId == _principal)
      require(context.scope.httpDriver.isInstanceOf[UrlConnectionHttpDriver] && Duration.between(context.clock.instant(), Instant.now()).abs().toMillis < 60000)
      if (stage == "existing") {
        // Observe only the explicitly selected folder; a label never becomes a provider key.
        val store: FileStoreChildren = GoogleDriveFileStore.createC(delivery.storeName,
          GoogleDriveCredentials.managedConnectionC(connectionid).TAKE).TAKE
        val interpreter = new UnitOfWorkInterpreter(new UnitOfWork(context))
        val parent = FileStoreRef(store.name, parentkey)
        val folder = FileStoreRef(store.name, destination)
        val metadata = interpreter.run(store.stat(folder)).TAKE
        require(metadata.reference == folder && metadata.kind == FileStoreKind.Collection &&
          metadata.name == foldername && metadata.parents == Vector(parent), "Selected source folder must retain its exact admitted ancestry")
        val page = interpreter.run(store.listChildren(folder, 50, None)).TAKE
        require(page.nextPageToken.isEmpty && page.items.size <= 50, "Selected folder exceeds the single-page read budget")
        val matches = page.items.filter(value => value.kind == FileStoreKind.Collection &&
          value.name == _text(selection, "packageLabel") && value.parents == Vector(folder))
        require(matches.size == 1 && matches.head.reference.store == store.name, "Exactly one selected package collection required")
        val packages = Json.obj("schema" -> _string("textus.knowledge-lake.material-packages/1"),
          "materials" -> Json.arr(Json.obj("materialRef" -> _string(_text(selection, "materialRef")),
            "authorizedSubjects" -> Json.arr(_string(_principal)), "storage" -> Json.obj(
              "provider" -> _string("google-drive"), "connectionId" -> _string(connectionid),
              "storeName" -> _string(store.name), "rootKey" -> _string(matches.head.reference.key),
              "parentKey" -> _string(folder.key)))))
        MaterialPackageReadCatalog.parseC(packages.noSpaces).TAKE
        val packagesettings = Map("google-drive.service-account.configuration" -> connection.toUri.toString,
          "material.reference.mode" -> "packages", "material.reference.packages" -> packages.noSpaces)
        val packagecreate = ComponentCreate(own.subsystem, ComponentOrigin.Builtin, Vector(descriptor)).withInstanceMetadata(
          ComponentInstanceMetadata(id.name, ComponentInstanceId.default(id).instance, packagesettings, componentId = Some(id)))
        val packagecomponent = Factory.createPrimaryC(packagecreate).TAKE.asInstanceOf[KnowledgeLakePrimaryComponent]
        new org.goldenport.cncf.component.ComponentFactory().bootstrapC(packagecomponent).TAKE
        own.subsystem.upsert(Vector(packagecomponent)); packagecomponent.withScopeContext(own.subsystem.globalRuntimeContext)
      }
      val engine = new HttpExecutionEngine(own.subsystem, Some(WebDescriptorResolver.resolveForRuntimeSubsystem(own.subsystem).TAKE))
      val listener = new Thread(() => new Http4sHttpServer(engine, port).start(), "esa63-native-loopback")
      listener.setDaemon(true); listener.start()
      _write(work.resolve("native-started.json"), Json.obj("pid" -> Json.fromLong(ProcessHandle.current().pid()), "startedAt" -> _string(Instant.now().toString)))
      val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.MINUTES.toNanos(12)
      def _wait_for_(name: String): Unit = {
        while (!Files.exists(work.resolve(name)) && !Files.exists(work.resolve("stop.request")) && System.nanoTime() < deadline) Thread.sleep(100)
        require(Files.exists(work.resolve(name)), "Bounded client request absent")
      }
      _wait_for_("probe.request")
      if (stage == "existing") {
        _write(work.resolve("existing-package-ready.json"), Json.obj("materialRef" -> _string(_text(selection, "materialRef")),
          "resourceId" -> _string("photo-cover"), "requestId" -> _string(_text(selection, "requestId"))))
        _wait_for_("existing-read.done")
        val registrationrows = db.snapshot().rows
        val deliveryrows = MaterialDeliveryOperationsFixture.documents(db)
        require(registrationrows.isEmpty && deliveryrows.isEmpty, "Existing package reads must leave both native domain stores empty")
        _write(work.resolve("native-existing-package-evidence.json"), Json.obj("mode" -> _string("packages"),
          "materialRef" -> _string(_text(selection, "materialRef")), "nativeRegistrationRows" -> Json.fromInt(registrationrows.size),
          "nativeDeliveryRows" -> Json.fromInt(deliveryrows.size), "noProviderWrites" -> Json.True))
      }
      if (stage == "persistence") {
        // Diagnose the actual HTTP host's local persistence before any provider
        // operation. Preparation creates only a local delivery record.
        val payload = Json.obj("requestId" -> _string("esa63-local-persistence-diagnostic"),
          "contribution" -> MaterialRegistrationOperationsFixture.contribution).noSpaces.getBytes(UTF_8)
        val http = URI.create(s"http://127.0.0.1:$port/rest/v1/knowledge-lake/material-delivery/prepare")
          .toURL.openConnection(Proxy.NO_PROXY).asInstanceOf[HttpURLConnection]
        http.setRequestMethod("POST"); http.setConnectTimeout(5000); http.setReadTimeout(30000)
        http.setRequestProperty("Authorization", "Bearer " + _token)
        http.setRequestProperty("Content-Type", "application/json")
        http.setFixedLengthStreamingMode(payload.length); http.setDoOutput(true)
        val code = try {
          Using.resource(http.getOutputStream)(_.write(payload))
          val status = http.getResponseCode
          val response = if (status < 400) http.getInputStream else http.getErrorStream
          if (response != null) Using.resource(response)(_.readNBytes(8388609))
          status
        } finally http.disconnect()
        val documents = MaterialDeliveryOperationsFixture.documents(db)
        val owners = db.connection { connection => Using.resource(connection.createStatement()) { statement =>
          Using.resource(statement.executeQuery("SELECT owner_id FROM " +
            MaterialRegistrationPersistenceFixture.quote(MaterialDeliveryOperationsFixture.deliveryTable))) { rows =>
            val values = Vector.newBuilder[String]
            while (rows.next()) values += rows.getString(1)
            values.result()
          }
        }}
        _write(work.resolve("persistence-diagnostic.json"), Json.obj(
          "httpStatus" -> Json.fromInt(code), "deliveryRows" -> Json.fromInt(documents.size),
          "registrationRows" -> Json.fromInt(db.snapshot().rows.size),
          "subsystemHostKeysPresent" -> Json.fromBoolean(host.keys.forall(own.subsystem.configuration.configuration.values.contains)),
          "componentSubsystemMatches" -> Json.fromBoolean(component.subsystem.contains(own.subsystem)),
          "componentIdentityMatches" -> Json.fromBoolean(component.componentId == id),
          "ownerMatches" -> Json.fromBoolean(documents.size == 1 && documents.head.hcursor
            .downField("registration").get[String]("owner").toOption.contains(_principal)),
          "physicalOwnerMatchesNativeIdentifier" -> Json.fromBoolean(owners == Vector(_storage_owner)),
          "physicalOwnerMatchesRawAuthenticationSubject" -> Json.fromBoolean(owners == Vector(_principal)),
          "providerOperationsInvoked" -> Json.False))
        require(code == 200 && documents.size == 1 && owners == Vector(_storage_owner),
          "Native HTTP preparation must independently persist one delivery with the native physical owner")
      }
      if (!Set("readiness", "persistence", "existing")(stage)) {
      val store = GoogleDriveFileStore.createC(delivery.storeName, GoogleDriveCredentials.managedConnectionC(connectionid).TAKE).TAKE
      val interpreter = new UnitOfWorkInterpreter(new UnitOfWork(context))
      val parent = FileStoreRef(store.name, parentkey)
      val parentmetadata = interpreter.run(store.stat(parent)).TAKE
      require(parentmetadata.kind == FileStoreKind.Collection)
      if (stage == "connection") _write(work.resolve("connection-probe.json"), Json.obj(
        "success" -> Json.True, "metadataReadOnly" -> Json.True,
        "referenceMatchesInput" -> Json.fromBoolean(parentmetadata.reference == parent)))
      if (stage == "display") {
        val sourcefolder = FileStoreRef(store.name, _text(selection, "folderKey"))
        val foldermetadata = interpreter.run(store.stat(sourcefolder)).TAKE
        require(foldermetadata.kind == FileStoreKind.Collection && foldermetadata.parents == Vector(parent) &&
          foldermetadata.name == foldername, "Confirmed source ancestry must be retained")
        val source = interpreter.run(store.read(FileStoreRef(store.name, _text(selection, "sourceFileKey")))).TAKE
        val sourcebytes = Using.resource(source.content.openInputStream())(_.readNBytes(8388609))
        require(java.util.Arrays.equals(sourcebytes, image), "Confirmed provider source must match every fixture byte")
        _write(work.resolve("source-admitted.json"), Json.obj("success" -> Json.True, "fullBytesMatch" -> Json.True))
        val rows = Esa63RegistrationEvidence.awaitRegistered(work, () => db.snapshot().rows, deadline)
        _write(work.resolve("registration-oracle-observation.json"), Json.obj(
          "registrationRows" -> Json.fromInt(rows.size),
          "registeredRows" -> Json.fromInt(rows.count(_("state").contains("registered"))),
          "actualCallerRows" -> Json.fromInt(rows.count(_("owner_id").contains(_storage_owner)))))
        require(rows.size == 1 && rows.head("state").contains("registered") && rows.head("owner_id").contains(_storage_owner),
          "One independently committed native registration with the actual caller required")
        val document = MaterialRegistrationDocument.parseC(parser.parse(rows.head("document").get).toOption.get).TAKE
        val reference = document.location.urn
        require(document.status == MaterialRegistrationDocument.Status.Registered && document.owner.value == _principal &&
          rows.head("materialRef").contains(reference), "Actual native PostgreSQL document identity and owner must agree")
        def _read_deliveries_(): Vector[MaterialDeliveryDocument] = {
          val current = MaterialDeliveryOperationsFixture.documents(db).map(value => MaterialDeliveryDocument.parseC(value).TAKE)
          require(current.size == 1 && rows.head("id").contains(current.head.registrationId.value) &&
            current.head.registration.owner.value == _principal && current.head.transferMethod == "googleDriveResumable" &&
            current.head.originalsComplete, "One matching native direct delivery with complete originals required")
          current
        }
        var deliveries = _read_deliveries_()
        while (deliveries.head.busy.contains("registration")) {
          if (Files.exists(work.resolve("stop.request")))
            throw new IllegalStateException("Native delivery observation stopped before registration checkpoint cleared")
          if (System.nanoTime() - deadline >= 0)
            throw new IllegalStateException("Native delivery observation deadline elapsed")
          Thread.sleep(100)
          deliveries = _read_deliveries_()
        }
        require(deliveries.size == 1 && rows.head("id").contains(deliveries.head.registrationId.value) &&
          deliveries.head.registration.owner.value == _principal &&
          deliveries.head.busy.isEmpty && deliveries.head.transferMethod == "googleDriveResumable" &&
          deliveries.head.originalsComplete, "One completed direct delivery without unresolved effects required")
        val deliverydocument = deliveries.head
        val stored = document.stored.get
        require(stored.files.size == 2 && document.contribution.originals.size == 1 && document.contribution.documents.size == 1)
        val rootmetadata = interpreter.run(store.stat(stored.root)).TAKE
        require(rootmetadata.parents == Vector(sourcefolder), "New package must stay under the confirmed test folder")
        for (collection <- Vector(deliverydocument.assets.get, deliverydocument.derived.get)) {
          require(interpreter.run(store.stat(collection)).TAKE.parents == Vector(stored.root))
        }
        val original = stored.files.find(_.path == "assets/photo-cover").get
        val originalbytes = Using.resource(interpreter.run(store.read(original.reference)).TAKE.content.openInputStream())(_.readNBytes(8388609))
        require(java.util.Arrays.equals(originalbytes, image), "Android-uploaded original must match every byte")
        val plan = MaterialPackage.planC(document.contribution, document.location, document.owner, document.createdBy).TAKE
        for (planned <- plan.documents) {
          val file = stored.files.find(_.path == planned.path).get
          val bytes = Using.resource(interpreter.run(store.read(file.reference)).TAKE.content.openInputStream())(_.readNBytes(8388609))
          require(parser.parse(new String(bytes, UTF_8)).toOption.contains(parser.parse(planned.body).toOption.get))
        }
        val manifestbytes = Using.resource(interpreter.run(store.read(stored.manifest)).TAKE.content.openInputStream())(_.readNBytes(8388609))
        require(parser.parse(new String(manifestbytes, UTF_8)).toOption.contains(plan.manifest), "Provider manifest must match the native committed plan")
        _write(work.resolve("native-provider-evidence.json"), Json.obj(
          "materialRef" -> _string(reference), "nativeRegistrationRows" -> Json.fromInt(1), "nativeDeliveryRows" -> Json.fromInt(1),
          "originalBytesEqual" -> Json.True, "derivedJsonEqual" -> Json.True, "manifestEqual" -> Json.True,
          "providerAncestryConfirmed" -> Json.True))
        _wait_for_("android.done")
        val observed = parser.parse(Files.readString(work.resolve("android-result.json"))).toOption.get
        require(_text(observed, "stage") == "registered" && _text(observed, "materialRef") == reference &&
          _text(observed, "requestId") == deliverydocument.requestId,
          "Actual registered Android result and native PostgreSQL delivery and Material identities must agree")
        _write(work.resolve("native-registration-evidence.json"), Json.obj("success" -> Json.True,
          "materialRef" -> _string(reference), "nativeRegistrationRows" -> Json.fromInt(1), "nativeDeliveryRows" -> Json.fromInt(1),
          "createdFolders" -> Json.fromInt(3), "createdFiles" -> Json.fromInt(3), "originalBytesEqual" -> Json.True,
          "derivedJsonEqual" -> Json.True, "manifestEqual" -> Json.True, "providerAncestryConfirmed" -> Json.True))
      }
      if (stage == "saved-display") {
        // Given the unchanged confirmed test folder,
        val sourcefolder = FileStoreRef(store.name, destination)
        // When independently reading its provider ancestry,
        val foldermetadata = interpreter.run(store.stat(sourcefolder)).TAKE
        // Then admit only that exact existing folder, without a new source or PNG evidence claim.
        require(foldermetadata.reference == sourcefolder && foldermetadata.kind == FileStoreKind.Collection &&
          foldermetadata.parents == Vector(parent) && foldermetadata.name == foldername,
          "Saved proof folder ancestry mismatch")
        // When retaining the explicit folder's pre-attempt children,
        val priorchildren = interpreter.run(store.listChildren(sourcefolder, 50, None)).TAKE
        // Then a bounded complete inventory can later detect an extra created package.
        require(priorchildren.nextPageToken.isEmpty && priorchildren.items.map(_.reference).distinct.size == priorchildren.items.size,
          "Explicit test folder must fit the bounded independent inventory")
        _write(work.resolve("source-admitted.json"), Json.obj("success" -> Json.True,
          "bookId" -> _string(savedbook), "original_count" -> Json.fromInt(savedoriginals.size), "fixtureDeclarationsAdmitted" -> Json.True))

        // Given actual Android work against the empty native stores,
        // When independently observing committed PostgreSQL registration,
        val rows = Esa63RegistrationEvidence.awaitRegistered(work, () => db.snapshot().rows, deadline)
        // Then retain the first committed observation before any Android completion marker.
        require(!Files.exists(work.resolve("android.done")), "Independent SQL evidence must precede Android correlation")
        require(rows.size == 1 && rows.head("state").contains("registered") && rows.head("owner_id").contains(_storage_owner),
          "Exactly one committed registration for the actual caller required")
        val document = MaterialRegistrationDocument.parseC(parser.parse(rows.head("document").get).toOption.get).TAKE
        val reference = document.location.urn
        require(document.status == MaterialRegistrationDocument.Status.Registered && document.owner.value == _principal &&
          rows.head("materialRef").contains(reference), "Native registration identity mismatch")
        def _saved_delivery_(): MaterialDeliveryDocument = {
          val documents = MaterialDeliveryOperationsFixture.documents(db).map(value => MaterialDeliveryDocument.parseC(value).TAKE)
          require(documents.size == 1 && rows.head("id").contains(documents.head.registrationId.value) &&
            documents.head.requestId == savedbook && documents.head.registration.owner.value == _principal &&
            documents.head.transferMethod == "googleDriveResumable" && documents.head.originalsComplete,
            "One exact saved Book direct delivery required")
          documents.head
        }
        var deliverydocument = _saved_delivery_()
        // Given a committed registration, When waiting for its final native CAS checkpoint,
        while (deliverydocument.busy.contains("registration")) {
          require(!Files.exists(work.resolve("stop.request")) && System.nanoTime() < deadline,
            "Saved native busy checkpoint did not clear")
          Thread.sleep(100)
          deliverydocument = _saved_delivery_()
        }
        // Then unresolved busy effects or changed immutable registration cannot pass.
        require(deliverydocument.busy.isEmpty && deliverydocument.registration.contribution.json == document.contribution.json,
          "Registered contribution and completed delivery must agree")
        val completedrows = db.snapshot().rows
        require(completedrows == rows, "Committed registration changed while clearing delivery busy")
        _write(work.resolve("native-saved-sql-observation.json"), Json.obj("source" -> _string("independent-postgresql"),
          "registrationRows" -> Json.fromInt(completedrows.size), "deliveryRows" -> Json.fromInt(1),
          "requestId" -> _string(deliverydocument.requestId), "materialRef" -> _string(reference),
          "actualOwner" -> Json.True, "directRoute" -> Json.True, "busyCleared" -> Json.True,
          "verifiedOriginals" -> Json.fromInt(deliverydocument.verifications.count(_.state == MaterialDeliveryVerificationState.Verified)),
          "clientCompletionObserved" -> Json.fromBoolean(Files.exists(work.resolve("android.done")))))
        val originals = document.contribution.originals
        require(originals.size == 6 && document.contribution.documents.size == 1 &&
          originals.map(_.resourceId) == savedoriginals.map(_._1) &&
          document.contribution.provenance.hcursor.get[String]("subjectId").toOption.contains(savedbook),
          "Immutable saved contribution identity/count mismatch")
        val stored = document.stored.get
        val root = deliverydocument.root.get
        val assets = deliverydocument.assets.get
        val derived = deliverydocument.derived.get
        require(stored.root == root && stored.files.size == 7 && deliverydocument.files.size == 7 &&
          deliverydocument.transfers.size == 6 && deliverydocument.verifications.size == 6,
          "Saved package must contain exactly six originals and one metadata file")
        val collectionrefs = Vector(root, assets, derived)
        require(collectionrefs.distinct.size == 3)
        // Given retained package placement,
        for ((collection, expectedparent) <- Vector(root -> sourcefolder, assets -> root, derived -> root)) {
          // When independently statting each actual collection,
          val observed = interpreter.run(store.stat(collection)).TAKE
          // Then root/assets/derived retain exact ancestry under the admitted folder and Drive.
          require(observed.reference == collection && observed.kind == FileStoreKind.Collection &&
            observed.parents == Vector(expectedparent) && observed.storageRoot.contains(parent),
            "Saved collection ancestry or Drive mismatch")
        }
        // Given the admitted folder's retained inventory and three actual package collections,
        // When independently listing only these explicit parents,
        val finalchildren = interpreter.run(store.listChildren(sourcefolder, 50, None)).TAKE
        // Then reject extra files/folders or hidden pagination under the admitted source parent.
        require(finalchildren.nextPageToken.isEmpty && finalchildren.items.map(_.reference).toSet ==
          priorchildren.items.map(_.reference).toSet + root && finalchildren.items.size == priorchildren.items.size + 1,
          "Saved attempt created unexpected test-folder children")
        val expectedchildren = Vector(root -> Set(assets, derived, stored.manifest),
          assets -> stored.files.filter(_.parent == assets).map(_.reference).toSet,
          derived -> stored.files.filter(_.parent == derived).map(_.reference).toSet)
        for ((collection, expected) <- expectedchildren) {
          // When reading one package collection's actual children,
          val children = interpreter.run(store.listChildren(collection, 50, None)).TAKE
          // Then its complete bounded child set equals the committed placement, with no extra object.
          require(children.nextPageToken.isEmpty && children.items.size == expected.size &&
            children.items.map(_.reference).toSet == expected, "Saved package contains extra or missing children")
        }
        val fileobservations = Vector.newBuilder[Json]
        for ((original, expected) <- originals.zip(savedoriginals)) {
          val (resource, length, digest) = expected
          // Given independent Python expectations and immutable native contribution,
          // When reading retained placement, reservation and verification facts,
          // Then require every declaration and observed digest to agree before provider reads.
          require(original.resourceId == resource && original.mediaType == "image/jpeg" &&
            original.byteLength == length && original.contentDigest.contains(digest), "Saved immutable expectation mismatch")
          val file = deliverydocument.files.find(_.path == "assets/" + resource).get
          val packaged = stored.files.find(_.path == file.path).get
          val transfer = deliverydocument.transfers.find(_.resourceId == resource).get
          val verification = deliverydocument.verification(resource)
          require(file.parent == assets && file.storageRoot.contains(parent) && packaged.reference == file.reference &&
            packaged.parent == assets && transfer.fileReference.contains(file.reference) && transfer.storageRoot == file.storageRoot &&
            transfer.upload.parent == assets && transfer.upload.name == resource && transfer.upload.mediaType.value == "image/jpeg" &&
            transfer.upload.byteLength == length && verification.state == MaterialDeliveryVerificationState.Verified &&
            verification.attempts >= 1 && verification.attempts <= 3 && verification.reason.isEmpty &&
            verification.fileReference.contains(file.reference) && verification.storageRoot == file.storageRoot &&
            verification.observedDigest.contains(digest), "Saved reservation/placement/verification binding mismatch")
          // Given real provider credentials and the retained reserved original,
          // When independently statting that final provider object,
          val observed = interpreter.run(store.stat(file.reference)).TAKE
          // Then require actual provider-computed SHA and exact ID/parent/Drive/MIME/length without downloading payload.
          require(observed.reference == file.reference && observed.kind == FileStoreKind.File && observed.parents == Vector(assets) &&
            observed.storageRoot == file.storageRoot && observed.byteLength.contains(length) && observed.mediaType.value == "image/jpeg" &&
            observed.contentDigest.contains(digest) && observed.contentDigest == verification.observedDigest,
            "Independent provider SHA/declaration mismatch")
          fileobservations += Json.obj("resourceId" -> _string(resource), "byteLength" -> Json.fromLong(length),
            "identityMatchesReservation" -> Json.True, "parentAndDriveMatch" -> Json.True, "providerSHA256Matches" -> Json.True)
        }
        require((stored.files.map(_.reference) :+ stored.manifest).distinct.size == 8 &&
          deliverydocument.manifest.contains(stored.manifest), "Exactly eight distinct final files required")
        val plan = MaterialPackage.planC(document.contribution, document.location, document.owner, document.createdBy).TAKE
        require(plan.originals.size == 6 && plan.documents.size == 1 &&
          document.contribution.documents.head.body.hcursor.downField("material").get[String]("id").toOption.contains(savedbook),
          "Derived metadata must contain the original saved Book identity")
        // Given the committed native plan,
        for (planned <- plan.documents) {
          val file = stored.files.find(_.path == planned.path).get
          // When independently statting the actual derived metadata file,
          val metadata = interpreter.run(store.stat(file.reference)).TAKE
          // Then its placement/declaration matches the native plan under the derived collection.
          require(file.parent == derived && metadata.reference == file.reference && metadata.parents == Vector(derived) &&
            metadata.kind == FileStoreKind.File && metadata.storageRoot.contains(parent) && metadata.mediaType.value == planned.mediaType &&
            metadata.byteLength.contains(planned.body.getBytes(UTF_8).length.toLong), "Derived metadata placement mismatch")
          // When reading only the actual derived JSON document,
          val bytes = Using.resource(interpreter.run(store.read(file.reference)).TAKE.content.openInputStream())(_.readNBytes(8388609))
          // Then its provider contents equal the saved Book metadata in the committed plan.
          require(parser.parse(new String(bytes, UTF_8)).toOption.contains(parser.parse(planned.body).toOption.get),
            "Provider saved Book JSON differs from committed plan")
        }
        // Given the retained manifest reference, When independently statting it,
        val manifestmetadata = interpreter.run(store.stat(stored.manifest)).TAKE
        // Then exact identity, parent, Drive and JSON declaration must agree.
        require(manifestmetadata.reference == stored.manifest && manifestmetadata.kind == FileStoreKind.File &&
          manifestmetadata.parents == Vector(root) && manifestmetadata.storageRoot.contains(parent) &&
          manifestmetadata.mediaType.value == "application/json", "Manifest placement mismatch")
        // When reading only that final manifest JSON,
        val manifestbytes = Using.resource(interpreter.run(store.read(stored.manifest)).TAKE.content.openInputStream())(_.readNBytes(8388609))
        // Then the actual provider document must equal the committed native manifest.
        require(parser.parse(new String(manifestbytes, UTF_8)).toOption.contains(plan.manifest), "Saved manifest differs from committed native plan")
        // Retain native SQL/delivery evidence and safe independent provider facts before allowing Android correlation or cleanup.
        _write(work.resolve("native-saved-delivery-observation.json"), deliverydocument.json)
        val evidence = Json.obj("schema" -> _string("esa63.saved-photo-provider-evidence/1"), "requestId" -> _string(savedbook),
          "materialRef" -> _string(reference), "original_count" -> Json.fromInt(originals.size),
          "createdFolders" -> Json.fromInt(collectionrefs.distinct.size), "createdFiles" -> Json.fromInt((stored.files.map(_.reference) :+ stored.manifest).distinct.size),
          "providerSHA256Verified" -> Json.fromInt(originals.size), "SQLRegistered" -> Json.fromInt(rows.size), "deliveryRows" -> Json.fromInt(1),
          "transferMethod" -> _string(deliverydocument.transferMethod), "busyCleared" -> Json.True,
          "derivedJsonEqual" -> Json.True, "manifestEqual" -> Json.True, "providerAncestryConfirmed" -> Json.True,
          "originals" -> Json.fromValues(fileobservations.result()))
        _write(work.resolve("native-provider-evidence.json"), evidence)
        _wait_for_("android.done")
        // Given retained independent SQL/provider evidence,
        // When reading the two actual Android process results after the completion marker,
        val sent = parser.parse(Files.readString(work.resolve("android-result.json"))).toOption.get
        val restored = parser.parse(Files.readString(work.resolve("android-saved-restore.json"))).toOption.get
        // Then both retain the same saved request/reference, verified counts and absent Studio ACK.
        for ((observed, expectedstage) <- Vector(sent -> "registered", restored -> "restored")) {
          require(_text(observed, "schema") == "esa63.saved-live-native-result/1" && _text(observed, "stage") == expectedstage &&
            _text(observed, "requestId") == savedbook && _text(observed, "sourceBookId") == savedbook &&
            _text(observed, "materialRef") == reference && _text(observed, "transferMethod") == "googleDriveResumable" &&
            observed.hcursor.get[Int]("original_count").toOption.contains(6) && observed.hcursor.get[Int]("verified").toOption.contains(6) &&
            observed.hcursor.get[Boolean]("StudioACKabsent").toOption.contains(true), "Saved Android/native evidence mismatch")
        }
        val sendpid = sent.hcursor.get[Long]("processId").toOption.get
        val restorepid = restored.hcursor.get[Long]("processId").toOption.get
        require(sendpid > 0 && restorepid > 0 && sendpid != restorepid && _text(sent, "proofId") == _text(restored, "proofId") &&
          restored.hcursor.get[Boolean]("restored").toOption.contains(true) && restored.hcursor.get[Int]("httpRequests").toOption.contains(0),
          "Saved restore must use a distinct passive process")
        _write(work.resolve("native-registration-evidence.json"), evidence.deepMerge(Json.obj("success" -> Json.True,
          "StudioACKabsent" -> Json.True, "sendProcessId" -> Json.fromLong(sendpid), "restoreProcessId" -> Json.fromLong(restorepid))))
      }
      if (stage == "prepare") {
        val folder = interpreter.run(store.createCollection(parent, foldername)).TAKE
        _write(work.resolve("cloud-progress.json"), Json.obj("createdFolders" -> Json.fromInt(1), "folderKey" -> _string(folder.reference.key), "createdFiles" -> Json.fromInt(0)))
        val upload = interpreter.run(store.prepareDirectUpload(folder.reference, "original-test-001.png", MimeType("image/png"), image.length)).TAKE
        val uri = URI.create(upload.uploadUri)
        require(uri.getScheme == "https" && uri.getHost == "www.googleapis.com" && uri.getRawUserInfo == null)
        val http = uri.toURL.openConnection(Proxy.NO_PROXY).asInstanceOf[HttpURLConnection]
        http.setRequestMethod("PUT"); http.setInstanceFollowRedirects(false); http.setConnectTimeout(10000); http.setReadTimeout(30000)
        http.setRequestProperty("Content-Type", "image/png"); http.setFixedLengthStreamingMode(image.length); http.setDoOutput(true)
        // Intentionally no Authorization header: the provider-issued capability is the only delegated authority.
        val code = try { Using.resource(http.getOutputStream)(_.write(image)); http.getResponseCode } finally http.disconnect()
        _write(work.resolve("anonymous-put.json"), Json.obj("status" -> Json.fromInt(code), "authorizationHeaderSent" -> Json.False))
        require(Set(200, 201)(code), "Provider did not accept capability-only upload")
        val metadata = interpreter.run(store.inspectDirectUpload(upload)).TAKE match {
          case DirectFileUploadStatus.Completed(value) => value
          case _ => throw new IllegalStateException("Provider has not independently confirmed completion")
        }
        val content = interpreter.run(store.read(metadata.reference)).TAKE
        val bytes = Using.resource(content.content.openInputStream())(_.readNBytes(8388609))
        require(java.util.Arrays.equals(bytes, image), "Provider readback must match every source byte")
        _write(work.resolve("material-source.json"), Json.obj("schema" -> _string("ess3.real-material-source/1"),
          "connectionId" -> _string(connectionid), "storeName" -> _string(store.name), "driveParentKey" -> _string(parentkey),
          "folderKey" -> _string(folder.reference.key), "folderName" -> _string(foldername), "sourceFileKey" -> _string(metadata.reference.key),
          "originalName" -> _string("original-test-001.png"), "mediaType" -> _string("image/png"), "byteLength" -> Json.fromInt(bytes.length),
          "width" -> Json.fromInt(1024), "height" -> Json.fromInt(1536), "createdFolders" -> Json.fromInt(1),
          "createdFiles" -> Json.fromInt(1), "fullBytesMatch" -> Json.True, "capabilityOnlyUpload" -> Json.True))
      }
      }
      _write(work.resolve("probe-complete.json"), Json.obj("success" -> Json.True, "stage" -> _string(stage)))
      _wait_for_("stop.request")
      passed = true
    } catch {
      case error: Throwable =>
        val nativefailure = error match {
          case exception: org.goldenport.ConsequenceException => exception.consequence match {
            case Consequence.Failure(conclusion) =>
              val observation = conclusion.observation
              Json.obj("status" -> Json.fromInt(conclusion.status.webCode.code),
                "category" -> _string(observation.taxonomy.category.name), "symptom" -> _string(observation.taxonomy.symptom.name),
                "sourcePositions" -> Json.fromValues(observation.cause.descriptor.facets.collect {
                  case org.goldenport.observation.Descriptor.Facet.SrcPos(pos) => Json.obj(
                    "file" -> _string(pos.file.replace('\\', '/').split('/').lastOption.getOrElse("unknown")), "line" -> Json.fromInt(pos.line))
                }))
            case _ => Json.obj()
          }
          case _ => Json.obj()
        }
        _write(work.resolve("native-failure.json"), Json.obj("success" -> Json.False,
        "native" -> nativefailure,
        "exceptionClass" -> _string(error.getClass.getName), "frames" -> Json.fromValues(error.getStackTrace.take(12).map(frame =>
          Json.obj("file" -> _string(Option(frame.getFileName).getOrElse("unknown")), "line" -> Json.fromInt(frame.getLineNumber)))),
        "diagnostic" -> _string("Exception messages and sensitive inputs suppressed")))
    } finally {
      handle.foreach(_.close()); database.foreach(_.close()); container.stop()
      _write(work.resolve("native-terminal.json"), Json.obj("success" -> Json.fromBoolean(passed), "cleanupComplete" -> Json.True))
    }
    System.exit(if (passed) 0 else 1)
  }
}
