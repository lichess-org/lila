package lila.appeal
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.config.NetDomain

final class AppealFlowUi(helpers: Helpers, ui: AppealUi)(using NetDomain):
  import helpers.*

  def userFlow(appeal: Appeal)(using Context, Me) =
    ui.page("Appeal"):
      main(cls := "page-small appeal")(
        div(cls := "box box-pad")(
          h1(cls := "box__top")(
            div(cls := "title")(
              span(cls := "appeal-topic")(appeal.topic.key),
              " Appeal in progress."
            )
          ),
          appeal.msgs.map(_ match
            case LegacyMessage(by, text, _) => div(cls := "box")(s"${by}: ${text}")
            case UserChoiceEvent(by, _, question, _, answer, _) =>
              div(cls := "box")(s"${question}\n${by}: ${answer}")
            case ModChoiceEvent(by, _, question, _, answer, _) =>
              div(cls := "box")(s"${question}\n${by}: ${answer}")
            case UserMessageEvent(by, text, _) => div(cls := "box")(s"${by}: ${text}")
            case ModMessageEvent(by, text, _) => div(cls := "box")(s"${by}: ${text}")),
          (AppealFlow.nextNode(appeal) match
            case Some(ChoiceNode(nodeId, Answerer.User, question, branches)) =>
              div(cls := "box")(
                postForm(cls := "appeal-choice", action := routes.Appeal.event(appeal.topic))(
                  p(question),
                  form3.hidden("kind", AppealMsg.Kind.userChoice.toString),
                  form3.hidden("nodeId", nodeId),
                  div(cls := "appeal-choice__answers")(
                    branches.toList.map: b =>
                      submitButton(cls := "button", name := "answerId", value := b.id)(b.answer)
                  )
                )
              )
            case _ => emptyFrag
          )
        )
        // TODO: reimplement somehow the below
        // userInactiveAppeals(appeals.filter(_ != appeal))
      )
