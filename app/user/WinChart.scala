package lila
package user

import http.Context

import scala.math.round
import scalaz.effects._
import com.codahale.jerkson.Json
import i18n.I18nKeys

final class WinChart(nbWin: Int, nbDraw: Int, nbLoss: Int, nbAi: Int) {

  val columns = Json generate List(
    "string" :: "Result" :: Nil,
    "number" :: "Games" :: Nil)

  def rows(trans: I18nKeys)(implicit ctx: Context) = Json generate {
    List(
      List(trans.nbWins.str(nbWin), nbWin),
      List(trans.nbLosses.str(nbLoss), nbLoss),
      List(trans.nbDraws.str(nbDraw), nbDraw),
      List("AI", nbAi))
  }
}
