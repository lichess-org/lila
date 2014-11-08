package lila.api

import play.api.libs.json._

import chess.format.pgn.Pgn
import lila.analyse.Analysis
import lila.game.Pov
import lila.pref.Pref
import lila.round.JsonView
import lila.security.Granter
import lila.tournament.{ Tournament, TournamentRepo }
import lila.user.User

private[api] final class RoundApi(
    jsonView: JsonView,
    analysisApi: AnalysisApi) {

  def player(pov: Pov, apiVersion: Int)(implicit ctx: Context): Fu[JsObject] =
    jsonView.playerJson(pov, ctx.pref, apiVersion, ctx.me,
      withBlurs = ctx.me ?? Granter(_.ViewBlurs)) zip
      (pov.game.tournamentId ?? TournamentRepo.byId) map {
        case (json, tourOption) => blindMode {
          withTournament(tourOption) {
            json
          }
        }
      }

  def watcher(pov: Pov, apiVersion: Int, tv: Option[Boolean], analysis: Option[(Pgn, Analysis)] = None)(implicit ctx: Context): Fu[JsObject] =
    jsonView.watcherJson(pov, ctx.pref, apiVersion, ctx.me, tv,
      withBlurs = ctx.me ?? Granter(_.ViewBlurs)) zip
      (pov.game.tournamentId ?? TournamentRepo.byId) map {
        case (json, tourOption) => blindMode {
          withTournament(tourOption) {
            withAnalysis(analysis) {
              json
            }
          }
        }
      }

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
