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
 *  version Oct. 25, 2026
 * @version Nov.  6, 2026
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
        EntityLoader[MaterialRegistrationRecord](_ => None), new MaterialRealmStateRef[MaterialRegistrationRecord])(using persistent)
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
private[material] final class MaterialRealmStateRef[T] extends Ref[cats.Id, EntityRealmState[T]] {
  private var _value = EntityRealmState[T](Map.empty)
  def get: EntityRealmState[T] = synchronized { _value }
  def set(value: EntityRealmState[T]): Unit = synchronized { _value = value }
  def access: (EntityRealmState[T], EntityRealmState[T] => Boolean) = synchronized {
    val snapshot = _value
    (snapshot, next => synchronized {
      if (_value == snapshot) { _value = next; true } else false
    })
  }
  def update(f: EntityRealmState[T] => EntityRealmState[T]): Unit =
    synchronized { _value = f(_value) }
  def modify[B](f: EntityRealmState[T] => (EntityRealmState[T], B)): B = synchronized {
    val (next, result) = f(_value)
    _value = next
    result
  }
  override def tryUpdate(f: EntityRealmState[T] => EntityRealmState[T]): Boolean = {
    update(f); true
  }
  override def tryModify[B](f: EntityRealmState[T] => (EntityRealmState[T], B)): Option[B] =
    Some(modify(f))
  override def modifyState[B](state: cats.data.State[EntityRealmState[T], B]): B =
    modify(value => state.run(value).value)
  override def tryModifyState[B](state: cats.data.State[EntityRealmState[T], B]): Option[B] =
    tryModify(value => state.run(value).value)
}
