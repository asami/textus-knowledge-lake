package org.simplemodeling.textus.knowledgelake.material

import cats.syntax.all.*
import io.circe.{Json, Printer}
import org.goldenport.Consequence
import org.goldenport.cncf.action.FunctionalActionCall
import org.goldenport.cncf.context.PrincipalId
import org.goldenport.cncf.entity.{EntityPersistent, EntityPersistentCreate, EntitySnapshot}
import org.goldenport.cncf.security.SecuritySubject
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.cncf.workspace.{FileObservedReadResult, FileSourceStability, FileWriteMode, FileWriteOutcome, WorkspaceFilePath}
import org.goldenport.observation.Descriptor
import org.goldenport.record.Record
import org.simplemodeling.model.datatype.EntityId

/*
 * Owning native ActionCall programs. Future ingress must authorize its producer,
 * source binding and TKL-managed grant before supplying a source capability.
 *
 * @since   Oct. 21, 2026
 *  version Nov.  6, 2026
 * @version Nov. 12, 2026
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

  protected final def materialize_referenced_registration(registrationId: EntityId,
    configuration: MaterialReferenceStorage): ExecUowM[EntitySnapshot[MaterialRegistrationRecord]] = {
    given EntityPersistent[MaterialRegistrationRecord] = MaterialRegistrationEntity.persistent
    for {
      loaded <- get_material_registration(registrationId)
      document <- exec_from(loaded.entity.document.beginC)
      _ <- exec_from(_reference_configuration(document, configuration))
      bindings <- exec_from(_reference_bindings(document.contribution, configuration))
      materializing <- entity_update(loaded.entity.copy(document = document), loaded.revision)
      observed <- bindings.foldLeft(exec_from(Consequence.success(Vector.empty[MaterialReferencedOriginal]))) {
        case (previous, (original, binding)) => for {
          accumulated <- previous
          result <- file_read_observed(binding.target, binding.filePath, configuration.policy)
          entry <- exec_from(MaterialReferencedOriginal.observed(original.resourceId, binding, result)
            .filter(value => value.baseline.byteLength == original.byteLength && result.stability != FileSourceStability.Changed)
            .fold[Consequence[MaterialReferencedOriginal]](
              Consequence.resourceInvalid("Referenced original is unavailable or changed during acquisition"))(Consequence.success))
        } yield accumulated :+ entry
      }
      _ <- bindings.foldLeft(exec_from(Consequence.unit)) { case (previous, (_, binding)) => for {
        _ <- previous
        _ <- file_separate_roots(binding.target, configuration.destinationTarget)
      } yield () }
      path <- exec_from(WorkspaceFilePath.parseC(document.location.logicalPath.split('/').last + ".json"))
      payload = Printer.noSpaces.copy(escapeNonAscii = true).print(Json.obj(
        "schema" -> Json.fromString("textus.material-reference-metadata/1"),
        "materialRef" -> Json.fromString(document.location.urn), "contribution" -> document.contribution.json,
        "references" -> Json.fromValues(observed.map(_.json))))
      written <- file_write_text(configuration.destinationTarget, path, payload, FileWriteMode.CreateNew)
      _ <- exec_from(written.outcome match {
        case FileWriteOutcome.Created => Consequence.unit
        case _ => Consequence.operationConflict("materializeMaterial", Vector(Descriptor.Facet.Reason("reference-metadata-conflict")))
      })
      completed <- exec_from(document.completeReferencesC(observed))
      registered <- entity_update(materializing.entity.copy(document = completed), materializing.revision)
    } yield registered
  }

  protected final def observe_referenced_original(document: MaterialRegistrationDocument, resourceId: String,
    configuration: MaterialReferenceStorage): ExecUowM[(MaterialReferencedOriginal, FileObservedReadResult)] = for {
    _ <- exec_from(_reference_configuration(document, configuration))
    reference <- exec_from(document.references.flatMap(_.find(_.resourceId == resourceId))
      .fold[Consequence[MaterialReferencedOriginal]](Consequence.resourceNotFound("Material resource was not found"))(Consequence.success))
    binding <- exec_from(configuration.resolve(reference.bindingId).filter(value =>
      value.workspace == reference.workspace && value.path == reference.path)
      .fold[Consequence[MaterialReferenceBinding]](Consequence.serviceUnavailable("Material source binding is unavailable"))(Consequence.success))
    observed <- file_read_observed(binding.target, binding.filePath, configuration.policy)
  } yield reference -> observed

  protected final def reference_bindings(contribution: MaterialContribution,
    configuration: MaterialReferenceStorage): Consequence[Vector[(MaterialOriginalContribution, MaterialReferenceBinding)]] =
    _reference_bindings(contribution, configuration)

  private def _reference_bindings(contribution: MaterialContribution, configuration: MaterialReferenceStorage):
      Consequence[Vector[(MaterialOriginalContribution, MaterialReferenceBinding)]] = {
    val entries = contribution.originals.map(original => MaterialReferenceStorage.bindingId(original)
      .flatMap(configuration.resolve).filter(binding => MaterialReferenceStorage.matches(original, binding)).map(original -> _))
    MaterialContribution._sequence(entries).filter(values => values.nonEmpty && values.map(_._2.id).distinct.size == values.size)
      .fold[Consequence[Vector[(MaterialOriginalContribution, MaterialReferenceBinding)]]](
        Consequence.argumentInvalid("Material source bindings are invalid"))(Consequence.success)
  }

  private def _reference_configuration(document: MaterialRegistrationDocument, configuration: MaterialReferenceStorage): Consequence[Unit] =
    if (document.storageBinding == configuration.id && document.location.lake == configuration.lake &&
      document.location.logicalPath.split("/", -1).dropRight(1).mkString("/") == configuration.logicalParent)
      Consequence.unit else Consequence.resourceInvalid("Material reference configuration does not match registration")

  private def _authenticated_principal(): Consequence[PrincipalId] = MaterialParticipantAdmission.forComponentC(
    core.component, execution_context.security).flatMap { _ =>
    val subject = SecuritySubject.from(execution_context.security)
    if (subject.isProviderAuthenticated) Consequence.success(PrincipalId(subject.subjectId))
    else Consequence.securityAuthenticationRequired("Material registration requires provider authentication")
  }
  private def _identity(identity: EntityId): Consequence[Unit] =
    if (Option(identity).exists(_.collection == MaterialRegistrationEntity.collectionId)) Consequence.unit
    else Consequence.argumentInvalid("Invalid Material registration identity")
}
