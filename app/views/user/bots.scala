package views.html
package user

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText
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
                div(cls := "box__top")(h1("Community bots")),
                botTable(all)
              )
            )
        }
      )
    )
  }

  private def botTable(users: List[User])(implicit ctx: Context) = table(cls := "slist slist-pad")(
    tbody(
      users.sortBy { u =>
        (if (u.isVerified) -1 else 1, -u.playTime.??(_.total))
      } map { u =>
        tr(
          td(userLink(u)),
          u.profile
            .ifTrue(ctx.noKid)
            .ifTrue(!u.marks.troll || ctx.is(u))
            .flatMap(_.nonEmptyBio)
            .map { bio =>
              td(shorten(bio, 400))
            } | td,
          td(cls := "rating")(u.best3Perfs.map {
            showPerfRating(u, _)
          }),
          u.playTime.fold(td) { playTime =>
            td(
              p(
                cls := "text",
                dataIcon := "C",
                st.title := trans.tpTimeSpentPlaying.txt(showPeriod(playTime.totalPeriod))
              )(showPeriod(playTime.totalPeriod)),
              playTime.nonEmptyTvPeriod.map { tvPeriod =>
                p(
                  cls := "text",
                  dataIcon := "1",
                  st.title := trans.tpTimeSpentOnTV.txt(showPeriod(tvPeriod))
                )(showPeriod(tvPeriod))
              }
            )
          },
          if (ctx is u) td
          else {
            td(
              a(
                dataIcon := "U",
                cls := List("button button-empty text" -> true),
                st.title := trans.challenge.challengeToPlay.txt(),
                href := s"${routes.Lobby.home()}?user=${u.username}#friend"
              )(trans.play())
            )
          }
        )
      }
    )
  )
}
