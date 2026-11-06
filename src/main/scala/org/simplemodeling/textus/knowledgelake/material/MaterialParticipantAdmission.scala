package org.simplemodeling.textus.knowledgelake.material

import org.goldenport.Consequence
import org.goldenport.cncf.component.Component
import org.goldenport.cncf.context.{SecurityContext, SubjectKind}
import org.goldenport.cncf.security.SecuritySubject

/** Participant admission precedes storage effects; existing owner/Entity guards still apply.
  * @since   Nov.  6, 2026
  * @version Nov.  6, 2026
  */
private[knowledgelake] object MaterialParticipantAdmission {
  def forComponentC(component: Option[Component], security: SecurityContext): Consequence[Unit] =
    admitC(component.collect { case runtime: MaterialParticipantRuntime => runtime }
      .flatMap(_._material_participant_configuration), security)

  def admitC(configuration: Option[MaterialParticipantConfiguration], security: SecurityContext): Consequence[Unit] =
    configuration match {
      case None => Consequence.unit // Legacy admission remains with its existing boundary owners.
      case Some(profile) =>
        val subject = SecuritySubject.from(security)
        val session = security.session.exists(value => value.sessionId.exists(_.nonEmpty) &&
          value.tokenKind.exists(_.equalsIgnoreCase("access")))
        val bearer = security.credentialTransports.exists(value => value.bearerHeader && !value.sessionHeader && !value.sessionCookie)
        if (subject.isProviderAuthenticated && security.authenticatedProvider.contains(profile.provider) &&
            security.subjectKind == SubjectKind.User && session && bearer) Consequence.unit
        else Consequence.securityPermissionDenied("Material participant is not admitted")
    }
}
