package lila.api

import chess.ByColor
import chess.format.Fen
import chess.format.pgn.Pgn
import play.api.i18n.Lang

import lila.analyse.{ Analysis, Annotator }
import lila.game.PgnDump.WithFlags
import lila.team.GameTeams

final class PgnDump(
    val dumper: lila.game.PgnDump,
    annotator: Annotator,
    simulApi: lila.simul.SimulApi,
    getTournamentName: lila.tournament.GetTourName,
    getSwissName: lila.swiss.GetSwissName
)(using Executor):

  private given Lang = lila.core.i18n.defaultLang

  def apply(
      game: Game,
      initialFen: Option[Fen.Full],
      analysis: Option[Analysis],
      flags: WithFlags,
      teams: Option[GameTeams] = None
  ): Fu[Pgn] =
    dumper(game, initialFen, flags, teams)
      .flatMap: pgn =>
        if flags.tags then
          game.simulId
            .so(simulApi.idToName)
            .orElse(game.tournamentId.so(getTournamentName.async))
            .orElse(game.swissId.so(getSwissName.async))
            .map(_.fold(pgn)(pgn.withEvent))
        else fuccess(pgn)
      .map: pgn =>
        val evaled = analysis.ifTrue(flags.evals).fold(pgn)(annotator.addEvals(pgn, _))
        if flags.literate then annotator(evaled, game, analysis)
        else evaled

  def formatter(
      flags: WithFlags
  ): (Game, Option[Fen.Full], Option[Analysis], Option[ByColor[TeamId]]) => Fu[
    String
  ] =
    (
        game: Game,
        initialFen: Option[Fen.Full],
        analysis: Option[Analysis],
        teams: Option[GameTeams]
    ) => apply(game, initialFen, analysis, flags, teams).map(annotator.toPgnString).dmap(_.value)
