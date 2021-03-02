package views.html.mod

import cats.data.NonEmptyList
import controllers.routes
import scala.util.matching.Regex

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText
import lila.report.Reason
import lila.report.Report
import lila.user.User

object inquiry {

  // simul game study relay tournament
  private val commFlagRegex = """\[FLAG\] (\w+)/(\w{8})(?:/w)? (.+)(?:\n|$)""".r

  def renderAtomText(atom: lila.report.Report.Atom) =
    richText(
      commFlagRegex.replaceAllIn(
        atom.simplifiedText,
        m => {
          val id = m.group(2)
          val path = m.group(1) match {
            case "game"       => routes.Round.watcher(id, "white")
            case "relay"      => routes.Relay.show("-", id)
            case "tournament" => routes.Tournament.show(id)
            case "swiss"      => routes.Swiss.show(id)
            case _            => s"/${m.group(1)}/$id"
          }
          Regex.quoteReplacement(s"$netBaseUrl$path ${m.group(3)}")
        }
      )
    )

  def apply(in: lila.mod.Inquiry)(implicit ctx: Context) = {
    def renderReport(r: lila.report.Report) =
      div(cls := "doc report")(
        r.bestAtoms(10).map { atom =>
          div(cls := "atom")(
            h3(
              reportScore(atom.score),
              userIdLink(atom.by.value.some, withOnline = false),
              " for ",
              strong(r.reason.name),
              " ",
              momentFromNow(atom.at)
            ),
            p(renderAtomText(atom))
          )
        }
      )

    def renderNote(r: lila.user.Note)(implicit ctx: Context) =
      (!r.dox || isGranted(_.Doxing)) option div(cls := "doc note")(
        h3("by ", userIdLink(r.from.some, withOnline = false), ", ", momentFromNow(r.date)),
        p(richText(r.text, expandImg = false))
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
                  userIdLink(e.mod.some, withOnline = false),
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
        div(
          cls := List(
            "dropper counter notes" -> true,
            "empty"                 -> in.notes.isEmpty
          )
        )(
          span(
            countTag(in.notes.size),
            "Notes"
          ),
          div(
            postForm(cls := "note", action := s"${routes.User.writeNote(in.user.username)}?note")(
              textarea(name := "text", placeholder := "Write a mod note"),
              input(tpe := "hidden", name := "mod", value := "true"),
              div(cls := "submission")(
                submitButton(cls := "button thin")("SEND")
              )
            ),
            in.notes.map(renderNote)
          )
        )
      ),
      div(cls := "links")(
        boostOpponents(in.report) map { opponents =>
          a(href := s"${routes.GameMod.index(in.user.id)}?opponents=${opponents.toList mkString ", "}")(
            "View",
            br,
            "Games"
          )
        },
        isGranted(_.Shadowban) option
          a(href := routes.Mod.communicationPublic(in.user.id))("View", br, "Comms")
      ),
      div(cls := "actions")(
        isGranted(_.ModMessage) option div(cls := "dropper warn buttons")(
          iconTag("e"),
          div(
            env.mod.presets.pmPresets.get().value.map { preset =>
              postForm(action := routes.Mod.warn(in.user.username, preset.name))(
                submitButton(cls := "fbt")(preset.name),
                autoNextInput
              )
            }
          )
        ),
        isGranted(_.MarkEngine) option {
          val url = routes.Mod.engine(in.user.username, !in.user.marks.engine).url
          div(cls := "dropper engine buttons")(
            postForm(action := url, title := "Mark as cheat")(
              markButton(in.user.marks.engine)(dataIcon := "n"),
              autoNextInput
            ),
            thenForms(url, markButton(false))
          )
        },
        isGranted(_.MarkBooster) option {
          val url = routes.Mod.booster(in.user.username, !in.user.marks.boost).url
          div(cls := "dropper booster buttons")(
            postForm(action := url, cls := "main", title := "Mark as booster or sandbagger")(
              markButton(in.user.marks.boost)(dataIcon := "9"),
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
              title := (if (in.user.marks.troll) "Un-shadowban" else "Shadowban"),
              cls := "main"
            )(
              markButton(in.user.marks.troll)(dataIcon := "c"),
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
          iconTag("u"),
          div(
            postForm(action := routes.Mod.notifySlack(in.user.id))(
              submitButton(cls := "fbt")("Notify Slack")
            ),
            postForm(action := routes.Report.xfiles(in.report.id))(
              submitButton(cls := List("fbt" -> true, "active" -> (in.report.room.key == "xfiles")))(
                "Move to X-Files"
              ),
              autoNextInput
            )
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
          action := routes.Report.process(in.report.id),
          title := "Dismiss this report as processed. (Hotkey: d)",
          cls := "process"
        )(
          submitButton(dataIcon := "E", cls := "fbt"),
          autoNextInput
        ),
        postForm(
          action := routes.Report.inquiry(in.report.id),
          title := "Cancel the inquiry, re-instore the report",
          cls := "cancel"
        )(
          submitButton(dataIcon := "L", cls := "fbt")
        )
      )
    )
  }

  private def boostOpponents(report: Report): Option[NonEmptyList[User.ID]] =
    (report.reason == Reason.Boost) ?? {
      report.atoms.toList
        .withFilter(_.byLichess)
        .flatMap(_.text.linesIterator)
        .collect {
          case farmWithRegex(userId)    => userId
          case sandbagWithRegex(userId) => userId
        }
        .toNel
    }

  private val farmWithRegex =
    ("^Boosting: farms rating points from @(" + User.historicalUsernameRegex.pattern + ")").r.unanchored
  private val sandbagWithRegex =
    ("^Sandbagging: throws games to @(" + User.historicalUsernameRegex.pattern + ")").r.unanchored

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
}
