package org.simplemodeling.textus.knowledgelake.material

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.sql.{Connection, DriverManager}
import java.util.UUID
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}
import cats.~>
import cats.syntax.all.*
import io.circe.{Json, parser}
import org.goldenport.{Conclusion, Consequence, ConsequenceT}
import org.goldenport.bag.Bag
import org.goldenport.cncf.action.{Action, ActionCall, ActionEngine, CommandAction}
import org.goldenport.cncf.bootstrap.{BootstrapConfig, CncfBootstrap}
import org.goldenport.cncf.component.*
import org.goldenport.cncf.context.*
import org.goldenport.cncf.datastore.{DataStore, EntityVersionedMutationFailure, QueryDirective, SearchResult}
import org.goldenport.cncf.datastore.sql.{PostgreSqlDialectDriver, SqlDataStore}
import org.goldenport.cncf.entity.{EntityPersistent, EntitySnapshot}
import org.goldenport.cncf.filestore.*
import org.goldenport.cncf.http.HttpDriver
import org.goldenport.cncf.log.LogBackendHolder
import org.goldenport.cncf.security.*
import org.goldenport.cncf.subsystem.*
import org.goldenport.cncf.unitofwork.{ExecProgram, ExecUowM, UnitOfWork, UnitOfWorkInterpreter, UnitOfWorkOp}
import org.goldenport.datatype.MimeType
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.postgresql.ds.PGSimpleDataSource
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.simplemodeling.model.datatype.EntityId
import org.simplemodeling.textus.knowledgelake.KnowledgeLakeComponent
import org.simplemodeling.textus.knowledgelake.impl.{KnowledgeLakeParticipantFactoryBase, KnowledgeLakePrimaryComponent}
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/*
 * Required PostgreSQL, normal native command/authorization and independent physical oracles.
 * Test-owned artifacts and cleanup are confined to one target/cncf/work episode.
 *
 * @since   Oct. 21, 2026
 * @version Oct. 22, 2026
 */
