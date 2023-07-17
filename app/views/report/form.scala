package views.html.report

import controllers.report.routes.{ Report as reportRoutes }
import controllers.routes
import play.api.data.Form

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.user.User

object form:

  def apply(form: Form[?], reqUser: Option[User] = None)(using ctx: PageContext) =
    views.html.base.layout(
      title = trans.reportAUser.txt(),
      moreCss = cssTag("form3"),
      moreJs = embedJsUnsafeLoadThen(
        """$('#form3-reason').on('change', function() {
            $('.report-reason').addClass('none').filter('.report-reason-' + this.value).removeClass('none');
          })"""
      )
    ):
      val defaultReason = form("reason").value orElse translatedReasonChoices.headOption.map(_._1)
      main(cls := "page-small box box-pad report")(
        h1(cls := "box__top")(trans.reportAUser()),
        postForm(
          cls    := "form3",
          action := s"${reportRoutes.create}${reqUser.so(u => "?username=" + u.username)}"
        )(
          div(cls := "form-group")(
            p(
              a(
                href     := routes.ContentPage.loneBookmark("report-faq"),
                dataIcon := licon.InfoCircle,
                cls      := "text"
              ):
                "Read more about Lichess reports"
            ),
            ctx.req.queryString.contains("postUrl") option p(
              "Here for DMCA or Intellectual Property Take Down Notice? ",
              a(href := views.html.site.contact.dmcaUrl)("Complete this form instead"),
              "."
            )
          ),
          form3.globalError(form),
          form3.group(form("username"), trans.user(), klass = "field_to complete-parent"): f =>
            reqUser
              .map: user =>
                frag(userLink(user), form3.hidden(f, user.id.value.some))
              .getOrElse:
                div(form3.input(f, klass = "user-autocomplete")(dataTag := "span", autofocus))
          ,
          if ctx.req.queryString contains "reason"
          then form3.hidden(form("reason"))
          else
            form3.group(form("reason"), trans.reason()): f =>
              form3.select(f, translatedReasonChoices, trans.whatIsIheMatter.txt().some)
          ,
          form3.group(form("text"), trans.description(), help = descriptionHelp(~defaultReason).some):
            form3.textarea(_)(rows := 8)
          ,
          form3.actions(
            a(href := routes.Lobby.home)(trans.cancel()),
            form3.submit(trans.send())
          )
        )
      )

  private def descriptionHelp(default: String)(using ctx: Context) = frag:
    import lila.report.Reason.*
    val englishPlease = " Your report will be processed faster if written in English."
    translatedReasonChoices
      .map(_._1)
      .distinct
      .map: key =>
        span(cls := List(s"report-reason report-reason-$key" -> true, "none" -> (default != key))):
          if key == Cheat.key || key == Boost.key then trans.reportDescriptionHelp()
          else if key == Username.key then
            "Please explain briefly what about this username is offensive." + englishPlease
          else if key == Comm.key || key == Sexism.key then
            "Please explain briefly what that user said that was abusive." + englishPlease
          else "Please explain briefly what happened." + englishPlease
