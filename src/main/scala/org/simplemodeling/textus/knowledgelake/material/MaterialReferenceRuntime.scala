package org.simplemodeling.textus.knowledgelake.material

import org.goldenport.Consequence
import org.goldenport.cncf.component.{Component, ComponentInit}
import org.goldenport.cncf.config.ComponentParameterKey
import io.circe.Json
import io.circe.jawn.JawnParser

/*
 * @since   Oct. 21, 2026
 * @version Oct. 25, 2026
 */
private[knowledgelake] object MaterialReferenceRuntime {
  enum Mode {
    case Bootstrap, Registered, Packages
  }
  val modeKey: ComponentParameterKey[String] = ComponentParameterKey.optionalString("material.reference.mode")
  val catalogKey: ComponentParameterKey[String] = ComponentParameterKey.optionalString("material.reference.catalog")
  val registeredBindingsKey: ComponentParameterKey[String] = ComponentParameterKey.optionalString("material.reference.registered.bindings")
  val packagesKey: ComponentParameterKey[String] = ComponentParameterKey.optionalString("material.reference.packages")

  final case class ReadBinding(connectionid: String, storename: String) {
    override def toString: String = "MaterialReadBinding(<redacted>)"
  }
  final class ReadBindings private[MaterialReferenceRuntime] (private val _entries: Map[String, ReadBinding]) {
    private[knowledgelake] def _lookup(id: String): Option[ReadBinding] = _entries.get(id)
    override def toString: String = "MaterialReadBindings(<redacted>)"
  }
  private val _parser = new JawnParser(Some(131072), false)
  private val _empty_bindings = new ReadBindings(Map.empty)
  private def _token(json: Json): Option[String] =
    json.asString.filter(_.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))
  private[knowledgelake] def _parse_bindings(text: String): Consequence[ReadBindings] = {
    val admitted = for {
      source <- Option(text).filter(_.length <= 131072)
      json <- _parser.parse(source).toOption
      root <- json.asObject.filter(_.keys.toSet == Set("schema", "bindings"))
      _ <- root("schema").filter(_ == Json.fromString("textus.knowledge-lake.material-read-bindings/1"))
      values <- root("bindings").flatMap(_.asArray)
      entries <- MaterialContribution._sequence(values.map { value => for {
        fields <- value.asObject.filter(_.keys.toSet == Set("id", "provider", "connectionId", "storeName"))
        _ <- fields("provider").filter(_ == Json.fromString("google-drive"))
        id <- fields("id").flatMap(_token)
        connection <- fields("connectionId").flatMap(_token)
        store <- fields("storeName").flatMap(_token)
      } yield id -> ReadBinding(connection, store) })
      _ <- Option.when(entries.map(_._1).distinct.size == entries.size)(())
    } yield new ReadBindings(entries.toMap)
    admitted.fold[Consequence[ReadBindings]](
      Consequence.configurationInvalid("Material registered read binding configuration is invalid"))(Consequence.success)
  }
}

/** One explicitly admitted reference mode and optional catalog per initialization. */
private[knowledgelake] trait MaterialReferenceRuntime extends Component {
  private var _reference_mode: MaterialReferenceRuntime.Mode = MaterialReferenceRuntime.Mode.Bootstrap
  private var _material_catalog: Option[MaterialCatalog] = None
  private var _registered_bindings: MaterialReferenceRuntime.ReadBindings = MaterialReferenceRuntime._empty_bindings
  private var _package_catalog: Option[MaterialPackageReadCatalog] = None
  private[knowledgelake] final def _material_reference_mode: MaterialReferenceRuntime.Mode = _reference_mode
  private[knowledgelake] final def materialCatalog: Option[MaterialCatalog] = _material_catalog
  private[knowledgelake] final def _material_read_binding(id: String): Option[MaterialReferenceRuntime.ReadBinding] =
    _registered_bindings._lookup(id)
  private[knowledgelake] final def _material_package_catalog: Option[MaterialPackageReadCatalog] = _package_catalog

  override protected def initialize_component_c(params: ComponentInit): Consequence[Unit] = for {
    _ <- super.initialize_component_c(params)
    selected <- initializationParameters.resolve(MaterialReferenceRuntime.modeKey)
    mode <- selected.value match {
      case None | Some("bootstrap") => Consequence.success(MaterialReferenceRuntime.Mode.Bootstrap)
      case Some("registered") => Consequence.success(MaterialReferenceRuntime.Mode.Registered)
      case Some("packages") => Consequence.success(MaterialReferenceRuntime.Mode.Packages)
      case _ => Consequence.configurationInvalid("Material reference mode configuration is invalid")
    }
    resolution <- initializationParameters.resolve(MaterialReferenceRuntime.catalogKey)
    catalog <- mode match {
      case MaterialReferenceRuntime.Mode.Bootstrap => resolution.value match {
        case None => Consequence.success(Option.empty[MaterialCatalog])
        case Some(text) => MaterialCatalog.parseC(text).map(Some(_))
      }
      case MaterialReferenceRuntime.Mode.Registered =>
        if (resolution.value.isEmpty) Consequence.success(Option.empty[MaterialCatalog])
        else Consequence.configurationInvalid("Registered Material reference mode cannot include a bootstrap catalog")
      case MaterialReferenceRuntime.Mode.Packages =>
        if (resolution.value.isEmpty) Consequence.success(Option.empty[MaterialCatalog])
        else Consequence.configurationInvalid("Packages Material reference mode cannot include a bootstrap catalog")
    }
    bindingresolution <- initializationParameters.resolve(MaterialReferenceRuntime.registeredBindingsKey)
    bindings <- mode match {
      case MaterialReferenceRuntime.Mode.Bootstrap =>
        if (bindingresolution.value.isEmpty) Consequence.success(MaterialReferenceRuntime._empty_bindings)
        else Consequence.configurationInvalid("Bootstrap Material reference mode cannot include registered read bindings")
      case MaterialReferenceRuntime.Mode.Registered => bindingresolution.value match {
        case None => Consequence.success(MaterialReferenceRuntime._empty_bindings)
        case Some(text) => MaterialReferenceRuntime._parse_bindings(text)
      }
      case MaterialReferenceRuntime.Mode.Packages =>
        if (bindingresolution.value.isEmpty) Consequence.success(MaterialReferenceRuntime._empty_bindings)
        else Consequence.configurationInvalid("Packages Material reference mode cannot include registered read bindings")
    }
    packageresolution <- initializationParameters.resolve(MaterialReferenceRuntime.packagesKey)
    packages <- mode match {
      case MaterialReferenceRuntime.Mode.Packages => packageresolution.value match {
        case None => Consequence.success(Option.empty[MaterialPackageReadCatalog])
        case Some(text) => MaterialPackageReadCatalog.parseC(text).map(Some(_))
      }
      case MaterialReferenceRuntime.Mode.Bootstrap | MaterialReferenceRuntime.Mode.Registered =>
        if (packageresolution.value.isEmpty) Consequence.success(Option.empty[MaterialPackageReadCatalog])
        else Consequence.configurationInvalid("Material package read configuration is invalid")
    }
  } yield {
    _reference_mode = mode
    _material_catalog = catalog
    _registered_bindings = bindings
    _package_catalog = packages
  }
}
