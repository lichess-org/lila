package lila.user
package ui

import lila.core.user.LightPerf
import lila.core.userId.UserId
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class LeaderboardFriends(helpers: Helpers):
  import helpers.{ *, given }

  def page(
      leaderboards: lila.rating.UserPerfs.Leaderboards,
      nbFriends: Set[UserId]
  )(using ctx: Context) =
    Page(trans.site.leaderboard.txt())
      .css(if nbFriends.size < 1 then "user.nofriends" else "user.list")
      .flag(_.fullScreen)
      .graph(
        title = trans.leaderboard.friendsLeaderboardTitle.txt(),
        url = s"$netBaseUrl${routes.User.list.url}",
        description = trans.leaderboard.friendsLeaderboardDescription.txt()
      ):
        main(cls := "page-menu full-screen-force")(
          if nbFriends.size < 1 then
            div(cls := "community page-menu__content box box-pad full-height")(
              div(cls := "community__empty")(
                h2(trans.leaderboard.noFriendsToCompare.txt()),
                p(
                  trans.leaderboard.noFriendsInfo1.txt(),
                  br,
                  trans.leaderboard.noFriendsInfo2.txt()
                ),
                div(cls := "actions")(
                  a(
                    cls  := "button button-fat",
                    href := routes.User.list
                  )(trans.leaderboard.explorePopularPlayers.txt())
                )
              )
            )
          else
            div(cls := "community page-menu__content box box-pad")(
              div(cls := "community__leaders")(
                h2(trans.leaderboard.howYouCompareAgainstFriends.pluralSame(nbFriends.size - 1)),
                div(cls := "leaderboards")(
                  userTopPerf(leaderboards.bullet, PerfKey.bullet),
                  userTopPerf(leaderboards.blitz, PerfKey.blitz),
                  userTopPerf(leaderboards.rapid, PerfKey.rapid),
                  userTopPerf(leaderboards.classical, PerfKey.classical),
                  userTopPerf(leaderboards.ultraBullet, PerfKey.ultraBullet),
                  userTopPerf(leaderboards.crazyhouse, PerfKey.crazyhouse),
                  userTopPerf(leaderboards.chess960, PerfKey.chess960),
                  userTopPerf(leaderboards.antichess, PerfKey.antichess),
                  userTopPerf(leaderboards.atomic, PerfKey.atomic),
                  userTopPerf(leaderboards.threeCheck, PerfKey.threeCheck),
                  userTopPerf(leaderboards.kingOfTheHill, PerfKey.kingOfTheHill),
                  userTopPerf(leaderboards.horde, PerfKey.horde),
                  userTopPerf(leaderboards.racingKings, PerfKey.racingKings)
                )
              )
            )
        )

  private def userTopPerf(users: List[LightPerf], pk: PerfKey)(using ctx: Context) =
    st.section(cls := "user-top")(
      h2(cls := "text", dataIcon := pk.perfIcon)(
        a(href := routes.User.topNb(200, pk))(pk.perfTrans)
      ),
      ol(users.map: l =>
        li(
          lightUserLink(l.user),
          ctx.pref.showRatings.option(l.rating)
        ))
    )