private[material] object MaterialRegistrationPersistenceFixture {
  val principal: String = "registration_owner"
  val foreign: String = "registration_foreign"
  val lake: String = "native-lake"
  val logicalParent: String = "2026/10"
  val binding: String = "test-owned-files"
  val tableName: String = DataStore.CollectionId.EntityStore(MaterialRegistrationEntity.collectionId).collectionName
  val revisionName: String = MaterialRegistrationEntity.revisionBinding.storageFieldName
  val columns: Vector[String] = Vector("id", "document", "materialRef", "state", revisionName,
    "audit", "short_id", "name", "created_at", "created_by", "updated_at", "updated_by",
    "post_status", "aliveness", "owner_id", "group_id", "privilege_id", "permission", "trace_id", "correlation_id",
    "titleSearchV1", "referenceSearchV1")
  val ownerPermission: String = "{\"owner\":{\"read\":true,\"write\":true,\"execute\":false},\"group\":{\"read\":false,\"write\":false,\"execute\":false},\"other\":{\"read\":false,\"write\":false,\"execute\":false}}"
  val readOnlyPermission: String = ownerPermission.replace("\"write\":true", "\"write\":false")
  val readDeniedPermission: String = readOnlyPermission.replace("\"read\":true", "\"read\":false")
  def take[A](result: Consequence[A]): A = result.TAKE
  def failure(result: Consequence[?]): Conclusion = result match {
    case Consequence.Failure(conclusion) => conclusion
    case _ => throw new AssertionError("A structured native failure is required")
  }
  def quote(name: String): String = "\"" + name.replace("\"", "\"\"") + "\""
  def lift[A](result: Consequence[A]): ExecUowM[A] = ConsequenceT.fromConsequence[ExecProgram, A](result)
  final case class PhysicalState(rows: Vector[Map[String, Option[String]]], writes: Vector[(String, String)])
  final case class Entry(reference: FileStoreRef, parent: FileStoreRef, label: String, kind: FileStoreKind, disk: Path) {
    override def toString: String = "Entry(<test-owned>)"
  }
  final class NativeContainer extends PostgreSQLContainer(DockerImageName.parse("postgres:17.11-bookworm")) {
    withDatabaseName("tkl_native_registration")
    withUsername("registration_test_owner")
    withPassword("owned-native-registration-test-only")
    withCommand("postgres", "-c", "fsync=on", "-c", "synchronous_commit=on")
  }
  final class Database(container: NativeContainer) extends AutoCloseable {
    val schema: String = "tkl_registration_" + UUID.randomUUID().toString.replace("-", "")
    val jdbcUrl: String = container.getJdbcUrl + (if (container.getJdbcUrl.contains("?")) "&" else "?") + "currentSchema=" + schema
    private val _container = container
    private var _closed = false
    Class.forName("org.postgresql.Driver")
    Using.resource(DriverManager.getConnection(container.getJdbcUrl, container.getUsername, container.getPassword)) { connection =>
      Using.resource(connection.createStatement())(_.execute("CREATE SCHEMA " + quote(schema)))
    }
    val dataSource: PGSimpleDataSource = {
      val datasource = new PGSimpleDataSource
      datasource.setURL(jdbcUrl)
      datasource.setUser(container.getUsername)
      datasource.setPassword(container.getPassword)
      datasource
    }
    def connection[A](body: Connection => A): A = Using.resource(dataSource.getConnection)(body)
    def statement(sql: String): Unit = connection { connection =>
      Using.resource(connection.createStatement()) { statement => statement.execute(sql); () }
    }
    def prepare(): Unit = {
      statement("CREATE TABLE " + quote(tableName) + " (" + columns.map(name => quote(name) +
        (if (name == revisionName) " BIGINT NOT NULL" else " TEXT") + (if (name == "id") " PRIMARY KEY" else "")).mkString(",") + ")")
      statement("CREATE TABLE native_write_audit (sequence BIGSERIAL PRIMARY KEY, resource_id TEXT NOT NULL, operation TEXT NOT NULL)")
      statement("CREATE FUNCTION registration_audit_write() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN " +
        "INSERT INTO native_write_audit(resource_id,operation) VALUES (COALESCE(NEW.id,OLD.id),TG_OP); RETURN COALESCE(NEW,OLD); END $$")
      statement("CREATE TRIGGER registration_write AFTER INSERT OR UPDATE OR DELETE ON " + quote(tableName) +
        " FOR EACH ROW EXECUTE FUNCTION registration_audit_write()")
    }
    /** Every call opens a separate JDBC connection, outside native datastore transactions. */
    def snapshot(): PhysicalState = connection { connection =>
      val rows = Using.resource(connection.createStatement()) { statement =>
        Using.resource(statement.executeQuery("SELECT " + columns.map(quote).mkString(",") + " FROM " + quote(tableName) + " ORDER BY id")) { result =>
          val rows = Vector.newBuilder[Map[String, Option[String]]]
          while (result.next()) rows += columns.map(name => name -> Option(result.getString(name))).toMap
          rows.result()
        }
      }
      val writes = Using.resource(connection.createStatement()) { statement =>
        Using.resource(statement.executeQuery("SELECT resource_id,operation FROM native_write_audit ORDER BY sequence")) { result =>
          val values = Vector.newBuilder[(String, String)]
          while (result.next()) values += result.getString(1) -> result.getString(2)
          values.result()
        }
      }
      PhysicalState(rows, writes)
    }
    def security(identity: EntityId, owner: String = principal, permission: String = ownerPermission): Unit = {
      connection { connection =>
        Using.resource(connection.prepareStatement("UPDATE " + quote(tableName) +
          " SET owner_id=?,group_id=?,privilege_id=?,permission=? WHERE id=?")) { statement =>
          Vector(owner, owner, owner, permission, identity.value).zipWithIndex.foreach { case (value, index) => statement.setString(index + 1, value) }
          if (statement.executeUpdate() != 1) throw new AssertionError("Expected exactly one test-owned registration row")
        }
      }
      statement("DELETE FROM native_write_audit")
    }
    def durability: (String, String) = connection { connection =>
      def _setting_(name: String): String = Using.resource(connection.createStatement()) { statement =>
        Using.resource(statement.executeQuery("SHOW " + name)) { result => result.next(); result.getString(1) }
      }
      _setting_("fsync") -> _setting_("synchronous_commit")
    }
    def close(): Unit = if (!_closed) {
      _closed = true
      Using.resource(DriverManager.getConnection(_container.getJdbcUrl, _container.getUsername, _container.getPassword)) { connection =>
        Using.resource(connection.createStatement())(_.execute("DROP SCHEMA " + quote(schema) + " CASCADE"))
      }
    }
  }

  /** Only a public SqlDataStore subclass hook changes acknowledgement, never stored domain data. */
  final class NativeStore(database: Database) extends SqlDataStore(PostgreSqlDialectDriver, database.dataSource) {
    private var _commits = 0
    private var _fault: Option[(Int, Boolean)] = None
    private var _searches = Vector.empty[(DataStore.CollectionId, QueryDirective)]
    def searches: Vector[(DataStore.CollectionId, QueryDirective)] = _searches
    override def search(collection: DataStore.CollectionId, directive: QueryDirective): Consequence[SearchResult] = {
      _searches :+= collection -> directive
      super.search(collection, directive)
    }
    val unknown: Consequence[Unit] = EntityVersionedMutationFailure.transactionIndeterminate("Test-owned native commit acknowledgement is unknown")
    val rejected: Consequence[Unit] = EntityVersionedMutationFailure.transactionFailure("Test-owned native commit was refused")
    def failAfter(updates: Int, afterCommit: Boolean): Unit = { _fault = Some((_commits + updates, afterCommit)) }
    override protected def versioned_mutation_commit(connection: Connection): Consequence[Unit] = {
      _commits += 1
      _fault match {
        case Some((selected, aftercommit)) if selected == _commits =>
          _fault = None
          if (aftercommit) { connection.commit(); unknown } else rejected
        case _ => super.versioned_mutation_commit(connection)
      }
    }
  }

  /** Counted physical files: bytes are actually written and later independently opened. */
  final class Destination(root: Path, database: Database, failureStage: Option[String] = None,
    afterWrite: Boolean = false) extends FileStore {
    val name: String = "registration-test-files"
    val parent: FileStoreRef = FileStoreRef(name, "configured-parent")
    private var _entries = Vector(Entry(parent, parent, "configured-parent", FileStoreKind.Collection, root))
    private var _events = Vector.empty[String]
    private var _observed = Vector.empty[PhysicalState]
    private val _failure_stage = failureStage
    private val _after_write = afterWrite
    val error: Consequence[FileStoreMetadata] = Consequence.serviceUnavailable("Test-owned Material provider failure")
    def entries: Vector[Entry] = _entries
    def events: Vector[String] = _events
    def observed: Vector[PhysicalState] = _observed
    def releaseObservations(): Unit = { _observed = Vector.empty }
    def bytes(reference: FileStoreRef): Vector[Byte] = Files.readAllBytes(_entries.find(_.reference == reference).get.disk).toVector
    def stat(reference: FileStoreRef): ExecUowM[FileStoreMetadata] = throw new AssertionError("Registration must not discover provider metadata")
    def read(reference: FileStoreRef): ExecUowM[FileStoreContent] = throw new AssertionError("Registration must not read destination files")
    def createCollection(parent: FileStoreRef, name: String): ExecUowM[FileStoreMetadata] =
      _create(parent, name, MimeType("application/octet-stream"), None)
    def create(parent: FileStoreRef, name: String, mediaType: MimeType, content: Bag): ExecUowM[FileStoreMetadata] =
      _create(parent, name, mediaType, Some(content))
    private def _create(parent: FileStoreRef, label: String, media: MimeType, content: Option[Bag]): ExecUowM[FileStoreMetadata] = {
      _observed :+= database.snapshot()
      val event = (if (content.isDefined) "file:" else "collection:") + label
      _events :+= event
      if (_failure_stage.contains(event) && !_after_write) lift(error)
      else {
        val reference = FileStoreRef(name, "physical-" + _events.size)
        val path = root.resolve(reference.key)
        val kind = if (content.isDefined) FileStoreKind.File else FileStoreKind.Collection
        content match {
          case Some(bag) => Using.resource(bag.openInputStream()) { input =>
            Using.resource(Files.newOutputStream(path))(output => { input.transferTo(output); () })
          }
          case None => Files.createDirectory(path)
        }
        _entries :+= Entry(reference, parent, label, kind, path)
        if (_failure_stage.contains(event) && _after_write) lift(error)
        else lift(Consequence.success(FileStoreMetadata(reference, label, media,
          content.map(_ => Files.size(path)), Vector(parent), kind)))
      }
    }
  }
  final class Originals(root: Path, contribution: MaterialContribution) extends MaterialOriginalSource {
    private var _calls = Vector.empty[MaterialOriginalContribution]
    private val _paths = contribution.originals.zipWithIndex.map { case (original, index) =>
      val path = root.resolve("source-" + index)
      Files.write(path, Vector.tabulate(original.byteLength.toInt)(value => (value + index).toByte).toArray)
      original -> path
    }
    def calls: Vector[MaterialOriginalContribution] = _calls
    def expected(original: MaterialOriginalContribution): Vector[Byte] = Files.readAllBytes(_paths.find(_._1 == original).get._2).toVector
    def read(original: MaterialOriginalContribution): ExecUowM[MaterialOriginalContent] = {
      _calls :+= original
      lift(Consequence.success(MaterialOriginalContent(original, Bag.fromBytes(expected(original).toArray))))
    }
  }

  private object Provider extends AuthenticationProvider {
    def name: String = "native-registration-synthetic-provider"
    def authenticate(request: AuthenticationRequest)(using ExecutionContext): Consequence[Option[AuthenticationResult]] =
      Consequence.success(request.sessionId.flatMap {
        case "owner-session" => Some(AuthenticationResult(PrincipalId(principal)))
        case "foreign-session" => Some(AuthenticationResult(PrincipalId(foreign)))
        case _ => None
      })
  }
  private final class NativeComponent extends KnowledgeLakePrimaryComponent {
    override def authenticationProviders: Vector[AuthenticationProvider] = Vector(Provider)
  }
  private object NativeFactory extends KnowledgeLakeParticipantFactoryBase with Component.PrimaryComponentFactory {
    override protected def create_Component(params: ComponentCreate): Component = new NativeComponent
    override protected def create_Core(params: ComponentCreate, comp: Component): Component.Core =
      component_core(KnowledgeLakeComponent.name, KnowledgeLakeComponent.componentId)
  }
  private final class NativeCommand[A](operation: String, owner: KnowledgeLakePrimaryComponent,
    program: Driver[A] => ExecUowM[A]) extends CommandAction {
    private val _program = program
    val request: Request = Request.of(component = owner.name, service = "NativeRegistrationFixture", operation = operation)
    def createCall(core: ActionCall.Core): ActionCall = new Driver[A](core, _program)
  }
  /** The only exposure of protected helpers is in this test-owned ordinary command ActionCall. */
  final class Driver[A](val core: ActionCall.Core, program: Driver[A] => ExecUowM[A])
      extends MaterialRegistrationSupport with ActionCall.Core.Holder {
    private val _program = program
    private var _result: Option[A] = None
    def result: Option[A] = _result
    def create(contribution: MaterialContribution): ExecUowM[EntitySnapshot[MaterialRegistrationRecord]] =
      create_material_registration(contribution, lake, logicalParent, binding)
    def get(identity: EntityId): ExecUowM[EntitySnapshot[MaterialRegistrationRecord]] = get_material_registration(identity)
    def materialize(identity: EntityId, writer: MaterialPackageWriter, source: MaterialOriginalSource): ExecUowM[EntitySnapshot[MaterialRegistrationRecord]] =
      materialize_material_registration(identity, writer, source)
    def search(text: Option[String], offset: Int, limit: Int): ExecUowM[Record] =
      search_registered_materials(text, offset, limit)
    def replaceDocument(snapshot: EntitySnapshot[MaterialRegistrationRecord],
      document: MaterialRegistrationDocument): ExecUowM[EntitySnapshot[MaterialRegistrationRecord]] = {
      given EntityPersistent[MaterialRegistrationRecord] = MaterialRegistrationEntity.persistent
      entity_update(snapshot.entity.copy(document = document), snapshot.revision)
    }
    protected def build_Program: ExecUowM[OperationResponse] = _program(this).map { value =>
      _result = Some(value)
      OperationResponse.Void()
    }
  }
  final class Session(val component: KnowledgeLakePrimaryComponent, val subsystem: Subsystem,
    val store: NativeStore, val database: Database, val root: Path) {
    private var _aborts = 0
    private var _commits = 0
    def aborts: Int = _aborts
    def commits: Int = _commits
    def execute[A](operation: String, session: Option[String] = Some("owner-session"))(
      program: Driver[A] => ExecUowM[A]): Consequence[A] = _with_context(session) { context =>
      val action = new NativeCommand[A](operation, component, program)
      val call = component.logic.createActionCall(action, context).asInstanceOf[Driver[A]]
      ActionEngine.create().execute(call).flatMap(_ => call.result match {
        case Some(result) => Consequence.success(result)
        case None => Consequence.operationInvalid("Native fixture did not return its committed snapshot")
      })
    }
    def query(operation: String, fields: Vector[(String, Any)] = Vector.empty,
      session: Option[String] = Some("owner-session"), driver: Option[HttpDriver] = None): Consequence[OperationResponse] =
        _with_context(session, driver) { context =>
      val request = Request.of(component = component.name, service = "Material", operation = operation,
        properties = fields.map { case (name, value) => Property(name, value, None) }.toList)
      component.logic.makeOperationRequest(request).flatMap {
        case action: Action => ActionEngine.create().execute(component.logic.createActionCall(action, context))
        case _ => Consequence.argumentInvalid("Expected a native Material query")
      }
    }
    private def _with_context[A](session: Option[String], optionaldriver: Option[HttpDriver] = None)(
      run: ExecutionContext => Consequence[A]): Consequence[A] = {
      val base = ExecutionContext.create().withScope(component.scopeContext)
      IngressSecurityResolver.resolve(base, session.map(value => Map("x-textus-session" -> value)).getOrElse(Map.empty[String, String])).flatMap { admitted =>
        val selected = admitted.executionContext
        lazy val uow: UnitOfWork = optionaldriver.fold[UnitOfWork](new UnitOfWork(context)) {
          case driver: MaterialReferenceFixture.Driver => new MaterialReferenceFixture.CountedUow(context, driver)
          case driver => new NativeHttpUow(context, driver)
        }
        lazy val interpreter: UnitOfWorkInterpreter = new UnitOfWorkInterpreter(uow)
        lazy val transform: UnitOfWorkOp ~> Consequence = new (UnitOfWorkOp ~> Consequence) {
          def apply[A](operation: UnitOfWorkOp[A]): Consequence[A] = interpreter.interpret(operation)
        }
        lazy val runtime: RuntimeContext = new RuntimeContext(component.scopeContext.core, () => uow, transform,
          work => { _commits += 1; work.commit(); () }, work => { _aborts += 1; work.abort(); () }, _ => (), "native-registration-fixture")
        lazy val context: ExecutionContext = ExecutionContext.withRuntimeContext(selected, runtime)
        // Public native datastore binding. External-default without another configured
        // provider preserves this explicit, caller-owned SQL datastore on normal routing.
        context.dataStoreSpace.useDataStore(store)
        run(context)
      }
    }
    def create(contribution: MaterialContribution): Consequence[EntitySnapshot[MaterialRegistrationRecord]] = execute("create")(_.create(contribution))
    def get(identity: EntityId, session: Option[String] = Some("owner-session")): Consequence[EntitySnapshot[MaterialRegistrationRecord]] =
      execute("get", session)(_.get(identity))
    def search(text: Option[String], offset: Int = 0, limit: Int = 20,
      session: Option[String] = Some("owner-session")): Consequence[Record] =
      execute("search", session)(_.search(text, offset, limit))
    def materialize(identity: EntityId, destination: Destination, source: Originals,
      session: Option[String] = Some("owner-session")): Consequence[EntitySnapshot[MaterialRegistrationRecord]] = {
      val writer = take(MaterialPackageWriter.createC(destination, lake, logicalParent, destination.parent))
      execute("materialize", session)(_.materialize(identity, writer, source))
    }
  }

  private final class NativeHttpUow(context: ExecutionContext, driver: HttpDriver) extends UnitOfWork(context) {
    override def withContext(ctx: ExecutionContext): UnitOfWork = new NativeHttpUow(ctx, driver)
    override def httpDriver: HttpDriver = driver
  }

  def withDatabase[A](container: NativeContainer)(body: Database => A): A = {
    val database = new Database(container)
    try { database.prepare(); body(database) } finally database.close()
  }
  /** A new bootstrap, component, store and interpreter can reopen the same real schema. */
  def withSession[A](database: Database, parameters: Map[String, String] = Map.empty,
    prepare: Path => Map[String, String] = _ => Map.empty)(body: Session => A): A = synchronized {
    val previousruntime = GlobalRuntimeContext.current
    val previousglobal = Try(GlobalContext.globalContext).toOption
    val previouslog = LogBackendHolder.backend
    val work = Files.createDirectories(Path.of("target/cncf/work/tkl-native-registration").toAbsolutePath.normalize)
    val root = Files.createTempDirectory(work, "episode-")
    var owned = Option.empty[org.goldenport.cncf.bootstrap.CncfHandle]
    val store = new NativeStore(database)
    try {
      Files.writeString(Files.createDirectories(root.resolve(".textus")).resolve("config.json"), Json.obj(
        "textus.component.datastores.application.policy" -> Json.fromString("external-default")).noSpaces, StandardCharsets.UTF_8)
      val additional = prepare(root)
      val settings = additional ++ parameters
      val descriptor = root.resolve("test.yaml")
      Files.writeString(descriptor, """kind: test-descriptor
        |execution:
        |  profile: controlled
        |  key: tkl-native-registration
        |  time:
        |    mode: manual
        |    start-at: '2026-10-21T00:00:00Z'
        |  random:
        |    mode: seeded
        |    seed: tkl-native-registration
        |  ids:
        |    mode: deterministic
        |  scheduler:
        |    mode: manual
        |  ordering:
        |    mode: deterministic
        |""".stripMargin, StandardCharsets.UTF_8)
      val handle = take(CncfBootstrap.initialize(BootstrapConfig(cwd = root, args = Array(s"--textus.test.descriptor=$descriptor"))))
      owned = Some(handle)
      val id = KnowledgeLakeComponent.componentId
      val componentdescriptor = ComponentDescriptor(name = Some(id.name), componentName = Some(id.name), schemaVersion = Some(3), componentId = Some(id))
      val creation = ComponentCreate(handle.subsystem, ComponentOrigin.Builtin, Vector(componentdescriptor))
      val configured = if (settings.isEmpty) creation else creation.withInstanceMetadata(ComponentInstanceMetadata(
        id.name, ComponentInstanceId.default(id).instance, settings, componentId = Some(id)))
      val component = take(NativeFactory.createPrimaryC(configured))
        .asInstanceOf[KnowledgeLakePrimaryComponent]
      take(new org.goldenport.cncf.component.ComponentFactory().bootstrapC(component))
      handle.subsystem.add(component)
      component.withScopeContext(handle.subsystem.globalRuntimeContext)
      handle.subsystem.withDescriptor(GenericSubsystemDescriptor(path = root.resolve("fixture-subsystem.yaml"), subsystemName = handle.subsystem.name,
        componentBindings = Vector(GenericSubsystemComponentBinding(component.name)),
        security = Some(GenericSubsystemSecurityBinding(authentication = Some(GenericSubsystemAuthenticationBinding(
          convention = Some("enabled"), fallbackPrivilege = Some("disabled"), providers = Vector(
            GenericSubsystemAuthenticationProviderBinding(name = Provider.name, component = component.name, enabled = Some(true)))))))))
      body(new Session(component, handle.subsystem, store, database, root))
    } finally {
      try owned.foreach(_.close())
      finally {
        try take(store.closeC())
        finally {
          try GlobalRuntimeContext.current = previousruntime
          finally {
            try previousglobal.foreach(GlobalContext.set)
            finally {
              try previouslog.foreach(LogBackendHolder.install)
              finally Using.resource(Files.walk(root)) { paths =>
                paths.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(path => Files.deleteIfExists(path))
              }
            }
          }
        }
      }
    }
  }
}

private[material] trait MaterialRegistrationPostgresqlSpec extends BeforeAndAfterAll { self: Suite =>
  private var _container: Option[MaterialRegistrationPersistenceFixture.NativeContainer] = None
  protected final def native_container: MaterialRegistrationPersistenceFixture.NativeContainer =
    _container.getOrElse(throw new IllegalStateException("Required native PostgreSQL prerequisite is absent"))
  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val container = new MaterialRegistrationPersistenceFixture.NativeContainer
    _container = Some(container)
    try container.start()
    catch {
      case scala.util.control.NonFatal(error) =>
        try container.stop() finally _container = None
        throw new IllegalStateException("Required native PostgreSQL testcontainer could not start", error)
    }
  }
  override protected def afterAll(): Unit =
    try _container.foreach(_.stop()) finally { _container = None; super.afterAll() }
}
