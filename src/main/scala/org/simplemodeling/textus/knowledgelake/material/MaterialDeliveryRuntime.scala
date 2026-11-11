package org.simplemodeling.textus.knowledgelake.material

import org.goldenport.Consequence
import org.goldenport.cncf.component.{Component, ComponentInit}
import org.goldenport.cncf.entity.*
import org.goldenport.cncf.entity.runtime.*

/* @since Oct. 25, 2026
 * @version Nov.  9, 2026
 */
private[knowledgelake] trait MaterialDeliveryRuntime extends Component {
  self: MaterialReferenceRuntime =>
  private var _delivery_configuration: Option[MaterialDeliveryConfiguration] = None
  private[knowledgelake] final def _material_delivery_configuration: Option[MaterialDeliveryConfiguration] = _delivery_configuration

  private var _legacy_decision: Option[MaterialDeliveryLegacyDecision] = None
  private[knowledgelake] final def _material_delivery_legacy_decision: Option[MaterialDeliveryLegacyDecision] = _legacy_decision

  override protected def initialize_component_c(params: ComponentInit): Consequence[Unit] = for {
    _ <- super.initialize_component_c(params)
    resolution <- initializationParameters.resolve(MaterialDeliveryConfiguration.configurationKey)
    configuration <- resolution.value match {
      case None => Consequence.success(Option.empty[MaterialDeliveryConfiguration])
      case Some(text) if _material_reference_mode == MaterialReferenceRuntime.Mode.Registered =>
        MaterialDeliveryConfiguration.parseC(text).map(Some(_))
      case Some(_) => Consequence.configurationInvalid("Material delivery requires registered Material mode")
    }
    legacyresolution <- initializationParameters.resolve(MaterialDeliveryLegacyRecovery.configurationKey)
    legacy <- legacyresolution.value match {
      case None => Consequence.success(Option.empty[MaterialDeliveryLegacyDecision])
      case Some(text) => MaterialDeliveryLegacyRecovery.parseC(text).map(Some(_))
    }
    _ <- Consequence {
      val persistent = MaterialDeliveryEntity.persistent
      val realm = new EntityRealm[MaterialDeliveryRecord](MaterialDeliveryEntity.ownerName,
        EntityLoader[MaterialDeliveryRecord](_ => None), new MaterialRealmStateRef[MaterialDeliveryRecord])(using persistent)
      val collection = new EntityCollection(EntityDescriptor(MaterialDeliveryEntity.collectionId,
        EntityRuntimePlan[MaterialDeliveryRecord](MaterialDeliveryEntity.ownerName, EntityMemoryPolicy.StoreOnly, None,
          partitionStrategy = PartitionStrategy.byOrganizationMonthUTC, maxPartitions = 4, maxEntitiesPerPartition = 16,
          concurrencyPolicy = EntityConcurrencyPolicy.Optimistic), persistent, Some(MaterialDeliveryEntity.revisionBinding)),
        EntityStorage(realm, None))
      entitySpace.registerEntity(MaterialDeliveryEntity.ownerName, collection)
      ()
    }
  } yield { _delivery_configuration = configuration; _legacy_decision = legacy }
}
