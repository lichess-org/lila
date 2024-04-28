package lila.user
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.core.user.LightPerf
import lila.core.perf.UserWithPerfs
import lila.core.perf.UserPerfs

final class UserList(helpers: Helpers, bits: UserBits):
  import helpers.{ *, given }

  def page(
      online: List[UserWithPerfs],
      leaderboards: lila.rating.UserPerfs.Leaderboards,
      nbAllTime: List[LightCount],
      tournamentWinners: Frag
  )(using ctx: Context) =
    main(cls := "page-menu")(
      bits.communityMenu("leaderboard"),
      div(cls := "community page-menu__content box box-pad")(
        st.section(cls := "community__online")(
          h2(trans.site.onlinePlayers()),
          ol(cls := "user-top"):
            online.map: u =>
              li(
                userLink(u),
                ctx.pref.showRatings.option(showBestPerf(u.perfs))
              )
        ),
        div(cls := "community__leaders")(
          h2(trans.site.leaderboard()),
          div(cls := "leaderboards")(
            userTopPerf(leaderboards.bullet, PerfKey.bullet),
            userTopPerf(leaderboards.blitz, PerfKey.blitz),
            userTopPerf(leaderboards.rapid, PerfKey.rapid),
            userTopPerf(leaderboards.classical, PerfKey.classical),
            userTopPerf(leaderboards.ultraBullet, PerfKey.ultraBullet),
            userTopActive(nbAllTime, trans.site.activePlayers(), icon = Icon.Swords.some),
            st.section(cls := "user-top")(
              h2(cls := "text", dataIcon := Icon.Trophy)(
                a(href := routes.Tournament.leaderboard)(trans.site.tournament())
              ),
              tournamentWinners
            ),
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

  private def userTopActive(users: List[LightCount], hTitle: Frag, icon: Option[Icon])(using Context) =
    st.section(cls := "user-top")(
      h2(cls := "text", dataIcon := icon.map(_.toString))(hTitle),
      ol(users.map: u =>
        li(
          lightUserLink(u.user),
          span(title := trans.site.gamesPlayed.txt())(s"#${u.count.localize}")
        ))
    )

  def top(users: List[LightPerf], title: String)(using ctx: Context) =
    main(cls := "page-small box")(
      boxTop(h1(a(href := routes.User.list, dataIcon := Icon.LessThan), title)),
      table(cls := "slist slist-pad")(
        tbody(
          users.mapWithIndex: (u, i) =>
            tr(
              td(i + 1),
              td(lightUserLink(u.user)),
              ctx.pref.showRatings.option(
                frag(
                  td(u.rating),
                  td(ratingProgress(u.progress))
                )
              )
            )
        )
      )
    )

  def bots(users: List[UserWithPerfs], title: String, bestPerfs: UserPerfs => List[PerfKey])(using Context) =
    main(cls := "page-menu bots")(
      bits.communityMenu("bots"),
      users.partition(_.isVerified) match
        case (featured, all) =>
          div(cls := "bots page-menu__content")(
            div(cls := "box bots__featured")(
              h1(cls := "box__top")("Featured bots"),
              botTable(featured, bestPerfs)
            ),
            div(cls := "box")(
              boxTop(
                h1("Community bots"),
                a(
                  cls  := "bots__about",
                  href := "https://lichess.org/blog/WvDNticAAMu_mHKP/welcome-lichess-bots"
                )("About Lichess Bots")
              ),
              botTable(all, bestPerfs)
            )
          )
    )

  private def botTable(users: List[UserWithPerfs], bestPerfs: UserPerfs => List[PerfKey])(using
      ctx: Context
  ) = div(cls := "bots__list")(
    users.map: u =>
      div(cls := "bots__list__entry")(
        div(cls := "bots__list__entry__desc")(
          div(cls := "bots__list__entry__head")(
            userLink(u),
            ctx.pref.showRatings.option(div(cls := "bots__list__entry__rating"):
              bestPerfs(u.perfs).map { showPerfRating(u.perfs, _) }
            )
          ),
          u.profile
            .ifTrue(ctx.kid.no)
            .ifTrue(!u.marks.troll || ctx.is(u))
            .flatMap(_.nonEmptyBio)
            .map { bio => td(shorten(bio, 400)) }
        ),
        a(
          dataIcon := Icon.Swords,
          cls      := List("bots__list__entry__play button button-empty text" -> true),
          st.title := trans.challenge.challengeToPlay.txt(),
          href     := s"${routes.Lobby.home}?user=${u.username}#friend"
        )(trans.site.play())
      )
  )
