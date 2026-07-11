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
    postForm(st.action := routes.Appeal.post(topic))(
      form3.globalError(form),
      form3.group(
        form("text"),
        if isNew then "Create an appeal" else "Add something to the appeal",
        help = frag("Please be concise. Maximum 1000 chars.").some
      )(f =>
        form3.textarea(f)(
          rows := 6,
          maxlength := Appeal.maxLengthClient
        )
      )(cls := "appeal-textarea"),
      form3.action(form3.submit(trans.site.send()))
    )

  def userShow(status: UserStatus, appeal: Appeal, form: Form[?])(using Context) =
    ui.page("Appeal"):
      main(cls := "page-small box box-pad appeal")(
        h1(cls := "box__top")(
          div(cls := "title")(span(cls := "appeal-topic")(appeal.topic.key), " appeal")
        ),
        AppealTopicApi
          .markMsg(status, appeal.topic)
          .map: msg =>
            h2(cls := "appeal__mark")(msg()),
        standardFlash,
        div(cls := "body")(
          appeal.msgs.map: msg =>
            div(cls := s"appeal__msg appeal__msg--${if appeal.isByMod(msg) then "mod" else "suspect"}")(
              div(cls := "appeal__msg__header")(
                ui.renderUser(appeal, msg.by, asMod = false),
                momentFromNowOnce(msg.at)
              ),
              div(cls := "appeal__msg__text")(richText(msg.text, expandImg = false))
            ),
          if appeal.isClosed then p(cls := "line-center-text")("This appeal is now closed.")
          else if !appeal.canAddMsg then
            p(cls := "line-center-text")("You can't add messages to this appeal at the moment.")
          else
            frag(
              appeal.isUnread.option(p(cls := "line-center-text")("Please wait for a moderator to reply.")),
              userForm(appeal.topic, form, isNew = false)
            )
        )
      )

  def modShow(appeal: Appeal, form: Form[?], modData: ModData)(using ctx: Context, me: Me) =
    import modData.*
    val userAppeals = relatedAppeals.count(_.user.is(user))
    ui.page(s"Appeal by ${user.username}"):
      main(cls := "box box-pad appeal")(
        h1(cls := "box__top")(
          div(cls := "title")(
            a(href := routes.Appeal.modQueue, dataIcon := Icon.LessThan, cls := "text"),
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
          if appeal.isClosed then p(cls := "line-center-text")("This appeal is now closed.")
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
              form3.action(form3.submit(trans.site.send()))
            )
          else emptyFrag
        ),
        div(cls := "appeal__actions", id := "appeal-actions")(
          inquiryBy match
            case None =>
              postForm(action := routes.Appeal.modHandle(appeal.user, appeal.topic))(
                submitButton(cls := "button")("Handle this appeal")
              )
            case Some(mod) if ctx.is(mod) =>
              frag(
                postForm(action := routes.Appeal.toggleClosed(appeal.user, appeal.topic, !appeal.isClosed))(
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
                AppealTopicApi.unmark(status, appeal.topic) match
                  case None => button(cls := "button button-empty", disabled)("Nothing to un-mark")
                  case Some((text, call)) =>
                    val appealUrl = routes.Appeal.modShow(appeal.user, appeal.topic).url
                    val actionUrl = addQueryParam(call.url, "referrer", appealUrl)
                    postForm(action := actionUrl):
                      submitButton(cls := "button button-green button-empty yes-no-confirm")(text)
              )
            case Some(mod) => p(cls := "line-center-text")(userIdLink(mod.some), nbsp, "is handling this.")
          ,
          postForm(
            action := routes.Appeal.sendToZulip(appeal.user, appeal.topic),
            cls := "appeal__actions__slack"
          )(submitButton(cls := "button button-empty")("Send to Zulip"))
        )
      )
