package lila.analyse

import monocle.syntax.all.*
import play.api.libs.json.*

import lila.common.Bus
import lila.core.game.WithInitialFen
import lila.tree.Analysis

final class Analyser(
    gameRepo: lila.core.game.GameRepo,
    analysisRepo: AnalysisRepo,
    divider: lila.core.game.Divider
)(using Executor)
    extends lila.tree.Analyser:

  export analysisRepo.{ byId, byGame as get }

  def save(analysis: Analysis, workHash: Option[() => Array[Byte]] = none): Funit =
    analysisRepo
      .save(analysis, analysis.studyId.isDefined.so(workHash).map(_()))
      .flatMap: _ =>
        analysis.id.gameId
          .so: id =>
            gameRepo
              .game(id)
              .flatMapz: prev =>
                val game = prev.focus(_.metadata.analysed).replace(true)
                for
                  _ <- gameRepo.setAnalysed(game.id, true)
                  _ = Bus.pub(actorApi.AnalysisReady(game, analysis))
                yield game.some
          .flatMap: game =>
            sendAnalysisProgress(analysis, complete = true, game)

  def progress(analysis: Analysis): Funit = sendAnalysisProgress(analysis, complete = false)

  def foundSameHash(forId: Analysis.Id, same: Analysis, workHash: Array[Byte]): Funit =
    save(same.copy(id = forId), (() => workHash).some)

  private def sendAnalysisProgress(
      analysis: Analysis,
      complete: Boolean,
      game: Option[Game] = none
  ): Funit =
    analysis.id match
      case Analysis.Id.Game(id) =>
        game
          .fold(gameRepo.gameWithInitialFen(id)): g =>
            gameRepo
              .initialFen(g)
              .map: fen =>
                WithInitialFen(g, fen).some
          .mapz: g =>
            Bus.pub(
              lila.tree.AnalysisProgress(
                id,
                () => makeProgressPayload(analysis, g.game, g.fen | g.game.variant.initialFen)
              )
            )
      case _ =>
        fuccess:
          Bus.pub(lila.tree.StudyAnalysisProgress(analysis, complete))

  private def makeProgressPayload(
      analysis: Analysis,
      game: Game,
      initialFen: chess.format.Fen.Full
  ): JsObject =
    import lila.tree.{ TreeBuilder, ExportOptions, Node }
    val tree = TreeBuilder(game, analysis.some, initialFen, ExportOptions.default, lila.log.system.warn)
    val division = divider(game.id, game.sans, game.variant, initialFen.some)
    Json.obj(
      "analysis" -> JsonView.bothPlayers(game.startedAtPly, analysis, division = division),
      "tree" -> Node.lichobileNodeJsonWriter.writes(tree)
    )
