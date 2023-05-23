package views.html.mod

import cats.data.NonEmptyList
import controllers.appeal.routes.{ Appeal as appealRoutes }
import controllers.report.routes.{ Report as reportRoutes }
import controllers.routes

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.richText
import lila.report.Reason
import lila.report.Report
import lila.user.User

object inquiry:

  // simul game study relay tournament
  private val commFlagRegex = """\[FLAG\] (\w+)/(\w{8})(?:/w)? (.+)""".r

  private def renderAtomText(text: String, highlight: Boolean) =
    text.split("\n").map { line =>
      val (link, text) = line match
        case commFlagRegex(tpe, id, text) =>
          val path = tpe match
            case "game"       => routes.Round.watcher(id, "white").url
            case "relay"      => routes.RelayRound.show("-", "-", id).url
            case "tournament" => routes.Tournament.show(id).url
            case "swiss"      => routes.Swiss.show(id).url
            case "forum"      => routes.ForumPost.redirect(id).url
            case _            => s"/$tpe/$id"
          a(href := path)(path).some -> text
        case text => None -> text
      frag(
        link,
        " ",
        if (highlight) communication.highlightBad(text) else frag(text),
        " "
      )
    }

  def apply(in: lila.mod.Inquiry)(implicit ctx: Context) =
    def renderReport(r: Report) =
      div(cls := "doc report")(
        r.bestAtoms(10).map { atom =>
          div(cls := "atom")(
            h3(
              reportScore(atom.score),
              userIdLink(atom.by.userId.some, withOnline = false),
              " for ",
              strong(r.reason.name),
              " ",
              momentFromNow(atom.at)
            ),
            p(renderAtomText(atom.simplifiedText, r.isComm))
          )
        }
      )

    def autoNextInput = input(cls := "auto-next", tpe := "hidden", name := "next", value := "1")

    def markButton(active: Boolean) =
      submitButton(
        cls := List(
          "fbt icon" -> true,
          "active"   -> active
        )
      )

    div(id := "inquiry")(
      i(title := "Costello the Inquiry Octopus", cls := "costello"),
      div(cls := "meat")(
        userLink(in.user, withBestRating = true, params = "?mod"),
        div(cls := "docs reports")(
          div(cls := "expendable")(
            in.allReports.map(renderReport)
          )
        ),
        isGranted(_.ModLog) option div(
          cls := List(
            "dropper counter history" -> true,
            "empty"                   -> in.history.isEmpty
          )
        )(
          span(
            countTag(in.history.size),
            "Mod log"
          ),
          in.history.nonEmpty option div(
            ul(
              in.history.map { e =>
                li(
                  userIdLink(e.mod.userId.some, withOnline = false),
                  " ",
                  b(e.showAction),
                  " ",
                  e.details,
                  " ",
                  momentFromNow(e.date)
                )
              }
            )
          )
        ),
        noteZone(in.user, in.notes)
      ),
      div(cls := "links")(
        isGranted(_.MarkBooster) option {
          val searchUrl = routes.User.games(in.user.username, "search")
          div(cls := "dropper view-games")(
            span("View", br, "Games"),
            div(
              a(
                cls := "fbt",
                href := s"$searchUrl?turnsMax=5&mode=1&players.loser=${in.user.id}&sort.field=d&sort.order=desc"
              )("Quick rated losses"),
              a(
                cls := "fbt",
                href := s"$searchUrl?turnsMax=5&mode=1&players.winner=${in.user.id}&sort.field=d&sort.order=desc"
              )("Quick rated wins"),
              isGranted(_.CheatHunter) option a(cls := "fbt", href := routes.GameMod.index(in.user.username))(
                "Hunter game list"
              ),
              boostOpponents(in.report, in.allReports, in.user) map { opponents =>
                a(
                  cls  := "fbt",
                  href := s"${routes.GameMod.index(in.user.id)}?opponents=${opponents.toList mkString ", "}"
                )("With these opponents")
              }
            )
          )
        },
        isGranted(_.Shadowban) option a(href := routes.Mod.communicationPublic(in.user.id))(
          "View",
          br,
          "Comms"
        ),
        in.report.isAppeal option a(href := appealRoutes.show(in.user.id))("View", br, "Appeal")
      ),
      div(cls := "actions")(
        isGranted(_.ModMessage) option div(cls := "dropper warn buttons")(
          iconTag(""),
          div(
            env.mod.presets.getPmPresets(ctx.me).value.map { preset =>
              postForm(action := routes.Mod.warn(in.user.username, preset.name))(
                submitButton(cls := "fbt", title := preset.text)(preset.name),
                autoNextInput
              )
            }
          )
        ),
        isGranted(_.MarkEngine) option {
          val url = routes.Mod.engine(in.user.username, !in.user.marks.engine).url
          div(cls := "dropper engine buttons")(
            postForm(action := url, cls := "main", title := "Mark as cheat")(
              markButton(in.user.marks.engine)(dataIcon := ""),
              autoNextInput
            ),
            thenForms(url, markButton(false))
          )
        },
        isGranted(_.MarkBooster) option {
          val url = routes.Mod.booster(in.user.username, !in.user.marks.boost).url
          div(cls := "dropper booster buttons")(
            postForm(action := url, cls := "main", title := "Mark as booster or sandbagger")(
              markButton(in.user.marks.boost)(dataIcon := ""),
              autoNextInput
            ),
            thenForms(url, markButton(false))
          )
        },
        isGranted(_.Shadowban) option {
          val url = routes.Mod.troll(in.user.username, !in.user.marks.troll).url
          div(cls := "dropper shadowban buttons")(
            postForm(
              action := url,
              title  := (if (in.user.marks.troll) "Un-shadowban" else "Shadowban"),
              cls    := "main"
            )(
              markButton(in.user.marks.troll)(dataIcon := ""),
              autoNextInput
            ),
            thenForms(url, markButton(false))
          )
        },
        isGranted(_.CloseAccount) option {
          val url = routes.Mod.alt(in.user.username, !in.user.marks.alt).url
          div(cls := "dropper alt buttons")(
            postForm(action := url, cls := "main", title := "Close alt account")(
              markButton(in.user.marks.alt)(i("A")),
              autoNextInput
            ),
            thenForms(url, markButton(false))
          )
        },
        div(cls := "dropper more buttons")(
          iconTag(""),
          div(
            isGranted(_.SendToZulip) option {
              val url =
                if (in.report.isAppeal) appealRoutes.sendToZulip(in.user.username)
                else routes.Mod.inquiryToZulip
              postForm(action := url)(
                submitButton(cls := "fbt")("Send to Zulip")
              )
            },
            isGranted(_.SendToZulip) option {
              postForm(action := routes.Mod.askUsertableCheck(in.user.username))(
                submitButton(cls := "fbt")("Ask for usertable check")
              )
            },
            isGranted(_.SendToZulip) option {
              postForm(action := routes.Mod.createNameCloseVote(in.user.username))(
                submitButton(cls := "fbt")("Create name-close vote")
              )
            },
            postForm(action := reportRoutes.xfiles(in.report.id))(
              submitButton(cls := List("fbt" -> true, "active" -> (in.report.room.key == "xfiles")))(
                "Move to X-Files"
              ),
              autoNextInput
            ),
            div(cls := "separator"),
            lila.memo.Snooze.Duration.values.map { snooze =>
              postForm(action := snoozeUrl(in.report, snooze.toString))(
                submitButton(cls := "fbt")(s"Snooze ${snooze.name}"),
                autoNextInput
              )
            }
          )
        )
      ),
      div(cls := "actions close")(
        span(cls := "switcher", title := "Automatically open next report")(
          span(cls := "switch")(
            form3.cmnToggle("auto-next", "auto-next", checked = true)
          )
        ),
        postForm(
          action := reportRoutes.process(in.report.id),
          title  := "Dismiss this report as processed. (Hotkey: d)",
          cls    := "process"
        )(
          submitButton(dataIcon := "", cls := "fbt"),
          autoNextInput
        ),
        postForm(
          action := reportRoutes.inquiry(in.report.id),
          title  := "Cancel the inquiry, re-instore the report",
          cls    := "cancel"
        )(
          submitButton(dataIcon := "", cls := "fbt")(in.alreadyMarked option disabled)
        )
      )
    )

  def noteZone(u: User, notes: List[lila.user.Note])(implicit ctx: Context) = div(
    cls := List(
      "dropper counter notes" -> true,
      "empty"                 -> notes.isEmpty
    )
  )(
    span(
      countTag(notes.size),
      "Notes"
    ),
    div(
      postForm(cls := "note", action := s"${routes.User.writeNote(u.username)}?inquiry=1")(
        form3.textarea(lila.user.UserForm.note("text"))(
          placeholder := "Write a mod note"
        ),
        div(cls := "submission")(
          submitButton(cls := "button thin", name := "noteType", value := "mod")("SEND"),
          isGranted(_.Admin) option submitButton(cls := "button thin", name := "noteType", value := "dox")(
            "SEND DOX"
          )
        )
      ),
      notes map { note =>
        (!note.dox || isGranted(_.Admin)) option div(cls := "doc note")(
          h3("by ", userIdLink(note.from.some, withOnline = false), ", ", momentFromNow(note.date)),
          p(richText(note.text, expandImg = false))
        )
      }
    )
  )

  private def snoozeUrl(report: Report, duration: String): String =
    if (report.isAppeal) appealRoutes.snooze(report.user, duration).url
    else reportRoutes.snooze(report.id, duration).url

  private def boostOpponents(
      report: Report,
      allReports: List[Report],
      reportee: User
  ): Option[NonEmptyList[UserId]] =
    (report.reason == Reason.Boost || reportee.marks.boost) ?? {
      allReports
        .filter(_.reason == Reason.Boost)
        .flatMap(_.atoms.toList)
        .withFilter(_.byLichess)
        .flatMap(_.text.linesIterator)
        .collect {
          case farmWithRegex(userId)     => List(userId)
          case sandbagWithRegex(userIds) => userIds.split(' ').toList.map(_.replace("@", ""))
        }
        .flatten
        .flatMap(UserStr.read)
        .flatMap(User.validateId)
        .distinct
        .toNel
    }

  private val farmWithRegex =
    ("^Boosting: farms rating points from @(" + User.historicalUsernameRegex.pattern + ")").r.unanchored
  private val sandbagWithRegex =
    "^Sandbagging: throws games to (.+)".r.unanchored

  private def thenForms(url: String, button: Tag) =
    div(
      postForm(
        action := url,
        button("And stay on this report"),
        form3.hidden("next", "0")
      ),
      postForm(
        action := url,
        button("Then open profile"),
        form3.hidden("then", "profile")
      )
    )
