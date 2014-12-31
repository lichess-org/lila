package lila.api

import play.api.libs.json._

import chess.format.pgn.Pgn
import lila.analyse.Analysis
import lila.common.LightUser
import lila.game.Pov
import lila.pref.Pref
import lila.round.JsonView
import lila.security.Granter
import lila.tournament.{ Tournament, TournamentRepo }
import lila.user.User

private[api] final class RoundApi(
    jsonView: JsonView,
    noteApi: lila.round.NoteApi,
    analysisApi: AnalysisApi,
    lightUser: String => Option[LightUser]) {

  def player(pov: Pov, apiVersion: Int, otherPovs: List[Pov])(implicit ctx: Context): Fu[JsObject] =
    jsonView.playerJson(pov, ctx.pref, apiVersion, ctx.me,
      withBlurs = ctx.me ?? Granter(_.ViewBlurs)) zip
      (pov.game.tournamentId ?? TournamentRepo.byId) zip
      (ctx.me ?? (me => noteApi.get(pov.gameId, me.id))) map {
        case ((json, tourOption), note) => (
          blindMode _ compose withTournament(tourOption)_ compose withNote(note)_ compose withOtherPovs(otherPovs)_
        )(json)
      }

  def watcher(pov: Pov, apiVersion: Int, tv: Option[Boolean],
    analysis: Option[(Pgn, Analysis)] = None,
    initialFen: Option[Option[String]] = None)(implicit ctx: Context): Fu[JsObject] =
    jsonView.watcherJson(pov, ctx.pref, apiVersion, ctx.me, tv,
      withBlurs = ctx.me ?? Granter(_.ViewBlurs), initialFen = initialFen) zip
      (pov.game.tournamentId ?? TournamentRepo.byId) zip
      (ctx.me ?? (me => noteApi.get(pov.gameId, me.id))) map {
        case ((json, tourOption), note) => (
          blindMode _ compose withTournament(tourOption)_ compose withNote(note)_
        )(json)
      }

  private def withOtherPovs(otherPovs: List[Pov])(json: JsObject) =
    if (otherPovs.isEmpty) json else json + ("simul" -> JsBoolean(true))

  private def withNote(note: String)(json: JsObject) =
    if (note.isEmpty) json else json + ("note" -> JsString(note))

  private def withAnalysis(a: Option[(Pgn, Analysis)])(json: JsObject) = a.fold(json) {
    case (pgn, analysis) => json + ("analysis" -> Json.obj(
      "moves" -> analysisApi.game(analysis, pgn),
      "white" -> analysisApi.player(chess.Color.White)(analysis),
      "black" -> analysisApi.player(chess.Color.Black)(analysis)
    ))
  }

  private def withTournament(tourOption: Option[Tournament])(json: JsObject) =
    tourOption.fold(json) { tour =>
      json + ("tournament" -> Json.obj(
        "id" -> tour.id,
        "name" -> tour.name,
        "running" -> tour.isRunning))
    }

  private def blindMode(js: JsObject)(implicit ctx: Context) =
    ctx.blindMode.fold(js + ("blind" -> JsBoolean(true)), js)
}
