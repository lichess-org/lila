package views.html
package appeal

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.appeal.Appeal
import lila.common.String.html.richText
import lila.mod.{ ModPreset, ModPresets }
import lila.report.Report.Inquiry
import lila.report.Suspect
import lila.user.User

object discussion {

  def apply(appeal: Appeal, textForm: Form[String])(implicit ctx: Context) =
    bits.layout("Appeal") {
      main(cls := "page-small box box-pad page appeal")(
        renderAppeal(appeal, textForm, asMod = false, presets = none)
      )
    }

  def show(
      appeal: Appeal,
      suspect: Suspect,
      inquiry: Option[Inquiry],
      textForm: Form[String],
      presets: ModPresets
  )(implicit ctx: Context) =
    bits.layout(s"Appeal by ${suspect.user.username}") {
      main(cls := "page-small box box-pad page appeal")(
        renderAppeal(
          appeal,
          textForm,
          asMod = true,
          inquiry = inquiry.map(_.mod).exists(ctx.userId.has),
          presets.some
        ),
        div(cls := "appeal__actions")(
          inquiry match {
            case None =>
              postForm(action := routes.Mod.spontaneousInquiry(appeal.id))(
                submitButton(cls := "button")("Handle this appeal")
              )
            case Some(Inquiry(mod, _)) if ctx.userId has mod =>
              postForm(action := routes.Appeal.mute(suspect.user.username))(
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
          postForm(action := routes.Appeal.notifySlack(suspect.user.id), cls := "appeal__actions__slack")(
            submitButton(cls := "button button-thin")("Send to slack")
          )
        )
      )
    }

  private def renderAppeal(
      appeal: Appeal,
      textForm: Form[String],
      asMod: Boolean,
      inquiry: Boolean = false,
      presets: Option[ModPresets]
  )(implicit ctx: Context) =
    frag(
      h1(
        "Appeal",
        asMod option frag(" by ", userIdLink(appeal.id.some))
      ),
      standardFlash(),
      div(cls := "body")(
        appeal.msgs.map { msg =>
          div(cls := s"appeal__msg appeal__msg--${if (appeal isByMod msg) "mod" else "suspect"}")(
            div(cls := "appeal__msg__header")(
              renderUser(appeal, msg.by, asMod),
              momentFromNowOnce(msg.at)
            ),
            div(cls := "appeal__msg__text")(richText(msg.text))
          )
        },
        if (!asMod && !appeal.canAddMsg) p("Please wait for a moderator to reply.")
        else
          (asMod == inquiry) option renderForm(
            textForm,
            action =
              if (asMod) routes.Appeal.reply(appeal.id).url
              else routes.Appeal.post.url,
            isNew = false,
            presets = presets ifTrue asMod
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
