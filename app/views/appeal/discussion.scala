package views.appeal

import play.api.data.Form

import lila.app.templating.Environment.{ *, given }

import lila.appeal.Appeal
import lila.common.String.html.richText
import lila.mod.IpRender.RenderIp
import lila.mod.{ ModPreset, ModPresets, UserWithModlog }
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

  def apply(appeal: Appeal, me: User, textForm: Form[?])(using PageContext) =
    ui.page("Appeal"):
      main(cls := "page-small box box-pad appeal")(
        renderAppeal(appeal, textForm, Right(me))
      )

  def show(
      appeal: Appeal,
      textForm: Form[?],
      modData: ModData
  )(using ctx: PageContext) =
    ui.page(s"Appeal by ${modData.suspect.user.username}") {
      main(cls := "box box-pad appeal")(
        renderAppeal(appeal, textForm, Left(modData)),
        div(cls := "appeal__actions", id := "appeal-actions")(
          modData.inquiry match
            case None =>
              postForm(action := routes.Mod.spontaneousInquiry(appeal.id))(
                submitButton(cls := "button")("Handle this appeal")
              )
            case Some(Inquiry(mod, _)) if ctx.userId.has(mod) =>
              postForm(action := routes.Appeal.mute(modData.suspect.user.username))(
                if appeal.isMuted then
                  submitButton("Un-mute")(
                    title := "Be notified about user replies again",
                    cls   := "button button-green button-thin"
                  )
                else
                  submitButton("Mute")(
                    title := "Don't be notified about user replies",
                    cls   := "button button-red button-thin"
                  )
              )
            case Some(Inquiry(mod, _)) => frag(userIdLink(mod.some), nbsp, "is handling this.")
          ,
          postForm(
            action := routes.Appeal.sendToZulip(modData.suspect.user.id),
            cls    := "appeal__actions__slack"
          )(
            submitButton(cls := "button button-thin")("Send to Zulip")
          )
        )
      )
    }

  private def renderAppeal(
      appeal: Appeal,
      textForm: Form[?],
      as: Either[ModData, User]
  )(using ctx: PageContext) =
    frag(
      h1(
        div(cls := "title")(
          "Appeal",
          as.isLeft.option(frag(" by ", userIdLink(appeal.id.some)))
        ),
        as.isLeft.option(
          div(cls := "actions")(
            a(
              cls  := "button button-empty mod-zone-toggle",
              href := routes.User.mod(appeal.id),
              titleOrText("Mod zone (Hotkey: m)"),
              dataIcon := Icon.Agent
            )
          )
        )
      ),
      as.toOption.map(user => h2(renderMark(user))),
      as.left.toOption.map: m =>
        given RenderIp = m.renderIp
        frag(
          div(cls := "mod-zone mod-zone-full none"),
          views.user.mod.otherUsers(m.mod, m.suspect.user, m.logins, m.appeals)(
            cls := "mod-zone communication__logins"
          )
        )
      ,
      standardFlash,
      div(cls := "body")(
        appeal.msgs.map: msg =>
          div(cls := s"appeal__msg appeal__msg--${if appeal.isByMod(msg) then "mod" else "suspect"}")(
            div(cls := "appeal__msg__header")(
              renderUser(appeal, msg.by, as.isLeft),
              if as.isRight then momentFromNowOnce(msg.at)
              else momentFromNowServer(msg.at)
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
                  if as.isLeft then routes.Appeal.reply(appeal.id).url
                  else routes.Appeal.post.url,
                isNew = false,
                presets = as.left.toOption.map(_.presets)
              )
            )
      )
    )

  private def renderMark(suspect: User)(using ctx: PageContext) =
    val query = isGranted(_.Appeals).so(ctx.req.queryString.toMap)
    if suspect.enabled.no || query.contains("alt") then trans.appeal.closedByModerators()
    else if suspect.marks.engine || query.contains("engine") then trans.appeal.engineMarked()
    else if suspect.marks.boost || query.contains("boost") then trans.appeal.boosterMarked()
    else if suspect.marks.troll || query.contains("shadowban") then trans.appeal.accountMuted()
    else if suspect.marks.rankban || query.contains("rankban") then trans.appeal.excludedFromLeaderboards()
    else trans.appeal.cleanAllGood()

  private def renderUser(appeal: Appeal, userId: UserId, asMod: Boolean)(using PageContext) =
    if appeal.isAbout(userId) then userIdLink(userId.some, params = asMod.so("?mod"))
    else
      span(
        userIdLink(UserId.lichess.some),
        isGranted(_.Appeals).option(
          frag(
            " (",
            userIdLink(userId.some),
            ")"
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
        .map { ps =>
          form3.actions(
            div(
              select(cls := "appeal-presets")(
                st.option(st.value := "")("Presets"),
                ps.value.map { case ModPreset(name, text, _) =>
                  st.option(
                    st.value := text,
                    st.title := text
                  )(name)
                }
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
        }
        .getOrElse(form3.submit(trans.site.send()))
    )
