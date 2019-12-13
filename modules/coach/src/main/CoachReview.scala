package lila.coach

import org.joda.time.DateTime

import lila.user.User

case class CoachReview(
    _id: String,     // user:coach
    userId: User.ID, // reviewer
    coachId: Coach.Id,
    score: Int,
    text: String,
    approved: Boolean,
    createdAt: DateTime,
    updatedAt: DateTime,
    moddedAt: Option[DateTime] = None // a mod disapproved it
) {

  def id = _id

  def pendingApproval = !approved && moddedAt.isEmpty
}

object CoachReview {

  def makeId(user: User, coach: Coach) = s"${user.id}:${coach.id.value}"

  case class Score(value: Double) extends AnyVal

  case class Reviews(list: List[CoachReview]) {

    def approved = list.filter(_.approved)

    lazy val averageScore: Option[Score] = approved.nonEmpty option {
      Score(approved.map(_.score).sum.toDouble / list.size)
    }
  }
}
