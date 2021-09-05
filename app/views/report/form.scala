package views.html.report

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import controllers.routes

object form {

  def apply(form: Form[_], reqUser: Option[User] = None, captcha: lila.common.Captcha)(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      title = trans.reportAUser.txt(),
      moreCss = cssTag("form3-captcha"),
      moreJs = captchaTag
    ) {
      main(cls := "page-small box box-pad report")(
        h1(trans.reportAUser()),
        postForm(
          cls := "form3",
          action := s"${routes.Report.create}${reqUser.??(u => "?username=" + u.username)}"
        )(
          div(cls := "form-group")(
            a(href := routes.Page.loneBookmark("report-faq"), dataIcon := "", cls := "text")(
              "Read more about Lichess reports"
            )
          ),
          form3.globalError(form),
          form3.group(form("username"), trans.user(), klass = "field_to complete-parent") { f =>
            reqUser
              .map { user =>
                frag(userLink(user), form3.hidden(f, user.id.some))
              }
              .getOrElse {
                div(form3.input(f, klass = "user-autocomplete")(dataTag := "span", autofocus))
              }
          },
          if (ctx.req.queryString contains "reason")
            form3.hidden(form("reason"))
          else
            form3.group(form("reason"), trans.reason()) { f =>
              form3.select(f, translatedReasonChoices, trans.whatIsIheMatter.txt().some)
            },
          form3.group(form("text"), trans.description(), help = trans.reportDescriptionHelp().some)(
            form3.textarea(_)(rows := 8)
          ),
          views.html.base.captcha(form, captcha),
          form3.actions(
            a(href := routes.Lobby.home)(trans.cancel()),
            form3.submit(trans.send())
          )
        )
      )
    }
}
