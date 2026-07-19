package lila.appeal
package ui

import play.api.data.Form

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.userId.ModId
import lila.core.config.NetDomain

case class ModData(
    mod: Me,
    status: UserStatus,
    presets: List[PairOf[String]],
    relatedAppeals: List[Appeal],
    inquiryBy: Option[ModId],
    markedByMe: Boolean,
    otherUsers: Tag
):
  export status.user

final class AppealDiscussionUi(helpers: Helpers, ui: AppealUi)(using NetDomain):
  import helpers.{ *, given }

  def userForm(topic: AppealTopic, form: Form[?], isNew: Boolean)(using Translate) =
    form3.fieldset(if isNew then "Create an appeal" else "Add something to the appeal", toggle = false.some)(
      cls := "form-toggle"
    ):
      postForm(st.action := routes.Appeal.post(topic))(
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
            userAppealMessages(appeal),
            if appeal.isClosed then appealIsClosed(appeal)
            else if !appeal.canAddMsg then
              p(cls := "line-center-text")("You can't add messages to this appeal at the moment.")
            else
              frag(
                appeal.isUnread.option(p(cls := "line-center-text")("Please wait for a moderator to reply.")),
                userForm(appeal.topic, form, isNew = false)
              )
          )
        ),
        userInactiveAppeals(appeals.filter(_ != appeal))
      )

  private def appealIsClosed(appeal: Appeal)(using Translate) = p(cls := "line-center-text")(
    appeal.closedUntil.fold(frag("This appeal is now closed")): until =>
      frag("Appeal paused until ", showDate(until))
  )

  private def userAppealMessages(appeal: Appeal)(using Context) =
    appeal.msgs.map: msg =>
      div(cls := s"appeal__msg appeal__msg--${if appeal.isByMod(msg) then "mod" else "suspect"}")(
        div(cls := "appeal__msg__header")(
          ui.renderUser(appeal, msg.by, asMod = false),
          momentFromNowOnce(msg.at)
        ),
        div(cls := "appeal__msg__text")(richText(msg.text, expandImg = false))
      )

  def userInactiveAppeals(appeals: List[Appeal])(using Context, Me) =
    appeals
      .sortBy(_.updatedAt)
      .reverse
      .map: appeal =>
        val titleTag =
          if Granter(_.Appeals)
          then a(href := routes.Appeal.modShow(appeal.user, appeal.topic))
          else span
        div(cls := "box box-pad appeal-closed")(
          div(cls := "box__top")(
            h1(
              span(cls := "appeal-topic")(appeal.topic.key),
              nbsp,
              titleTag:
                if appeal.isClosed then
                  appeal.closedUntil.fold[Frag]("Appeal closed"): until =>
                    frag("Appeal paused until ", showDate(until))
                else "Appeal on hold"
            )
          ),
          userAppealMessages(appeal)
        )

  def modShow(appeal: Appeal, form: Form[?], modData: ModData)(using ctx: Context, me: Me) =
    import modData.*
    val userAppeals = relatedAppeals.count(_.user.is(user))
    ui.page(s"Appeal by ${user.username}"):
      main(cls := "appeal")(
        div(cls := "box box-pad")(
          h1(cls := "box__top")(
            div(cls := "title")(
              ui.backLink,
              span(cls := "appeal-topic")(appeal.topic.key),
              " appeal by ",
              userIdLink(user.some),
              (userAppeals > 1).option:
                a(href := routes.Appeal.modShowAll(user.username), cls := "appeal__all")(
                  small(s" ($userAppeals appeals)")
                )
            ),
            div(cls := "actions")(
              a(
                cls := "button button-empty mod-zone-toggle",
                href := routes.User.mod(user.username),
                titleOrText("Mod zone (Hotkey: m)"),
                dataIcon := Icon.Agent
              )
            )
          ),
          div(cls := "mod-zone mod-zone-full none"),
          otherUsers(cls := "mod-zone communication__logins"),
          div(cls := "body")(
            appeal.msgs.map: msg =>
              div(cls := s"appeal__msg appeal__msg--${if appeal.isByMod(msg) then "mod" else "suspect"}")(
                div(cls := "appeal__msg__header")(
                  ui.renderUser(appeal, msg.by, asMod = true),
                  pastMomentServer(msg.at)
                ),
                div(cls := "appeal__msg__text")(richText(msg.text, expandImg = false))
              ),
            standardFlash.orElse:
              markedByMe.option:
                div(dataIcon := Icon.CautionTriangle, cls := "marked-by-me text"):
                  "You have marked this user. Appeal should be handled by another moderator"
            ,
            if appeal.isClosed then appealIsClosed(appeal)
            else if me.is(inquiryBy) then
              postForm(st.action := routes.Appeal.modReply(appeal.user, appeal.topic))(
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
            else emptyFrag
          ),
          div(cls := "appeal__actions")(
            inquiryBy match
              case None =>
                postForm(action := routes.Appeal.modHandle(appeal.user, appeal.topic))(
                  submitButton(cls := "button")("Handle this appeal")
                )
              case Some(mod) if ctx.is(mod) =>
                frag(
                  postForm(action := routes.Appeal.toggleClosed(appeal.user, appeal.topic, appeal.isOpen))(
                    if appeal.isClosed then
                      submitButton("Re-open")(
                        cls := "button button-green button-empty"
                      )
                    else
                      submitButton("Close")(
                        title := "Close this appeal",
                        cls := "button button-red button-empty"
                      )
                  ),
                  postForm(action := routes.Appeal.toggleClosed(appeal.user, appeal.topic, true))(
                    form3.selectLowLevel("months", AppealForm.untilMonths, default = "Pause".some)
                  ),
                  if appeal.topic == AppealTopic.blog
                  then a(href := routes.Ublog.index(user.username), cls := "button button-empty")("View blog")
                  else
                    AppealTopicApi.unmark(status, appeal.topic) match
                      case None =>
                        button(cls := "button button-green button-empty", disabled)("Nothing to un-mark")
                      case Some((text, call)) =>
                        val appealUrl = routes.Appeal.modShow(appeal.user, appeal.topic).url
                        val actionUrl = addQueryParam(call.url, "referrer", appealUrl)
                        postForm(action := actionUrl):
                          submitButton(cls := "button button-green button-empty yes-no-confirm")(text)
                      ,
                  appeal.isOpen.option:
                    postForm(action := routes.Appeal.toggleRead(appeal.user, appeal.topic, appeal.isUnread))(
                      submitButton(cls := "button button-dim button-empty"):
                        if appeal.isUnread then "Set read" else "Set Unread"
                    )
                )
              case Some(mod) =>
                button(userIdLink(mod.some), nbsp, "is handling this.")(
                  disabled,
                  cls := "button button-empty disabled"
                )
            ,
            postForm(
              action := routes.Appeal.sendToZulip(appeal.user, appeal.topic),
              cls := "appeal__actions__zulip"
            )(submitButton(cls := "button button-empty")("Send to Zulip"))
          )
        ),
        userInactiveAppeals(relatedAppeals.filter(_.user.is(user)).filter(_ != appeal))
      )
