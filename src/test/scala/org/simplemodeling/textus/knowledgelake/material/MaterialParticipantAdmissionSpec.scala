package org.simplemodeling.textus.knowledgelake.material

import java.util.concurrent.atomic.AtomicReference
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.cncf.action.{ActionCall, ProcedureActionCall, QueryAction}
import org.goldenport.cncf.component.{Component, ComponentId, ComponentInit, ComponentInstanceId, ComponentOrigin}
import org.goldenport.cncf.context.{ExecutionContext, PrincipalId, ScopeContext, ScopeKind, SecurityContext, SessionContext, SubjectKind}
import org.goldenport.cncf.cli.RunMode
import org.goldenport.cncf.testutil.RuntimeBindingAdmissionFixture
import org.goldenport.configuration.{Configuration, ConfigurationTrace, ConfigurationValue, ResolvedConfiguration}
import org.goldenport.cncf.http.{Http4sHttpServer, HttpExecutionEngine}
import org.goldenport.cncf.operation.{CmlOperationAccess, CmlOperationDefinition}
import org.goldenport.cncf.security.{AuthenticationProvider, AuthenticationProviderIdentity, AuthenticationRequest, AuthenticationResult}
import org.goldenport.cncf.subsystem.*
import org.goldenport.protocol.{Protocol, Request}
import org.goldenport.protocol.operation.{OperationRequest, OperationResponse}
import org.goldenport.protocol.spec as spec
import org.http4s.{Header, Headers, HttpRoutes, Method, Request as HRequest, Uri}
import org.http4s.implicits.*
import org.typelevel.ci.CIString
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Controlled synthetic provider and in-memory real Http4s/native ingress.
  * This is not production UserAccount or live Drive qualification.
  * @since   Nov.  6, 2026
  * @version Nov.  6, 2026
  */
private[material] object MaterialParticipantFixture {
  val identity = AuthenticationProviderIdentity("participant-provider", "participant-provider", "participant-fixture")
  /** Separate explicit multi-user ingress; host fixtures retain their controlled defaults. */
  def withIngress[A](host: Subsystem, providerName: String = identity.providerName,
    tokenKind: Option[String] = Some("access"), subjectKind: SubjectKind = SubjectKind.User)(body: MaterialParticipantIngress => A): A = {
    val node = SystemNode.create()
    val configuration = ResolvedConfiguration(Configuration(host.configuration.configuration.values +
      (SubsystemUserMode.CONFIGURATION_KEY -> ConfigurationValue.StringValue("multi-user"))), ConfigurationTrace.empty)
    val scope = ScopeContext(ScopeKind.Subsystem, "participant-native-http", Some(host.globalRuntimeContext),
      ExecutionContext.create().observability)
    val subsystem = new Subsystem("participant-native-http", scopeContext = Some(scope),
      configuration = configuration, runMode = RunMode.Server, systemnode = node)
    try body(new MaterialParticipantIngress(subsystem, providerName, tokenKind, subjectKind))
    finally try subsystem.shutdownC().TAKE finally node.shutdownC().TAKE
  }

  def configuration(provider: AuthenticationProviderIdentity = identity): String = Json.obj(
    "schema" -> Json.fromString("textus.material-participant/1"), "profile" -> Json.fromString("same-origin-user-account"),
    "trustedProvider" -> Json.obj("bindingName" -> Json.fromString(provider.bindingName),
      "providerName" -> Json.fromString(provider.providerName), "componentName" -> Json.fromString(provider.componentName))).noSpaces
}

/** Test-only probe captures the actual post-ingress context, never writes provenance.
  * Reflection invokes the existing in-memory Http4s routes solely because their
  * adapter entry is package-scoped; no product test API or network listener is added.
  */
