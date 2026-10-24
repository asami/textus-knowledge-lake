package org.simplemodeling.textus.knowledgelake.impl

import cats.syntax.all.*
import java.util.Locale
import org.goldenport.{Consequence, ConsequenceT}
import org.goldenport.bag.BinaryBag
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.entity.EntitySnapshot
import org.goldenport.cncf.filestore.{FileStoreKind, FileStoreRef, GoogleDriveCredentials, GoogleDriveFileStore, GoogleDriveConnectionRuntime}
import org.goldenport.cncf.security.SecuritySubject
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.datatype.{ContentType, MimeType}
import org.goldenport.http.{HttpResponse, HttpStatus}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.knowledgelake.KnowledgeLakeComponent
import org.simplemodeling.textus.knowledgelake.material.*
import org.simplemodeling.textus.knowledgelake.{value => dto}
import org.simplemodeling.model.datatype.EntityId

/*
 * @since   Oct. 21, 2026
 * @version Oct. 25, 2026
 */
private[knowledgelake] final class MaterialServiceFactory extends KnowledgeLakeComponent.MaterialServiceFactory {
  import KnowledgeLakeComponent.MaterialService.*
  import MaterialServiceFactory.*
  override def createSearchMaterialsActionCall(core: ActionCall.Core, action: MaterialSearchQuery): SearchMaterialsActionCall =
    SearchCall(core, action)
  override def createGetMaterialActionCall(core: ActionCall.Core, action: MaterialSelectionQuery): GetMaterialActionCall =
    GetCall(core, action)
  override def createReadOriginalActionCall(core: ActionCall.Core, action: OriginalSelectionQuery): ReadOriginalActionCall =
    ReadCall(core, action)
  override def createCreateMaterialRegistrationActionCall(core: ActionCall.Core,
    action: MaterialRegistrationCreateCommand): CreateMaterialRegistrationActionCall = CreateRegistrationCall(core, action)
  override def createGetMaterialRegistrationActionCall(core: ActionCall.Core,
    action: MaterialRegistrationSelectionQuery): GetMaterialRegistrationActionCall = GetRegistrationCall(core, action)
  override def createMaterializeMaterialActionCall(core: ActionCall.Core,
    action: MaterialRegistrationMaterializeCommand): MaterializeMaterialActionCall = MaterializeCall(core, action)
}

