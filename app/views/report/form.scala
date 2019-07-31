package views.html.report

import play.api.data.Form

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.user.User

import controllers.routes

object form {

  def apply(form: Form[_], reqUser: Option[User] = None, captcha: lidraughts.common.Captcha)(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.reportAUser.txt(),
      moreCss = cssTag("form3-captcha"),
      moreJs = captchaTag
    ) {
        main(cls := "page-small box box-pad report")(
          h1(trans.reportAUser()),
          st.form(
            cls := "form3",
            action := s"${routes.Report.create()}${reqUser.??(u => "?username=" + u.username)}",
            method := "post"
          )(
              form3.globalError(form),
              form3.group(form("username"), trans.user(), klass = "field_to") { f =>
                reqUser.map { user =>
                  frag(userLink(user), form3.hidden(f, user.id.some))
                }.getOrElse {
                  div(form3.input(f, klass = "user-autocomplete")(dataTag := "span"))
                }
              },
              form3.group(form("reason"), trans.reason()) { f =>
                form3.select(f, translatedReasonChoices, trans.whatIsIheMatter.txt().some)
              },
              form3.group(form("text"), trans.description(), help = trans.reportDescriptionHelp().some)(form3.textarea(_)(rows := 8)),
              views.html.base.captcha(form, captcha),
              form3.actions(
                a(href := routes.Lobby.home())(trans.cancel()),
                form3.submit(trans.send())
              )
            )
        )
      }

  def flag(username: String, resource: String, text: String) =
    st.form(action := routes.Report.flag, method := "post", cls := "comm-flag")(
      form3.hidden("username", username),
      form3.hidden("resource", resource),
      form3.hidden("text", text take 140),
      button(
        tpe := "submit",
        cls := "button button-empty button-red confirm",
        dataIcon := "j",
        title := "Report spam or offensive language"
      )
    )
}
