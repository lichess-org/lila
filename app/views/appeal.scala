package views.html

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

object appeal {

  def home(appeal: Option[Appeal], textForm: Form[_])(implicit ctx: Context) =
    layout("Appeal") {
      main(cls := "page-small box box-pad page appeal")(
        appeal match {
          case Some(a) => renderAppeal(a, textForm, asMod = false, presets = none)
          case None    => newAppeal(textForm)
        }
      )
    }

  def newAppeal(textForm: Form[_])(implicit ctx: Context) =
    frag(
      h1("Appeal a moderation decision"),
      renderHelp,
      div(cls := "body")(
        renderForm(textForm, action = routes.Appeal.post.url, isNew = true, presets = none)
      )
    )

  def show(
      appeal: Appeal,
      suspect: Suspect,
      inquiry: Option[Inquiry],
      textForm: Form[_],
      presets: ModPresets
  )(implicit
      ctx: Context
  ) =
    layout(s"Appeal by ${suspect.user.username}") {
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
          }
        )
      )
    }

  def queue(
      appeals: List[Appeal],
      inquiries: Map[User.ID, Inquiry],
      scores: lila.report.Room.Scores,
      streamers: Int,
      nbAppeals: Int
  )(implicit ctx: Context) =
    views.html.report.list.layout("appeal", scores, streamers, nbAppeals)(
      table(cls := "slist slist-pad see appeal-queue")(
        thead(
          tr(
            th("By"),
            th("Last message"),
            th(isGranted(_.Presets) option a(href := routes.Mod.presets("appeal"))("Presets"))
          )
        ),
        tbody(
          appeals.map { appeal =>
            tr(cls := List("new" -> appeal.isUnread))(
              td(
                userIdLink(appeal.id.some)
              ),
              td(appeal.msgs.lastOption map { msg =>
                frag(
                  userIdLink(msg.by.some),
                  " ",
                  momentFromNowOnce(msg.at),
                  p(shorten(msg.text, 200))
                )
              }),
              td(
                a(href := routes.Appeal.show(appeal.id), cls := "button button-empty")("View"),
                inquiries.get(appeal.id) map { i =>
                  frag(userIdLink(i.mod.some), nbsp, "is handling this")
                }
              )
            )
          }
        )
      )
    )

  private def layout(title: String)(body: Frag)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = frag(
        cssTag("form3"),
        cssTag("appeal")
      ),
      moreJs = embedJsUnsafeLoadThen(
        """$('select.appeal-presets').on('change', e => $('#form3-text').val(e.target.value))"""
      )
    )(body)

  private def renderAppeal(
      appeal: Appeal,
      textForm: Form[_],
      asMod: Boolean,
      inquiry: Boolean = false,
      presets: Option[ModPresets]
  )(implicit
      ctx: Context
  ) =
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

  private def renderHelp =
    div(cls := "appeal__help")(
      p(
        "If your account has been restricted for violation of ",
        a(href := routes.Page.tos)("the Lichess rules"),
        " you may file an appeal here."
      ),
      p(
        "You can read more about the appeal process ",
        a(href := routes.Page.loneBookmark("appeal"))("here.")
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

  private def renderForm(form: Form[_], action: String, isNew: Boolean, presets: Option[ModPresets])(implicit
      ctx: Context
  ) =
    postForm(st.action := action)(
      form3.globalError(form),
      form3.group(form("text"), if (isNew) "Create an appeal" else "Add something to the appeal")(
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
