package views.html
package study

import controllers.routes
import play.api.i18n.Lang
import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.removeMultibyteSymbols
import lila.study.{ Order, Study }

object bits:

  def orderSelect(order: Order, active: String, url: String => Call)(using Context) =
    val orders =
      if (active == "all") Order.withoutSelector
      else if (active startsWith "topic") Order.list
      else Order.withoutMine
    views.html.base.bits.mselect(
      "orders",
      span(order.name()),
      orders.map { o =>
        a(href := url(o.key), cls := (order == o).option("current"))(o.name())
      }
    )

  def newForm()(using Context) =
    postForm(cls := "new-study", action := routes.Study.create)(
      submitButton(cls := "button button-green", dataIcon := "", title := trans.study.createStudy.txt())
    )

  def authLinks(active: String, order: Order)(using Context) =
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

  def widget(s: Study.WithChaptersAndLiked, tag: Tag = h2)(using ctx: Context) =
    frag(
      a(cls := "overlay", href := routes.Study.show(s.study.id), title := s.study.name),
      div(cls := "top", dataIcon := "")(
        div(
          tag(cls := "study-name")(s.study.name),
          span(
            !s.study.isPublic option frag(
              iconTag("")(cls := "private", ariaTitle(trans.study.`private`.txt())),
              " "
            ),
            iconTag(if (s.liked) "" else ""),
            " ",
            s.study.likes.value,
            " • ",
            titleNameOrId(s.study.ownerId),
            " • ",
            momentFromNow(s.study.createdAt)
          )
        )
      ),
      div(cls := "body")(
        ol(cls := "chapters")(
          s.chapters.map { name =>
            li(cls := "text", dataIcon := "")(
              if (ctx.userId.exists(s.study.isMember)) name
              else removeMultibyteSymbols(name.value)
            )
          }
        ),
        ol(cls := "members")(
          s.study.members.members.values
            .take(Study.previewNbMembers)
            .map { m =>
              li(cls := "text", dataIcon := (if (m.canContribute) "" else ""))(titleNameOrId(m.id))
            }
            .toList
        )
      )
    )

  def streamers(streamers: List[UserId])(implicit lang: Lang) =
    streamers.nonEmpty option div(cls := "context-streamers none")(
      streamers map views.html.streamer.bits.contextual
    )
