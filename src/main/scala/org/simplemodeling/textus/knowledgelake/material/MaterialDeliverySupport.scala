package org.simplemodeling.textus.knowledgelake.material

import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.cncf.action.FunctionalActionCall
import org.goldenport.cncf.entity.{EntityPersistent, EntityPersistentCreate, EntitySnapshot, EntityQuery, EntitySearchScope, EntityVisibilityScope}
import org.goldenport.cncf.filestore.*
import org.goldenport.cncf.security.SecuritySubject
import org.goldenport.cncf.directive.Query
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.observation.Descriptor
import org.goldenport.record.Record
import org.simplemodeling.model.datatype.EntityId

/* Native delivery persistence and passive projections.
 * @since   Oct. 25, 2026
 * @version Nov.  6, 2026
 */
private[knowledgelake] trait MaterialDeliverySupport extends FunctionalActionCall {
  protected final def delivery_subject(): Consequence[String] = MaterialParticipantAdmission.forComponentC(
    core.component, execution_context.security).flatMap { _ =>
    val subject = SecuritySubject.from(execution_context.security)
    if (subject.isProviderAuthenticated) Consequence.success(subject.subjectId)
    else Consequence.securityAuthenticationRequired("Material delivery requires provider authentication")
  }
  protected final def delivery_configuration(): Consequence[Option[MaterialDeliveryConfiguration]] =
    core.component.collect { case runtime: MaterialDeliveryRuntime => runtime }
      .fold[Consequence[Option[MaterialDeliveryConfiguration]]](Consequence.serviceUnavailable("Material delivery runtime is unavailable"))(
        runtime => Consequence.success(runtime._material_delivery_configuration))

  protected final def delivery_destination(): ExecUowM[Record] = for {
    subject <- exec_from(delivery_subject())
    configuration <- exec_from(delivery_configuration())
    result <- exec_from(configuration match {
      case None => Consequence.success(Record.data("availability" -> "configurationRequired"))
      case Some(value) if !value.allowedSubjects.contains(subject) =>
        Consequence.securityPermissionDenied("Material delivery caller is not admitted")
      case Some(value) =>
        val available = core.component.exists {
          case runtime: GoogleDriveConnectionRuntime => runtime.hasGoogleDriveConfiguration
          case _ => false
        }
        if (!available) Consequence.success(Record.data("destinationId" -> value.id, "label" -> value.label,
          "availability" -> "unavailable", "maxResourceBytes" -> value.maxResourceBytes))
        else delivery_provider(value).map { store =>
          val route = MaterialDeliveryRoute.select(store, value.preferDirect)
          val base = Record.data("destinationId" -> value.id, "label" -> value.label,
            "availability" -> "available", "transferMethod" -> route.method, "maxResourceBytes" -> value.maxResourceBytes,
            "contentIntegrity" -> "sha256")
          route.reason.fold(base)(reason => base ++ Record.data("transferReason" -> reason))
        }
    })
  } yield result

  protected final def prepare_delivery(requestId: String, contribution: io.circe.Json): ExecUowM[MaterialDeliveryRecord] = for {
    subject <- exec_from(delivery_subject())
    identity <- exec_from(MaterialDeliveryEntity.correlationC(subject, requestId))
    admitted <- exec_from(MaterialContribution.parseC(contribution))
    existing <- find_delivery(requestId)
    result <- existing match {
      case Some(record) => exec_from(if (record.document.registration.contribution.json == admitted.json)
        Consequence.success(record) else delivery_conflict("prepare", "request-contribution-conflict"))
      case None => _claim_new_delivery(subject, identity, requestId, admitted)
    }
  } yield result

  private def _claim_new_delivery(subject: String, identity: EntityId, requestid: String,
    admitted: MaterialContribution): ExecUowM[MaterialDeliveryRecord] = {
    given EntityPersistentCreate[MaterialDeliveryDraft] = MaterialDeliveryEntity.createPersistent
    given EntityPersistent[MaterialDeliveryRecord] = MaterialDeliveryEntity.persistent
    for {
      selected <- exec_from(delivery_configuration())
      configuration <- exec_from(selected.fold[Consequence[MaterialDeliveryConfiguration]](
        Consequence.serviceUnavailable("Material delivery configuration is required"))(Consequence.success))
      _ <- exec_from(if (configuration.allowedSubjects.contains(subject)) Consequence.unit
        else Consequence.securityPermissionDenied("Material delivery caller is not admitted"))
      _ <- exec_from(if (admitted.originals.forall(_.byteLength <= configuration.maxResourceBytes)) Consequence.unit
        else Consequence.argumentInvalid("Material delivery original exceeds the configured limit"))
      leaf = opaque_id("knowledge-lake.mobile-material")
      location <- exec_from(MaterialLocation.createC(configuration.lake,
        if (configuration.logicalParent.isEmpty) leaf else configuration.logicalParent + "/" + leaf))
      registration <- exec_from(MaterialRegistrationDocument.preparedC(admitted, location, configuration.id,
        org.goldenport.cncf.context.PrincipalId(subject), org.goldenport.cncf.context.PrincipalId(subject)))
      registrationid = entity_id(MaterialRegistrationEntity.collectionId, "knowledge-lake.mobile-registration")
      provider <- exec_from(delivery_provider(configuration))
      route = MaterialDeliveryRoute.select(provider, configuration.preferDirect)
      _ <- exec_from(if ((admitted.originals.forall(_.contentDigest.isDefined) || admitted.originals.forall(_.contentDigest.isEmpty)) &&
        (route.method != "googleDriveResumable" || admitted.originals.forall(_.contentDigest.isDefined))) Consequence.unit
        else Consequence.argumentInvalid("Material delivery requires complete original content digests"))
      document = MaterialDeliveryDocument(requestid, configuration, registrationid, registration,
        transferMethod = route.method, transferReason = route.reason,
        verifications = admitted.originals.filter(_.contentDigest.isDefined).map(value => MaterialDeliveryVerification(value.resourceId)))
      _ <- use_component_application_datastore()
      claimed <- entity_claim_or_load[MaterialDeliveryDraft, MaterialDeliveryRecord](MaterialDeliveryDraft(identity, document))
      loaded <- load_delivery(claimed.id)
      _ <- exec_from(if (loaded.entity.document.registration.contribution.json == admitted.json) Consequence.unit
        else delivery_conflict("prepare", "request-contribution-conflict"))
    } yield loaded.entity
  }

  protected final def load_delivery(identity: EntityId): ExecUowM[EntitySnapshot[MaterialDeliveryRecord]] = {
    given EntityPersistent[MaterialDeliveryRecord] = MaterialDeliveryEntity.persistent
    for {
      subject <- exec_from(delivery_subject())
      _ <- exec_from(if (identity.collection == MaterialDeliveryEntity.collectionId) Consequence.unit
        else Consequence.argumentInvalid("Invalid Material delivery reference"))
      _ <- use_component_application_datastore()
      snapshot <- entity_load_snapshot[MaterialDeliveryRecord](identity)
      _ <- exec_from(if (snapshot.entity.document.registration.owner.value == subject) Consequence.unit
        else Consequence.securityPermissionDenied("Material delivery caller is not admitted"))
    } yield snapshot
  }

  protected final def find_delivery(requestId: String): ExecUowM[Option[MaterialDeliveryRecord]] = {
    given EntityPersistent[MaterialDeliveryRecord] = MaterialDeliveryEntity.persistent
    for {
      subject <- exec_from(delivery_subject())
      identity <- exec_from(MaterialDeliveryEntity.correlationC(subject, requestId))
      _ <- use_component_application_datastore()
      matches <- entity_search(EntityQuery[MaterialDeliveryRecord](MaterialDeliveryEntity.collectionId,
        Query.plan(Record.empty, where = Query.Eq("id", identity.value)), scope = EntitySearchScope.Store,
        visibilityScope = Some(EntityVisibilityScope.Owner)))
      found <- exec_from(if (matches.data.size <= 1) Consequence.success(matches.data.headOption)
        else Consequence.resourceInvalid("Material delivery correlation is ambiguous"))
      _ <- exec_from(if (found.forall(_.document.registration.owner.value == subject)) Consequence.unit
        else Consequence.securityPermissionDenied("Material delivery caller is not admitted"))
    } yield found
  }

  protected final def save_delivery(snapshot: EntitySnapshot[MaterialDeliveryRecord], document: MaterialDeliveryDocument): ExecUowM[EntitySnapshot[MaterialDeliveryRecord]] = {
    given EntityPersistent[MaterialDeliveryRecord] = MaterialDeliveryEntity.persistent
    entity_update(snapshot.entity.copy(document = document), snapshot.revision)
  }

  protected final def delivery_provider(configuration: MaterialDeliveryConfiguration): Consequence[FileStore] = for {
    credentials <- GoogleDriveCredentials.managedConnectionC(configuration.connectionId)
    store <- GoogleDriveFileStore.createC(configuration.storeName, credentials)
  } yield store

  protected final def completed_delivery_registration(document: MaterialDeliveryDocument): Consequence[MaterialRegistrationDocument] = for {
    begun <- document.registration.beginC
    receipt <- document.packageC
    completed <- begun.completeC(receipt)
  } yield completed

  protected final def delivery_projection(record: MaterialDeliveryRecord): ExecUowM[Record] = {
    given EntityPersistent[MaterialRegistrationRecord] = MaterialRegistrationEntity.persistent
    val document = record.document
    for {
      registration <- if (document.manifest.isDefined) entity_search(MaterialRegistrationEntity.referenceQuery(document.registration.location.urn)).flatMap { matches =>
        if (matches.data.size <= 1 && matches.data.forall(_.id == document.registrationId)) exec_from(Consequence.success(matches.data.headOption))
        else exec_from(Consequence.resourceInvalid("Material delivery registration is ambiguous"))
      } else exec_from(Consequence.success(Option.empty[MaterialRegistrationRecord]))
      registered <- registration match {
        case None => exec_from(Consequence.success(false))
        case Some(value) => exec_from(completed_delivery_registration(document).flatMap(expected =>
          if (value.document.json == expected.json) Consequence.success(true)
          else Consequence.resourceInvalid("Material delivery registration does not match")))
      }
    } yield {
      val failed = document.verifications.find(_.state == MaterialDeliveryVerificationState.Failed)
      val verifying = document.registration.contribution.originals.exists(value => value.contentDigest.isDefined &&
        document.originalUploaded(value.resourceId) && document.verification(value.resourceId).state != MaterialDeliveryVerificationState.Verified)
      val state = if (registered) "registered" else if (document.integrityUpgradeRequired || document.busy.isDefined) "reconciliationRequired"
        else if (failed.isDefined) "verificationFailed" else if (verifying) "verifying"
        else if (document.originalsComplete) "readyToRegister" else if (document.files.nonEmpty) "transferring" else "prepared"
      val resources = document.registration.contribution.originals.map { value =>
        val progress = Record.data(
        "resourceId" -> value.resourceId, "mediaType" -> value.mediaType, "byteLength" -> value.byteLength,
        "state" -> (if (document.originalUploaded(value.resourceId)) "uploaded"
          else if (document.busy.contains("original:" + value.resourceId)) "reconciliationRequired" else "pending"))
        value.contentDigest.fold(progress) { digest =>
          val check = document.verification(value.resourceId)
          val checked = progress ++ Record.data("contentDigest" -> Record.data("algorithm" -> digest.algorithm, "value" -> digest.value),
            "verificationState" -> check.state.value, "verificationAttempts" -> check.attempts)
          check.reason.fold(checked)(reason => checked ++ Record.data("verificationReason" -> reason))
        }
      }
      val base = Record.data("deliveryRef" -> record.id.value, "requestId" -> document.requestId,
        "revision" -> record.revision.value, "destinationId" -> document.configuration.id,
        "destinationLabel" -> document.configuration.label, "state" -> state, "resources" -> resources,
        "transferMethod" -> document.transferMethod)
      val routed = document.transferReason.fold(base)(reason => base ++ Record.data("transferReason" -> reason))
      if (registered) routed ++ Record.data("materialRef" -> document.registration.location.urn)
      else if (document.integrityUpgradeRequired) routed ++ Record.data("actionRequired" -> "integrityUpgradeRequired")
      else if (document.busy.isDefined) routed ++ Record.data("actionRequired" -> "operatorReconciliationRequired")
      else if (failed.isDefined) routed ++ Record.data("actionRequired" -> failed.get.reason.get)
      else routed
    }
  }

  protected final def delivery_conflict[A](operation: String, reason: String): Consequence[A] =
    Consequence.operationConflict(operation, Vector(Descriptor.Facet.Reason(reason)))
}
