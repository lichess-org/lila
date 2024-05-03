package views.report

import play.api.data.Form

import lila.app.UiEnv.{ *, given }
import lila.report.ui.ReportUi.*
import lila.report.Room
import lila.report.Report.WithSuspect
import lila.rating.UserPerfsExt.bestPerfs

val ui = lila.report.ui.ReportUi(helpers)

def list(
    reports: List[lila.report.Report.WithSuspect],
    filter: String,
    scores: Room.Scores,
    streamers: Int,
    appeals: Int
)(using Context) =
  layout(filter, scores, streamers, appeals)(
    table(cls := "slist slist-pad see")(
      thead(
        tr(
          th("Report"),
          th("By"),
          th
        )
      ),
      tbody(
        reports.map {
          case WithSuspect(r, sus, _) if !r.isComm || isGranted(_.Shadowban) =>
            tr(cls := List("new" -> r.open))(
              td(
                reportScore(r.score),
                strong(r.reason.name.capitalize),
                br,
                userLink(sus.user, params = "?mod"),
                br,
                p(cls := "perfs")(sus.perfs.bestPerfs(2).map(showPerfRating)),
                views.mod.user.userMarks(sus.user, none)
              ),
              td(cls := "atoms")(
                r.bestAtoms(3).map { atom =>
                  div(cls := "atom")(
                    span(cls := "head")(
                      reportScore(atom.score),
                      " ",
                      userIdLink(atom.by.userId.some),
                      " ",
                      momentFromNowOnce(atom.at)
                    ),
                    p(
                      cls := List(
                        "text"  -> true,
                        "large" -> (atom.text.length > 100 || atom.text.linesIterator.size > 3)
                      )
                    )(shorten(atom.text, 200))
                  )
                },
                (r.atoms.size > 3).option(i(cls := "more")("And ", r.atoms.size - 3, " more"))
              ),
              td(
                r.inquiry match
                  case None =>
                    if r.done.isDefined then
                      postForm(action := routes.Report.inquiry(r.id), cls := "reopen")(
                        submitButton(dataIcon := Icon.PlayTriangle, cls := "text button button-metal")(
                          "Reopen"
                        )
                      )
                    else
                      postForm(action := routes.Report.inquiry(r.id), cls := "inquiry")(
                        submitButton(dataIcon := Icon.PlayTriangle, cls := "button button-metal")
                      )
                  case Some(inquiry) =>
                    frag(
                      "Open by ",
                      userIdLink(inquiry.mod.some)
                    )
              )
            )
          case _ => emptyFrag
        }
      )
    )
  )

private val scoreTag = tag("score")

def layout(filter: String, scores: Room.Scores, streamers: Int, appeals: Int)(using ctx: Context) =
  Page("Reports")
    .cssTag("mod.report")
    .wrap: body =>
      main(cls := "page-menu")(
        views.mod.ui.menu("report"),
        div(id := "report_list", cls := "page-menu__content box")(
          div(cls := "header")(
            i(cls := "icon"),
            span(cls := "tabs")(
              a(
                href := routes.Report.listWithFilter("all"),
                cls  := List("active" -> (filter == "all"))
              )(
                "All",
                scoreTag(scores.highest)
              ),
              ctx.me.soUse {
                Room.values
                  .filter(Room.isGranted)
                  .map { room =>
                    a(
                      href := routes.Report.listWithFilter(room.key),
                      cls := List(
                        "active"            -> (filter == room.key),
                        s"room-${room.key}" -> true
                      )
                    )(
                      room.name,
                      scores.get(room).filter(20 <=).map(scoreTag(_))
                    )
                  }
                  .toList
              }: List[Frag],
              (appeals > 0 && isGranted(_.Appeals)).option(
                a(
                  href := routes.Appeal.queue(),
                  cls := List(
                    "new"    -> true,
                    "active" -> (filter == "appeal")
                  )
                )(
                  countTag(appeals),
                  "Appeals"
                )
              ),
              (isGranted(_.Streamers) && streamers > 0).option(
                a(href := s"${routes.Streamer.index()}?requests=1", cls := "new")(
                  countTag(streamers),
                  "Streamers"
                )
              )
            )
          ),
          body
        )
      )

def form(form: Form[?], reqUser: Option[User] = None)(using ctx: Context) =
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
