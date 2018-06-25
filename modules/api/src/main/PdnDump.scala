package lidraughts.api

import org.joda.time.DateTime
import play.api.libs.iteratee._
import scala.concurrent.duration._

import draughts.format.FEN
import draughts.format.pdn.Pdn
import lidraughts.analyse.{ Analysis, AnalysisRepo, Annotator }
import lidraughts.game.Game
import lidraughts.game.PdnDump.WithFlags
import lidraughts.game.{ Game, GameRepo, Query }

final class PdnDump(
    val dumper: lidraughts.game.PdnDump,
    annotator: Annotator,
    getSimulName: String => Fu[Option[String]],
    getTournamentName: String => Option[String]
) {

  def apply(game: Game, initialFen: Option[FEN], analysis: Option[Analysis], flags: WithFlags): Fu[Pdn] =
    dumper(game, initialFen, flags) flatMap { pdn =>
      if (flags.tags) (game.simulId ?? getSimulName) map { simulName =>
        simulName.orElse(game.tournamentId flatMap getTournamentName).fold(pdn)(pdn.withEvent)
      }
      else fuccess(pdn)
    } map { pdn =>
      val evaled = analysis.ifTrue(flags.evals).fold(pdn)(addEvals(pdn, _))
      if (flags.literate) annotator(evaled, analysis, game.opening, game.winnerColor, game.status)
      else evaled
    }

  private def addEvals(p: Pdn, analysis: Analysis): Pdn = analysis.infos.foldLeft(p) {
    case (pdn, info) => pdn.updateTurn(info.turn, turn =>
      turn.update(info.color, move => {
        val comment = info.cp.map(_.pieces.toString)
          .orElse(info.win.map(m => s"#${m.value}"))
        move.copy(
          comments = comment.map(c => s"[%eval $c]").toList ::: move.comments
        )
      }))
  }

  def formatter(flags: WithFlags) =
    Enumeratee.mapM[(Game, Option[FEN], Option[Analysis])].apply[String] {
      case (game, initialFen, analysis) => toPdnString(game, initialFen, analysis, flags)
    }

  def toPdnString(game: Game, initialFen: Option[FEN], analysis: Option[Analysis], flags: WithFlags) =
    apply(game, initialFen, analysis, flags).map { pdn =>
      // merge analysis & eval comments
      // 1. 32-27 {[%eval 0.17]} {[%clk 0:00:30]}
      // 1. 32-27 {[%eval 0.17] [%clk 0:00:30]}
      s"$pdn\n\n\n".replaceIf("]} {[", "] [")
    }
}
