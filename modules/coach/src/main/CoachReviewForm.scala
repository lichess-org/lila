package lila.coach

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

object CoachReviewForm {

  lazy val form = Form(mapping(
    "text" -> nonEmptyText(minLength = 3, maxLength = 1000),
    "score" -> number(min = 1, max = 5)
  )(Data.apply)(Data.unapply))

  case class Data(text: String, score: Int)
}
