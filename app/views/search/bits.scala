package views.html.search

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.data.Form
import scala.util.chaining._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.gameSearch.{ Query, Sorting }

private object bits {

  import trans.search._

  private val dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
  private val dateMin       = "2011-01-01"
  private def dateMinMax: List[Modifier] =
    List(min := dateMin, max := dateFormatter.print(DateTime.now.plusDays(1)))

  def of(form: Form[_])(implicit ctx: Context) =
    new {

      def dataReqs =
        List("winner", "loser", "sente", "gote").map { f =>
          data(s"req-$f") := ~form("players")(f).value
        }

      def colors(hide: Boolean) =
        shogi.Color.all.map { color =>
          tr(cls := List(s"${color.name}User user-row" -> true, "none" -> hide))(
            th(label(`for` := form3.id(form("players")(color.name)))(standardColorName(color))),
            td(cls := "single")(
              st.select(
                id   := form3.id(form("players")(color.name)),
                name := form("players")(color.name).name
              )(
                option(cls := "blank", value := "")
              )
            )
          )
        }

      def winner(hide: Boolean) =
        form("players")("winner") pipe { field =>
          tr(cls := List("winner user-row" -> true, "none" -> hide))(
            th(label(`for` := form3.id(field))(trans.winner())),
            td(cls := "single")(
              st.select(id := form3.id(field), name := field.name)(
                option(cls := "blank", value        := "")
              )
            )
          )
        }

      def loser(hide: Boolean) =
        form("players")("loser") pipe { field =>
          tr(cls := List("loser user-row" -> true, "none" -> hide))(
            th(label(`for` := form3.id(field))(trans.search.loser())),
            td(cls := "single")(
              st.select(id := form3.id(field), name := field.name)(
                option(cls := "blank", value        := "")
              )
            )
          )
        }

      def rating =
        tr(
          th(
            label(
              trans.rating(),
              " ",
              span(cls := "help", title := "The average rating of both players")("(?)")
            )
          ),
          td(
            div(cls := "half")(from(), " ", form3.select(form("ratingMin"), Query.averageRatings, "".some)),
            div(cls := "half")(to(), " ", form3.select(form("ratingMax"), Query.averageRatings, "".some))
          )
        )

      def hasAi =
        tr(
          th(
            label(`for` := form3.id(form("hasAi")))(
              trans.opponent(),
              " ",
              span(cls := "help", title := humanOrComputer.txt())("(?)")
            )
          ),
          td(cls := "single opponent")(form3.select(form("hasAi"), Query.hasAis, "".some))
        )

      def aiLevel =
        tr(cls := "aiLevel none")(
          th(label(trans.search.aiLevel())),
          td(
            div(cls := "half")(from(), " ", form3.select(form("aiLevelMin"), Query.aiLevels, "".some)),
            div(cls := "half")(to(), " ", form3.select(form("aiLevelMax"), Query.aiLevels, "".some))
          )
        )

      def source =
        tr(
          th(label(`for` := form3.id(form("source")))(trans.search.source())),
          td(cls := "single")(form3.select(form("source"), Query.sources, "".some))
        )

      def perf =
        tr(
          th(label(`for` := form3.id(form("perf")))(trans.variant())),
          td(cls := "single")(
            form3.select(
              form("perf"),
              lila.rating.PerfType.nonPuzzle map { v =>
                v.id -> v.trans
              },
              "".some
            )
          )
        )

      def mode =
        tr(
          th(label(`for` := form3.id(form("mode")))(trans.mode())),
          td(cls := "single")(form3.select(form("mode"), Query.modes, "".some))
        )

      def turns =
        tr(
          th(label(nbTurns())),
          td(
            div(cls := "half")(from(), " ", form3.select(form("pliesMin"), Query.plies, "".some)),
            div(cls := "half")(to(), " ", form3.select(form("pliesMax"), Query.plies, "".some))
          )
        )

      def duration =
        tr(
          tr(
            th(label(trans.duration())),
            td(
              div(cls := "half")(from(), " ", form3.select(form("durationMin"), Query.durations, "".some)),
              div(cls := "half")(to(), " ", form3.select(form("durationMax"), Query.durations, "".some))
            )
          )
        )

      def clockTime =
        tr(
          th(label(trans.clockInitialTime())),
          td(
            div(cls := "half")(
              from(),
              " ",
              form3.select(form("clock")("initMin"), Query.clockInits, "".some)
            ),
            div(cls := "half")(to(), " ", form3.select(form("clock")("initMax"), Query.clockInits, "".some))
          )
        )

      def clockIncrement =
        tr(
          th(label(trans.clockIncrement())),
          td(
            div(cls := "half")(from(), " ", form3.select(form("clock")("incMin"), Query.clockIncs, "".some)),
            div(cls := "half")(to(), " ", form3.select(form("clock")("incMax"), Query.clockIncs, "".some))
          )
        )

      def clockByoyomi =
        tr(
          th(label(trans.clockByoyomi())),
          td(
            div(cls := "half")(from(), " ", form3.select(form("clock")("byoMin"), Query.clockByos, "".some)),
            div(cls := "half")(to(), " ", form3.select(form("clock")("byoMax"), Query.clockByos, "".some))
          )
        )

      def status =
        tr(
          th(label(`for` := form3.id(form("status")))(result())),
          td(cls := "single")(form3.select(form("status"), Query.statuses, "".some))
        )

      def winnerColor =
        tr(
          th(label(`for` := form3.id(form("winnerColor")))(trans.search.winnerColor())),
          td(cls := "single")(form3.select(form("winnerColor"), Query.winnerColors, "".some))
        )

      def date =
        tr(cls := "date")(
          th(label(trans.search.date())),
          td(
            div(cls := "half")(from(), " ", form3.input(form("dateMin"), "date")(dateMinMax: _*)),
            div(cls := "half")(to(), " ", form3.input(form("dateMax"), "date")(dateMinMax: _*))
          )
        )

      def sort =
        tr(
          th(label(trans.search.sortBy())),
          td(
            div(cls := "half")(form3.select(form("sort")("field"), Sorting.fields)),
            div(cls := "half")(form3.select(form("sort")("order"), Sorting.orders))
          )
        )

      def analysed = {
        val field = form("analysed")
        tr(
          th(
            label(`for` := form3.id(field))(
              trans.search.analysis(),
              " ",
              span(cls := "help", title := onlyAnalysed.txt())("(?)")
            )
          ),
          td(cls := "single")(
            form3.cmnToggle(form3.id(field), field.name, checked = field.value.has("1"), value = "1")
          )
        )
      }
    }
}
