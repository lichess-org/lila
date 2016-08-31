package lila.coach

import org.joda.time.DateTime

import lila.user.User

case class CoachReview(
  _id: String, // random
  userId: User.ID, // reviewer
  coachId: Coach.Id,
  rating: Int,
  title: String,
  text: String,
  approved: Boolean,
  createdAt: DateTime,
  updatedAt: DateTime)

object CoachReview {

  case class Score(value: Double) extends AnyVal {

  }

  case class Reviews(list: List[CoachReview]) {

    def approved = list.filter(_.approved)

    lazy val averageScore: Option[Score] = approved.nonEmpty option {
      Score(approved.map(_.rating).sum.toDouble / list.size)
    }
  }
}
