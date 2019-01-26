package views.html
package study

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.user.User

import controllers.routes

object bits {

  def newButton()(implicit ctx: Context) = ctx.isAuth option
    form(cls := "new_study", action := routes.Study.create, method := "post")(
      button(`type` := "submit", cls := "button")("New study")
    )

  def newForm()(implicit ctx: Context) =
    form(cls := "new_study", action := routes.Study.create, method := "post")(
      button(`type` := "submit", cls := "button frameless hint--top", dataHint := "New study")(
        iconTag("O")
      )
    )

  def authLinks(me: User, active: String, order: lila.study.Order)(implicit ctx: Context) = frag(
    a(cls := "mine" == active option "mine", href := routes.Study.mine(order.key))("My studies"),
    a(cls := "mineMember" == active option "mineMember", href := routes.Study.mineMember(order.key))("Studies I contribute to"),
    a(cls := "minePublic" == active option "minePublic", href := routes.Study.minePublic(order.key))("My public studies"),
    a(cls := "minePrivate" == active option "minePrivate", href := routes.Study.minePrivate(order.key))("My private studies"),
    a(cls := "mineLikes" == active option "mineLikes", href := routes.Study.mineLikes(order.key))("Favourite studies")
  )

  def widget(s: lila.study.Study.WithChaptersAndLiked)(implicit ctx: Context) = frag(
    a(cls := "overlay", href := routes.Study.show(s.study.id.value)),
    h2(
      iconTag("4")(cls := "icon"),
      strong(s.study.name.value),
      span(
        iconTag(if (s.liked) "" else ""),
        " ",
        s.study.likes.value,
        " • ",
        usernameOrId(s.study.ownerId),
        " • ",
        momentFromNow(s.study.createdAt)
      )
    ),
    div(cls := "body")(
      ol(cls := "chapters")(
        s.chapters.take(4).map { name =>
          li(cls := "text", dataIcon := "K")(name.value)
        }
      ),
      ol(cls := "members")(
        s.study.members.members.values.take(4).map { m =>
          li(cls := "text", dataIcon := (if (m.canContribute) "" else "v"))(usernameOrId(m.id))
        } toList
      )
    )
  )
}
