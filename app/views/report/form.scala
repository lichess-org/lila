package views.report

import play.api.data.Form

import lila.app.UiEnv.{ *, given }

import lila.report.ReportUi.translatedReasonChoices

object form:

  def apply(form: Form[?], reqUser: Option[User] = None)(using ctx: Context) =
    Page(trans.site.reportAUser.txt())
      .cssTag("form3")
      .js(
        embedJsUnsafeLoadThen(
          """$('#form3-reason').on('change', function() {
            $('.report-reason').addClass('none').filter('.report-reason-' + this.value).removeClass('none');
          })"""
        )
      ):
        val defaultReason = form("reason").value.orElse(translatedReasonChoices.headOption.map(_._1))
        main(cls := "page-small box box-pad report")(
          h1(cls := "box__top")(trans.site.reportAUser()),
          postForm(
            cls    := "form3",
            action := s"${routes.Report.create}${reqUser.so(u => "?username=" + u.username)}"
          )(
            div(cls := "form-group")(
              p(
                a(
                  href     := routes.Cms.lonePage("report-faq"),
                  dataIcon := Icon.InfoCircle,
                  cls      := "text"
                ):
                  "Read more about Lichess reports"
              ),
              ctx.req.queryString
                .contains("postUrl")
                .option(
                  p(
                    "Here for DMCA or Intellectual Property Take Down Notice? ",
                    a(href := lila.web.ui.contact.dmcaUrl)("Complete this form instead"),
                    "."
                  )
                )
            ),
            form3.globalError(form),
            form3.group(form("username"), trans.site.user(), klass = "field_to complete-parent"): f =>
              reqUser
                .map: user =>
                  frag(userLink(user), form3.hidden(f, user.id.value.some))
                .getOrElse:
                  div(form3.input(f, klass = "user-autocomplete")(dataTag := "span", autofocus))
            ,
            if ctx.req.queryString contains "reason"
            then form3.hidden(form("reason"))
            else
              form3.group(form("reason"), trans.site.reason()): f =>
                form3.select(f, translatedReasonChoices, trans.site.whatIsIheMatter.txt().some)
            ,
            form3.group(form("text"), trans.site.description(), help = descriptionHelp(~defaultReason).some):
              form3.textarea(_)(rows := 8)
            ,
            form3.actions(
              a(href := routes.Lobby.home)(trans.site.cancel()),
              form3.submit(trans.site.send())
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
          if key == Cheat.key || key == Boost.key then trans.site.reportDescriptionHelp()
          else if key == Username.key then
            "Please explain briefly what about this username is offensive." + englishPlease
          else if key == Comm.key || key == Sexism.key then
            "Please explain briefly what that user said that was abusive." + englishPlease
          else "Please explain briefly what happened." + englishPlease
