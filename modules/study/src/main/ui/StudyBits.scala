package lila.study
package ui

import lila.common.String.removeMultibyteSymbols
import lila.core.study.Order
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class StudyBits(helpers: Helpers):
  import helpers.{ *, given }

  def orderSelect(order: Order, active: String, url: Order => Call)(using Context) =
    val orders =
      if active == "all" then Orders.withoutSelector
      else if active.startsWith("topic") then Orders.list
      else Orders.withoutMine
    lila.ui.bits.mselect(
      "orders",
      span(Orders.name(order)()),
      orders.map: o =>
        a(href := url(o), cls := (order == o).option("current"))(Orders.name(o)())
    )

  def newForm()(using Context) =
    postForm(cls := "new-study", action := routes.Study.create)(
      submitButton(
        cls := "button button-green",
        dataIcon := Icon.PlusButton,
        title := trans.study.createStudy.txt()
      )
    )

  def authLinks(active: String, order: Order)(using Context) =
    def activeCls(c: String) = cls := (c == active).option("active")
    frag(
      a(activeCls("mine"), href := routes.Study.mine(order))(trans.study.myStudies()),
      a(activeCls("mineMember"), href := routes.Study.mineMember(order))(
        trans.study.studiesIContributeTo()
      ),
      a(activeCls("minePublic"), href := routes.Study.minePublic(order))(trans.study.myPublicStudies()),
      a(activeCls("minePrivate"), href := routes.Study.minePrivate(order))(
        trans.study.myPrivateStudies()
      ),
      a(activeCls("mineLikes"), href := routes.Study.mineLikes(order))(trans.study.myFavoriteStudies())
    )

  def widget(s: Study.WithChaptersAndLiked, tag: Tag = h2)(using ctx: Context) =
    frag(
      a(cls := "overlay", href := routes.Study.show(s.study.id), title := s.study.name),
      div(cls := "top")(
        div(cls := "study__icon")(
          s.study.flair
            .map(iconFlair)
            .getOrElse(iconTag(Icon.StudyBoard))
        ),
        div(
          tag(cls := "study-name")(s.study.name),
          span(
            (!s.study.isPublic).option(
              frag(
                iconTag(Icon.Padlock)(cls := "private", ariaTitle(trans.study.`private`.txt())),
                " "
              )
            ),
            iconTag(if s.liked then Icon.Heart else Icon.HeartOutline),
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
          s.chapters.map: name =>
            li(cls := "text", dataIcon := Icon.DiscBigOutline)(
              if ctx.userId.exists(s.study.isMember) then name
              else removeMultibyteSymbols(name.value)
            )
        ),
        ol(cls := "members")(
          s.study.members.members.values
            .take(Study.previewNbMembers)
            .map: m =>
              li(cls := "text", dataIcon := (if m.canContribute then Icon.RadioTower else Icon.Eye))(
                titleNameOrId(m.id)
              )
            .toList
        )
      )
    )
