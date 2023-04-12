package lila.coach

import lila.user.User

case class CoachReview(
    _id: String,    // user:coach
    userId: UserId, // reviewer
    coachId: Coach.Id,
    score: Int,
    text: String,
    approved: Boolean,
    createdAt: Instant,
    updatedAt: Instant,
    moddedAt: Option[Instant] = None // a mod disapproved it
):

  inline def id = _id

  def pendingApproval = !approved && moddedAt.isEmpty

object CoachReview:

  def makeId(user: User, coach: Coach) = s"${user.id}:${coach.id.value}"

  case class Reviews(list: List[CoachReview]):
    def approved = list.filter(_.approved)
