package org.simplemodeling.textus.knowledgelake.material

import cats.effect.Ref
import org.goldenport.Consequence
import org.goldenport.cncf.component.{Component, ComponentInit}
import org.goldenport.cncf.entity.*
import org.goldenport.cncf.entity.runtime.*

/*
 * Passive native registration; Entity and its application datastore own persistence.
 *
 * @since   Oct. 21, 2026
 * @version Oct. 25, 2026
 */
private[knowledgelake] trait MaterialRegistrationRuntime extends Component {
  self: MaterialReferenceRuntime =>
  private var _registration_configuration: Option[MaterialRegistrationConfiguration] = None
  private[knowledgelake] final def _material_registration_configuration: Option[MaterialRegistrationConfiguration] =
    _registration_configuration

  override protected def initialize_component_c(params: ComponentInit): Consequence[Unit] = for {
    _ <- super.initialize_component_c(params)
    resolution <- initializationParameters.resolve(MaterialRegistrationConfiguration.configurationKey)
    configuration <- resolution.value match {
      case None => Consequence.success(Option.empty[MaterialRegistrationConfiguration])
      case Some(text) => _material_reference_mode match {
        case MaterialReferenceRuntime.Mode.Registered => MaterialRegistrationConfiguration._parse(text).map(Some(_))
        case MaterialReferenceRuntime.Mode.Bootstrap =>
          Consequence.configurationInvalid("Bootstrap Material reference mode cannot include registration configuration")
        case MaterialReferenceRuntime.Mode.Packages =>
          Consequence.configurationInvalid("Package Material reference mode cannot include registration configuration")
      }
    }
    _ <- Consequence {
      val persistent = MaterialRegistrationEntity.persistent
      val realm = new EntityRealm[MaterialRegistrationRecord](MaterialRegistrationEntity.ownerName,
        EntityLoader[MaterialRegistrationRecord](_ => None), new MaterialRegistrationRealmStateRef)(using persistent)
      val collection = new EntityCollection(EntityDescriptor(MaterialRegistrationEntity.collectionId,
        EntityRuntimePlan[MaterialRegistrationRecord](MaterialRegistrationEntity.ownerName, EntityMemoryPolicy.StoreOnly, None,
          partitionStrategy = PartitionStrategy.byOrganizationMonthUTC, maxPartitions = 4,
          maxEntitiesPerPartition = 16, concurrencyPolicy = EntityConcurrencyPolicy.Optimistic),
        persistent, Some(MaterialRegistrationEntity.revisionBinding)), EntityStorage(realm, None))
      entitySpace.registerEntity(MaterialRegistrationEntity.ownerName, collection)
      ()
    }
  } yield { _registration_configuration = configuration }
}

/** Native realm state only, never a second registration database. */
private final class MaterialRegistrationRealmStateRef extends Ref[cats.Id, EntityRealmState[MaterialRegistrationRecord]] {
  private var _value = EntityRealmState[MaterialRegistrationRecord](Map.empty)
  def get: EntityRealmState[MaterialRegistrationRecord] = synchronized { _value }
  def set(value: EntityRealmState[MaterialRegistrationRecord]): Unit = synchronized { _value = value }
  def access: (EntityRealmState[MaterialRegistrationRecord], EntityRealmState[MaterialRegistrationRecord] => Boolean) = synchronized {
    val snapshot = _value
    (snapshot, next => synchronized {
      if (_value == snapshot) { _value = next; true } else false
    })
  }
  def update(f: EntityRealmState[MaterialRegistrationRecord] => EntityRealmState[MaterialRegistrationRecord]): Unit =
    synchronized { _value = f(_value) }
  def modify[B](f: EntityRealmState[MaterialRegistrationRecord] => (EntityRealmState[MaterialRegistrationRecord], B)): B = synchronized {
    val (next, result) = f(_value)
    _value = next
    result
  }
  override def tryUpdate(f: EntityRealmState[MaterialRegistrationRecord] => EntityRealmState[MaterialRegistrationRecord]): Boolean = {
    update(f); true
  }
  override def tryModify[B](f: EntityRealmState[MaterialRegistrationRecord] => (EntityRealmState[MaterialRegistrationRecord], B)): Option[B] =
    Some(modify(f))
  override def modifyState[B](state: cats.data.State[EntityRealmState[MaterialRegistrationRecord], B]): B =
    modify(value => state.run(value).value)
  override def tryModifyState[B](state: cats.data.State[EntityRealmState[MaterialRegistrationRecord], B]): Option[B] =
    tryModify(value => state.run(value).value)
}
