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
            appeal.msgs.map(ui.renderMsg(appeal)),
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
            appeal.msgs.map(ui.renderMsg(appeal)),
            renderNextNode(appeal, modData.some),
            standardFlash.orElse(markedByMe.option(ui.markedByMeWarning)),
            if appeal.isClosed then ui.appealIsClosed(appeal)
            // else if me.is(inquiryBy) then modReplyForm(appeal, form, presets)
            else emptyFrag
          ),
          ui.modActions(appeal, modData)
        ),
        ui.userInactiveAppeals(userAppeals.filter(_ != appeal))
      )

  private def renderNextNode(appeal: Appeal, modData: Option[ModData] = None)(using ctx: Context, me: Me) =
    val isMod = me.isnt(appeal.user)
    val isHandledByMe = me.is(modData.flatMap(_.inquiryBy))
    AppealFlow.nextNode(appeal) match
      case Some(cn: ChoiceNode) if cn.answerer == Answerer.User =>
        if isMod then renderPendingUserChoice(cn)
        else renderChoiceForm(appeal, cn)
      case Some(cn: ChoiceNode) if cn.answerer == Answerer.Mod =>
        if isMod then renderChoiceForm(appeal, cn, isHandledByMe)
        else
          p(cls := "line-center-text"):
            "Your appeal is under review. You will receive a message when there is an update."
      case _ =>
        if (isMod && isHandledByMe) || (!isMod && appeal.canAddMsg) then renderMessageForm(appeal)
        else emptyFrag

  private def renderMessageForm(appeal: Appeal)(using ctx: Context, me: Me) =
    val isMod = me.isnt(appeal.user)
    postForm(
      cls := "",
      action := (if isMod then routes.Appeal.modEvent(appeal.user, appeal.topic)
                 else routes.Appeal.userEvent(appeal.topic))
    )(
      form3.hidden("kind", AppealMsg.Kind.message.toString),
      form3.group(
        AppealEventForm.messageForm("text"),
        "",
        help = frag("Please be concise. Maximum 1000 chars.").some
      )(f =>
        form3.textarea(f)(
          rows := 6,
          maxlength := Appeal.maxLength * 1.1
        )
      )(cls := "appeal-textarea"),
      form3.action(form3.submit("Send"))
    )

  private def renderChoiceForm(appeal: Appeal, cn: ChoiceNode, enabled: Boolean = true)(using me: Me) =
    val isMod = me.isnt(appeal.user)
    postForm(
      cls := "appeal__choice",
      action := (if isMod then routes.Appeal.modEvent(appeal.user, appeal.topic)
                 else routes.Appeal.userEvent(appeal.topic))
    )(
      p(cls := "appeal__choice__question")(cn.question),
      form3.hidden("kind", AppealMsg.Kind.choice.toString),
      form3.hidden("nodeId", cn.id),
      div(cls := "appeal__choice__answers")(
        cn.branches.toList.map: b =>
          submitButton(
            cls := "button button-no-upper",
            (!enabled).option(disabled := true),
            name := "answerId",
            value := b.id
          )(b.answer)
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
