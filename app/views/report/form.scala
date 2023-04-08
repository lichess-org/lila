package views.html.report

import controllers.report.routes.{ Report as reportRoutes }
import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.user.User

object form:

  def apply(form: Form[?], reqUser: Option[User] = None, captcha: lila.common.Captcha)(using
      ctx: Context
  ) =
    views.html.base.layout(
      title = trans.reportAUser.txt(),
      moreCss = cssTag("form3-captcha"),
      moreJs = captchaTag
    ) {
      main(cls := "page-small box box-pad report")(
        h1(cls := "box__top")(trans.reportAUser()),
        postForm(
          cls    := "form3",
          action := s"${reportRoutes.create}${reqUser.??(u => "?username=" + u.username)}"
        )(
          div(cls := "form-group")(
            p(
              a(href := routes.Page.loneBookmark("report-faq"), dataIcon := "", cls := "text")(
                "Read more about Lichess reports"
              )
            ),
            ctx.req.queryString.contains("postUrl") option p(
              "Here for DMCA or Intellectual Property Take Down Notice? ",
              a(href := views.html.site.contact.dmcaUrl)("Complete this form instead"),
              "."
            )
          ),
          form3.globalError(form),
          form3.group(form("username"), trans.user(), klass = "field_to complete-parent") { f =>
            reqUser
              .map { user =>
                frag(userLink(user), form3.hidden(f, user.id.value.some))
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
