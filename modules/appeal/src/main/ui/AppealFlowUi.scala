package lila.appeal
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.config.NetDomain

final class AppealFlowUi(helpers: Helpers, ui: AppealUi)(using NetDomain):
  import helpers.{ *, given }

  def userShow(appeal: Appeal, appeals: List[Appeal])(using Context, Me) =
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
            appealEvents(appeal),
            userNextNode(appeal)
          )
        ),
        ui.userInactiveAppeals(appeals.filter(_ != appeal))
      )

  def modShow(appeal: Appeal, modData: ModData)(using ctx: Context, me: Me) =
    import modData.*
    val onFlow = appeal.msgs.forall(isFlowMsg)
    val handling = me.is(inquiryBy)
    ui.page(s"Appeal by ${user.username}"):
      main(
        cls := List(
          "appeal" -> true,
          "appeal--on-flow" -> onFlow,
          "appeal--automated" -> (onFlow && handling)
        )
      )(
        div(cls := "box box-pad")(
          ui.modHeader(appeal, modData),
          div(cls := "mod-zone mod-zone-full none"),
          appeal.accounts.map(ui.accountsDisclosure),
          otherUsers(cls := "mod-zone communication__logins"),
          div(cls := "body")(
            if onFlow then flowTrack(frag(appealEvents(appeal), modNextNode(appeal, modData)))
            else appealEvents(appeal, track = true),
            (onFlow && handling).option(
              button(
                tpe := "button",
                cls := "button button-empty button-no-upper appeal__leave-flow"
              )("Leave automated flow")
            ),
            standardFlash.orElse(markedByMe.option(ui.markedByMeWarning)),
            if appeal.isClosed then ui.appealIsClosed(appeal)
            else if handling then modMessageForm(appeal, modData)
            else emptyFrag
          ),
          div(cls := "appeal__manual")(ui.modActions(appeal, modData))
        ),
        ui.userInactiveAppeals(userAppeals.filter(_ != appeal))
      )

  private def appealEvent(appeal: Appeal)(msg: AppealMsg)(using Context, Me) =
    div(id := appeal.isLast(msg).option("appeal-last-msg"))(ui.event(appeal)(msg))

  private def appealEvents(appeal: Appeal, track: Boolean = false)(using Context, Me) =
    if track then
      val (flowMsgs, rest) = appeal.msgs.span(isFlowMsg)
      frag(
        if flowMsgs.nonEmpty then flowTrack(frag(flowMsgs.map(appealEvent(appeal))*)) else emptyFrag,
        frag(rest.map(appealEvent(appeal))*)
      )
    else frag(appeal.msgs.map(appealEvent(appeal))*)

  private def flowTrack(body: Frag) =
    div(cls := "appeal__flow")(
      span(cls := "appeal__flow__track", title := "Automated appeal flow"),
      body
    )

  private def isFlowMsg(msg: AppealMsg) = msg match
    case _: ChoiceEvent => true
    case _: ActionEvent => true
    case _: MessageEvent => false
    case _: LegacyMessage => false

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
      case Some(cn: ChoiceNode) =>
        div(cls := "appeal__next"):
          if cn.answerer == Answerer.User then pendingUserChoice(cn)
          else choiceForm(appeal, cn, enabled = me.is(modData.inquiryBy))
      case _ => emptyFrag

  private def userMessageForm(appeal: Appeal)(using Context): Frag =
    postForm(action := routes.Appeal.userEvent(appeal.topic))(
      form3.hidden("kind", AppealMsg.Kind.message.toString),
      form3.group(
        AppealEventForm.userMessageForm("text"),
        "Add something to the appeal",
        help = frag("Please be concise. Maximum 1000 chars.").some
      )(form3.textarea(_)(rows := 6, maxlength := Appeal.maxLength * 1.1))(
        cls := "appeal-textarea"
      ),
      form3.action(form3.submit("Send"))
    )

  private def modMessageForm(appeal: Appeal, modData: ModData)(using Context): Frag =
    postForm(cls := "appeal__manual", action := routes.Appeal.modEvent(appeal.user, appeal.topic))(
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
          AppealEventForm.modMessageForm("text"),
          "Add something to the appeal",
          half = true,
          help = AppealFlow
            .nextNode(appeal)
            .isDefined
            .option(frag("Note: by adding a reply, you will exit the automated flow"))
        )(form3.textarea(_)(rows := 15))(cls := "appeal-textarea")
      ),
      form3.action(
        form3.submit("Send & close", nameValue = ("close", "true").some, icon = none)(
          cls := "button-red button-empty"
        ),
        form3.submit("Send")(cls := "button-empty"),
        form3.submit("Send & dismiss", nameValue = ("dismiss", "true").some)(
          cls := "button-green",
          title := "Dismiss the appeal as processed"
        )
      )
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
