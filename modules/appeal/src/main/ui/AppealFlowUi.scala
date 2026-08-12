package lila.appeal
package ui

import play.api.data.Form

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.userId.ModId
import lila.core.config.NetDomain

final class AppealFlowUi(helpers: Helpers, ui: AppealUi)(using NetDomain):
  import helpers.{ *, given }

  def userFlow(status: UserStatus, appeal: Appeal, form: Form[?], appeals: List[Appeal])(using Context, Me) =
    ui.page("Appeal"):
      main(cls := "page-small appeal")(
        div(cls := "box box-pad")(
          h1(cls := "box__top")(
            div(cls := "title")(span(cls := "appeal-topic")(appeal.topic.key), " Appeal in progress. Hello world")
          ),
        ),
        // userInactiveAppeals(appeals.filter(_ != appeal))
      )


