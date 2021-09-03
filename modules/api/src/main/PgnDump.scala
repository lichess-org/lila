package lila.api

import shogi.format.FEN
import shogi.format.pgn.Kif
import lila.analyse.{ Analysis, Annotator }
import lila.game.Game
import lila.game.PgnDump.WithFlags
import lila.team.GameTeams

final class PgnDump(
    val dumper: lila.game.PgnDump,
    annotator: Annotator,
    simulApi: lila.simul.SimulApi,
    getTournamentName: lila.tournament.GetTourName,
    getSwissName: lila.swiss.GetSwissName
)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val lang = lila.i18n.defaultLang

  def apply(
      game: Game,
      initialFen: Option[FEN],
      analysis: Option[Analysis],
      flags: WithFlags,
      teams: Option[GameTeams] = None,
      realPlayers: Option[RealPlayers] = None
  ): Fu[Kif] =
    dumper(game, initialFen, flags, teams) flatMap { kif =>
      if (flags.tags) (game.simulId ?? simulApi.idToName) map { simulName =>
        simulName
          .orElse(game.tournamentId flatMap getTournamentName.get)
          .orElse(game.swissId map lila.swiss.Swiss.Id flatMap getSwissName.apply)
          .fold(kif)(kif.withEvent)
      }
      else fuccess(kif)
    } map { kif =>
      val evaled = analysis.ifTrue(flags.evals).fold(kif)(addEvals(kif, _))
      if (flags.literate) annotator(evaled, analysis, game.opening, game.winnerColor, game.status)
      else evaled
    } map { kif =>
      realPlayers.fold(kif)(_.update(game, kif))
    }

  private def addEvals(p: Kif, analysis: Analysis): Kif =
    analysis.infos.foldLeft(p) { case (kif, info) =>
      kif.updatePly(
        info.ply,
        move => {
          val comment = info.cp
            .map(_.pawns.toString)
            .orElse(info.mate.map(m => s"#${m.value}"))
          move.copy(
            comments = comment.map(c => s"[%eval $c]").toList ::: move.comments
          )
        }
      )
    }

  def formatter(flags: WithFlags) =
    (
        game: Game,
        initialFen: Option[FEN],
        analysis: Option[Analysis],
        teams: Option[GameTeams],
        realPlayers: Option[RealPlayers]
    ) => apply(game, initialFen, analysis, flags, teams, realPlayers) dmap toKifString

  def toKifString(kif: Kif) = {
    s"$kif\n\n\n"
  }
}
