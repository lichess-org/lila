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
      highest: Int,
      notAnExploit: String,
      signed: Option[String]
  )

  val run = Form(
    mapping(
      "puzzles"      -> number(min = 1, max = 200),
      "score"        -> number(min = 1, max = 200),
      "moves"        -> number(min = 1, max = 900),
      "errors"       -> number(min = 0, max = 50),
      "combo"        -> number(min = 1, max = 900),
      "time"         -> number(min = 1, max = 900),
      "highest"      -> number(min = lila.rating.Glicko.minRating, max = 4000),
      "notAnExploit" -> nonEmptyText.verifying(_ == notAnExploit),
      "signed"       -> optional(nonEmptyText)
    )(RunData.apply)(RunData.unapply)
  )

  val notAnExploit =
    "Yes, we know that you can send whatever score you like. That's why there's no leaderboards and no competition."
}
