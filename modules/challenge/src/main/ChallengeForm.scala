package lila.challenge

import play.api.data._
import play.api.data.Forms._

final class ChallengeForm {

  val decline = Form(
    mapping(
      "reason" -> optional(nonEmptyText)
    )(DeclineData.apply _)(DeclineData.unapply _)
  )

  case class DeclineData(reason: Option[String]) {

    def realReason = reason.fold(Challenge.DeclineReason.default)(Challenge.DeclineReason.apply)
  }
}
