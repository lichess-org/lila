package lila.round

import play.api.libs.json.Json

import lila.game.{ Pov, Game }
import lila.pref.Pref
import lila.round.Env.{ current => roundEnv }

trait RoundHelper {

  def hijackEnabled(game: Game) = game.rated && roundEnv.HijackEnabled

  def moretimeSeconds = roundEnv.moretimeSeconds

  def roundPlayerJsData(pov: Pov, version: Int, pref: Pref, apiVersion: Int) =
    roundEnv.jsonView.playerJson(pov, version, pref, apiVersion)

  def roundWatcherJsData(pov: Pov, version: Int, tv: Boolean, pref: Pref) =
    roundEnv.jsonView.watcherJson(pov, version, tv, pref)
}
