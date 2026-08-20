package lila.appeal
package ui

import play.api.data.Form

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.config.NetDomain

final class AppealFlowUi(helpers: Helpers, ui: AppealUi)(using NetDomain):
  import helpers.*

  def userFlow(status: UserStatus, appeal: Appeal, form: Form[?], appeals: List[Appeal])(using Context, Me) =
    ui.page("Appeal"):
      main(cls := "page-small appeal")(
        div(cls := "box box-pad")(
          h1(cls := "box__top")(
            div(cls := "title")(
              span(cls := "appeal-topic")(appeal.topic.key),
              " Appeal in progress."
            )
          ),
          // TODO: render user visible previous events
          (AppealFlowApi.nextNode(appeal) match
            case Some(UserChoiceNode(question, branches)) => div(cls := "box")(
              postForm(cls := "appeal-choice", action := routes.Appeal.event(appeal.topic))(
                p(question),
                form3.hidden("kind", AppealMsg.Kind.userChoice.toString),
                div(cls := "appeal-choice__answers")(
                  branches.map: (answer, _) =>
                    submitButton(cls := "button", name := "answer", value := answer)(answer)
                )
              )
            )
            case _ => emptyFrag
          )
        )
        // TODO: reimplement somehow the below
        // userInactiveAppeals(appeals.filter(_ != appeal))
      )
