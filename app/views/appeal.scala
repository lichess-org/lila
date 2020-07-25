package views.html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText
import play.api.data.Form
import lila.appeal.Appeal
import controllers.routes
import lila.user.User
import play.api.i18n.Lang

object appeal2 {

  def home(appeal: Option[Appeal], textForm: Form[_])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(
        cssTag("form3"),
        cssTag("appeal")
      ),
      title = "Appeal"
    ) {
      main(cls := "page-small box box-pad page appeal")(
        appeal match {
          case Some(a) => myAppeal(a, textForm)
          case None    => newAppeal(textForm)
        }
      )
    }

  def newAppeal(textForm: Form[_])(implicit ctx: Context) =
    frag(
      h1("Appeal a moderation decision"),
      renderHelp,
      div(cls := "body")(
        renderForm(textForm)
      )
    )

  def myAppeal(appeal: Appeal, textForm: Form[_])(implicit ctx: Context) =
    frag(
      h1(if (appeal.isOpen) "Ongoing appeal" else "Closed appeal"),
      standardFlash(),
      renderHelp,
      div(cls := "body")(
        renderStatus(appeal),
        appeal.msgs.map { msg =>
          div(cls := "appeal__msg")(
            div(cls := "appeal__msg__header")(
              renderUser(appeal, msg.by),
              momentFromNowOnce(msg.at)
            ),
            div(cls := "appeal__msg__text")(richText(msg.text))
          )
        }
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
        a(href := routes.Page.tos())("the Lichess rules"),
        ", and you are absolutely certain that you did not break ",
        a(href := routes.Page.tos())("said rules"),
        ", then you may file an appeal here."
      ),
      p(
        "If you did break ",
        a(href := routes.Page.tos())("the Lichess rules"),
        ", even once, then your account is lost. We don't have the luxury of being forgiving."
      ),
      p(
        strong("Do not lie in an appeal"),
        ". It would result in a lifetime ban, ",
        "and the automatic termination of any future account you make."
      )
    )

  private def renderUser(appeal: Appeal, userId: User.ID)(implicit lang: Lang) =
    userIdLink((if (appeal isAbout userId) userId else User.lichessId).some)

  private def renderForm(form: Form[_])(implicit ctx: Context) =
    postForm(action := routes.Appeal.post())(
      form3.globalError(form),
      form3.group(form("text"), trans.description())(
        form3.textarea(_)(rows := 8)
      ),
      form3.action(form3.submit(trans.send()))
    )
}
