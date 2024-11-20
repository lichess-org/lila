package lila.storm

import chess.IntRating
import play.api.data.*
import play.api.data.Forms.*

object StormForm:

  case class RunData(
      puzzles: Int,
      score: Int,
      moves: Int,
      errors: Int,
      combo: Int,
      time: Int,
      highest: IntRating,
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
      "highest"      -> lila.rating.formMapping,
      "notAnExploit" -> nonEmptyText.verifying(_ == notAnExploit),
      "signed"       -> optional(nonEmptyText)
    )(RunData.apply)(unapply)
  )

  val notAnExploit =
    "Yes, we know that you can send whatever score you like. That's why there's no leaderboards and no competition."
