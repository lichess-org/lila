package views.appeal

import play.api.data.Form

import lila.app.UiEnv.{ *, given }
import lila.appeal.Appeal
import lila.common.String.html.richText
import lila.mod.IpRender.RenderIp
import lila.mod.{ AppealPresets, UserWithModlog }
import lila.report.Report.Inquiry
import lila.report.Suspect
import lila.core.misc.AppealTopic

object discussion:

  case class ModData(
      mod: Me,
      suspect: Suspect,
      presets: AppealPresets,
      logins: lila.security.UserLogins.TableData[UserWithModlog],
      appeals: List[Appeal],
      renderIp: RenderIp,
      inquiry: Option[Inquiry],
      markedByMe: Boolean
  )

  def userForm(topic: AppealTopic, form: Form[?], isNew: Boolean)(using Translate) =
    postForm(st.action := routes.Appeal.post(topic))(
      form3.globalError(form),
      form3.split(
        form3.group(
          form("text"),
          if isNew then "Create an appeal" else "Add something to the appeal",
          help = frag("Please be concise. Maximum 1000 chars.").some
        )(f =>
          form3.textarea(f)(
            rows := 6,
            maxlength := Appeal.maxLengthClient
          )
        )(cls := "appeal-textarea")
      ),
      form3.action(form3.submit(trans.site.send()))
    )

  def userShow(appeal: Appeal, me: User, form: Form[?])(using Context) =
    ui.page("Appeal"):
      main(cls := "page-small box box-pad appeal")(
        h1(cls := "box__top")(
          div(cls := "title")(span(cls := "appeal-topic")(appeal.topic.key), " appeal")
        ),
        h2(cls := "appeal__mark")(ui.renderMark(me)),
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
    given RenderIp = renderIp
    ui.page(s"Appeal by ${suspect.user.username}"):
      main(cls := "box box-pad appeal")(
        h1(cls := "box__top")(
          div(cls := "title")(
            span(cls := "appeal-topic")(appeal.topic.key),
            " appeal by ",
            userIdLink(appeal.user.some)
          ),
          div(cls := "actions")(
            a(
              cls := "button button-empty mod-zone-toggle",
              href := routes.User.mod(appeal.user),
              titleOrText("Mod zone (Hotkey: m)"),
              dataIcon := Icon.Agent
            )
          )
        ),
        div(cls := "mod-zone mod-zone-full none"),
        views.user.mod.otherUsers(suspect.user, logins, appeals, readOnly = true)(
          cls := "mod-zone communication__logins"
        ),
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
          else if inquiry.exists(_.mod.is(me)) then
            postForm(st.action := routes.Appeal.modReply(appeal.user, appeal.topic))(
              form3.globalError(form),
              form3.split(
                div(cls := "appeal-presets form-group form-half")(
                  presets.value.map: preset =>
                    button(
                      tpe := "button",
                      st.value := preset.text,
                      st.title := preset.text
                    )(preset.name)
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
          inquiry match
            case None =>
              postForm(action := routes.Appeal.modHandle(appeal.user, appeal.topic))(
                submitButton(cls := "button")("Handle this appeal")
              )
            case Some(Inquiry(mod, _)) if ctx.is(mod) =>
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
              )
            case Some(Inquiry(mod, _)) =>
              p(cls := "line-center-text")(userIdLink(mod.some), nbsp, "is handling this.")
          ,
          postForm(
            action := routes.Appeal.sendToZulip(appeal.user, appeal.topic),
            cls := "appeal__actions__slack"
          )(submitButton(cls := "button button-empty")("Send to Zulip"))
        )
      )