private[knowledgelake] object MaterialServiceFactory {
  import KnowledgeLakeComponent.MaterialService.*

  private def _registration_mode(core: ActionCall.Core): Consequence[Unit] = _mode(core).flatMap {
    case MaterialReferenceRuntime.Mode.Registered => Consequence.unit
    case MaterialReferenceRuntime.Mode.Bootstrap | MaterialReferenceRuntime.Mode.Packages =>
      Consequence.serviceUnavailable("Material registration requires registered mode")
  }
  private def _registration_configuration(core: ActionCall.Core): Consequence[MaterialRegistrationConfiguration] =
    core.component.collect { case runtime: MaterialRegistrationRuntime => runtime }
      .flatMap(_._material_registration_configuration).fold[Consequence[MaterialRegistrationConfiguration]](
        Consequence.serviceUnavailable("Material registration is unavailable"))(Consequence.success)
  private def _registration_subject(core: ActionCall.Core, configuration: MaterialRegistrationConfiguration): Consequence[Unit] =
    if (configuration.allowedsubjects.contains(SecuritySubject.from(core.executionContext.security).subjectId)) Consequence.unit
    else Consequence.securityPermissionDenied("Material registration caller is not admitted")
  private def _registration_result(snapshot: EntitySnapshot[MaterialRegistrationRecord]): OperationResponse =
    OperationResponse.RecordResponse(dto.MaterialRegistrationResult(snapshot.entity.id.value, snapshot.revision.value,
      snapshot.entity.document.status.value, snapshot.entity.document.location.urn).toRecord())

  private final case class CreateRegistrationCall(core: ActionCall.Core, override val action: MaterialRegistrationCreateCommand)
      extends CreateMaterialRegistrationActionCall with MaterialRegistrationSupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      _ <- exec_from(_registration_mode(core))
      configuration <- exec_from(_registration_configuration(core))
      _ <- exec_from(_registration_subject(core, configuration))
      contribution <- exec_from(MaterialContribution.parseC(action.contribution))
      _ <- exec_from(MaterialRegistrationSource._admit(contribution, configuration))
      snapshot <- create_material_registration(contribution, configuration.lake, configuration.logicalparent, configuration.id)
    } yield _registration_result(snapshot)
  }
  private final case class GetRegistrationCall(core: ActionCall.Core, override val action: MaterialRegistrationSelectionQuery)
      extends GetMaterialRegistrationActionCall with MaterialRegistrationSupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      _ <- exec_from(_registration_mode(core))
      query <- exec_from(dto.MaterialRegistrationSelectionQuery.createC(action.request.toRecord))
      identity <- exec_from(EntityId.parse(query.registrationId))
      snapshot <- get_material_registration(identity)
    } yield _registration_result(snapshot)
  }
  private final case class MaterializeCall(core: ActionCall.Core, override val action: MaterialRegistrationMaterializeCommand)
      extends MaterializeMaterialActionCall with MaterialRegistrationSupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      _ <- exec_from(_registration_mode(core))
      query <- exec_from(dto.MaterialRegistrationMaterializeCommand.createC(action.request.toRecord))
      identity <- exec_from(EntityId.parse(query.registrationId))
      loaded <- get_material_registration(identity)
      _ <- exec_from(loaded.entity.document.beginC)
      configuration <- exec_from(_registration_configuration(core))
      _ <- exec_from(_registration_subject(core, configuration))
      document = loaded.entity.document
      _ <- exec_from(if (document.storageBinding == configuration.id && document.location.lake == configuration.lake &&
        document.location.logicalPath.split("/", -1).dropRight(1).mkString("/") == configuration.logicalparent)
          Consequence.unit else Consequence.resourceInvalid("Material registration does not match its configured destination"))
      originals <- exec_from(MaterialRegistrationSource._admit(document.contribution, configuration))
      _ <- exec_from(_connection_configured(core, "Material registration connection is unavailable"))
      credentials <- exec_from(GoogleDriveCredentials.managedConnectionC(configuration.connectionid))
      store <- exec_from(GoogleDriveFileStore.createC(configuration.storename, credentials))
      writer <- exec_from(MaterialPackageWriter.createC(store, configuration.lake, configuration.logicalparent,
        FileStoreRef(store.name, configuration.destinationparentkey)))
      source = MaterialRegistrationSource._create(configuration, store, originals)
      snapshot <- materialize_material_registration(identity, writer, source)
    } yield _registration_result(snapshot)
  }

  private def _mode(core: ActionCall.Core): Consequence[MaterialReferenceRuntime.Mode] = {
    val subject = SecuritySubject.from(core.executionContext.security)
    if (!subject.isProviderAuthenticated) Consequence.securityPermissionDenied("Material caller authentication is required")
    else core.component.collect { case runtime: MaterialReferenceRuntime => runtime }
      .fold[Consequence[MaterialReferenceRuntime.Mode]](Consequence.serviceUnavailable("Material reference runtime is unavailable"))(
        runtime => Consequence.success(runtime._material_reference_mode))
  }
  private def _catalog(core: ActionCall.Core): Consequence[(String, MaterialCatalog)] = {
    val subject = SecuritySubject.from(core.executionContext.security)
    if (!subject.isProviderAuthenticated) Consequence.securityPermissionDenied("Material caller authentication is required")
    else core.component.collect { case runtime: MaterialReferenceRuntime => runtime }.flatMap(_.materialCatalog)
      .fold[Consequence[(String, MaterialCatalog)]](Consequence.serviceUnavailable("Material reference catalog is unavailable"))(
        catalog => Consequence.success(subject.subjectId -> catalog))
  }
  private def _selector(value: String, maximum: Int): Consequence[Unit] =
    if (MaterialCatalog.validText(value, maximum)) Consequence.unit
    else Consequence.argumentInvalid("Material selection is invalid")

  private def _packages(core: ActionCall.Core): Consequence[(String, MaterialPackageReadCatalog)] =
    core.component.collect { case runtime: MaterialReferenceRuntime => runtime }.flatMap(_._material_package_catalog)
      .fold[Consequence[(String, MaterialPackageReadCatalog)]](
        Consequence.serviceUnavailable("Material package read configuration is unavailable"))(
        catalog => Consequence.success(SecuritySubject.from(core.executionContext.security).subjectId -> catalog))

  private def _package_snapshot(core: ActionCall.Core,
    binding: MaterialPackageReadCatalog.Binding): ExecUowM[MaterialPackageReader.Snapshot] = for {
    _ <- _package_pure(_connection_configured(core, "Material package connection is unavailable"))
    credentials <- _package_pure(GoogleDriveCredentials.managedConnectionC(binding.connectionid))
    store <- _package_pure(GoogleDriveFileStore.createC(binding.storename, credentials))
    snapshot <- MaterialPackageReader._read(binding, store)
  } yield snapshot

  private def _package_search(core: ActionCall.Core, query: dto.MaterialSearchQuery): ExecUowM[Record] = for {
    admitted <- _package_pure(_packages(core))
    snapshots <- admitted._2.authorized(admitted._1).foldLeft(
      _package_pure(Consequence.success(Vector.empty[MaterialPackageReader.Snapshot]))) { (previous, binding) => for {
        accumulated <- previous
        snapshot <- _package_snapshot(core, binding)
      } yield accumulated :+ snapshot
    }
  } yield {
    val needle = query.text.getOrElse("").toLowerCase(Locale.ROOT)
    val matches = snapshots.filter(value => value.title.toLowerCase(Locale.ROOT).contains(needle) ||
      value.materialref.toLowerCase(Locale.ROOT).contains(needle)).sortBy(_.materialref)
    val offset = query.offset.getOrElse(0)
    val limit = query.limit.getOrElse(20)
    Record.data("items" -> matches.slice(offset, (offset.toLong + limit).min(Int.MaxValue).toInt).map(_.summary),
      "offset" -> offset, "limit" -> limit, "hasMore" -> (offset.toLong + limit < matches.size.toLong))
  }

  private def _package_get(core: ActionCall.Core, reference: String): ExecUowM[MaterialPackageReader.Snapshot] = for {
    admitted <- _package_pure(_packages(core))
    binding <- _package_pure(admitted._2.resolve(admitted._1, reference))
    snapshot <- _package_snapshot(core, binding)
  } yield snapshot

  private def _package_pure[A](value: Consequence[A]): ExecUowM[A] = ConsequenceT.fromConsequence(value)

  private def _registered_metadata(record: MaterialRegistrationRecord): Record = {
    val document = record.document
    Record.data("materialRef" -> document.location.urn, "title" -> document.contribution.title,
      "resources" -> document.contribution.originals.map(original => Record.data("resourceId" -> original.resourceId,
        "role" -> original.role, "mediaType" -> original.mediaType, "byteLength" -> original.byteLength)),
      "provenance" -> Vector(Record.data("kind" -> "registration", "reference" -> record.id.value)))
  }

  private final case class SearchCall(core: ActionCall.Core, override val action: MaterialSearchQuery)
      extends SearchMaterialsActionCall with MaterialRegistrationSupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      query <- exec_from(dto.MaterialSearchQuery.createC(action.request.toRecord))
      _ <- exec_from(if (query.text.forall(text => text.length <= 256 && !text.exists(Character.isISOControl)) &&
          query.offset.forall(_ >= 0) && query.limit.forall(limit => limit >= 1 && limit <= 100)) Consequence.unit
        else Consequence.argumentInvalid("Material search parameters are invalid"))
      mode <- exec_from(_mode(core))
      result <- mode match {
        case MaterialReferenceRuntime.Mode.Bootstrap => exec_from(_catalog(core)).map(admitted =>
          admitted._2.search(admitted._1, query.text, query.offset.getOrElse(0), query.limit.getOrElse(20)))
        case MaterialReferenceRuntime.Mode.Registered =>
          search_registered_materials(query.text, query.offset.getOrElse(0), query.limit.getOrElse(20))
        case MaterialReferenceRuntime.Mode.Packages => _package_search(core, query)
      }
    } yield OperationResponse.RecordResponse(result)
  }
  private final case class GetCall(core: ActionCall.Core, override val action: MaterialSelectionQuery)
      extends GetMaterialActionCall with MaterialRegistrationSupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      query <- exec_from(dto.MaterialSelectionQuery.createC(action.request.toRecord))
      _ <- exec_from(_selector(query.materialRef, 2048))
      mode <- exec_from(_mode(core))
      result <- mode match {
        case MaterialReferenceRuntime.Mode.Bootstrap => for {
          admitted <- exec_from(_catalog(core))
          entry <- exec_from(admitted._2.resolve(admitted._1, query.materialRef))
        } yield entry.metadata
        case MaterialReferenceRuntime.Mode.Registered => get_registered_material(query.materialRef).map(_registered_metadata)
        case MaterialReferenceRuntime.Mode.Packages => _package_get(core, query.materialRef).map(_.metadata)
      }
    } yield OperationResponse.RecordResponse(result)
  }
  private final case class ReadCall(core: ActionCall.Core, override val action: OriginalSelectionQuery)
      extends ReadOriginalActionCall with MaterialRegistrationSupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      query <- exec_from(dto.OriginalSelectionQuery.createC(action.request.toRecord))
      _ <- exec_from(_selector(query.materialRef, 2048))
      _ <- exec_from(_selector(query.resourceId, 256))
      mode <- exec_from(_mode(core))
      result <- mode match {
        case MaterialReferenceRuntime.Mode.Bootstrap => _read_bootstrap(query)
        case MaterialReferenceRuntime.Mode.Registered => _read_registered(query)
        case MaterialReferenceRuntime.Mode.Packages => _read_package(query)
      }
    } yield result

    private def _read_bootstrap(query: dto.OriginalSelectionQuery): ExecUowM[OperationResponse] = for {
      admitted <- exec_from(_catalog(core))
      entry <- exec_from(admitted._2.resolve(admitted._1, query.materialRef))
      resource <- exec_from(entry.resources.find(_.resourceid == query.resourceId)
        .fold[Consequence[MaterialResourceBinding]](Consequence.resourceNotFound("Material resource was not found"))(Consequence.success))
      result <- _read_resource(resource, "knowledge-lake-originals")
    } yield result

    private def _read_registered(query: dto.OriginalSelectionQuery): ExecUowM[OperationResponse] = for {
      record <- get_registered_material(query.materialRef)
      document = record.document
      index <- exec_from(Option(document.contribution.originals.indexWhere(_.resourceId == query.resourceId)).filter(_ >= 0)
        .fold[Consequence[Int]](Consequence.resourceNotFound("Material resource was not found"))(Consequence.success))
      binding <- exec_from(core.component.collect { case runtime: MaterialReferenceRuntime => runtime }
        .flatMap(_._material_read_binding(document.storageBinding))
        .fold[Consequence[MaterialReferenceRuntime.ReadBinding]](
          Consequence.serviceUnavailable("Registered Material original access is unavailable"))(Consequence.success))
      stored <- exec_from(document.stored.fold[Consequence[MaterialRegistrationDocument.Stored]](_invalid)(Consequence.success))
      _ <- exec_from(if ((Vector(stored.root, stored.manifest) ++ stored.files.flatMap(file => Vector(file.reference, file.parent)))
        .forall(_.store == binding.storename)) Consequence.unit else _invalid)
      file <- exec_from(stored.files.lift(index).fold[Consequence[MaterialRegistrationDocument.File]](_invalid)(Consequence.success))
      original = document.contribution.originals(index)
      resource = MaterialResourceBinding(original.resourceId, original.mediaType, Some(original.byteLength),
        MaterialStorage(binding.connectionid, file.reference.key, file.parent.key))
      result <- _read_resource(resource, binding.storename)
    } yield result

    private def _read_package(query: dto.OriginalSelectionQuery): ExecUowM[OperationResponse] = for {
      _ <- exec_from(if (query.resourceId.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,127}")) Consequence.unit
        else Consequence.argumentInvalid("Material selection is invalid"))
      snapshot <- _package_get(core, query.materialRef)
      original <- exec_from(snapshot.originals.find(_.resourceid == query.resourceId)
        .fold[Consequence[MaterialPackageReader.Original]](
          Consequence.resourceNotFound("Material resource was not found"))(Consequence.success))
      result <- _read_resource(original.binding, original.reference.store)
    } yield result

    private def _read_resource(resource: MaterialResourceBinding, storename: String): ExecUowM[OperationResponse] = for {
      _ <- exec_from(_connection_configured(core, "Material original connection is unavailable"))
      credentials <- exec_from(GoogleDriveCredentials.managedConnectionC(resource.storage.connectionid))
      store <- exec_from(GoogleDriveFileStore.createC(storename, credentials))
      reference = FileStoreRef(store.name, resource.storage.resourcekey)
      metadata <- store.stat(reference)
      _ <- exec_from(if (metadata.kind == FileStoreKind.File &&
          metadata.parents.contains(FileStoreRef(store.name, resource.storage.parentkey)) &&
          metadata.mediaType.value == resource.mediatype && resource.bytelength.forall(value => metadata.byteLength.contains(value)))
        Consequence.unit else _invalid)
      content <- store.read(reference)
      binary <- exec_from(if (content.mediaType.value == resource.mediatype &&
          resource.bytelength.forall(value => content.content.metadata.size.contains(value))) content.content match {
        case bag: BinaryBag => Consequence.success(bag)
        case _ => _invalid
      } else _invalid)
    } yield OperationResponse.Http(HttpResponse.Binary(HttpStatus.Ok, ContentType(MimeType(resource.mediatype), None), binary,
      Record.data("Cache-Control" -> "private, no-store", "X-Content-Type-Options" -> "nosniff",
        "Content-Security-Policy" -> "default-src 'none'; sandbox", "Content-Disposition" -> "attachment; filename=\"original\"")))
  }
  private def _connection_configured(core: ActionCall.Core, message: String): Consequence[Unit] =
    core.component match {
      case Some(runtime: GoogleDriveConnectionRuntime) if runtime.hasGoogleDriveConfiguration => Consequence.unit
      case _ => Consequence.serviceUnavailable(message)
    }

  private def _invalid[A]: Consequence[A] = Consequence.resourceInvalid("Material original does not match its configured resource")
}
