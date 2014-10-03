package lila.api

import play.api.libs.json._

import lila.game.Pov
import lila.pref.Pref
import lila.round.JsonView
import lila.tournament.TournamentRepo
import lila.user.User

private[api] final class RoundApi(jsonView: JsonView) {

  def player(pov: Pov, apiVersion: Int)(implicit ctx: Context): Fu[JsObject] =
    jsonView.playerJson(pov, ctx.pref, apiVersion, ctx.me) zip
      (pov.game.tournamentId ?? TournamentRepo.byId) map {
        case (json, tourOption) => tourOption.fold(json) { tour =>
          json + (
            "tournament" -> Json.obj(
              "id" -> tour.id,
              "name" -> tour.name,
              "running" -> tour.isRunning
            )
          )
        }
      }
}
