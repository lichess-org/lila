package views.html
package search

import play.api.data.Form

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.gameSearch.{ Query, Sorting }
import lidraughts.user.User

import controllers.routes

object user {
  def apply(u: User, form: Form[_])(implicit ctx: Context) = st.form(
    rel := "nofollow",
    cls := "search realtime form3",
    action := routes.User.games(u.username, "search"),
    method := "GET",
    data("req-winner") := form("players")("winner").value,
    data("req-loser") := form("players")("loser").value,
    data("req-white") := form("players")("white").value,
    data("req-black") := form("players")("black").value
  )(table(
      tr(cls := "header")(
        th(colspan := 2)(trans.advancedSearch())
      ),
      tr(
        th(trans.date()),
        td(
          div(cls := "half")(trans.from(), form3.select(form("dateMin"), Query.dates, "".some)),
          div(cls := "half")(trans.to(), form3.select(form("dateMin"), Query.dates, "".some))
        )
      ),
      tr(
        th(label(trans.rating(), " ", span(cls := "help", title := trans.searchRatingsHelp.txt())("(?)"))),
        td(
          div(cls := "half")(trans.from(), form3.select(form("ratingMin"), translatedAverageRatingChoices, "".some)),
          div(cls := "half")(trans.to(), form3.select(form("ratingMax"), translatedAverageRatingChoices, "".some))
        )
      ),
      tr(
        th(trans.numberOfTurns()),
        td(
          div(cls := "half")(trans.from(), form3.select(form("turnsMin"), translatedTurnsChoices, "".some)),
          div(cls := "half")(trans.to(), form3.select(form("turnsMax"), translatedTurnsChoices, "".some))
        )
      ),
      tr(
        th(label(trans.duration())),
        td(
          div(cls := "half")(trans.from(), form3.select(form("durationMin"), translatedDurationChoices, "".some)),
          div(cls := "half")(trans.to(), form3.select(form("durationMax"), translatedDurationChoices, "".some))
        )
      ),
      tr(
        th(label(trans.clockInitialTime())),
        td(
          div(cls := "half")(trans.from(), form3.select(form("clock")("initMin"), translatedClockInitChoices, "".some)),
          div(cls := "half")(trans.from(), form3.select(form("clock")("initMax"), translatedClockInitChoices, "".some))
        )
      ),
      tr(
        th(label(trans.increment())),
        td(
          div(cls := "half")(trans.from(), form3.select(form("clock")("incMin"), translatedClockIncChoices, "".some)),
          div(cls := "half")(trans.from(), form3.select(form("clock")("incMax"), translatedClockIncChoices, "".some))
        )
      ),
      tr(
        th(label(`for` := form3.id(form("source")))(trans.source())),
        td(cls := "single")(form3.select(form("source"), Query.sources, "".some))
      ),
      tr(
        th(label(`for` := form3.id(form("perf")))(trans.variant())),
        td(cls := "single")(form3.select(form("perf"), Query.perfs, "".some))
      ),
      tr(
        th(label(`for` := form3.id(form("mode")))(trans.mode())),
        td(cls := "single")(form3.select(form("mode"), translatedModeChoicesById, "".some))
      )
    ), table(
      tr(
        th(label(`for` := form3.id(form("hasAi")))(trans.opponent(), " ", span(cls := "help", title := trans.searchOpponentHelp.txt())("(?)"))),
        td(cls := "single opponent")(form3.select(form("hasAi"), translatedHasAiChoices, "".some))
      ),
      tr(cls := "aiLevel none")(
        th(label("A.I. level")),
        td(
          div(cls := "half")(trans.from(), form3.select(form("aiLevelMin"), Query.aiLevels, "".some)),
          div(cls := "half")(trans.to(), form3.select(form("aiLevelMax"), Query.aiLevels, "".some))
        )
      ),
      tr(cls := "opponentName")(
        th(label(`for` := form3.id(form("players")("b")))("Opponent name")),
        td(cls := "usernames")(
          st.input(`type` := "hidden", value := u.id, name := "players.a"),
          form3.input(form("players")("b"))
        )
      ),
      tr(cls := "winner user_row")(
        th(label(`for` := form3.id(form("players")("winner")))(trans.winner())),
        td(cls := "single")(
          st.select(id := form3.id(form("players")("winner")), name := form("players")("winner").name)(
            option(cls := "blank", value := "")
          )
        )
      ),
      tr(cls := "loser user_row")(
        th(label(`for` := form3.id(form("players")("loser")))(trans.loser())),
        td(cls := "single")(
          st.select(id := form3.id(form("players")("loser")), name := form("players")("loser").name)(
            option(cls := "blank", value := "")
          )
        )
      ),
      draughts.Color.all.map { color =>
        tr(cls := s"${color.name}User user_row")(
          th(label(`for` := form3.id(form("players")(color.name)))(color.fold(trans.white, trans.black)())),
          td(cls := "single")(
            st.select(id := form3.id(form("players")(color.name)), name := form("players")(color.name).name)(
              option(cls := "blank", value := "")
            )
          )
        )
      },
      tr(
        th(label(`for` := form3.id(form("status")))(trans.result())),
        td(cls := "single")(form3.select(form("status"), Query.statuses, "".some))
      ),
      tr(
        th(label(`for` := form3.id(form("winnerColor")))(trans.winnerColor())),
        td(cls := "single")(form3.select(form("winnerColor"), translatedWinnerColorChoices, "".some))
      ),
      tr(
        th(label(trans.sort())),
        td(
          div(cls := "half")(trans.sortBy(), " ", form3.select(form("sort")("field"), translatedSortFieldChoices)),
          div(cls := "half")(form3.select(form("sort")("order"), translatedSortOrderChoices))
        )
      ),
      tr(
        th(label(`for` := form3.id(form("analysed")))("Analysis ", span(cls := "help", title := trans.searchAnalysisHelp.txt())("(?)"))),
        td(cls := "single")(
          st.input(`type` := "checkbox", id := form3.id(form("analysed")), name := form("analysed").name, value := "1", checked := form("analysed").value.has("1"))
        )
      )
    ))
}
