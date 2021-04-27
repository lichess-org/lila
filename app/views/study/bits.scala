package views.html
package study

import play.api.i18n.Lang
import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.study.Order

import controllers.routes

object bits {

  def orderSelect(order: Order, active: String, url: String => Call)(implicit ctx: Context) = {
    val orders =
      if (active == "all") Order.allButOldest
      else if (active startsWith "topic") Order.allWithMine
      else Order.all
    views.html.base.bits.mselect(
      "orders",
      span(order.name()),
      orders map { o =>
        a(href := url(o.key), cls := (order == o).option("current"))(o.name())
      }
    )
  }

  def newForm()(implicit ctx: Context) =
    postForm(cls := "new-study", action := routes.Study.create)(
      submitButton(cls := "button button-green", dataIcon := "O", title := trans.study.createStudy.txt())
    )

  def authLinks(active: String, order: lila.study.Order)(implicit ctx: Context) = {
    def activeCls(c: String) = cls := (c == active).option("active")
    frag(
      a(activeCls("mine"), href := routes.Study.mine(order.key))(trans.study.myStudies()),
      a(activeCls("mineMember"), href := routes.Study.mineMember(order.key))(
        trans.study.studiesIContributeTo()
      ),
      a(activeCls("minePublic"), href := routes.Study.minePublic(order.key))(trans.study.myPublicStudies()),
      a(activeCls("minePrivate"), href := routes.Study.minePrivate(order.key))(
        trans.study.myPrivateStudies()
      ),
      a(activeCls("mineLikes"), href := routes.Study.mineLikes(order.key))(trans.study.myFavoriteStudies())
    )
  }

  def widget(s: lila.study.Study.WithChaptersAndLiked, tag: Tag = h2)(implicit ctx: Context) =
    frag(
      a(cls := "overlay", href := routes.Study.show(s.study.id.value), title := s.study.name.value),
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
          s.study.members.members.values
            .take(4)
            .map { m =>
              li(cls := "text", dataIcon := (if (m.canContribute) "" else "v"))(usernameOrId(m.id))
            }
            .toList
        )
      )
    )

  def streamers(streamers: List[lila.user.User.ID])(implicit lang: Lang) =
    streamers.nonEmpty option div(cls := "context-streamers none")(
      streamers map views.html.streamer.bits.contextual
    )
}
