package lila.storm

import play.api.data._
import play.api.data.Forms._

import lila.common.Form.{ numberIn, stringIn }

object StormForm {

  case class RunData(
      puzzles: Int,
      score: Int,
      moves: Int,
      errors: Int,
      combo: Int,
      time: Int,
      highest: Int
  )

  val run = Form(
    mapping(
      "puzzles" -> number(min = 1, max = 200),
      "score"   -> number(min = 1, max = 200),
      "moves"   -> number(min = 1, max = 900),
      "errors"  -> number(min = 1, max = 50),
      "combo"   -> number(min = 1, max = 900),
      "time"    -> number(min = 1, max = 900),
      "highest" -> number(min = lila.rating.Glicko.minRating, max = 4000)
    )(RunData.apply)(RunData.unapply)
  )
}
