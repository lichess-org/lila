package views.html
package appeal

import controllers.routes
import controllers.appeal.routes.{ Appeal as appealRoutes }
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.appeal.Appeal
import lila.common.String.html.richText
import lila.mod.IpRender.RenderIp
import lila.mod.{ ModPreset, ModPresets, UserWithModlog }
import lila.report.Report.Inquiry
import lila.report.Suspect
import lila.user.{ Holder, User }

object discussion:

  case class ModData(
      mod: Holder,
      suspect: Suspect,
      presets: ModPresets,
      logins: lila.security.UserLogins.TableData[UserWithModlog],
      appeals: List[lila.appeal.Appeal],
      renderIp: RenderIp,
      inquiry: Option[Inquiry],
      markedByMe: Boolean
  )

  def apply(appeal: Appeal, me: User, textForm: Form[String])(implicit ctx: Context) =
    bits.layout("Appeal") {
      main(cls := "page-small box box-pad appeal")(
        renderAppeal(appeal, textForm, Right(me))
      )
    }

  def show(
      appeal: Appeal,
      textForm: Form[String],
      modData: ModData
  )(implicit ctx: Context) =
    bits.layout(s"Appeal by ${modData.suspect.user.username}") {
      main(cls := "box box-pad appeal")(
        renderAppeal(appeal, textForm, Left(modData)),
        div(cls := "appeal__actions", id := "appeal-actions")(
          modData.inquiry match {
            case None =>
              postForm(action := routes.Mod.spontaneousInquiry(appeal.id))(
                submitButton(cls := "button")("Handle this appeal")
              )
            case Some(Inquiry(mod, _)) if ctx.userId has mod =>
              postForm(action := appealRoutes.mute(modData.suspect.user.username))(
                if (appeal.isMuted)
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
          },
          postForm(
            action := appealRoutes.sendToZulip(modData.suspect.user.id),
            cls    := "appeal__actions__slack"
          )(
            submitButton(cls := "button button-thin")("Send to Zulip")
          )
        )
      )
    }

  private def renderAppeal(
      appeal: Appeal,
      textForm: Form[String],
      as: Either[ModData, User]
  )(implicit ctx: Context) =
    frag(
      h1(
        div(cls := "title")(
          "Appeal",
          as.isLeft option frag(" by ", userIdLink(appeal.id.some))
        ),
        as.isLeft option div(cls := "actions")(
          a(
            cls  := "button button-empty mod-zone-toggle",
            href := routes.User.mod(appeal.id),
            titleOrText("Mod zone (Hotkey: m)"),
            dataIcon := ""
          )
        )
      ),
      as.toOption.map(user => h2(renderMark(user))),
      as.left.toOption map { m =>
        frag(
          div(cls := "mod-zone mod-zone-full none"),
          views.html.user.mod.otherUsers(m.mod, m.suspect.user, m.logins, m.appeals)(using ctx, m.renderIp)(
            cls := "mod-zone communication__logins"
          )
        )
      },
      standardFlash,
      div(cls := "body")(
        appeal.msgs.map { msg =>
          div(cls := s"appeal__msg appeal__msg--${if (appeal isByMod msg) "mod" else "suspect"}")(
            div(cls := "appeal__msg__header")(
              renderUser(appeal, msg.by, as.isLeft),
              if (as.isRight) momentFromNowOnce(msg.at)
              else momentFromNowServer(msg.at)
            ),
            div(cls := "appeal__msg__text")(richText(msg.text))
          )
        },
        as.left.exists(_.markedByMe) option div(dataIcon := "", cls := "marked-by-me text")(
          "You have marked this user. Appeal should be handled by another moderator"
        ),
        if (as.isRight && !appeal.canAddMsg) p("Please wait for a moderator to reply.")
        else
          as.fold(_.inquiry.isDefined, _ => true) option renderForm(
            textForm,
            action =
              if (as.isLeft) appealRoutes.reply(appeal.id).url
              else appealRoutes.post.url,
            isNew = false,
            presets = as.left.toOption.map(_.presets)
          )
      )
    )

  private def renderMark(suspect: User)(implicit ctx: Context) =
    val query = isGranted(_.Appeals) ?? ctx.req.queryString.toMap
    if (suspect.enabled.no || query.contains("alt")) tree.closedByModerators
    else if (suspect.marks.engine || query.contains("engine")) tree.engineMarked
    else if (suspect.marks.boost || query.contains("boost")) tree.boosterMarked
    else if (suspect.marks.troll || query.contains("shadowban")) tree.accountMuted
    else if (suspect.marks.rankban || query.contains("rankban")) tree.excludedFromLeaderboards
    else tree.cleanAllGood

  private def renderUser(appeal: Appeal, userId: UserId, asMod: Boolean)(implicit ctx: Context) =
    if (appeal isAbout userId) userIdLink(userId.some, params = asMod ?? "?mod")
    else
      span(
        userIdLink(User.lichessId.some),
        isGranted(_.Appeals) option frag(
          " (",
          userIdLink(userId.some),
          ")"
        )
      )

  def renderForm(form: Form[String], action: String, isNew: Boolean, presets: Option[ModPresets])(using
      ctx: Context
  ) =
    postForm(st.action := action)(
      form3.globalError(form),
      form3.group(
        form("text"),
        if (isNew) "Create an appeal" else "Add something to the appeal",
        help = !isGranted(_.Appeals) option frag("Please be concise. Maximum 1000 chars.")
      )(f => form3.textarea(f.copy(constraints = Seq.empty))(rows := 6, maxlength := Appeal.maxLengthClient)),
      presets.map { ps =>
        form3.actions(
          div(
            select(cls := "appeal-presets")(
              option(st.value := "")("Presets"),
              ps.value.map { case ModPreset(name, text, _) =>
                option(
                  st.value := text,
                  st.title := text
                )(name)
              }
            ),
            isGranted(_.Presets) option a(href := routes.Mod.presets("appeal"))("Edit presets")
          ),
          form3.submit(trans.send())
        )
      } getOrElse form3.submit(trans.send())
    )
