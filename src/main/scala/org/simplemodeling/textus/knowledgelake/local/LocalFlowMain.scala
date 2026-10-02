package org.simplemodeling.textus.knowledgelake.local

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import org.simplemodeling.textus.knowledgelake.model.*
import scala.util.control.NonFatal

/** Repository-fixture execution with persisted state and derived offline exports.
 * @since Oct. 2, 2026
 * @version Oct. 2, 2026
 * @author ASAMI, Tomoharu
 */
object LocalFlowMain:
  def execute(fixturePath: Path, storeDirectory: Path): Either[LocalError, LocalFlowReport] =
    val decoded = try
      LocalFlowJson.decodeFixture(Files.readString(fixturePath, StandardCharsets.UTF_8)).left.map(errors => LocalPreparationStore._invalid(errors, None))
    catch case NonFatal(error) => Left(LocalError("fixture-read", s"Cannot read UTF-8 fixture: ${error.getMessage}", LocalWriteOutcome.NotApplied, None))
    decoded.flatMap { fixture =>
      val store = new FilePreparationStore(storeDirectory)
      store.read().flatMap { initial =>
        if initial.revision != 0 then Left(LocalError("already-initialized", "Selected store is already initialized", LocalWriteOutcome.NotApplied, Some(initial.revision)))
        else
          val command = fixture.preparation
          val proposalcommand = fixture.proposal
          for
            seeded <- store.compareAndSet(0L, LocalDraft(fixture.initial, Vector.empty))
            prepared <- PreparationFlow.prepare(store, seeded.revision, command.contextRef, command.inputId, command.runId, command.result)
            proposed <- PreparationFlow.propose(store, prepared.revision, proposalcommand.producerId, proposalcommand.proposalId, proposalcommand.materialRef)
            proposal <- proposed.bundle.proposals.find(value => value.producerId == proposalcommand.producerId && value.proposalId == proposalcommand.proposalId)
              .toRight(LocalError("invalid", "Prepared proposal was not found", LocalWriteOutcome.NotApplied, Some(proposed.revision)))
            delivered <- fixture.deliveries.foldLeft[Either[LocalError, LocalState]](Right(proposed)) { (accumulated, delivery) =>
              accumulated.flatMap(current => ProposalDelivery.execute(store, current.revision,
                ProposalRequest(delivery.correlationId, delivery.operation, proposal), new FixtureProposalTransport(Vector(delivery.observation))))
            }
            persisted <- store.read()
            report <- _export(fixture, persisted, proposal, storeDirectory)
          yield report
      }
    }

  def main(args: Array[String]): Unit =
    if args.length != 2 then throw new IllegalStateException("Expected exactly two arguments: UTF-8 fixture path and explicit store/output directory")
    execute(Path.of(args(0)), Path.of(args(1))) match
      case Left(error) => throw new IllegalStateException(s"${error.code}: ${error.message}; write outcome ${error.writeOutcome}; observed revision ${error.observedRevision}")
      case Right(report) =>
        LocalFlowJson.encodeReport(report) match
          case Left(errors) => throw new IllegalStateException(s"Report encoding failed: ${errors.map(_.message).mkString("; ")}")
          case Right(text) => println(s"TKL_LOCAL_FLOW_RESULT=${io.circe.parser.parse(text).toOption.get.noSpaces}")

  private def _export(fixture: LocalFlowFixture, state: LocalState, proposal: RawCandidateProposal, directory: Path): Either[LocalError, LocalFlowReport] =
    val bundlepath = directory.resolve("preparation-bundle.json")
    val markdownpath = directory.resolve("prepared-material.md")
    val requestpath = directory.resolve("proposal-request.json")
    val first = fixture.deliveries.head
    val projections = for
      bundle <- PreparationJson.encode(state.bundle)
      markdown <- PreparedMaterialMarkdown.render(state.bundle, fixture.proposal.materialRef)
      request <- LocalFlowJson.encodeRequest(ProposalRequest(first.correlationId, first.operation, proposal))
    yield (bundle, markdown, request)
    projections.left.map(errors => LocalError("projection-write", errors.map(_.message).mkString("; "), LocalWriteOutcome.Applied, Some(state.revision))).flatMap { (bundle, markdown, request) =>
      try
        Files.writeString(bundlepath, bundle, StandardCharsets.UTF_8)
        Files.writeString(markdownpath, markdown, StandardCharsets.UTF_8)
        Files.writeString(requestpath, request, StandardCharsets.UTF_8)
        Right(LocalFlowReport(state.revision, fixture.preparation.runId, fixture.proposal.materialRef,
          ProposalKey(proposal.producerId, proposal.proposalId), state.deliveries.flatMap(_.observation.map(_.outcome)),
          bundlepath.toString, markdownpath.toString, requestpath.toString, LocalFlowJson.ASSURANCE))
      catch case NonFatal(error) => Left(LocalError("projection-write", s"Primary state committed; projection write failed: ${error.getMessage}", LocalWriteOutcome.Applied, Some(state.revision)))
    }
