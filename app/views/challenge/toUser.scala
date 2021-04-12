package views
package html.challenge

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User
import lila.challenge.Challenge

object toUser {

  def apply(user: User, challenges: List[Challenge])(implicit ctx: Context) = {
    val name         = user.username
    val challengeUrl = s"${routes.Lobby.home}?user=$name#friend"
    views.html.base.layout(
      title = s"Challenge to ${name}",
      moreCss = cssTag("challenge.page")
    )(
      main(cls := "page-menu")(
        div(cls := "page-menu__content challenge-list box")(
          div(cls := "box__top")(
            h1(
              s"Challenge to ",
              userLink(user)
            ),
            ctx.isAuth option div(cls := "box__top__actions")(
              a(
                href := challengeUrl,
                cls := "button button-green text",
                dataIcon := "O"
              )("Create a challenge")
            )
          ),
          table(cls := "slist slist-pad")(
            tbody(
              challenges.zipWithIndex.map { case (c, i) =>
                tr(
                  td(i + 1),
                  td(
                    userIdLink(c.challengerUserId),
                    c.challengerUser.map(u => frag(" (", u.rating.show, ")"))
                  ),
                  td(modeName(c.mode)),
                  td(c.clock.fold("No Clock")(_.show)),
                  td(variantName(c.variant)),
                  td(momentFromNow(c.createdAt))
                )
              }
            )
          )
        )
      )
    )
  }
}
