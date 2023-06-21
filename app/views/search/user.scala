package views.html.search

import play.api.data.Form

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.user.User

import controllers.routes

object user:

  import trans.search.*

  def apply(u: User, form: Form[?])(using PageContext) =
    val commons = bits of form
    import commons.*
    st.form(
      noFollow,
      cls    := "search__form",
      action := routes.User.games(u.username, "search"),
      method := "GET"
    )(dataReqs)(
      table(
        date,
        rating,
        turns,
        duration,
        clockTime,
        clockIncrement,
        source,
        perf,
        mode
      ),
      table(
        hasAi,
        aiLevel,
        tr(cls := "opponentName")(
          th(label(`for` := form3.id(form("players")("b")))(opponentName())),
          td(cls := "usernames")(
            st.input(tpe                          := "hidden", value := u.id, name := "players.a"),
            form3.input(form("players")("b"))(tpe := "text")
          )
        ),
        winner(hide = false),
        loser(hide = false),
        colors(hide = false),
        status,
        winnerColor,
        sort,
        analysed,
        tr(cls := "action")(
          th,
          td(button(cls := "button")(search()))
        )
      )
    )
