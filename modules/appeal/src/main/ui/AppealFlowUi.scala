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
          div(cls := "body")(
            appeal.msgs.map(renderMsg(appeal)),
            renderNextNode(appeal)
          )
        )
        // TODO: reimplement the below
        // userInactiveAppeals(appeals.filter(_ != appeal))
      )

  private def renderMsg(appeal: Appeal)(msg: AppealMsg)(using Context) =
    msg match
      case UserChoiceEvent(by, _, question, _, answer, at) =>
        renderChoiceEvent(appeal, by, question, answer, at)
      case ModChoiceEvent(by, _, question, _, answer, at) =>
        renderChoiceEvent(appeal, by, question, answer, at)
      case _ =>
        div(cls := s"appeal__msg appeal__msg--${if appeal.isByMod(msg) then "mod" else "suspect"}")(
          div(cls := "appeal__msg__header")(
            ui.renderUser(appeal, msg.by, asMod = false),
            momentFromNowOnce(msg.at)
          ),
          div(cls := "appeal__msg__text")(richText(msg.text, expandImg = false))
        )

  private def renderChoiceEvent(appeal: Appeal, by: UserId, question: String, answer: String, at: Instant)(
      using Context
  ) =
    div(cls := "appeal__choice-event")(
      p(cls := "appeal__choice-event__question")(question),
      div(cls := "appeal__choice-event__selection")(
        span(cls := "appeal__choice-event__answer text")(answer),
        span(cls := "appeal__choice-event__meta")(
          ui.renderUser(appeal, by, asMod = false),
          span(" · "),
          momentFromNowOnce(at)
        )
      )
    )

  private def renderNextNode(appeal: Appeal) =
    AppealFlow.nextNode(appeal) match
      case Some(ChoiceNode(nodeId, Answerer.User, question, branches)) =>
        postForm(cls := "appeal__choice", action := routes.Appeal.event(appeal.topic))(
          p(cls := "appeal__choice__question")(question),
          form3.hidden("kind", AppealMsg.Kind.userChoice.toString),
          form3.hidden("nodeId", nodeId),
          div(cls := "appeal__choice__answers")(
            branches.toList.map: b =>
              submitButton(cls := "button button-no-upper", name := "answerId", value := b.id)(b.answer)
          )
        )
      case _ => emptyFrag
