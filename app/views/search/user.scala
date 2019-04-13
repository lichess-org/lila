package views.html
package search

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.gameSearch.{ Query, Sorting }
import lila.user.User

import controllers.routes

object user {
  def apply(u: User, form: Form[_])(implicit ctx: Context) = st.form(
    rel := "nofollow",
    cls := "search realtime",
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
        th(label("Date")),
        td(
          div(cls := "half")("From ", form3.select(form("dateMin"), Query.dates, "".some)),
          div(cls := "half")("To ", form3.select(form("dateMin"), Query.dates, "".some))
        )
      ),
      tr(
        th(label(trans.rating(), " ", span(cls := "help", title := "The average rating of both players")("(?)"))),
        td(
          div(cls := "half")("From ", form3.select(form("ratingMin"), Query.averageRatings, "".some)),
          div(cls := "half")("To ", form3.select(form("ratingMax"), Query.averageRatings, "".some))
        )
      ),
      tr(
        th(label("Number of turns")),
        td(
          div(cls := "half")("From ", form3.select(form("turnsMin"), Query.turns, "".some)),
          div(cls := "half")("To ", form3.select(form("turnsMax"), Query.turns, "".some))
        )
      ),
      tr(
        th(label(trans.duration())),
        td(
          div(cls := "half")("From ", form3.select(form("durationMin"), Query.durations, "".some)),
          div(cls := "half")("To ", form3.select(form("durationMax"), Query.durations, "".some))
        )
      ),
      tr(
        th(label("Clock initial time")),
        td(
          div(cls := "half")("From ", form3.select(form("clock")("initMin"), Query.clockInits, "".some)),
          div(cls := "half")("From ", form3.select(form("clock")("initMax"), Query.clockInits, "".some))
        )
      ),
      tr(
        th(label("Clock increment")),
        td(
          div(cls := "half")("From ", form3.select(form("clock")("incMin"), Query.clockIncs, "".some)),
          div(cls := "half")("From ", form3.select(form("clock")("incMax"), Query.clockIncs, "".some))
        )
      ),
      tr(
        th(label(`for` := form3.id(form("source")))("Source")),
        td(cls := "single")(form3.select(form("source"), Query.sources, "".some))
      ),
      tr(
        th(label(`for` := form3.id(form("perf")))(trans.variant())),
        td(cls := "single")(form3.select(form("perf"), Query.perfs, "".some))
      ),
      tr(
        th(label(`for` := form3.id(form("mode")))(trans.mode())),
        td(cls := "single")(form3.select(form("mode"), Query.modes, "".some))
      )
    ), table(
      tr(
        th(label(`for` := form3.id(form("hasAi")))(trans.opponent(), " ", span(cls := "help", title := "Whether the player's opponent was human or a computer")("(?)"))),
        td(cls := "single opponent")(form3.select(form("hasAi"), Query.hasAis, "".some))
      ),
      tr(cls := "aiLevel none")(
        th(label("A.I. level")),
        td(
          div(cls := "half")("From ", form3.select(form("aiLevelMin"), Query.aiLevels, "".some)),
          div(cls := "half")("To ", form3.select(form("aiLevelMax"), Query.aiLevels, "".some))
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
        th(label(`for` := form3.id(form("players")("loser")))("Loser")),
        td(cls := "single")(
          st.select(id := form3.id(form("players")("loser")), name := form("players")("loser").name)(
            option(cls := "blank", value := "")
          )
        )
      ),
      chess.Color.all.map { color =>
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
        th(label(`for` := form3.id(form("status")))("Result")),
        td(cls := "single")(form3.select(form("status"), Query.statuses, "".some))
      ),
      tr(
        th(label(`for` := form3.id(form("winnerColor")))("Winner color")),
        td(cls := "single")(form3.select(form("winnerColor"), Query.winnerColors, "".some))
      ),
      tr(
        th(label("Sort")),
        td(
          div(cls := "half")("By ", form3.select(form("sort")("field"), Sorting.fields)),
          div(cls := "half")("Order ", form3.select(form("sort")("order"), Sorting.orders))
        )
      ),
      tr(
        th(label(`for` := form3.id(form("analysed")))("Analysis ", span(cls := "help", title := "Whether computer analysis is available or not")("(?)"))),
        td(cls := "single")(
          st.input(`type` := "checkbox", id := form3.id(form("analysed")), name := form("analysed").name, value := "1", checked := form("analysed").value.has("1"))
        )
      )
    ))
}
