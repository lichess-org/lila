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
    val hasNextFlowStep = onFlow && appeal.nextNode.isDefined
    val handling = me.is(inquiryBy)
    ui.page(s"Appeal by ${user.username}"):
      main(
        cls := List(
          "appeal" -> true,
          "appeal--automated" -> (hasNextFlowStep && handling)
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
            (hasNextFlowStep && handling).option(
              button(
                tpe := "button",
                cls := "button button-empty button-no-upper text appeal__leave-flow",
                dataIcon := Icon.BubbleConvo
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
    case _: FreeResponseEvent => true
    case _: ActionEvent => true
    case _: MessageEvent => false
    case _: LegacyMessage => false

  private def userNextNode(appeal: Appeal)(using Context, Me) =
    AppealFlow.nextNode(appeal) match
      case Some(q: QuestionNode) if q.answerer == Answerer.Mod =>
        p(cls := "line-center-text"):
          "Your appeal is under review. You will receive a message when there is an update."
      case Some(cn: ChoiceNode) => choiceForm(appeal, cn)
      case Some(rn: FreeResponseNode) => freeResponseForm(appeal, rn)
      case _ => appeal.canAddMsg.so(userMessageForm(appeal))

  private def modNextNode(appeal: Appeal, modData: ModData)(using ctx: Context, me: Me) =
    AppealFlow.nextNode(appeal) match
      case Some(cn: ChoiceNode) =>
        div(cls := "appeal__next"):
          if cn.answerer == Answerer.User then pendingUserChoice(appeal, cn)
          else choiceForm(appeal, cn, enabled = me.is(modData.inquiryBy), preview = true)
      case Some(rn: FreeResponseNode) =>
        div(cls := "appeal__next"):
          if rn.answerer == Answerer.User then pendingUserFreeResponse(rn)
          else freeResponseForm(appeal, rn, enabled = me.is(modData.inquiryBy))
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

  private def choiceForm(
      appeal: Appeal,
      cn: ChoiceNode,
      enabled: Boolean = true,
      preview: Boolean = false
  )(using me: Me) =
    val isMod = me.isnt(appeal.user)
    postForm(
      cls := "appeal__choice",
      action := (if isMod then routes.Appeal.modEvent(appeal.user, appeal.topic)
                 else routes.Appeal.userEvent(appeal.topic))
    )(
      p(cls := "appeal__choice__question")(cn.question),
      form3.hidden("kind", AppealMsg.Kind.choice.toString),
      form3.hidden("nodeId", cn.id),
      if preview then
        linkedAnswers(appeal, cn.branches.toList): b =>
          submitButton(
            cls := "button button-no-upper",
            (!enabled).option(disabled := true),
            name := "answerId",
            value := b.id
          )(b.answer)
      else
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

  private def freeResponseForm(appeal: Appeal, node: FreeResponseNode, enabled: Boolean = true)(using
      ctx: Context,
      me: Me
  ) =
    val isMod = me.isnt(appeal.user)
    val form = if isMod then AppealEventForm.modFreeResponseForm else AppealEventForm.userFreeResponseForm
    postForm(
      cls := "appeal__free-response",
      action := (if isMod then routes.Appeal.modEvent(appeal.user, appeal.topic)
                 else routes.Appeal.userEvent(appeal.topic))
    )(
      p(cls := "appeal__free-response__question")(node.question),
      form3.hidden("kind", AppealMsg.Kind.freeResponse.toString),
      form3.hidden("nodeId", node.id),
      form3.group(
        form("freeResponse"),
        "",
        help = (!isMod).option(frag("Please be concise. Maximum 1000 chars."))
      )(field =>
        form3.textarea(field)(
          rows := 6,
          (!isMod).option(maxlength := Appeal.maxLength),
          (!enabled).option(disabled := true)
        )
      ),
      form3.action:
        form3.submit("Send")((!enabled).option(disabled := true))
    )

  private def pendingUserFreeResponse(node: FreeResponseNode) =
    div(cls := "appeal__free-response appeal__free-response--pending")(
      p(cls := "appeal__free-response__waiting")("Awaiting the user's response"),
      p(cls := "appeal__free-response__question")(node.question)
    )

  private def pendingUserChoice(appeal: Appeal, cn: ChoiceNode) =
    div(cls := "appeal__choice appeal__choice--pending")(
      p(cls := "appeal__choice__waiting")("Awaiting the user's answer"),
      p(cls := "appeal__choice__question")(cn.question),
      linkedAnswers(appeal, cn.branches.toList)(b => span(cls := "appeal__choice__option")(b.answer))
    )

  private def linkedAnswers(appeal: Appeal, branches: List[AnswerBranch])(answer: AnswerBranch => Frag) =
    def row(cells: List[Frag], between: => Frag) =
      div(cls := "appeal__choice__linked__row")(interleave(cells, between))
    def cell(body: Frag) = div(cls := "appeal__choice__cell")(body)
    def spacer = div(cls := "appeal__choice__or appeal__choice__or--spacer")("or")
    div(cls := "appeal__choice__linked")(
      row(branches.map(b => cell(answer(b))), div(cls := "appeal__choice__or")("or")),
      row(branches.map(_ => cell(div(cls := "appeal__choice__arrow", aria.hidden := "true"))), spacer),
      row(branches.map(b => cell(nextNodePreview(appeal, b))), spacer)
    )

  private def interleave(items: List[Frag], between: => Frag): List[Frag] =
    items match
      case Nil => Nil
      case only :: Nil => only :: Nil
      case head :: tail => head :: between :: interleave(tail, between)

  private def nextNodePreview(appeal: Appeal, branch: AnswerBranch): Frag =
    AppealFlow.findNodeById(appeal.topic, branch.nextNodeId).fold(emptyFrag)(nodePreview(appeal))

  private def nodePreview(appeal: Appeal)(node: AppealNode): Frag =
    node match
      case cn: ChoiceNode =>
        val (modifier, who) = cn.answerer match
          case Answerer.User => ("user", "User will answer")
          case Answerer.Mod => ("mod", "Moderator will answer")
        div(cls := s"appeal__choice__preview appeal__choice__preview--$modifier")(
          p(cls := "appeal__choice__preview__kicker")(who),
          p(cls := "appeal__choice__preview__body")(cn.question.trim),
          div(cls := "appeal__choice__preview__answers")(
            cn.branches.toList.map: b =>
              div(cls := "appeal__choice__preview__answer")(b.answer)
          )
        )
      case rn: FreeResponseNode =>
        val (modifier, who) = rn.answerer match
          case Answerer.User => ("user", "User will write a response")
          case Answerer.Mod => ("mod", "Moderator will write a response")
        div(cls := "appeal__choice__followup")(
          previewBox(modifier, who, rn.question),
          AppealFlow.findNodeById(appeal.topic, rn.nextNodeId).fold(emptyFrag)(nodePreview(appeal))
        )
      case an: ActionNode =>
        div(cls := "appeal__choice__followup appeal__choice__followup--steps")(
          interleave(
            List[Frag](previewBox("action", "Message", an.text)) ++ an.effects.map(effectBox),
            div(cls := "appeal__choice__arrow", aria.hidden := "true")
          )
        )

  private def previewBox(modifier: String, kicker: String, body: String) =
    div(cls := s"appeal__choice__preview appeal__choice__preview--$modifier")(
      p(cls := "appeal__choice__preview__kicker")(kicker),
      p(cls := "appeal__choice__preview__body")(body.trim)
    )

  private def effectBox(effect: AppealEffect) =
    div(cls := "appeal__choice__preview appeal__choice__preview--effect")(effectLabel(effect))

  private def effectLabel(effect: AppealEffect): String =
    effect match
      case AppealEffect.Sleep(months) =>
        val monthUnit = if months == 1 then "month" else "months"
        s"Pauses the appeal for $months $monthUnit"
      case AppealEffect.Close => "Closes the appeal"
      case AppealEffect.Unmark => "Removes the mark"
