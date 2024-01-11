package lila.api

import shogi.format.Notation
import lila.analyse.{ Analysis, Annotator }
import lila.game.Game
import lila.game.NotationDump.WithFlags
import lila.team.GameTeams

final class NotationDump(
    val dumper: lila.game.NotationDump,
    annotator: Annotator,
    simulApi: lila.simul.SimulApi,
    getTournamentName: lila.tournament.GetTourName
)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val lang = lila.i18n.defaultLang

  def apply(
      game: Game,
      analysis: Option[Analysis],
      flags: WithFlags,
      teams: Option[GameTeams] = None,
      realPlayers: Option[RealPlayers] = None
  ): Fu[Notation] =
    dumper(game, flags, teams) flatMap { notation =>
      if (flags.tags) (game.simulId ?? simulApi.idToName) map { simulName =>
        simulName
          .orElse(game.tournamentId flatMap getTournamentName.get)
          .fold(notation)(notation.withEvent)
      }
      else fuccess(notation)
    } map { notation =>
      val evaled = analysis.ifTrue(flags.evals).fold(notation)(addEvals(notation, _))
      if (flags.literate) annotator(evaled, analysis)
      else evaled
    } map { notation =>
      realPlayers.fold(notation)(_.update(game, notation))
    }

  private def addEvals(p: Notation, analysis: Analysis): Notation =
    analysis.infos.foldLeft(p) { case (notation, info) =>
      notation.updatePly(
        info.ply,
        move => {
          val comment = info.cp
            .map(_.pawns.toString)
            .orElse(info.mate.map(m => s"mate${m.value}"))
          move.copy(
            comments = comment.map(c => s"[%eval $c]").toList ::: move.comments
          )
        }
      )
    }

  def formatter(flags: WithFlags) =
    (
        game: Game,
        analysis: Option[Analysis],
        teams: Option[GameTeams],
        realPlayers: Option[RealPlayers]
    ) => apply(game, analysis, flags, teams, realPlayers) dmap toNotationString

  def toNotationString(notation: Notation) = {
    s"$notation\n\n\n"
  }
}
