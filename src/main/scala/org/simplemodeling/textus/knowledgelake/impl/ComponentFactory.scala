package org.simplemodeling.textus.knowledgelake.impl

import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentDescriptor, ComponentId}
import org.goldenport.cncf.config.ComponentParameterKey
import org.goldenport.cncf.filestore.GoogleDriveConnectionRuntime
import org.goldenport.cncf.oauth.OAuthConnectionRuntime
import org.simplemodeling.textus.knowledgelake.KnowledgeLakeComponent
import org.simplemodeling.textus.knowledgelake.material.{MaterialReferenceRuntime, MaterialRegistrationConfiguration, MaterialRegistrationRuntime}

/*
 * @since   Oct. 21, 2026
 * @version Oct. 25, 2026
 */
final class ComponentFactory extends Component.BundleFactory {
  override def primaryFactory: Component.PrimaryComponentFactory = KnowledgeLakePrimaryFactory
  override def componentletFactories: Vector[Component.ComponentletFactory] = Vector.empty
}

abstract class KnowledgeLakeParticipantFactoryBase extends KnowledgeLakeComponent.Factory {
  override def initializationParameterDeclarations: Vector[ComponentParameterKey[?]] =
    super.initializationParameterDeclarations ++ Vector(OAuthConnectionRuntime.configurationKey,
      GoogleDriveConnectionRuntime.configurationKey,
      MaterialReferenceRuntime.modeKey, MaterialReferenceRuntime.catalogKey, MaterialReferenceRuntime.registeredBindingsKey,
      MaterialReferenceRuntime.packagesKey,
      MaterialRegistrationConfiguration.configurationKey)

  protected final def component_core(name: String, componentId: ComponentId): Component.Core =
    spec_create(name, componentId, Vector(KnowledgeLakeComponent.MaterialService))

  override val Material: KnowledgeLakeComponent.MaterialServiceFactory = new MaterialServiceFactory
}

class KnowledgeLakePrimaryComponent extends KnowledgeLakeComponent with GoogleDriveConnectionRuntime with MaterialReferenceRuntime with MaterialRegistrationRuntime {
  override def componentDescriptors: Vector[ComponentDescriptor] = super.componentDescriptors :+
    ComponentDescriptor(name = Some(KnowledgeLakeComponent.componentId.name),
      componentName = Some(KnowledgeLakeComponent.componentId.name), schemaVersion = Some(3),
      componentId = Some(KnowledgeLakeComponent.componentId))
}

object KnowledgeLakePrimaryFactory extends KnowledgeLakeParticipantFactoryBase with Component.PrimaryComponentFactory {
  override protected def create_Component(params: ComponentCreate): Component = new KnowledgeLakePrimaryComponent
  override protected def create_Core(params: ComponentCreate, comp: Component): Component.Core =
    component_core(KnowledgeLakeComponent.name, KnowledgeLakeComponent.componentId)
}
