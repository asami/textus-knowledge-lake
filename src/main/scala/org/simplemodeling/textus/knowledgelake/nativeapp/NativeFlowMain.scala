package org.simplemodeling.textus.knowledgelake.nativeapp

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import org.goldenport.Consequence
import org.goldenport.cncf.bootstrap.BootstrapConfig
import org.goldenport.cncf.cli.{CncfRuntime, RunMode}
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.subsystem.SubsystemCurrentUserEvidence
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.simplemodeling.textus.knowledgelake.KnowledgeLakeComponent
import scala.jdk.CollectionConverters.*

/** Owned normal offline runtime entry for real native request resolution and dispatch.
 * @since Oct. 3, 2026
 * @version Oct. 3, 2026
 * @author ASAMI, Tomoharu
 */
object NativeFlowMain:
  private val NORMAL_SUBSYSTEM_DESCRIPTOR = """{
    "subsystem": "knowledge-lake-local",
    "components": [
      {
        "namespace": "org.simplemodeling.textus",
        "id": "KnowledgeLake",
        "version": "0.1.0-SNAPSHOT"
      }
    ],
    "security": {
      "authentication": {
        "convention": "disabled",
        "fallback_privilege": "disabled",
        "local_subject": {
          "id": "tkl-local-user",
          "roles": [],
          "capabilities": [],
          "security_level": "user"
        }
      }
    },
    "builtin": {
      "exclude": [
        "org.goldenport.cncf.Admin",
        "org.goldenport.cncf.Auth",
        "org.goldenport.cncf.Client",
        "org.goldenport.cncf.Blob",
        "org.goldenport.cncf.Tag",
        "org.goldenport.cncf.Tool",
        "org.goldenport.cncf.Debug",
        "org.goldenport.cncf.Event",
        "org.goldenport.cncf.JobControl",
        "org.goldenport.cncf.Workflow",
        "org.goldenport.cncf.Metrics",
        "org.goldenport.cncf.MessageDeliveryStub",
        "org.goldenport.cncf.Specification"
      ]
    }
  }"""

  def execute(fixturePath: Path, storeDirectory: Path): Consequence[OperationResponse] =
    executeRequest(Request(Some(KnowledgeLakeComponent.name), Some("LocalPreparationService"), "executeFixture",
      Nil, Nil, List(Property("fixturePath", fixturePath.toString, None), Property("storeDirectory", storeDirectory.toString, None)), None))

  def executeRequest(request: Request): Consequence[OperationResponse] =
    Consequence(Files.createTempDirectory("tkl-native-runtime-")).flatMap { runtimedirectory =>
      _with_cleanup {
        val runtime = new CncfRuntime()
        _with_cleanup {
          val developmentroot = Path.of(".").toAbsolutePath.normalize()
          Files.writeString(runtimedirectory.resolve("subsystem.yaml"), NORMAL_SUBSYSTEM_DESCRIPTOR, StandardCharsets.UTF_8)
          var applicationcomponent: Option[Component] = None
          runtime.initializeHandle(BootstrapConfig(
            cwd = runtimedirectory,
            args = _runtime_arguments(runtimedirectory, developmentroot),
            modeHint = Some(RunMode.Command),
            extraComponents = subsystem => {
              val component = NativeFlowComponent.Factory.createPrimaryC(ComponentCreate(subsystem, ComponentOrigin.Embed)).TAKE
              applicationcomponent = Some(component)
              Vector(component)
            }
          )).flatMap { handle =>
            _with_cleanup {
              val subsystem = handle.subsystem
              applicationcomponent match
                case Some(component) =>
                  subsystem.upsertC(Vector(component)).flatMap { activesubsystem =>
                    activesubsystem.executionProfileC.flatMap { profile =>
                      if profile.currentUserEvidence != SubsystemCurrentUserEvidence.Fixed then
                        Consequence.configurationInvalid("Native runtime requires fixed current-user evidence")
                      else if activesubsystem.components.map(_.name).toVector != Vector(KnowledgeLakeComponent.name) then
                        Consequence.configurationInvalid("Native runtime requires exactly the generated KnowledgeLake component")
                      else activesubsystem.executeOperationResponse(request)
                    }
                  }
                case None =>
                  Consequence.configurationInvalid("Native runtime requires the generated application factory callback")
            } {
              handle.close()
            }
          }
        } {
          runtime.closeEmbedding()
        }
      } {
        _delete_runtime_directory(runtimedirectory)
      }
    }

  def main(args: Array[String]): Unit =
    if args.length != 2 then
      Consequence.failure[Unit](new IllegalArgumentException("Expected exactly two arguments: UTF-8 fixture path and explicit store/output directory")).TAKE
    val reportjson = Consequence((Path.of(args(0)), Path.of(args(1))))
      .flatMap(paths => execute(paths._1, paths._2))
      .flatMap(_report_json).TAKE
    println(s"TKL_NATIVE_FLOW_RESULT=$reportjson")

  private def _runtime_arguments(runtimedirectory: Path, developmentroot: Path): Array[String] =
    Array(
      s"--textus.subsystem.file=${runtimedirectory.resolve("subsystem.yaml")}",
      s"--textus.local-data.root=${runtimedirectory.resolve(".cncf")}",
      "--textus.component.factory-class=",
      "--textus.discover.classes=false",
      "--textus.component.dependencies.resolve.enabled=false",
      "--textus.component.dependencies.offline=true",
      "--textus.binding=@s/knowledge-lake-local/default:textus.subsystem.user-mode=standalone",
      "--textus.binding=@s/knowledge-lake-local/default:textus.execution.profile=standard",
      "--textus.binding=textus.repository.dir=",
      s"--textus.binding=textus.repository.component.dev.dir=$developmentroot",
      "--textus.binding=textus.component.dir=",
      "--textus.binding=textus.component.dev.dir=",
      "--textus.binding=textus.component.car.dir=",
      "--textus.binding=textus.component.file=",
      "--textus.binding=textus.subsystem.dev.dir=",
      "--textus.binding=textus.subsystem.sar.dir=",
      s"--textus.binding=textus.collaborator.repositories=${runtimedirectory.resolve("collaborator.d")}",
      "--textus.binding=@s/knowledge-lake-local/default:textus.import.data.file=",
      "--textus.binding=@s/knowledge-lake-local/default:textus.import.entity.file=",
      "--no-default-components",
      "command")

  private def _with_cleanup[A](body: => Consequence[A])(cleanup: => Unit): Consequence[A] =
    val result = Consequence(body).flatMap(result => result)
    val cleanupresult = Consequence(cleanup)
    result.flatMap(value => cleanupresult.map(_ => value))

  private def _delete_runtime_directory(runtimedirectory: Path): Unit =
    val paths = Files.walk(runtimedirectory)
    try paths.iterator().asScala.toVector.sortBy(_.getNameCount).reverse.foreach(path => Files.deleteIfExists(path))
    finally paths.close()

  private def _report_json(response: OperationResponse): Consequence[String] =
    response match
      case recordResponse: OperationResponse.RecordResponse =>
        recordResponse.record.getAny("reportJson") match
          case Some(reportjson: String) => Consequence.success(reportjson)
          case _ => Consequence.failure(new IllegalStateException("Native response requires a String reportJson"))
      case _ => Consequence.failure(new IllegalStateException("Native execution requires a RecordResponse"))
