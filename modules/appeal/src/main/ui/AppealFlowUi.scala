package lila.appeal
package ui

import play.api.data.Form

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.config.NetDomain

final class AppealFlowUi(helpers: Helpers, ui: AppealUi)(using NetDomain):
  import helpers.{ *, given }

  def userFlow(appeal: Appeal, appeals: List[Appeal])(using Context, Me) =
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
        ),
        ui.userInactiveAppeals(appeals.filter(_ != appeal))
      )

  // TODO:
  def modFlow(appeal: Appeal, form: Form[?], modData: ModData)(using ctx: Context, me: Me) =
    import modData.*
    ui.page(s"Appeal by ${user.username}"):
      main(cls := "appeal")(
        div(cls := "box box-pad")(
          ui.modHeader(appeal, modData),
          div(cls := "mod-zone mod-zone-full none"),
          appeal.accounts.map(ui.renderAccountsDisclosure),
          otherUsers(cls := "mod-zone communication__logins"),
          div(cls := "body")(
            appeal.msgs.map(renderMsg(appeal)),
            renderNextNode(appeal),
            standardFlash.orElse(markedByMe.option(ui.markedByMeWarning)),
            if appeal.isClosed then ui.appealIsClosed(appeal)
            // else if me.is(inquiryBy) then modReplyForm(appeal, form, presets)
            else emptyFrag
          ),
          ui.modActions(appeal, modData)
        ),
        ui.userInactiveAppeals(userAppeals.filter(_ != appeal))
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

  private def renderNextNode(appeal: Appeal)(using me: Me) =
    val isUser = me.is(appeal.user)
    AppealFlow.nextNode(appeal) match
      case Some(cn: ChoiceNode) if cn.answerer == Answerer.User =>
        if isUser then renderChoiceForm(appeal.topic, cn) else renderPendingUserChoice(cn)
      case Some(cn: ChoiceNode) if cn.answerer == Answerer.Mod && !isUser =>
        renderChoiceForm(appeal.topic, cn)
      case _ if isUser =>
        p(cls := "line-center-text"):
          "Your appeal is under review. You will receive a message when there is an update."
      case _ => emptyFrag

  private def renderChoiceForm(topic: AppealTopic, cn: ChoiceNode) =
    postForm(cls := "appeal__choice", action := routes.Appeal.event(topic))(
      p(cls := "appeal__choice__question")(cn.question),
      form3.hidden("kind", AppealMsg.Kind.userChoice.toString),
      form3.hidden("nodeId", cn.id),
      div(cls := "appeal__choice__answers")(
        cn.branches.toList.map: b =>
          submitButton(cls := "button button-no-upper", name := "answerId", value := b.id)(b.answer)
      )
    )

  private def renderPendingUserChoice(cn: ChoiceNode) =
    div(cls := "appeal__choice appeal__choice--pending")(
      p(cls := "appeal__choice__waiting")("Awaiting the user's answer"),
      p(cls := "appeal__choice__question")(cn.question),
      div(cls := "appeal__choice__answers")(
        cn.branches.toList.map: b =>
          span(cls := "appeal__choice__option")(b.answer)
      )
    )
