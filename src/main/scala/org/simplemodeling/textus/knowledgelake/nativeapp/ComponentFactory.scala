package org.simplemodeling.textus.knowledgelake.nativeapp

import cats.implicits.*
import java.nio.file.Path
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.component.{Component, ComponentCreate}
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.operation.OperationResponse
import org.simplemodeling.textus.knowledgelake.KnowledgeLakeComponent
import org.simplemodeling.textus.knowledgelake.KnowledgeLakeComponent.LocalPreparationServiceService.{ExecuteFixtureActionCall, FixtureExecutionCommand}
import org.simplemodeling.textus.knowledgelake.local.*
import org.simplemodeling.textus.knowledgelake.value.FixtureExecutionResult
import scala.util.control.NonFatal

/** Structured local failure retained by the native framework consequence.
 * @since Oct. 3, 2026
 * @version Oct. 3, 2026
 * @author ASAMI, Tomoharu
 */
final class NativeFlowFailure(val localError: LocalError) extends RuntimeException(
  s"${localError.code}: ${localError.message}; writeOutcome=${localError.writeOutcome}; observedRevision=${localError.observedRevision}"
)

/** Generated component activation for the accepted local preparation operation.
 * @since Oct. 3, 2026
 * @version Oct. 3, 2026
 * @author ASAMI, Tomoharu
 */
object NativeFlowComponent:
  /** Restricts the generated application catalog and supplies its action factory.
   * @since Oct. 3, 2026
   * @version Oct. 3, 2026
   * @author ASAMI, Tomoharu
   */
  object Factory extends KnowledgeLakeComponent.Factory:
    override protected def create_Core(params: ComponentCreate, component: Component): Component.Core =
      spec_create(KnowledgeLakeComponent.name, KnowledgeLakeComponent.componentId,
        Vector(KnowledgeLakeComponent.LocalPreparationServiceService))

    override val LocalPreparationService: KnowledgeLakeComponent.LocalPreparationServiceServiceFactory =
      new KnowledgeLakeComponent.LocalPreparationServiceServiceFactory:
        override def createExecuteFixtureActionCall(core: ActionCall.Core, action: FixtureExecutionCommand): ExecuteFixtureActionCall =
          new NativeExecuteFixtureActionCall(core, action)

  /** Executes the persisted local flow through the generated CommandAction hook.
   * @since Oct. 3, 2026
   * @version Oct. 3, 2026
   * @author ASAMI, Tomoharu
   */
  private final class NativeExecuteFixtureActionCall(
    val core: ActionCall.Core,
    override val action: FixtureExecutionCommand
  ) extends ExecuteFixtureActionCall:
    override protected def build_Program: ExecUowM[OperationResponse] =
      for
        fixturepath <- exec_from(action_required_property_string("fixturePath"))
        storedirectory <- exec_from(action_required_property_string("storeDirectory"))
        paths <- exec_from(_paths(fixturepath, storedirectory))
        response <- exec_from(_execute_fixture(paths._1, paths._2))
      yield response

  private def _paths(fixturepath: String, storedirectory: String): Consequence[(Path, Path)] =
    try Consequence.success((Path.of(fixturepath), Path.of(storedirectory)))
    catch case NonFatal(error) =>
      _failure(LocalError("invalid-path", s"Cannot convert explicit local path: ${error.getMessage}", LocalWriteOutcome.NotApplied, None))

  private def _execute_fixture(fixturepath: Path, storedirectory: Path): Consequence[OperationResponse] =
    LocalFlowMain.execute(fixturepath, storedirectory) match
      case Left(error) => _failure(error)
      case Right(report) => _encode_report(report)

  private def _encode_report(report: LocalFlowReport): Consequence[OperationResponse] =
    try
      LocalFlowJson.encodeReport(report) match
        case Left(errors) =>
          _failure(LocalError("report-encode", errors.map(_.message).mkString("; "), LocalWriteOutcome.Applied, Some(report.storeRevision)))
        case Right(reportjson) => Consequence.success(OperationResponse(FixtureExecutionResult(reportjson).toRecord()))
    catch case NonFatal(error) =>
      _failure(LocalError("report-encode", s"Primary state committed; report encoding failed: ${error.getMessage}", LocalWriteOutcome.Applied, Some(report.storeRevision)))

  private def _failure[A](error: LocalError): Consequence[A] =
    Consequence.failure(new NativeFlowFailure(error))
