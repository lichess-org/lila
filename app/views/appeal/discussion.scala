package views.html
package appeal

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.appeal.Appeal
import lila.common.String.html.richText
import lila.mod.IpRender.RenderIp
import lila.mod.{ ModPreset, ModPresets }
import lila.report.Report.Inquiry
import lila.report.Suspect
import lila.user.{ Holder, User }

object discussion {

  case class ModData(
      mod: Holder,
      suspect: Suspect,
      presets: ModPresets,
      logins: lila.security.UserLogins.TableData,
      appeals: List[lila.appeal.Appeal],
      renderIp: RenderIp,
      inquiry: Option[Inquiry]
  )

  def apply(appeal: Appeal, textForm: Form[String])(implicit ctx: Context) =
    bits.layout("Appeal") {
      main(cls := "page-small box box-pad appeal")(
        renderAppeal(appeal, textForm, modData = none)
      )
    }

  def show(
      appeal: Appeal,
      textForm: Form[String],
      modData: ModData
  )(implicit ctx: Context) =
    bits.layout(s"Appeal by ${modData.suspect.user.username}") {
      main(cls := "box box-pad appeal")(
        renderAppeal(appeal, textForm, modData.some),
        div(cls := "appeal__actions", id := "appeal-actions")(
          modData.inquiry match {
            case None =>
              postForm(action := routes.Mod.spontaneousInquiry(appeal.id))(
                submitButton(cls := "button")("Handle this appeal")
              )
            case Some(Inquiry(mod, _)) if ctx.userId has mod =>
              postForm(action := routes.Appeal.mute(modData.suspect.user.username))(
                if (appeal.isMuted)
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
          },
          postForm(
            action := routes.Appeal.notifySlack(modData.suspect.user.id),
            cls := "appeal__actions__slack"
          )(
            submitButton(cls := "button button-thin")("Send to slack")
          )
        )
      )
    }

  private def renderAppeal(
      appeal: Appeal,
      textForm: Form[String],
      modData: Option[ModData]
  )(implicit ctx: Context) =
    frag(
      h1(
        div(cls := "title")(
          "Appeal",
          modData.isDefined option frag(" by ", userIdLink(appeal.id.some))
        ),
        div(cls := "actions")(
          a(
            cls := "button button-empty mod-zone-toggle",
            href := routes.User.mod(appeal.id),
            titleOrText("Mod zone (Hotkey: m)"),
            dataIcon := ""
          )
        )
      ),
      modData map { m =>
        frag(
          div(cls := "mod-zone mod-zone-full none"),
          views.html.user.mod.otherUsers(m.mod, m.suspect.user, m.logins, m.appeals)(ctx, m.renderIp)(
            cls := "mod-zone communication__logins"
          )
        )
      },
      standardFlash(),
      div(cls := "body")(
        appeal.msgs.map { msg =>
          div(cls := s"appeal__msg appeal__msg--${if (appeal isByMod msg) "mod" else "suspect"}")(
            div(cls := "appeal__msg__header")(
              renderUser(appeal, msg.by, modData.isDefined),
              momentFromNowOnce(msg.at)
            ),
            div(cls := "appeal__msg__text")(richText(msg.text))
          )
        },
        if (modData.isEmpty && !appeal.canAddMsg) p("Please wait for a moderator to reply.")
        else
          modData.fold(true)(_.inquiry.isDefined) option renderForm(
            textForm,
            action =
              if (modData.isDefined) routes.Appeal.reply(appeal.id).url
              else routes.Appeal.post.url,
            isNew = false,
            presets = modData.map(_.presets)
          )
      )
    )

  private def renderUser(appeal: Appeal, userId: User.ID, asMod: Boolean)(implicit ctx: Context) =
    if (appeal isAbout userId) userIdLink(userId.some)
    else
      span(
        userIdLink(User.lichessId.some),
        isGranted(_.Appeals) option frag(
          " (",
          userIdLink(userId.some),
          ")"
        )
      )

  def renderForm(form: Form[String], action: String, isNew: Boolean, presets: Option[ModPresets])(implicit
      ctx: Context
  ) =
    postForm(st.action := action)(
      form3.globalError(form),
      form3.group(
        form("text"),
        if (isNew) "Create an appeal" else "Add something to the appeal",
        help = !isGranted(_.Appeals) option frag("Please be concise. Maximum 1000 chars.")
      )(
        form3.textarea(_)(rows := 6)
      ),
      presets.map { ps =>
        form3.actions(
          div(
            select(cls := "appeal-presets")(
              option(st.value := "")("Presets"),
              ps.value.map { case ModPreset(name, text) =>
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
}
