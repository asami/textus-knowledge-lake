package org.simplemodeling.textus.knowledgelake.material

import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.cncf.action.FunctionalActionCall
import org.goldenport.cncf.context.PrincipalId
import org.goldenport.cncf.entity.{EntityPersistent, EntityPersistentCreate, EntitySnapshot}
import org.goldenport.cncf.security.SecuritySubject
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.observation.Descriptor
import org.goldenport.record.Record
import org.simplemodeling.model.datatype.EntityId

/*
 * Owning native ActionCall programs. Future ingress must authorize its producer,
 * source binding and TKL-managed grant before supplying a source capability.
 *
 * @since   Oct. 21, 2026
 * @version Oct. 21, 2026
 */
private[knowledgelake] trait MaterialRegistrationSupport extends FunctionalActionCall {
  /** Internal owner-registration view; ordinary native read authorization filters results before paging. */
  protected final def search_registered_materials(text: Option[String], offset: Int, limit: Int): ExecUowM[Record] = {
    given EntityPersistent[MaterialRegistrationRecord] = MaterialRegistrationEntity.persistent
    for {
      _ <- exec_from(_authenticated_principal())
      _ <- exec_from(if (text.forall(value => Option(value).exists(item => item.length <= 256 &&
        !item.exists(Character.isISOControl))) && offset >= 0 && limit >= 1 && limit <= 100) Consequence.unit
        else Consequence.argumentInvalid("Material search parameters are invalid"))
      _ <- use_component_application_datastore()
      result <- entity_search(MaterialRegistrationEntity.searchQuery(text))
      matches = result.data.sortBy(_.document.location.urn)
      end = offset.toLong + limit
    } yield Record.data("items" -> matches.slice(offset, end.min(Int.MaxValue).toInt).map(value =>
      Record.data("materialRef" -> value.document.location.urn, "title" -> value.document.contribution.title)),
      "offset" -> offset, "limit" -> limit, "hasMore" -> (end < matches.size.toLong))
  }

  protected final def get_registered_material(materialRef: String): ExecUowM[MaterialRegistrationRecord] = {
    given EntityPersistent[MaterialRegistrationRecord] = MaterialRegistrationEntity.persistent
    for {
      _ <- exec_from(_authenticated_principal())
      _ <- exec_from(if (MaterialCatalog.validText(materialRef, 2048)) Consequence.unit
        else Consequence.argumentInvalid("Material selection is invalid"))
      _ <- use_component_application_datastore()
      result <- entity_search(MaterialRegistrationEntity.referenceQuery(materialRef))
      record <- exec_from(result.data.size match {
        case 0 => Consequence.resourceNotFound("Material was not found")
        case 1 => Consequence.success(result.data.head)
        case _ => Consequence.operationConflict("getMaterial", Vector(Descriptor.Facet.Reason("material-reference-ambiguous")))
      })
    } yield record
  }

  protected final def create_material_registration(contribution: MaterialContribution, lake: String,
    logicalParent: String, storageBinding: String): ExecUowM[EntitySnapshot[MaterialRegistrationRecord]] = {
    given EntityPersistentCreate[MaterialRegistrationDraft] = MaterialRegistrationEntity.createPersistent
    given EntityPersistent[MaterialRegistrationRecord] = MaterialRegistrationEntity.persistent
    for {
      principal <- exec_from(_authenticated_principal())
      _ <- exec_from(if (Option(logicalParent).exists(value => value.isEmpty ||
        MaterialLocation.createC(lake, value).isSuccess)) Consequence.unit
        else Consequence.argumentInvalid("Invalid TKL material location"))
      leaf = opaque_id("knowledge-lake.material-package")
      location <- exec_from(MaterialLocation.createC(lake, if (logicalParent.isEmpty) leaf else logicalParent + "/" + leaf))
      document <- exec_from(MaterialRegistrationDocument.preparedC(contribution, location, storageBinding, principal, principal))
      _ <- use_component_application_datastore()
      created <- entity_create(MaterialRegistrationDraft(document))
      snapshot <- entity_load_snapshot[MaterialRegistrationRecord](created.id)
    } yield snapshot
  }

  protected final def get_material_registration(registrationId: EntityId): ExecUowM[EntitySnapshot[MaterialRegistrationRecord]] = {
    given EntityPersistent[MaterialRegistrationRecord] = MaterialRegistrationEntity.persistent
    for {
      _ <- exec_from(_authenticated_principal())
      _ <- exec_from(_identity(registrationId))
      _ <- use_component_application_datastore()
      snapshot <- entity_load_snapshot[MaterialRegistrationRecord](registrationId)
    } yield snapshot
  }

  protected final def materialize_material_registration(registrationId: EntityId, writer: MaterialPackageWriter,
    originals: MaterialOriginalSource): ExecUowM[EntitySnapshot[MaterialRegistrationRecord]] = {
    given EntityPersistent[MaterialRegistrationRecord] = MaterialRegistrationEntity.persistent
    for {
      loaded <- get_material_registration(registrationId)
      document <- exec_from(loaded.entity.document.beginC)
      materializing <- entity_update(loaded.entity.copy(document = document), loaded.revision)
      stored <- writer.materialize(document.contribution, document.location, document.owner, document.createdBy, originals)
      completed <- exec_from(document.completeC(stored))
      registered <- entity_update(materializing.entity.copy(document = completed), materializing.revision)
    } yield registered
  }

  private def _authenticated_principal(): Consequence[PrincipalId] = {
    val subject = SecuritySubject.from(execution_context.security)
    if (subject.isProviderAuthenticated) Consequence.success(PrincipalId(subject.subjectId))
    else Consequence.securityAuthenticationRequired("Material registration requires provider authentication")
  }
  private def _identity(identity: EntityId): Consequence[Unit] =
    if (Option(identity).exists(_.collection == MaterialRegistrationEntity.collectionId)) Consequence.unit
    else Consequence.argumentInvalid("Invalid Material registration identity")
}
