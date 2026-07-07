package views.appeal

import play.api.data.Form

import lila.app.UiEnv.{ *, given }
import lila.appeal.Appeal
import lila.common.String.html.richText
import lila.mod.IpRender.RenderIp
import lila.mod.{ AppealPresets, UserWithModlog }
import lila.report.Report.Inquiry
import lila.report.Suspect

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

  def apply(appeal: Appeal, me: User, textForm: Form[?])(using Context) =
    ui.page("Appeal"):
      main(cls := "page-small box box-pad appeal")(
        renderAppeal(appeal, textForm, Right(me))
      )

  def show(
      appeal: Appeal,
      textForm: Form[?],
      modData: ModData
  )(using ctx: Context) =
    ui.page(s"Appeal by ${modData.suspect.user.username}"):
      main(cls := "box box-pad appeal")(
        renderAppeal(appeal, textForm, Left(modData)),
        div(cls := "appeal__actions", id := "appeal-actions")(
          modData.inquiry match
            case None =>
              postForm(action := routes.Appeal.modHandle(appeal.user, appeal.topic))(
                submitButton(cls := "button")("Handle this appeal")
              )
            case Some(Inquiry(mod, _)) if ctx.userId.has(mod) =>
              postForm(action := routes.Appeal.toggleClosed(appeal.user, appeal.topic))(
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
            case Some(Inquiry(mod, _)) => frag(userIdLink(mod.some), nbsp, "is handling this.")
          ,
          postForm(
            action := routes.Appeal.sendToZulip(appeal.user, appeal.topic),
            cls := "appeal__actions__slack"
          )(
            submitButton(cls := "button button-empty")("Send to Zulip")
          )
        )
      )

  private def renderAppeal(
      appeal: Appeal,
      textForm: Form[?],
      as: Either[ModData, User]
  )(using ctx: Context) =
    frag(
      h1(cls := "box__top")(
        div(cls := "title")(
          span(cls := "appeal-topic")(appeal.topic.key),
          " appeal",
          as.isLeft.option(frag(" by ", userIdLink(appeal.user.some)))
        ),
        as.isLeft.option(
          div(cls := "actions")(
            a(
              cls := "button button-empty mod-zone-toggle",
              href := routes.User.mod(appeal.user),
              titleOrText("Mod zone (Hotkey: m)"),
              dataIcon := Icon.Agent
            )
          )
        )
      ),
      as.toOption.map(user => h2(cls := "appeal__mark")(ui.renderMark(user))),
      as.left.toOption.map: m =>
        given RenderIp = m.renderIp
        frag(
          div(cls := "mod-zone mod-zone-full none"),
          views.user.mod.otherUsers(m.suspect.user, m.logins, m.appeals, readOnly = true)(
            cls := "mod-zone communication__logins"
          )
        )
      ,
      standardFlash,
      div(cls := "body")(
        appeal.msgs.map: msg =>
          div(cls := s"appeal__msg appeal__msg--${if appeal.isByMod(msg) then "mod" else "suspect"}")(
            div(cls := "appeal__msg__header")(
              ui.renderUser(appeal, msg.by, as.isLeft),
              if as.isRight then momentFromNowOnce(msg.at)
              else pastMomentServer(msg.at)
            ),
            div(cls := "appeal__msg__text")(richText(msg.text, expandImg = false))
          ),
        as.left
          .exists(_.markedByMe)
          .option(
            div(dataIcon := Icon.CautionTriangle, cls := "marked-by-me text")(
              "You have marked this user. Appeal should be handled by another moderator"
            )
          ),
        if as.isRight && !appeal.canAddMsg then p("Please wait for a moderator to reply.")
        else
          as.fold(_.inquiry.isDefined, _ => true)
            .option(
              renderForm(
                textForm,
                action =
                  if as.isLeft then routes.Appeal.modReply(appeal.user, appeal.topic)
                  else routes.Appeal.post(appeal.topic),
                isNew = false,
                presets = as.left.toOption.map(_.presets)
              )
            )
      )
    )

  def renderForm(form: Form[?], action: Call, isNew: Boolean, presets: Option[AppealPresets])(using
      Translate,
      Option[Me]
  ) =
    postForm(st.action := action)(
      form3.globalError(form),
      form3.split(
        presets.map: ps =>
          div(cls := "appeal-presets form-group form-half")(
            ps.value.map: preset =>
              button(
                tpe := "button",
                st.value := preset.text,
                st.title := preset.text
              )(preset.name)
          ),
        form3.group(
          form("text"),
          if isNew then "Create an appeal" else "Add something to the appeal",
          help = (!isGranted(_.Appeals)).option(frag("Please be concise. Maximum 1000 chars.")),
          half = presets.isDefined
        )(f =>
          form3.textarea(f.copy(constraints = Seq.empty))(
            rows := (if presets.isDefined then 15 else 6),
            maxlength := Appeal.maxLengthForMe
          )
        )(cls := "appeal-textarea")
      ),
      form3.action:
        form3.submit(trans.site.send())
    )
