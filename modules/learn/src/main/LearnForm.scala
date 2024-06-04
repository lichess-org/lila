package lila.learn

import play.api.data._
import play.api.data.Forms._

object LearnForm {

  val scoreForm = Form(
    mapping(
      "stage" -> nonEmptyText,
      "level" -> number,
      "score" -> number
    )(ScoreEntry.apply)(ScoreEntry.unapply)
  )

  val scoresForm = Form(
    mapping(
      "scores" -> list(
        mapping(
          "stage" -> nonEmptyText,
          "level" -> number,
          "score" -> number
        )(ScoreEntry.apply)(ScoreEntry.unapply)
      )
    )(identity)(Some(_))
  )

}
