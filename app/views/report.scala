package views.report
import lila.app.UiEnv.{ *, given }
import lila.mod.ui.PendingCounts
import lila.rating.UserPerfsExt.bestPerfs
import lila.report.Report.WithSuspect
import lila.report.Room
import lila.report.ui.ReportUi.*

val ui = lila.report.ui.ReportUi(helpers)

def list(
    reports: List[lila.report.Report.WithSuspect],
    filter: String,
    scores: Room.Scores,
    pending: PendingCounts
)(using Context) =
  layout(filter, scores, pending)(
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
                      postForm(action := routes.Report.inquiry(r.id.value), cls := "reopen")(
                        submitButton(dataIcon := Icon.PlayTriangle, cls := "text button button-metal")(
                          "Reopen"
                        )
                      )
                    else
                      postForm(action := routes.Report.inquiry(r.id.value), cls := "inquiry")(
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

def layout(filter: String, scores: Room.Scores, pending: PendingCounts)(using
    ctx: Context
) =
  Page("Reports")
    .css("mod.report")
    .wrap: body =>
      main(cls := "page-menu")(
        views.mod.ui.menu("report"),
        div(id := "report_list", cls := "page-menu__content box")(
          div(cls := "header")(
            i(cls := "icon"),
            span(cls := "tabs")(
              isGranted(_.SeeReport).option:
                a(
                  href := routes.Report.listWithFilter("all"),
                  cls  := List("active" -> (filter == "all"))
                )(
                  "All",
                  scoreTag(scores.highest)
                )
              ,
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
              (isGranted(_.Appeals)).option(
                a(
                  href := routes.Appeal.queue(),
                  cls := List(
                    "new"    -> true,
                    "active" -> (filter == "appeal")
                  )
                )(
                  countTag(pending.appeals),
                  "Appeals"
                )
              ),
              (isGranted(_.Streamers)).option(
                a(href := s"${routes.Streamer.index()}?requests=1", cls := "new")(
                  countTag(pending.streamers),
                  "Streamers"
                )
              ),
              isGranted(_.TitleRequest).option(
                a(
                  href := routes.TitleVerify.queue,
                  cls := List(
                    "new"    -> true,
                    "active" -> (filter == "title")
                  )
                )(
                  countTag(pending.titles),
                  "Titles"
                )
              )
            )
          ),
          body
        )
      )
