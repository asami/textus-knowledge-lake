package org.simplemodeling.textus.knowledgelake.material

import cats.syntax.all.*
import org.goldenport.{Consequence, ConsequenceT}
import org.goldenport.cncf.filestore.{FileStoreKind, FileStoreRef, GoogleDriveFileStore}
import org.goldenport.cncf.unitofwork.{ExecProgram, ExecUowM}

/*
 * A capability for exactly the already admitted originals under one configured parent.
 * Input receipt extras remain provenance, never credential or destination authority.
 *
 * @since   Oct. 22, 2026
 * @version Oct. 22, 2026
 */
private[knowledgelake] final class MaterialRegistrationSource private (
  private val _configuration: MaterialRegistrationConfiguration,
  private val _store: GoogleDriveFileStore,
  private val _originals: Vector[(MaterialOriginalContribution, String)]
) extends MaterialOriginalSource {
  def read(original: MaterialOriginalContribution): ExecUowM[MaterialOriginalContent] = for {
    remoteid <- MaterialRegistrationSource._lift(_originals.find(_._1 == original)
      .fold[Consequence[String]](MaterialRegistrationSource._invalid)(entry => Consequence.success(entry._2)))
    reference = FileStoreRef(_store.name, remoteid)
    metadata <- _store.stat(reference)
    _ <- MaterialRegistrationSource._lift(if (metadata.kind == FileStoreKind.File &&
      metadata.parents.contains(FileStoreRef(_store.name, _configuration.sourceparentkey)) &&
      metadata.mediaType.value == original.mediaType && metadata.byteLength.contains(original.byteLength))
        Consequence.unit else MaterialRegistrationSource._invalid)
    content <- _store.read(reference)
    _ <- MaterialRegistrationSource._lift(if (content.mediaType.value == original.mediaType &&
      content.content.metadata.size.contains(original.byteLength)) Consequence.unit else MaterialRegistrationSource._invalid)
  } yield MaterialOriginalContent(original, content.content)

  override def toString: String = "MaterialRegistrationSource(<admitted>)"
}

private[knowledgelake] object MaterialRegistrationSource {
  private[knowledgelake] def _admit(contribution: MaterialContribution,
    configuration: MaterialRegistrationConfiguration): Consequence[Vector[(MaterialOriginalContribution, String)]] = {
    val admitted = Option.when(configuration.sourceformat == "nict-book-material-transfer/1")(()).flatMap { _ =>
      MaterialContribution._sequence(contribution.originals.map { original => for {
        source <- original.sourceReference.asObject
        declaration <- source("original").flatMap(_.asObject)
        _ <- declaration("id").flatMap(_.asString).filter(_ == original.resourceId)
        _ <- declaration("mediaType").flatMap(_.asString).filter(_ == original.mediaType)
        _ <- declaration("byteLength").flatMap(_.asNumber).flatMap(_.toLong).filter(_ == original.byteLength)
        receipt <- source("receipt").flatMap(_.asObject)
        _ <- receipt("providerId").flatMap(_.asString).filter(_ == "google-drive")
        remoteid <- receipt("remoteId").flatMap(_.asString).filter(_.matches("[A-Za-z0-9_-]{1,256}"))
      } yield original -> remoteid })
    }
    admitted.fold[Consequence[Vector[(MaterialOriginalContribution, String)]]](
      Consequence.argumentInvalid("Material original source reference is invalid"))(Consequence.success)
  }

  private[knowledgelake] def _create(configuration: MaterialRegistrationConfiguration, store: GoogleDriveFileStore,
    originals: Vector[(MaterialOriginalContribution, String)]): MaterialRegistrationSource =
    new MaterialRegistrationSource(configuration, store, originals)

  private def _invalid[A]: Consequence[A] =
    Consequence.resourceInvalid("Material original source does not match its configured resource")
  private def _lift[A](result: Consequence[A]): ExecUowM[A] = ConsequenceT.fromConsequence[ExecProgram, A](result)
}
