package views.html
package study

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.user.User

import controllers.routes

object bits {

  def newForm()(implicit ctx: Context) =
    postForm(cls := "new-study", action := routes.Study.create)(
      submitButton(cls := "button button-green", dataIcon := "O", title := trans.study.createStudy.txt())
    )

  def authLinks(me: User, active: String, order: lidraughts.study.Order)(implicit ctx: Context) = {
    def activeCls(c: String) = cls := (c == active).option("active")
    frag(
      a(activeCls("mine"), href := routes.Study.mine(order.key))(trans.study.myStudies()),
      a(activeCls("mineMember"), href := routes.Study.mineMember(order.key))(trans.study.studiesIContributeTo()),
      a(activeCls("minePublic"), href := routes.Study.minePublic(order.key))(trans.study.myPublicStudies()),
      a(activeCls("minePrivate"), href := routes.Study.minePrivate(order.key))(trans.study.myPrivateStudies()),
      a(activeCls("mineLikes"), href := routes.Study.mineLikes(order.key))(trans.study.myFavoriteStudies())
    )
  }

  def widget(s: lidraughts.study.Study.WithChaptersAndLiked, tag: Tag = h2)(implicit ctx: Context) = frag(
    a(cls := "overlay", href := routes.Study.show(s.study.id.value)),
    div(cls := "top", dataIcon := "4")(
      div(
        tag(cls := "study-name")(s.study.name.value),
        span(
          !s.study.isPublic option frag(
            iconTag("a")(cls := "private", ariaTitle(trans.study.`private`.txt())),
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

  def streamers(streams: List[lidraughts.streamer.Stream]) =
    streams.nonEmpty option div(cls := "streamers none")(
      streams.map { s =>
        views.html.streamer.bits.contextual(s.streamer.userId)
      }
    )
}
