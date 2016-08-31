package lila.coach

import org.joda.time.DateTime

import lila.user.User

case class CoachReview(
  _id: String, // user:coach
  userId: User.ID, // reviewer
  coachId: Coach.Id,
  score: Int,
  text: String,
  approved: Boolean,
  createdAt: DateTime,
  updatedAt: DateTime)

object CoachReview {

  case class WithUser(review: CoachReview, user: User)

  def makeId(user: User, coach: Coach) = s"${user.id}:${coach.id.value}"

  case class Score(value: Double) extends AnyVal {

  }

  case class Reviews(list: List[CoachReview.WithUser]) {

    def approved = list.filter(_.review.approved)

    lazy val averageScore: Option[Score] = approved.nonEmpty option {
      Score(approved.map(_.review.score).sum.toDouble / list.size)
    }
  }
}
