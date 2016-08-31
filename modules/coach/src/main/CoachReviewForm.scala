package lila.coach

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

object CoachReviewForm {

  lazy val form = Form(mapping(
    "title" -> nonEmptyText(minLength = 3, maxLength = 100),
    "text" -> nonEmptyText(minLength = 3, maxLength = 1000),
    "score" -> number(min = 1, max = 5)
  )(Data.apply)(Data.unapply))

  case class Data(title: String, text: String, score: Int)
}
