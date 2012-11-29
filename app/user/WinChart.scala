package lila
package user

import http.Context

import scala.math.round
import scalaz.effects._
import play.api.libs.json.Json
import i18n.I18nKeys

final class WinChart(nbWin: Int, nbDraw: Int, nbLoss: Int, nbAi: Int) {

  val columns = Json.arr(
    Json.arr("string", "Result"),
    Json.arr("number", "Games")
  )

  def rows(trans: I18nKeys)(implicit ctx: Context) = Json.arr(
    Json.arr(trans.nbWins.str(nbWin), nbWin),
    Json.arr(trans.nbLosses.str(nbLoss), nbLoss),
    Json.arr(trans.nbDraws.str(nbDraw), nbDraw),
    Json.arr("%d AI" format nbAi, nbAi)
  )
}
