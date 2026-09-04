package lila.study
package ui

import lila.common.String.removeMultibyteSymbols
import lila.core.study.StudyOrder
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class StudyBits(helpers: Helpers):
  import helpers.{ *, given }

  def orderSelect(order: StudyOrder, active: StudyGroup, url: StudyOrder => Call)(using Context) =
    val orders =
      if active == StudyGroup.search then Orders.search
      else if active == StudyGroup.all then Orders.withoutSelector
      else if active.isTopic then Orders.list
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
        iconEl := Icon.plusButton,
        title := trans.study.createStudy.txt()
      )
    )

  def authLinks(activeCls: StudyGroup => AttrPair, order: StudyGroup => StudyOrder)(using Context) =
    frag(
      a(activeCls(StudyGroup.mine), href := routes.Study.mine(order(StudyGroup.mine)))(
        trans.study.myStudies()
      ),
      a(activeCls(StudyGroup.mineMember), href := routes.Study.mineMember(order(StudyGroup.mineMember)))(
        trans.study.studiesIContributeTo()
      ),
      a(activeCls(StudyGroup.minePublic), href := routes.Study.minePublic(order(StudyGroup.minePublic)))(
        trans.study.myPublicStudies()
      ),
      a(activeCls(StudyGroup.minePrivate), href := routes.Study.minePrivate(order(StudyGroup.minePrivate)))(
        trans.study.myPrivateStudies()
      ),
      a(activeCls(StudyGroup.mineLikes), href := routes.Study.mineLikes(order(StudyGroup.mineLikes)))(
        trans.study.myFavoriteStudies()
      )
    )

  def widget(s: Study.WithChaptersAndLiked, tag: Tag = h2)(using ctx: Context) =
    frag(
      a(cls := "overlay", href := routes.Study.show(s.study.id), title := s.study.name),
      canUnfeature.option(unfeatureForm(s)),
      div(cls := "top")(
        div(cls := "study__icon")(
          s.study.flair
            .map(iconFlair)
            .getOrElse(iconEl(Icon.studyBoard))
        ),
        div(
          tag(cls := "study-name")(s.study.name),
          span(cls := "study-meta")(
            (!s.study.isPublic).option(
              frag(
                iconEl(Icon.padlock)(cls := "private", ariaTitle(trans.study.`private`.txt())),
                " "
              )
            ),
            iconEl(if s.liked then Icon.heart else Icon.heartOutline),
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
            li(cls := "text", iconEl := Icon.discBigOutline)(
              if ctx.userId.exists(s.study.isMember) then name
              else removeMultibyteSymbols(name.value)
            )
        ),
        ol(cls := "members")(
          s.study.members.members.values
            .take(Study.previewNbMembers)
            .map: m =>
              li(cls := "text", iconEl := (if m.canContribute then Icon.radioTower else Icon.eye))(
                titleNameOrId(m.id)
              )
            .toList
        )
      )
    )

  private def unfeatureForm(s: Study.WithChaptersAndLiked) =
    postForm(cls := "study-unfeature", action := s"${routes.Study.admin(s.study.id)}?unfeature=1"):
      submitButton(
        cls := "button button-red button-empty yes-no-confirm",
        iconEl := Icon.trash,
        title := "Unfeature trash study"
      )
