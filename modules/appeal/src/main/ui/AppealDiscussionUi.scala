package lila.appeal
package ui

import play.api.data.Form

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.config.NetDomain

final class AppealDiscussionUi(helpers: Helpers, ui: AppealUi)(using NetDomain):
  import helpers.{ *, given }

  def userForm(topic: AppealTopic, form: Form[?], isNew: Boolean)(using Translate) =
    val formContent = postForm(st.action := routes.Appeal.post(topic))(
      form3.globalError(form),
      form3.group(
        form("text"),
        "",
        help = frag("Please be concise. Maximum 1000 chars.").some
      )(f =>
        form3.textarea(f)(
          rows := 6,
          maxlength := Appeal.maxLength * 1.1
        )
      )(cls := "appeal-textarea"),
      form3.action(form3.submit(trans.site.send()))
    )
    if isNew then formContent
    else form3.fieldset("Add something to the appeal", toggle = false.some)(cls := "form-toggle")(formContent)

  def userShow(status: UserStatus, appeal: Appeal, form: Form[?], appeals: List[Appeal])(using Context, Me) =
    ui.page("Appeal"):
      main(cls := "page-small appeal")(
        div(cls := "box box-pad")(
          h1(cls := "box__top")(
            div(cls := "title")(span(cls := "appeal-topic")(appeal.topic.key), " Appeal in progress")
          ),
          AppealTopicApi
            .markMsg(status, appeal.topic)
            .map: msg =>
              h2(cls := "appeal__mark")(msg()),
          standardFlash,
          div(cls := "body")(
            ui.userAppealMessages(appeal),
            if appeal.isClosed then ui.appealIsClosed(appeal)
            else if !appeal.canAddMsg then
              p(cls := "line-center-text")("You can't add messages to this appeal at the moment.")
            else
              frag(
                appeal.isUnread.option(p(cls := "line-center-text")("Please wait for a moderator to reply.")),
                userForm(appeal.topic, form, isNew = false)
              )
          ),
          appeal.isOpen.option:
            postForm(cls := "appeal__withdraw", action := routes.Appeal.withdraw(appeal.topic))(
              submitButton(
                cls := "button button-red button-empty yes-no-confirm",
                title :=
                  "Withdrawing this appeal will close this request. You will not be able to appeal this restriction, and we will consider this case closed.\n\nAre you sure you want to withdraw your appeal?"
              )("Withdraw appeal")
            )
        ),
        ui.userInactiveAppeals(appeals.filter(_ != appeal))
      )

  def modShow(appeal: Appeal, form: Form[?], modData: ModData)(using ctx: Context, me: Me) =
    import modData.*
    ui.page(s"Appeal by ${user.username}"):
      main(cls := "appeal")(
        div(cls := "box box-pad")(
          ui.modHeader(appeal, modData),
          div(cls := "mod-zone mod-zone-full none"),
          appeal.accounts.map(ui.renderAccountsDisclosure),
          otherUsers(cls := "mod-zone communication__logins"),
          div(cls := "body")(
            ui.modAppealMessages(appeal),
            standardFlash.orElse(markedByMe.option(ui.markedByMeWarning)),
            if appeal.isClosed then ui.appealIsClosed(appeal)
            else if me.is(inquiryBy) then modReplyForm(appeal, form, presets)
            else emptyFrag
          ),
          ui.modActions(appeal, modData)
        ),
        ui.userInactiveAppeals(userAppeals.filter(_ != appeal))
      )

  private def modReplyForm(appeal: Appeal, form: Form[?], presets: List[PairOf[String]])(using Context) =
    postForm(st.action := s"${routes.Appeal.modReply(appeal.user, appeal.topic)}#appeal-last-msg")(
      form3.globalError(form),
      form3.split(
        div(cls := "appeal-presets form-group form-half")(
          presets.map: (name, text) =>
            button(
              tpe := "button",
              st.value := text,
              st.title := text
            )(name)
        ),
        form3.group(
          form("text"),
          "Add something to the appeal",
          half = true
        )(form3.textarea(_)(rows := 15))(cls := "appeal-textarea")
      ),
      form3.action(
        form3.submit("Send & close", nameValue = ("close", "true").some, icon = none)(
          cls := "button-red button-empty"
        ),
        form3.submit(trans.site.send())(cls := "button-empty"),
        form3.submit("Send & dismiss", nameValue = ("dismiss", "true").some)(
          cls := "button-green",
          title := "Dismiss the appeal as processed"
        )
      )
    )
