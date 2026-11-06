package org.simplemodeling.textus.knowledgelake.impl

import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.Request
import org.goldenport.protocol.operation.OperationResponse
import org.simplemodeling.model.datatype.EntityId
import org.simplemodeling.textus.knowledgelake.KnowledgeLakeComponent
import org.simplemodeling.textus.knowledgelake.material.*
import org.simplemodeling.textus.knowledgelake.{value => dto}

/* @since Oct. 25, 2026
 * @version Nov.  6, 2026
 */
private[knowledgelake] final class MaterialDeliveryServiceFactory extends KnowledgeLakeComponent.MaterialDeliveryServiceFactory {
  import KnowledgeLakeComponent.MaterialDeliveryService.*
  import MaterialDeliveryServiceFactory.*
  override def createGetDestinationActionCall(core: ActionCall.Core, action: Void): GetDestinationActionCall = DestinationCall(core, action)
  override def createPrepareActionCall(core: ActionCall.Core, action: MaterialDeliveryPrepareCommand): PrepareActionCall = PrepareCall(core, action)
  override def createFindForRequestActionCall(core: ActionCall.Core, action: MaterialDeliveryRequestQuery): FindForRequestActionCall = FindCall(core, action)
  override def createGetActionCall(core: ActionCall.Core, action: MaterialDeliverySelectionQuery): GetActionCall = GetCall(core, action)
  override def createUploadOriginalActionCall(core: ActionCall.Core, action: MaterialDeliveryUploadCommand): UploadOriginalActionCall = UploadCall(core, action)
  override def createFinalizeActionCall(core: ActionCall.Core, action: MaterialDeliveryFinalizeCommand): FinalizeActionCall = FinalizeCall(core, action)
  override def createPrepareOriginalTransferActionCall(core: ActionCall.Core, action: MaterialDeliveryDirectPrepareCommand): PrepareOriginalTransferActionCall = DirectPrepareCall(core, action)
  override def createConfirmOriginalTransferActionCall(core: ActionCall.Core, action: MaterialDeliveryDirectConfirmCommand): ConfirmOriginalTransferActionCall = DirectConfirmCall(core, action)
}

private[knowledgelake] object MaterialDeliveryServiceFactory {
  import KnowledgeLakeComponent.MaterialDeliveryService.*
  private def _admit(request: Request, keys: Set[String]): Consequence[Unit] =
    MaterialDeliveryInput.recordC(request, keys).map(_ => ())
  private final case class DestinationCall(core: ActionCall.Core, override val action: Void)
      extends GetDestinationActionCall with MaterialDeliverySupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      _ <- exec_from(_admit(action.request, Set.empty))
      result <- delivery_destination()
    } yield OperationResponse.RecordResponse(result)
  }
  private final case class PrepareCall(core: ActionCall.Core, override val action: MaterialDeliveryPrepareCommand)
      extends PrepareActionCall with MaterialDeliverySupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      _ <- exec_from(_admit(action.request, Set("requestId", "contribution")))
      record <- prepare_delivery(action.requestId, action.contribution)
      result <- delivery_projection(record)
    } yield OperationResponse.RecordResponse(result)
  }
  private final case class FindCall(core: ActionCall.Core, override val action: MaterialDeliveryRequestQuery)
      extends FindForRequestActionCall with MaterialDeliverySupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      _ <- exec_from(_admit(action.request, Set("requestId")))
      input <- exec_from(MaterialDeliveryInput.recordC(action.request, Set("requestId")))
      query <- exec_from(dto.MaterialDeliveryRequestQuery.createC(input))
      record <- find_delivery(query.requestId)
      result <- record match {
        case None => exec_from(Consequence.success(org.goldenport.record.Record.data("found" -> false)))
        case Some(value) => delivery_projection(value).map(snapshot => org.goldenport.record.Record.data("found" -> true, "delivery" -> snapshot))
      }
    } yield OperationResponse.RecordResponse(result)
  }
  private final case class GetCall(core: ActionCall.Core, override val action: MaterialDeliverySelectionQuery)
      extends GetActionCall with MaterialDeliverySupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      _ <- exec_from(_admit(action.request, Set("deliveryRef")))
      input <- exec_from(MaterialDeliveryInput.recordC(action.request, Set("deliveryRef")))
      query <- exec_from(dto.MaterialDeliverySelectionQuery.createC(input))
      identity <- exec_from(EntityId.parse(query.deliveryRef))
      record <- load_delivery(identity)
      result <- delivery_projection(record.entity)
    } yield OperationResponse.RecordResponse(result)
  }
  private final case class UploadCall(core: ActionCall.Core, override val action: MaterialDeliveryUploadCommand)
      extends UploadOriginalActionCall with MaterialDeliveryStorageSupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      input <- exec_from(MaterialDeliveryOriginalInput.parseC(action.request))
      record <- upload_delivery(input)
      result <- delivery_projection(record)
    } yield OperationResponse.RecordResponse(result)
  }
  private final case class FinalizeCall(core: ActionCall.Core, override val action: MaterialDeliveryFinalizeCommand)
      extends FinalizeActionCall with MaterialDeliveryStorageSupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      _ <- exec_from(_admit(action.request, Set("deliveryRef")))
      input <- exec_from(MaterialDeliveryInput.recordC(action.request, Set("deliveryRef")))
      query <- exec_from(dto.MaterialDeliveryFinalizeCommand.createC(input))
      identity <- exec_from(EntityId.parse(query.deliveryRef))
      record <- finalize_delivery(identity)
      result <- delivery_projection(record)
    } yield OperationResponse.RecordResponse(result)
  }
  private final case class DirectPrepareCall(core: ActionCall.Core, override val action: MaterialDeliveryDirectPrepareCommand)
      extends PrepareOriginalTransferActionCall with MaterialDeliveryStorageSupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      _ <- exec_from(_admit(action.request, Set("deliveryRef", "resourceId")))
      input <- exec_from(MaterialDeliveryInput.recordC(action.request, Set("deliveryRef", "resourceId")))
      query <- exec_from(dto.MaterialDeliveryDirectPrepareCommand.createC(input))
      identity <- exec_from(EntityId.parse(query.deliveryRef))
      result <- prepare_direct_transfer(identity, query.resourceId)
    } yield OperationResponse.RecordResponse(result)
  }
  private final case class DirectConfirmCall(core: ActionCall.Core, override val action: MaterialDeliveryDirectConfirmCommand)
      extends ConfirmOriginalTransferActionCall with MaterialDeliveryStorageSupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      _ <- exec_from(_admit(action.request, Set("deliveryRef", "resourceId", "transferRef", "result")))
      identity <- exec_from(EntityId.parse(action.deliveryRef))
      record <- confirm_direct_transfer(identity, action.resourceId, action.transferRef, action.result)
      result <- delivery_projection(record)
    } yield OperationResponse.RecordResponse(result)
  }
}
