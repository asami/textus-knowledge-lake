package org.simplemodeling.textus.knowledgelake.material

import org.goldenport.Consequence
import org.goldenport.cncf.component.{Component, ComponentInit}

/** Passive opt-in configuration; malformed selected trust fails initialization.
  * @since   Nov.  6, 2026
  * @version Nov.  6, 2026
  */
private[knowledgelake] trait MaterialParticipantRuntime extends Component {
  private var _participant_configuration: Option[MaterialParticipantConfiguration] = None
  private[knowledgelake] final def _material_participant_configuration: Option[MaterialParticipantConfiguration] = _participant_configuration
  override protected def initialize_component_c(params: ComponentInit): Consequence[Unit] = for {
    _ <- super.initialize_component_c(params)
    selected <- initializationParameters.resolve(MaterialParticipantConfiguration.configurationKey)
    configuration <- selected.value match {
      case None => Consequence.success(Option.empty[MaterialParticipantConfiguration])
      case Some(text) => MaterialParticipantConfiguration.parseC(text).map(Some(_))
    }
  } yield { _participant_configuration = configuration }
}
