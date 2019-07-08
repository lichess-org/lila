package views.html
package study

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import controllers.routes

object bits {

  def newButton()(implicit ctx: Context) = ctx.isAuth option
    form(cls := "new-study", action := routes.Study.create, method := "post")(
      button(tpe := "submit", cls := "button")("New study")
    )

  def newForm()(implicit ctx: Context) =
    form(cls := "new-study", action := routes.Study.create, method := "post")(
      button(tpe := "submit", cls := "button button-green", dataIcon := "O", title := "New study")
    )

  def authLinks(me: User, active: String, order: lila.study.Order)(implicit ctx: Context) = {
    def activeCls(c: String) = cls := (c == active).option("active")
    frag(
      a(activeCls("mine"), href := routes.Study.mine(order.key))("My studies"),
      a(activeCls("mineMember"), href := routes.Study.mineMember(order.key))("Studies I contribute to"),
      a(activeCls("minePublic"), href := routes.Study.minePublic(order.key))("My public studies"),
      a(activeCls("minePrivate"), href := routes.Study.minePrivate(order.key))("My private studies"),
      a(activeCls("mineLikes"), href := routes.Study.mineLikes(order.key))("Favourite studies")
    )
  }

  def widget(s: lila.study.Study.WithChaptersAndLiked, tag: Tag = h2)(implicit ctx: Context) = frag(
    a(cls := "overlay", href := routes.Study.show(s.study.id.value)),
    div(cls := "top", dataIcon := "4")(
      div(
        tag(cls := "study-name")(s.study.name.value),
        span(
          !s.study.isPublic option frag(
            iconTag("a")(cls := "private", ariaTitle("Private")),
            " "
          ),
          iconTag(if (s.liked) "" else ""),
          " ",
          s.study.likes.value,
          " • ",
          usernameOrId(s.study.ownerId),
          " • ",
          momentFromNow(s.study.createdAt)
        )
      )
    ),
    div(cls := "body")(
      ol(cls := "chapters")(
        s.chapters.map { name =>
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

  def streamers(streams: List[lila.streamer.Stream]) =
    streams.nonEmpty option div(cls := "streamers none")(
      streams.map { s =>
        views.html.streamer.bits.contextual(s.streamer.userId)
      }
    )
}