private[material] final class MaterialParticipantIngress(subsystem: Subsystem,
  providerName: String = MaterialParticipantFixture.identity.providerName,
  tokenKind: Option[String] = Some("access"), subjectKind: SubjectKind = SubjectKind.User) {
  private val _observed = new AtomicReference[Option[SecurityContext]](None)
  subsystem.withDescriptor(GenericSubsystemDescriptor(path = java.nio.file.Path.of("<synthetic-participant>"),
    subsystemName = subsystem.name,
    componentBindings = Vector(GenericSubsystemComponentBinding("org.example.MaterialParticipantFixture")),
    security = Some(GenericSubsystemSecurityBinding(authentication = Some(GenericSubsystemAuthenticationBinding(
      convention = Some("enabled"), fallbackPrivilege = Some("disabled"), providers = Vector(
        GenericSubsystemAuthenticationProviderBinding(name = providerName, component = "participant-fixture", enabled = Some(true)))))))))
  RuntimeBindingAdmissionFixture.admit(subsystem)
  private val _component = _create_component(subsystem, providerName, tokenKind, subjectKind)
  subsystem.add(_component)
  private val _server = new Http4sHttpServer(new HttpExecutionEngine(subsystem))
  private val _app = {
    val method = classOf[Http4sHttpServer].getDeclaredMethods.find(_.getName == "routes").get
    method.setAccessible(true)
    method.invoke(_server, null.asInstanceOf[AnyRef]).asInstanceOf[HttpRoutes[IO]].orNotFound
  }
  def context(subject: String, extraHeaders: Vector[(String, String)] = Vector.empty): SecurityContext = {
    _observed.set(None)
    val token = if (subject == MaterialRegistrationPersistenceFixture.principal) "owner-token" else "foreign-token"
    val headers = Vector(Header.Raw(CIString("Authorization"), "Bearer " + token)) ++
      extraHeaders.map { case (name, value) => Header.Raw(CIString(name), value) }
    val response = _app.run(HRequest[IO](method = Method.GET,
      uri = Uri.unsafeFromString("/rest/v1/participant-fixture/probe/check")).withHeaders(Headers(headers))).unsafeRunSync()
    _observed.get() match {
      case Some(security) =>
        response.body.compile.drain.unsafeRunSync()
        security
      case None =>
        val bytes = response.body.take(4096).compile.toVector.unsafeRunSync()
        val parsed = io.circe.parser.parse(new String(bytes.toArray, java.nio.charset.StandardCharsets.UTF_8)).toOption
        val error = parsed.map(value => value.hcursor.downField("error").focus.getOrElse(value).hcursor)
        val diagnostic = Json.obj(
          "status" -> Json.fromInt(response.status.code),
          "statusText" -> Json.fromString(response.status.reason),
          "detailCode" -> error.flatMap(_.get[Long]("detailCode").toOption).map(Json.fromLong).getOrElse(Json.Null),
          "userMode" -> subsystem.subsystemUserModeC.toOption.map(value => Json.fromString(value.mode.toString)).getOrElse(Json.Null),
          "currentUserEvidence" -> subsystem.executionProfileC.toOption.map(value => Json.fromString(value.currentUserEvidence.toString)).getOrElse(Json.Null),
          "probeComponent" -> Json.fromString(_component.name),
          "probeArtifactComponent" -> _component.artifactMetadata.flatMap(_.component).map(Json.fromString).getOrElse(Json.Null),
          "authenticationBindings" -> Json.fromValues(subsystem.resolvedSecurityWiring.authentication.enabledProviders.map(binding =>
            Json.obj("binding" -> Json.fromString(binding.name), "component" -> Json.fromString(binding.componentName),
              "provider" -> binding.provider.map(value => Json.fromString(value.name)).getOrElse(Json.Null)))))
        throw new AssertionError("Synthetic native participant probe did not reach its operation: " + diagnostic.noSpaces)
    }
  }
  private def _create_component(subsystem: Subsystem, providername: String, tokenkind: Option[String], subjectkind: SubjectKind): Component = {
    val observed = _observed
    val operation = new spec.OperationDefinition {
      val specification: spec.OperationDefinition.Specification = spec.OperationDefinition.Specification.Builder("check")
        .copy(response = spec.ResponseDefinition(result = List(org.goldenport.schema.XString))).build()
      def createOperationRequest(req: Request): Consequence[OperationRequest] = {
        val selected = req
        Consequence.success(new QueryAction {
          val request: Request = selected
          def createCall(core: ActionCall.Core): ActionCall = {
            val captured = core
            new ProcedureActionCall {
              val core: ActionCall.Core = captured
              def execute(): Consequence[OperationResponse] = {
                observed.set(Some(executionContext.security))
                Consequence.success(OperationResponse.Scalar("synthetic-ingress"))
              }
            }
          }
        })
      }
    }
    val service = spec.ServiceDefinition(name = "probe", operations = spec.OperationDefinitionGroup(cats.data.NonEmptyVector.of(operation)))
    val id = ComponentId("org.example.MaterialParticipantFixture")
    val component = new Component {
      override def operationDefinitions: Vector[CmlOperationDefinition] = Vector(CmlOperationDefinition(name = "check", kind = "QUERY",
        inputType = "Probe", outputType = "Probe", inputValueKind = "QUERY_VALUE", access = Some(CmlOperationAccess("authenticated_only"))))
      override def authenticationProviders: Vector[AuthenticationProvider] = Vector(new AuthenticationProvider {
        val name: String = providername
        def authenticate(request: AuthenticationRequest)(using ExecutionContext): Consequence[Option[AuthenticationResult]] = {
          val principal = request.accessToken.collect {
            case "owner-token" => MaterialRegistrationPersistenceFixture.principal
            case "foreign-token" => MaterialRegistrationPersistenceFixture.foreign
          }
          Consequence.success(principal.map(value => AuthenticationResult(PrincipalId(value), subjectKind = subjectkind,
            session = Some(SessionContext(sessionId = Some("synthetic-current-session"), tokenKind = tokenkind)))))
        }
      })
    }.withArtifactMetadata(Component.ArtifactMetadata(sourceType = "synthetic-spec", name = "participant-fixture",
      version = "0", component = Some("participant-fixture")))
    val core = Component.Core.create(id.name, id, ComponentInstanceId.default(id), Protocol(
      services = spec.ServiceDefinitionGroup(Vector(service)), handler = org.goldenport.protocol.handler.ProtocolHandler.default))
    component.initialize(ComponentInit(subsystem, core, ComponentOrigin.Builtin))
    component
  }
}

final class MaterialParticipantAdmissionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Explicit participant trust" should {
    "require closed server-owned configuration while keeping absent configuration outside the participant profile" in {
      Given("the exact profile and malformed/missing trusted binding fields")
      val valid = MaterialParticipantFixture.configuration()
      val malformed = Vector("{}", "null", valid.replace("bindingName", "other"),
        valid.replace("participant-provider", "*"), valid.replace("same-origin-user-account", "client-selected"))
      When("configuration is parsed without reading credentials or provider data")
      val admitted = MaterialParticipantConfiguration.parseC(valid)
      Then("only the complete exact profile is admitted")
      admitted.isSuccess shouldBe true
      malformed.foreach(value => MaterialParticipantConfiguration.parseC(value).isSuccess shouldBe false)
      intercept[org.goldenport.ConsequenceException] {
        MaterialReferenceFixture.withSession(participantConfiguration = Some("{}")) { _ =>
          fail("Malformed configured participant trust must not initialize a usable component")
        }
      }
      MaterialParticipantAdmission.admitC(None, ExecutionContext.create().security).isSuccess shouldBe true
    }
    "use exact selected provider and physical bearer provenance rather than public claims or copies" in {
      MaterialReferenceFixture.withSession(participantConfiguration = Some(MaterialParticipantFixture.configuration())) { session =>
        Given("a controlled synthetic binding through actual native HTTP ingress")
        session.withParticipantIngress() { ingress =>
        val security = ingress.context(MaterialRegistrationPersistenceFixture.principal)
        val configuration = MaterialParticipantConfiguration.parseC(MaterialParticipantFixture.configuration()).TAKE
        When("the participant boundary evaluates the actual source and equal public copies")
        Then("only native matching provider/access/bearer admission succeeds")
        security.authenticatedProvider shouldBe Some(MaterialParticipantFixture.identity)
        MaterialParticipantAdmission.admitC(Some(configuration), security).isSuccess shouldBe true
        MaterialParticipantAdmission.admitC(Some(configuration), security.copy()).isSuccess shouldBe false
        val names = Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.take(32).mkString)
        val property = Prop.forAll(names) { suffix =>
          val claimed = configuration.copy(provider = configuration.provider.copy(componentName = "foreign-" + suffix))
          !MaterialParticipantAdmission.admitC(Some(claimed), security).isSuccess
        }
        Test.check(Test.Parameters.default.withMinSuccessfulTests(100), property).passed shouldBe true
        Vector(Vector("Cookie" -> "cncf-session=synthetic"), Vector("X-Textus-Session" -> "synthetic")).foreach { headers =>
          val mixed = ingress.context(MaterialRegistrationPersistenceFixture.principal, headers)
          MaterialParticipantAdmission.admitC(Some(configuration), mixed).isSuccess shouldBe false
        }
        val publicclaim = SecurityContext(new org.goldenport.cncf.context.Principal {
          val id: PrincipalId = PrincipalId(MaterialRegistrationPersistenceFixture.principal)
          val attributes: Map[String, String] = Map("authenticated" -> "true",
            "authenticatedProvider.bindingName" -> MaterialParticipantFixture.identity.bindingName,
            "authenticatedProvider.providerName" -> MaterialParticipantFixture.identity.providerName,
            "authenticatedProvider.componentName" -> MaterialParticipantFixture.identity.componentName,
            "credentialTransports.bearerHeader" -> "true")
        }, Set.empty, org.goldenport.cncf.context.SecurityLevel("user"), SubjectKind.User,
          Some(SessionContext(sessionId = Some("claimed-session"), tokenKind = Some("access"))))
        publicclaim.authenticatedProvider shouldBe None
        publicclaim.credentialTransports shouldBe None
        MaterialParticipantAdmission.admitC(Some(configuration), publicclaim).isSuccess shouldBe false
        }
      }
    }
    "reject other provider, refresh-purpose and non-user native ingress even when its subject is authenticated" in {
      Vector(("other-provider", Some("access"), SubjectKind.User),
        (MaterialParticipantFixture.identity.providerName, Some("refresh"), SubjectKind.User),
        (MaterialParticipantFixture.identity.providerName, None, SubjectKind.User),
        (MaterialParticipantFixture.identity.providerName, Some("access"), SubjectKind.Service)).foreach { (provider, kind, subject) =>
        MaterialReferenceFixture.withSession() { session =>
          Given("a provider-authenticated synthetic subject outside the participant profile")
          session.withParticipantIngress(provider, kind, subject) { ingress =>
          val security = ingress.context(MaterialRegistrationPersistenceFixture.principal,
            Vector("X-Authenticated-Provider" -> MaterialParticipantFixture.identity.providerName))
          When("the exact configured participant profile is applied")
          val result = MaterialParticipantAdmission.admitC(Some(MaterialParticipantConfiguration.parseC(MaterialParticipantFixture.configuration()).TAKE), security)
          Then("the profile refuses the subject without provider storage effects")
          result.isSuccess shouldBe false
          }
        }
      }
    }
  }
}
