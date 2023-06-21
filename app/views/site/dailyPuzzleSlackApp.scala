package views.html.site

import lila.app.templating.Environment.*
import lila.app.ui.ScalatagsTemplate.{ *, given }
import controllers.routes

object dailyPuzzleSlackApp:

  def apply()(using PageContext) =
    views.html.base.layout(
      title = "Daily Chess Puzzle by Lichess (Slack App)",
      moreCss = cssTag("page")
    ) {
      main(cls := "page page-small box box-pad")(
        h1(cls := "box__top")("Daily Chess Puzzle by Lichess (Slack App)"),
        div(cls := "body")(
          p(
            "Spice up your Slack workspace with a daily chess puzzle from ",
            a(href := "/")("lichess.org"),
            "."
          ),
          a(
            href := "https://slack.com/oauth/v2/authorize?client_id=17688987239.964622027363&scope=commands,incoming-webhook"
          )(
            img(
              alt     := "Add to Slack",
              heightA := 40,
              widthA  := 139,
              src     := assetUrl("images/add-to-slack.png")
            )
          ),
          h2("Summary"),
          p(
            "By default, the app will post the ",
            a(href := routes.Puzzle.daily)("daily chess puzzle"),
            " from Lichess to the channel in which it was installed every day (at the same time of day it was installed). Use the ",
            code("/puzzletime"),
            " command to change this setting, e.g. ",
            code("/puzzletime 14:45"),
            ". To post the daily puzzle on demand, use the ",
            code("/puzzle"),
            " command."
          ),
          h2("Commands"),
          ul(
            li(code("/puzzlehelp"), " - Displays helpful instructions"),
            li(
              code("/puzzletime HH:MM"),
              " - Sets the time of day the daily puzzle should be posted (per channel)"
            ),
            li(code("/puzzle"), " - Posts the daily puzzle")
          ),
          h2("Privacy Policy"),
          p(
            "The app only collects and stores information necessary to deliver the service, which is limited to OAuth authentication information, Slack workspace/channel identifiers and app configuration settings. No personal information is processed except for the username of users invoking slash commands. No personal information is stored."
          ),
          h2("Support and feedback"),
          p(
            // Contact email, because Slack requires a support channel without
            // mandatory registration.
            "Give us feedback or ask questions ",
            a(href := routes.ForumCateg.show("lichess-feedback"))(
              "in the forum"
            ),
            ". The source code is available at ",
            a(href := "https://github.com/arex1337/lichess-daily-puzzle-slack-app")(
              "github.com/arex1337/lichess-daily-puzzle-slack-app"
            ),
            "."
          )
        )
      )
    }
