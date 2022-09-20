package views.html
package user

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

object bots {

  def apply(users: List[User])(implicit ctx: Context) = {

    val title = s"${users.size} Online bots"

    val sorted = users.sortBy { -_.playTime.??(_.total) }

    views.html.base.layout(
      title = title,
      moreCss = frag(cssTag("slist"), cssTag("user.list")),
      wrapClass = "full-screen-force"
    )(
      main(cls := "page-menu bots")(
        user.bits.communityMenu("bots"),
        sorted.partition(_.isVerified) match {
          case (featured, all) =>
            div(cls := "bots page-menu__content")(
              div(cls := "box bots__featured")(
                div(cls := "box__top")(h1("Featured bots")),
                botTable(featured)
              ),
              div(cls := "box")(
                div(cls := "box__top")(
                  h1("Community bots"),
                  a(
                    cls  := "bots__about",
                    href := "https://lichess.org/blog/WvDNticAAMu_mHKP/welcome-lichess-bots"
                  )(
                    "About Lichess Bots"
                  )
                ),
                botTable(all)
              )
            )
        }
      )
    )
  }

  private def botTable(users: List[User])(implicit ctx: Context) = {
    table(cls := "slist slist-pad")(
      tbody(
        users map { u =>
          {
            val bio = u.profile
              .ifTrue(ctx.noKid)
              .ifTrue(!u.marks.troll || ctx.is(u))
              .flatMap(_.nonEmptyBio)
              .map { bio =>
                shorten(bio, 400)
              };

            val challengeButton =
              if (ctx is u) span
              else {
                a(
                  dataIcon := "",
                  cls      := List("button button-empty text" -> true),
                  st.title := trans.challenge.challengeToPlay.txt(),
                  href     := s"${routes.Lobby.home}?user=${u.username}#friend"
                )(trans.play())
              };

            tr(
              td(cls := "mobile", colspan := 3)(
                userLink(u),
                br,
                br,
                bio,
                br,
                br,
                div(style := "text-align: center")(challengeButton)
              ),
              td(userLink(u)),
              td(bio),
              ctx.pref.showRatings option td(cls := "rating")(u.bestAny3Perfs.map {
                showPerfRating(u, _)
              }),
              u.playTime.fold(td) { playTime =>
                td(
                  p(
                    cls      := "text",
                    dataIcon := "",
                    st.title := trans.tpTimeSpentPlaying.txt(showPeriod(playTime.totalPeriod))
                  )(showPeriod(playTime.totalPeriod)),
                  playTime.nonEmptyTvPeriod.map { tvPeriod =>
                    p(
                      cls      := "text",
                      dataIcon := "",
                      st.title := trans.tpTimeSpentOnTV.txt(showPeriod(tvPeriod))
                    )(showPeriod(tvPeriod))
                  }
                )
              },
              td(challengeButton)
            )
          }
        }
      )
    )
  }
}
