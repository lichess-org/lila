package views.html
package user

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.user.User

object bots:

  def apply(users: List[User])(implicit ctx: Context) =

    val title = s"${users.size} Online bots"

    val sorted = users.sortBy { -_.playTime.??(_.total) }

    views.html.base.layout(
      title = title,
      moreCss = frag(cssTag("slist"), cssTag("bot.list")),
      wrapClass = "full-screen-force"
    )(
      main(cls := "page-menu bots")(
        user.bits.communityMenu("bots"),
        sorted.partition(_.isVerified) match {
          case (featured, all) =>
            div(cls := "bots page-menu__content")(
              div(cls := "box bots__featured")(
                h1(cls := "box__top")("Featured bots"),
                botTable(featured)
              ),
              div(cls := "box")(
                boxTop(
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

  private def botTable(users: List[User])(implicit ctx: Context) = div(cls := "bots__list")(
    users map { u =>
      div(cls := "bots__list__entry")(
        div(cls := "bots__list__entry__desc")(
          div(cls := "bots__list__entry__head")(
            userLink(u),
            ctx.pref.showRatings option div(cls := "bots__list__entry__rating")(
              u.bestAny3Perfs.map { showPerfRating(u, _) }
            )
          ),
          u.profile
            .ifTrue(ctx.noKid)
            .ifTrue(!u.marks.troll || ctx.is(u))
            .flatMap(_.nonEmptyBio)
            .map { bio => td(shorten(bio, 400)) }
        ),
        a(
          dataIcon := "",
          cls      := List("bots__list__entry__play button button-empty text" -> true),
          st.title := trans.challenge.challengeToPlay.txt(),
          href     := s"${routes.Lobby.home}?user=${u.username}#friend"
        )(trans.play())
      )
    }
  )
