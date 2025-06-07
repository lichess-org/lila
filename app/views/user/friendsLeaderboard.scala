package views.user

import lila.app.UiEnv.*
import lila.core.userId.UserId

object friendsLeaderboard:

  private lazy val ui = lila.user.ui.LeaderboardFriends(helpers)

  def apply(
      leaderboards: lila.rating.UserPerfs.Leaderboards,
      nbFriends: Set[UserId]
  )(using Context) =
    ui.page(leaderboards, nbFriends)
