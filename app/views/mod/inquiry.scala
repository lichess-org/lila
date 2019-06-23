package views.html.mod

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText

import controllers.routes

object inquiry {

  def apply(in: lila.mod.Inquiry)(implicit ctx: Context) = {

    def renderReport(r: lila.report.Report) =
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

    def renderNote(r: lila.user.Note) =
      div(cls := "doc note")(
        h3("by ", userIdLink(r.from.some, withOnline = false), ", ", momentFromNow(r.date)),
        p(richText(r.text))
      )

    def autoNextInput =
      input(cls := "auto-next", tpe := "hidden", name := "next", value := "1")

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
            form(cls := "note", action := s"${routes.User.writeNote(in.user.username)}?note", method := "post")(
              textarea(name := "text", placeholder := "Write a mod note"),
              input(tpe := "hidden", name := "mod", value := "true"),
              div(cls := "submission")(
                button(tpe := "submit", cls := "button thin")("SEND")
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
        div(cls := "dropper warn")(
          span(cls := "icon", dataIcon := "e"),
          div(
            lila.message.ModPreset.all.map { preset =>
              form(method := "post", action := routes.Mod.warn(in.user.username, preset.subject))(
                button(cls := "fbt", tpe := "submit")(preset.subject),
                autoNextInput
              )
            },
            form(method := "get", action := routes.Message.form)(
              input(tpe := "hidden", name := "mod", value := "1"),
              input(tpe := "hidden", name := "user", value := "@in.user.id"),
              button(cls := "fbt", tpe := "submit")("Custom message")
            )
          )
        ),
        isGranted(_.MarkEngine) option
          form(method := "post", action := routes.Mod.engine(in.user.username, !in.user.engine), title := "Mark as cheat")(
            button(dataIcon := "n", cls := List(
              "fbt icon" -> true,
              "active" -> in.user.engine
            ), tpe := "submit"),
            autoNextInput
          ),
        isGranted(_.MarkBooster) option
          form(method := "post", action := routes.Mod.booster(in.user.username, !in.user.booster), title := "Mark as booster or sandbagger")(
            button(dataIcon := "9", cls := List(
              "fbt icon" -> true,
              "active" -> in.user.booster
            ), tpe := "submit"),
            autoNextInput
          ),
        isGranted(_.Shadowban) option
          form(method := "post", action := routes.Mod.troll(in.user.username, !in.user.booster),
            title := (if (in.user.troll) "Un-shadowban" else "Shadowban"),
            button(dataIcon := "c", cls := List(
              "fbt icon" -> true,
              "active" -> in.user.troll
            ), tpe := "submit"),
            autoNextInput),
        div(cls := "dropper more")(
          span(cls := "icon", dataIcon := "u"),
          div(
            form(method := "post", action := routes.Mod.notifySlack(in.user.id))(
              button(cls := "fbt", tpe := "submit")("Notify Slack")
            ),
            form(method := "post", action := routes.Report.xfiles(in.report.id))(
              button(cls := List("fbt" -> true, "active" -> (in.report.room.key == "xfiles")), tpe := "submit")("Move to X-Files"),
              autoNextInput
            )
          )
        )
      ),
      div(cls := "actions close")(
        span(cls := "switcher", title := "Automatically open next report")(
          span(cls := "switch")(
            input(id := "auto-next", cls := "cmn-toggle", tpe := "checkbox", checked),
            label(`for` := "auto-next")
          )
        ),
        form(action := routes.Report.process(in.report.id), method := "post", title := "Dismiss this report as processed.", cls := "process")(
          button(tpe := "submit", dataIcon := "E", cls := "fbt"),
          autoNextInput
        ),
        form(action := routes.Report.inquiry(in.report.id), method := "post", title := "Cancel the inquiry, re-instore the report", cls := "cancel")(
          button(tpe := "submit", dataIcon := "L", cls := "fbt")
        )
      )
    )
  }
}
