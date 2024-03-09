package views.html

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.appeal.Appeal
import lila.common.String.html.richText
import lila.report.Report.Inquiry
import lila.report.Suspect
import lila.user.User
import play.api.data.Form
import play.api.i18n.Lang

object appeal2 {

  def home(appeal: Option[Appeal], textForm: Form[_])(implicit ctx: Context) =
    layout("Appeal") {
      main(cls := "page-small box box-pad page appeal")(
        appeal match {
          case Some(a) => renderAppeal(a, textForm, asMod = false)
          case None    => newAppeal(textForm)
        }
      )
    }

  def newAppeal(textForm: Form[_])(implicit ctx: Context) =
    frag(
      h1("Appeal a moderation decision"),
      renderHelp,
      div(cls := "body")(
        renderForm(textForm, action = routes.Appeal.post.url, isNew = true)
      )
    )

  def show(appeal: Appeal, suspect: Suspect, inquiry: Option[Inquiry], textForm: Form[_])(implicit
      ctx: Context
  ) =
    layout(s"Appeal by ${suspect.user.username}") {
      main(cls := "page-small box box-pad page appeal")(
        renderAppeal(appeal, textForm, asMod = true, inquiry = inquiry.map(_.mod).exists(ctx.userId.has)),
        div(cls := "appeal__actions")(
          inquiry match {
            case None =>
              postForm(action := routes.Mod.spontaneousInquiry(appeal.id))(
                submitButton(cls := "button")("Handle this appeal")
              )
            case Some(Inquiry(mod, _)) if ctx.userId has mod =>
              frag(
                postForm(action := routes.Report.inquiry(appeal.id))(
                  submitButton(cls := "button button-metal button-thin")("Release this appeal")
                ),
                if (appeal.isOpen)
                  frag(
                    postForm(action             := routes.Appeal.act(suspect.user.username, "close"))(
                      submitButton("Close")(cls := "button button-red button-thin")
                    ),
                    if (appeal.isMuted)
                      postForm(action               := routes.Appeal.act(suspect.user.username, "open"))(
                        submitButton("Un-mute")(cls := "button button-green button-thin")
                      )
                    else
                      postForm(action            := routes.Appeal.act(suspect.user.username, "mute"))(
                        submitButton("Mute")(cls := "button button-red button-thin")
                      )
                  )
                else
                  postForm(action            := routes.Appeal.act(suspect.user.username, "open"))(
                    submitButton("Open")(cls := "button button-green button-thin")
                  )
              )
            case Some(Inquiry(mod, _)) => frag(userIdLink(mod.some), " is handling this.")
          }
        )
      )
    }

  def queue(
      appeals: List[Appeal],
      inquiries: Map[User.ID, Inquiry],
      counts: lila.report.Room.Counts,
      streamers: Int,
      nbAppeals: Int
  )(implicit ctx: Context) =
    views.html.report.list.layout("appeal", counts, streamers, nbAppeals)(
      table(cls := "slist slist-pad see")(
        thead(
          tr(
            th("By"),
            th("Last message"),
            th
          )
        ),
        tbody(
          appeals.map { appeal =>
            tr(cls := List("new" -> appeal.isOpen))(
              td(
                userIdLink(appeal.id.some)
              ),
              td(appeal.msgs.lastOption map { msg =>
                msg.text
              }),
              td(
                a(href := routes.Appeal.show(appeal.id), cls := "button button-metal")("View"),
                inquiries.get(appeal.id) map { i =>
                  frag(userIdLink(i.mod.some), " is handling this")
                }
              )
            )
          }
        )
      )
    )

  private def layout(title: String)(body: Frag)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(
        cssTag("form3"),
        cssTag("appeal")
      ),
      title = title
    )(body)

  private def renderAppeal(appeal: Appeal, textForm: Form[_], asMod: Boolean, inquiry: Boolean = false)(
      implicit ctx: Context
  ) =
    frag(
      h1(
        if (appeal.isOpen) "Ongoing appeal" else "Closed appeal",
        asMod option frag(" : ", userIdLink(appeal.id.some))
      ),
      standardFlash(),
      !asMod option renderHelp,
      div(cls := "body")(
        !asMod option renderStatus(appeal),
        appeal.msgs.map { msg =>
          div(cls := s"appeal__msg appeal__msg--${if (appeal isByMod msg) "mod" else "suspect"}")(
            div(cls := "appeal__msg__header")(
              renderUser(appeal, msg.by),
              momentFromNowOnce(msg.at)
            ),
            div(cls := "appeal__msg__text")(richText(msg.text))
          )
        },
        (asMod == inquiry) && appeal.isOpen option renderForm(
          textForm,
          action =
            if (asMod) routes.Appeal.reply(appeal.id).url
            else routes.Appeal.post.url,
          isNew = false
        )
      )
    )

  private def renderStatus(appeal: Appeal) =
    div(cls := "appeal__status")(
      appeal.status match {
        case Appeal.Status.Closed => "The appeal is closed."
        case Appeal.Status.Unread => "The appeal is being reviewed by the moderation team."
        case Appeal.Status.Read   => "The appeal is open and awaiting your input."
        case Appeal.Status.Muted  => "The appeal is on hold."
      }
    )

  private def renderHelp =
    div(cls := "appeal__help")(
      p(
        "If your account has been restricted for violation of ",
        a(href := routes.Page.tos)("the Lishogi rules"),
        ", and you are absolutely certain that you did not break ",
        a(href := routes.Page.tos)("said rules"),
        ", then you may file an appeal here."
      ),
      p(
        "If you did break ",
        a(href := routes.Page.tos)("the Lishogi rules"),
        ", even once, then your account is lost. We don't have the luxury of being forgiving."
      ),
      p(
        strong("Do not lie in an appeal"),
        ". It would result in a lifetime ban, ",
        "and the automatic termination of any future account you make."
      )
    )

  private def renderUser(appeal: Appeal, userId: User.ID)(implicit lang: Lang) =
    userIdLink((if (appeal isAbout userId) userId else User.lishogiId).some)

  private def renderForm(form: Form[_], action: String, isNew: Boolean)(implicit ctx: Context) =
    postForm(st.action := action)(
      form3.globalError(form),
      form3.group(form("text"), if (isNew) "Create an appeal" else "Add something to the appeal")(
        form3.textarea(_)(rows := 6)
      ),
      form3.submit(trans.send())
    )
}
