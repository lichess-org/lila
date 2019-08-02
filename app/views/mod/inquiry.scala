package views.html.mod

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.richText

import controllers.routes

object inquiry {

  def apply(in: lidraughts.mod.Inquiry)(implicit ctx: Context) = {

    def renderReport(r: lidraughts.report.Report) =
      div(cls := "doc report")(
        r.bestAtoms(10).toList.map { atom =>
          div(cls := "atom")(
            h3(
              reportScore(atom.score),
              userIdLink(atom.by.value.some, withOnline = false),
              " for ", strong(r.reason.name),
              " ", momentFromNow(atom.at)
            ),
            p(richText(atom.simplifiedText))
          )
        }
      )

    def renderNote(r: lidraughts.user.Note) =
      div(cls := "doc note")(
        h3("by ", userIdLink(r.from.some, withOnline = false), ", ", momentFromNow(r.date)),
        p(richText(r.text))
      )

    def autoNextInput = input(cls := "auto-next", tpe := "hidden", name := "next", value := "1")

    div(id := "inquiry")(
      i(title := "Costello the Inquiry Octopus", cls := "costello"),
      div(cls := "meat")(
        userLink(in.user, withBestRating = true, params = "?mod"),
        div(cls := "docs reports")(
          div(cls := "expendable")(
            in.allReports.map(renderReport)
          )
        ),
        div(cls := List(
          "dropper counter history" -> true,
          "empty" -> in.history.isEmpty
        ))(
          span(
            countTag(in.history.size),
            "Mod log"
          ),
          in.history.nonEmpty option div(
            ul(
              in.history.map { e =>
                li(
                  userIdLink(e.mod.some, withOnline = false),
                  " ", b(e.showAction), " ", e.details, " ", momentFromNow(e.date)
                )
              }
            )
          )
        ),
        div(cls := List(
          "dropper counter notes" -> true,
          "empty" -> in.notes.isEmpty
        ))(
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
        in.report.boostWith.map { userId =>
          a(href := s"${routes.User.games(in.user.id, "search")}?players.b=${userId}")("View", br, "Games")
        }.getOrElse {
          in.report.bestAtomByHuman.map { atom =>
            a(href := s"${routes.User.games(in.user.id, "search")}?players.b=${atom.by.value}")("View", br, "Games")
          }
        },
        isGranted(_.Shadowban) option
          a(href := routes.Mod.communicationPublic(in.user.id))("View", br, "Comms")
      ),
      div(cls := "actions")(
        div(cls := "dropper warn buttons")(
          iconTag("e"),
          div(
            lidraughts.message.ModPreset.all.map { preset =>
              postForm(action := routes.Mod.warn(in.user.username, preset.subject))(
                submitButton(cls := "fbt")(preset.subject),
                autoNextInput
              )
            },
            form(method := "get", action := routes.Message.form)(
              input(tpe := "hidden", name := "mod", value := "1"),
              input(tpe := "hidden", name := "user", value := "@in.user.id"),
              submitButton(cls := "fbt")("Custom message")
            )
          )
        ),
        isGranted(_.MarkEngine) option {
          val url = routes.Mod.engine(in.user.username, !in.user.engine).url
          def button(active: Boolean) = submitButton(cls := List(
            "fbt icon" -> true,
            "active" -> active
          ))
          div(cls := "dropper engine buttons")(
            postForm(action := url, title := "Mark as cheat")(
              button(in.user.engine)(dataIcon := "n"),
              autoNextInput
            ),
            thenForms(in, url, button(false))
          )
        },
        isGranted(_.MarkBooster) option {
          val url = routes.Mod.booster(in.user.username, !in.user.booster).url
          def button(active: Boolean) = submitButton(cls := List(
            "fbt icon" -> true,
            "active" -> active
          ))
          div(cls := "dropper booster buttons")(
            postForm(action := url, cls := "main", title := "Mark as booster or sandbagger")(
              button(in.user.booster)(dataIcon := "9"),
              autoNextInput
            ),
            thenForms(in, url, button(false))
          )
        },
        isGranted(_.Shadowban) option {
          val url = routes.Mod.troll(in.user.username, !in.user.troll).url
          def button(active: Boolean) = submitButton(cls := List(
            "fbt icon" -> true,
            "active" -> active
          ))
          div(cls := "dropper shadowban buttons")(
            postForm(
              action := url,
              title := (if (in.user.troll) "Un-shadowban" else "Shadowban"),
              cls := "main"
            )(
                button(in.user.troll)(dataIcon := "c"),
                autoNextInput
              ),
            thenForms(in, url, button(false))
          )
        }
      /* div(cls := "dropper more buttons")(
          iconTag("u"),
          div(
            postForm(action := routes.Mod.notifySlack(in.user.id))(
              submitButton(cls := "fbt")("Notify Slack")
            ),
            postForm(action := routes.Report.xfiles(in.report.id))(
              submitButton(cls := List("fbt" -> true, "active" -> (in.report.room.key == "xfiles")))("Move to X-Files"),
              autoNextInput
            )
          )
        )*/
      ),
      div(cls := "actions close")(
        span(cls := "switcher", title := "Automatically open next report")(
          span(cls := "switch")(
            input(id := "auto-next", cls := "cmn-toggle", tpe := "checkbox", checked),
            label(`for` := "auto-next")
          )
        ),
        postForm(action := routes.Report.process(in.report.id), title := "Dismiss this report as processed.", cls := "process")(
          submitButton(dataIcon := "E", cls := "fbt"),
          autoNextInput
        ),
        postForm(action := routes.Report.inquiry(in.report.id), title := "Cancel the inquiry, re-instore the report", cls := "cancel")(
          submitButton(dataIcon := "L", cls := "fbt")
        )
      )
    )
  }

  private def thenInput(what: String) = input(tpe := "hidden", name := "then", value := what)
  private def thenForms(in: lidraughts.mod.Inquiry, url: String, button: Tag) = div(
    postForm(
      action := url,
      button("And stay on this report"),
      thenInput("back")
    ),
    postForm(
      action := url,
      button("Then open profile"),
      thenInput("profile")
    )
  )
}
