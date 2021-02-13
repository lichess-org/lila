package lila.coach

import play.api.data._
import play.api.data.Forms._

object CoachReviewForm {

  lazy val form = Form(
    mapping(
      "text"  -> text(minLength = 3, maxLength = 2010),
      "score" -> number(min = 1, max = 5)
    )(Data.apply)(Data.unapply)
  )

  case class Data(text: String, score: Int)
}
