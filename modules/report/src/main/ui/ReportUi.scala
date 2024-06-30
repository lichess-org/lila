package lila.report
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.core.i18n.{ Translate, I18nKey as trans }
import play.api.data.Form

object ReportUi:

  def reportScore(score: Report.Score): Frag =
    span(cls := s"score ${score.color}")(score.value.toInt)

final class ReportUi(helpers: Helpers):
  import helpers.{ given, * }
  import ReportUi.*

  def form(form: Form[?], reqUser: Option[User] = None)(using ctx: Context) =
    Page(trans.site.reportAUser.txt())
      .css("bits.form3")
      .js(
        embedJsUnsafeLoadThen(
          """$('#form3-reason').on('change', function() {
            $('.report-reason').addClass('none').filter('.report-reason-' + this.value).removeClass('none');
          })"""
        )
      ):
        val defaultReason = form("reason").value.orElse(translatedReasonChoices.headOption.map(_._1.key))
        main(cls := "page-small box box-pad report")(
          h1(cls := "box__top")(trans.site.reportAUser()),
          postForm(
            cls    := "form3",
            action := s"${routes.Report.create}${reqUser.so(u => "?username=" + u.username)}"
          )(
            div(cls := "form-group")(
              p(
                a(
                  href     := routes.Cms.lonePage(lila.core.id.CmsPageKey("report-faq")),
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
                    a(href := "/dmca")("Complete this form instead"),
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
                form3.select(
                  f,
                  translatedReasonChoices.map((r, t) => (r.key, t)),
                  trans.site.whatIsIheMatter.txt().some
                )
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

  private def descriptionHelp(current: String)(using ctx: Context) = frag:
    import Reason.*
    val englishPlease = " Your report will be processed faster if written in English."
    translatedReasonChoices
      .map(_._1)
      .distinct
      .map: reason =>
        span(
          cls := List(s"report-reason report-reason-${reason.key}" -> true, "none" -> (current != reason.key))
        ):
          if reason == Cheat || reason == Boost then trans.site.reportDescriptionHelp()
          else if reason == Username then
            "Please explain briefly what about this username is offensive." + englishPlease
          else if reason.isComm then
            "Please explain briefly what that user said that was abusive." + englishPlease
          else "Please explain briefly what happened." + englishPlease

  private def translatedReasonChoices(using Translate) =
    import Reason.*
    List(
      (Cheat, trans.site.cheat.txt()),
      (Stall, "Stalling / Leaving Games"),
      (Boost, "Sandbagging / Boosting / Match fixing"),
      (VerbalAbuse, "Verbal abuse / Cursing / Trolling"),
      (Violence, "Violence / Threats"),
      (Harass, "Harassment / Bullying / Stalking"),
      (SelfHarm, "Suicide / Self-Injury"),
      (Hate, "Hate Speech"),
      (Spam, "Spamming"),
      (Username, trans.site.username.txt()),
      (Other, trans.site.other.txt())
    )

  def thanks(userId: UserId, blocked: Boolean)(using ctx: Context) =
    val title = "Thanks for the report"
    Page(title)
      .js(
        embedJsUnsafeLoadThen("""
        $('button.report-block').one('click', function() {
        const $button = $(this);
        $button.find('span').text('Blocking...');
        fetch(this.dataset.action, {method:'post'})
          .then(() => $button.find('span').text('Blocked!'));
        });
        """)
      ):
        main(cls := "page-small box box-pad")(
          h1(cls := "box__top")(title),
          p("The moderators will review it very soon, and take appropriate action."),
          br,
          br,
          (!blocked).option(
            p(
              "In the meantime, you can block this user: ",
              submitButton(
                attr("data-action") := routes.Relation.block(userId),
                cls                 := "report-block button",
                st.title            := trans.site.block.txt()
              )(span(cls := "text", dataIcon := Icon.NotAllowed)("Block ", titleNameOrId(userId)))
            )
          ),
          br,
          br,
          p(
            a(href := routes.Lobby.home)("Return to Lichess homepage")
          )
        )
