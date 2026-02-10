package views.appeal

import play.api.data.Form

import lila.app.UiEnv.{ *, given }
import lila.appeal.Appeal
import lila.common.String.html.richText
import lila.mod.IpRender.RenderIp
import lila.mod.{ ModPresets, UserWithModlog }
import lila.report.Report.Inquiry
import lila.report.Suspect

object discussion:

  case class ModData(
      mod: Me,
      suspect: Suspect,
      presets: ModPresets,
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
              postForm(action := s"${routes.Mod.spontaneousInquiry(appeal.userId)}?appeal=1")(
                submitButton(cls := "button")("Handle this appeal")
              )
            case Some(Inquiry(mod, _)) if ctx.userId.has(mod) =>
              postForm(action := routes.Appeal.mute(modData.suspect.user.username))(
                if appeal.isMuted then
                  submitButton("Un-mute")(
                    title := "Be notified about user replies again",
                    cls := "button button-green button-thin"
                  )
                else
                  submitButton("Mute")(
                    title := "Don't be notified about user replies",
                    cls := "button button-red button-thin"
                  )
              )
            case Some(Inquiry(mod, _)) => frag(userIdLink(mod.some), nbsp, "is handling this.")
          ,
          postForm(
            action := routes.Appeal.sendToZulip(modData.suspect.user.id),
            cls := "appeal__actions__slack"
          )(
            submitButton(cls := "button button-thin")("Send to Zulip")
          )
        )
      )

  private def renderAppeal(
      appeal: Appeal,
      textForm: Form[?],
      as: Either[ModData, User]
  )(using ctx: Context) =
    frag(
      h1(
        div(cls := "title")(
          "Appeal",
          as.isLeft.option(frag(" by ", userIdLink(appeal.id.some)))
        ),
        as.isLeft.option(
          div(cls := "actions")(
            a(
              cls := "button button-empty mod-zone-toggle",
              href := routes.User.mod(appeal.userId),
              titleOrText("Mod zone (Hotkey: m)"),
              dataIcon := Icon.Agent
            )
          )
        )
      ),
      as.toOption.map(user => h2(ui.renderMark(user))),
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
                  if as.isLeft then routes.Appeal.reply(appeal.userId).url
                  else routes.Appeal.post.url,
                isNew = false,
                presets = as.left.toOption.map(_.presets)
              )
            )
      )
    )

  def renderForm(form: Form[?], action: String, isNew: Boolean, presets: Option[ModPresets])(using
      Translate,
      Option[Me]
  ) =
    postForm(st.action := action)(
      form3.globalError(form),
      form3.group(
        form("text"),
        if isNew then "Create an appeal" else "Add something to the appeal",
        help = (!isGranted(_.Appeals)).option(frag("Please be concise. Maximum 1000 chars."))
      )(f => form3.textarea(f.copy(constraints = Seq.empty))(rows := 6, maxlength := Appeal.maxLengthClient)),
      presets
        .map: ps =>
          form3.actions(
            div(
              select(cls := "appeal-presets")(
                st.option(st.value := "")("Presets"),
                ps.value.map: preset =>
                  st.option(
                    st.value := preset.text,
                    st.title := preset.text
                  )(preset.name)
              ),
              isGranted(_.Presets).option(a(href := routes.Mod.presets("appeal"))("Edit presets"))
            ),
            form3.submit(
              "Send and process appeal",
              nameValue = ("process" -> true.toString).some
            ),
            form3.submit(
              trans.site.send(),
              nameValue = ("process" -> false.toString).some
            )
          )
        .getOrElse(form3.submit(trans.site.send()))
    )
