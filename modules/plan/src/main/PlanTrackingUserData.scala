package lila.plan

import lila.user.User

case class PlanTrackingUserData(
  user: User,
  isStreamer: Boolean,
  nbTournaments: Int,
  medianTournamentRank: Option[Int],
  nbFollowers: Int,
  nbFollowing: Int,
  nbForumPosts: Int)
