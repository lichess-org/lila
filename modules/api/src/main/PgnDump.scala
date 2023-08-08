package lila.api

import chess.format.Fen
import chess.format.pgn.Pgn

import lila.analyse.{ Analysis, Annotator }
import lila.game.Game
import lila.game.PgnDump.WithFlags
import lila.team.GameTeams
import play.api.i18n.Lang
import chess.ByColor
import chess.format.EpdFen

final class PgnDump(
    val dumper: lila.game.PgnDump,
    annotator: Annotator,
    simulApi: lila.simul.SimulApi,
    getTournamentName: lila.tournament.GetTourName,
    getSwissName: lila.swiss.GetSwissName
)(using Executor):

  private given Lang = lila.i18n.defaultLang

  def apply(
      game: Game,
      initialFen: Option[Fen.Epd],
      analysis: Option[Analysis],
      flags: WithFlags,
      teams: Option[GameTeams] = None,
      realPlayers: Option[RealPlayers] = None
  ): Fu[Pgn] =
    dumper(game, initialFen, flags, teams) flatMap { pgn =>
      if flags.tags then
        (game.simulId so simulApi.idToName)
          .orElse(game.tournamentId so getTournamentName.async)
          .orElse(game.swissId so getSwissName.async) map {
          _.fold(pgn)(pgn.withEvent)
        }
      else fuccess(pgn)
    } map { pgn =>
      val evaled = analysis.ifTrue(flags.evals).fold(pgn)(annotator.addEvals(pgn, _))
      if flags.literate then annotator(evaled, game, analysis)
      else evaled
    } map { pgn =>
      realPlayers.fold(pgn)(_.update(game, pgn))
    }

  def formatter(
      flags: WithFlags
  ): (Game, Option[EpdFen], Option[Analysis], Option[ByColor[TeamId]], Option[RealPlayers]) => Future[
    String
  ] =
    (
        game: Game,
        initialFen: Option[Fen.Epd],
        analysis: Option[Analysis],
        teams: Option[GameTeams],
        realPlayers: Option[RealPlayers]
    ) =>
      apply(game, initialFen, analysis, flags, teams, realPlayers) dmap annotator.toPgnString dmap (_.value)
