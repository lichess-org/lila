package lila.coach

import play.api.data.*
import play.api.data.Forms.*

object CoachReviewForm:

  lazy val form = Form(
    mapping(
      "text"  -> text(minLength = 3, maxLength = 2010),
      "score" -> number(min = 1, max = 5)
    )(Data.apply)(unapply)
  )

  case class Data(text: String, score: Int)
