package lila.appeal
package ui

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
            appeal.msgs.map(ui.message(appeal)),
            userNextNode(appeal)
          )
        ),
        ui.userInactiveAppeals(appeals.filter(_ != appeal))
      )

  def modFlow(appeal: Appeal, modData: ModData)(using ctx: Context, me: Me) =
    import modData.*
    ui.page(s"Appeal by ${user.username}"):
      main(cls := "appeal")(
        div(cls := "box box-pad")(
          ui.modHeader(appeal, modData),
          div(cls := "mod-zone mod-zone-full none"),
          appeal.accounts.map(ui.accountsDisclosure),
          otherUsers(cls := "mod-zone communication__logins"),
          div(cls := "body")(
            modAppealMessages(appeal),
            standardFlash.orElse(markedByMe.option(ui.markedByMeWarning)),
            modNextNode(appeal, modData),
            if appeal.isClosed then ui.appealIsClosed(appeal)
            else if me.is(inquiryBy) then modMessageForm(appeal, modData)
            else emptyFrag
          ),
          ui.modActions(appeal, modData)
        ),
        ui.userInactiveAppeals(userAppeals.filter(_ != appeal))
      )

  private def modAppealMessages(appeal: Appeal)(using Context, Me) =
    appeal.msgs.map: msg =>
      div(
        id := appeal.isLast(msg).option("appeal-last-msg")
      )(ui.message(appeal)(msg))

  private def userNextNode(appeal: Appeal)(using Context, Me) =
    AppealFlow.nextNode(appeal) match
      case Some(cn: ChoiceNode) if cn.answerer == Answerer.User =>
        choiceForm(appeal, cn)
      case Some(cn: ChoiceNode) if cn.answerer == Answerer.Mod =>
        p(cls := "line-center-text"):
          "Your appeal is under review. You will receive a message when there is an update."
      case _ => appeal.canAddMsg.so(userMessageForm(appeal))

  private def modNextNode(appeal: Appeal, modData: ModData)(using me: Me) =
    AppealFlow.nextNode(appeal) match
      case Some(cn: ChoiceNode) if cn.answerer == Answerer.User =>
        pendingUserChoice(cn)
      case Some(cn: ChoiceNode) if cn.answerer == Answerer.Mod =>
        choiceForm(appeal, cn, enabled = me.is(modData.inquiryBy))
      case _ => emptyFrag

  private def userMessageForm(appeal: Appeal)(using Context): Frag =
    postForm(action := routes.Appeal.userEvent(appeal.topic))(
      form3.hidden("kind", AppealMsg.Kind.message.toString),
      form3.group(
        AppealEventForm.messageForm("text"),
        "Add something to the appeal",
        help = frag("Please be concise. Maximum 1000 chars.").some
      )(form3.textarea(_)(rows := 6, maxlength := Appeal.maxLength * 1.1))(
        cls := "appeal-textarea"
      ),
      form3.action(form3.submit("Send"))
    )

  private def modMessageForm(appeal: Appeal, modData: ModData)(using Context): Frag =
    postForm(action := routes.Appeal.modEvent(appeal.user, appeal.topic))(
      form3.hidden("kind", AppealMsg.Kind.message.toString),
      form3.split(
        div(cls := "appeal-presets form-group form-half")(
          modData.presets.map: (name, text) =>
            button(
              tpe := "button",
              st.value := text,
              st.title := text
            )(name)
        ),
        form3.group(
          AppealEventForm.messageForm("text"),
          "Add something to the appeal",
          half = true,
          help = AppealFlow
            .nextNode(appeal)
            .isDefined
            .option(frag("Note: by adding a reply, you will exit the automated flow"))
        )(form3.textarea(_)(rows := 15))(cls := "appeal-textarea")
      ),
      form3.action(form3.submit("Send"))
    )

  private def choiceForm(appeal: Appeal, cn: ChoiceNode, enabled: Boolean = true)(using me: Me) =
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

  private def pendingUserChoice(cn: ChoiceNode) =
    div(cls := "appeal__choice appeal__choice--pending")(
      p(cls := "appeal__choice__waiting")("Awaiting the user's answer"),
      p(cls := "appeal__choice__question")(cn.question),
      div(cls := "appeal__choice__answers")(
        cn.branches.toList.map: b =>
          span(cls := "appeal__choice__option")(b.answer)
      )
    )
