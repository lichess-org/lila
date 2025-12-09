package lila.gameSearch
package ui

import play.api.data.Form
import scalalib.paginator.Paginator

import java.time.format.DateTimeFormatter

import lila.core.i18n.Translate
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class GameSearchUi(helpers: Helpers)(
    gameWidgets: Seq[Game] => Context ?=> Frag
):
  import helpers.{ *, given }
  import trans.search as trs

  def index(form: Form[?], paginator: Option[Paginator[Game]] = None, nbGames: Long)(using
      ctx: Context
  ) =
    val f = SearchForm(helpers)(form)
    Page(trs.searchInXGames.txt(nbGames.localize, nbGames))
      .js(Esm("bits.gameSearch"))
      .js(infiniteScrollEsmInit)
      .css("bits.search"):
        main(cls := "box page-small search")(
          h1(cls := "box__top")(trs.advancedSearch()),
          st.form(
            noFollow,
            cls := "box__pad search__form",
            action := s"${routes.Search.index()}#results",
            method := "GET"
          )(f.dataReqs)(
            globalError(form),
            table(
              tr(
                th(label(trans.site.players())),
                td(cls := "usernames two-columns")(List("a", "b").map { p =>
                  div(form3.input(form("players")(p))(tpe := "text"))
                })
              ),
              f.colors(hide = true),
              f.winner(hide = true),
              f.loser(hide = true),
              f.rating,
              f.hasAi,
              f.aiLevel,
              source,
              f.perf,
              f.mode,
              f.turns,
              f.duration,
              f.clock,
              f.status,
              f.winnerColor,
              f.date,
              f.sort,
              f.analysed,
              tr(
                th,
                td(cls := "action")(
                  submitButton(cls := "button")(trans.search.search()),
                  div(cls := "wait")(
                    spinner,
                    trs.searchInXGames(nbGames.localize)
                  )
                )
              )
            )
          ),
          div(cls := "search__result", id := "results")(
            paginator.map { pager =>
              val permalink =
                a(cls := "permalink", href := routes.Search.index(), noFollow)("Permalink")
              if pager.nbResults > 0 then
                frag(
                  div(cls := "search__status box__pad")(
                    strong(trs.xGamesFound(pager.nbResults.localize, pager.nbResults)),
                    " • ",
                    permalink
                  ),
                  div(cls := "search__rows infinite-scroll")(
                    gameWidgets(pager.currentPageResults),
                    pagerNext(pager, np => routes.Search.index(np).url)
                  )
                )
              else
                div(cls := "search__status box__pad")(
                  strong(trs.xGamesFound(0)),
                  " • ",
                  permalink
                )
            }
          )
        )

  def login(nbGames: Long)(using Context) =
    Page(trans.search.searchInXGames.txt(nbGames.localize, nbGames))
      .css("bits.search"):
        main(cls := "box box-pad page-small search search-login")(
          h1(cls := "box__top")(trans.search.advancedSearch()),
          div(cls := "search__login")(
            p(a(href := routes.Auth.signup)(trans.site.youNeedAnAccountToDoThat()))
          )
        )

  def user(u: User, form: Form[?])(using Context) =
    val f = SearchForm(helpers)(form)
    st.form(
      noFollow,
      cls := "search__form",
      action := routes.User.games(u.username, "search"),
      method := "GET"
    )(f.dataReqs)(
      table(
        f.date,
        f.rating,
        f.turns,
        f.duration,
        f.clock,
        f.source,
        f.perf,
        f.mode
      ),
      table(
        f.hasAi,
        f.aiLevel,
        tr(cls := "opponentName")(
          th(label(`for` := form3.id(form("players")("b")))(trs.opponentName())),
          td(cls := "usernames")(
            form3.hidden("players.a", u.id),
            form3.input(form("players")("b"))(tpe := "text")
          )
        ),
        f.winner(hide = false),
        f.loser(hide = false),
        f.colors(hide = false),
        f.status,
        f.winnerColor,
        f.sort,
        f.analysed,
        tr(cls := "action")(
          th,
          td(button(cls := "button")(trs.search()))
        )
      )
    )

final class SearchForm(helpers: Helpers)(form: Form[?])(using Translate):
  import helpers.*
  import trans.search as trs

  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  private val dateMin = "2011-01-01"
  private def dateMinMax: List[Modifier] =
    List(min := dateMin, max := dateFormatter.print(nowInstant.plusDays(1)))

  def dataReqs =
    List("winner", "loser", "white", "black").map: f =>
      data(s"req-$f") := form("players")(f).value.orZero

  def colors(hide: Boolean) =
    Color.all.map: color =>
      tr(cls := List(s"${color.name}User user-row" -> true, "none" -> hide))(
        th(
          label(`for` := form3.id(form("players")(color.name)))(
            color.fold(trans.site.white(), trans.site.black())
          )
        ),
        td(
          st.select(
            id := form3.id(form("players")(color.name)),
            name := form("players")(color.name).name
          )(
            st.option(cls := "blank", value := "")
          )
        )
      )

  def winner(hide: Boolean) =
    val field = form("players")("winner")
    tr(cls := List("winner user-row" -> true, "none" -> hide))(
      th(label(`for` := form3.id(field))(trans.site.winner())),
      td(
        st.select(id := form3.id(field), name := field.name)(
          st.option(cls := "blank", value := "")
        )
      )
    )

  def loser(hide: Boolean) =
    val field = form("players")("loser")
    tr(cls := List("loser user-row" -> true, "none" -> hide))(
      th(label(`for` := form3.id(field))(trans.search.loser())),
      td(
        st.select(id := form3.id(field), name := field.name)(
          st.option(cls := "blank", value := "")
        )
      )
    )

  def rating =
    tr(
      th(
        label(
          trans.site.rating(),
          " ",
          span(cls := "help", title := trs.ratingExplanation.txt())("(?)")
        )
      ),
      td(cls := "two-columns")(
        div(trs.from(), " ", form3.select(form("ratingMin"), FormHelpers.averageRatings, "".some)),
        div(trs.to(), " ", form3.select(form("ratingMax"), FormHelpers.averageRatings, "".some))
      )
    )

  def hasAi =
    tr(
      th(
        label(`for` := form3.id(form("hasAi")))(
          trans.site.opponent(),
          " ",
          span(cls := "help", title := trs.humanOrComputer.txt())("(?)")
        )
      ),
      td(cls := "opponent")(form3.select(form("hasAi"), FormHelpers.hasAis, "".some))
    )

  def aiLevel =
    tr(cls := "aiLevel none")(
      th(label(trans.search.aiLevel())),
      td(cls := "two-columns")(
        div(trs.from(), " ", form3.select(form("aiLevelMin"), FormHelpers.aiLevels, "".some)),
        div(trs.to(), " ", form3.select(form("aiLevelMax"), FormHelpers.aiLevels, "".some))
      )
    )

  def source =
    tr(
      th(label(`for` := form3.id(form("source")))(trans.search.source())),
      td(form3.select(form("source"), FormHelpers.sources, "".some))
    )

  def perf =
    tr(
      th(label(`for` := form3.id(form("perf")))(trans.site.variant())),
      td(
        form3.select(
          form("perf"),
          perfKeys.map: v =>
            v.id -> v.perfTrans,
          "".some
        )
      )
    )

  def mode =
    tr(
      th(label(`for` := form3.id(form("mode")))(trans.site.mode())),
      td(form3.select(form("mode"), FormHelpers.modes, "".some))
    )

  def turns =
    tr(
      th(label(trs.nbTurns())),
      td(cls := "two-columns")(
        div(trs.from(), " ", form3.select(form("turnsMin"), FormHelpers.turns, "".some)),
        div(trs.to(), " ", form3.select(form("turnsMax"), FormHelpers.turns, "".some))
      )
    )

  def duration =
    tr(
      tr(
        th(label(trans.site.duration())),
        td(cls := "two-columns")(
          div(trs.from(), " ", form3.select(form("durationMin"), FormHelpers.durations, "".some)),
          div(trs.to(), " ", form3.select(form("durationMax"), FormHelpers.durations, "".some))
        )
      )
    )

  def clock =
    tr(
      th(label(trans.site.clock())),
      td(cls := "two-columns")(
        div(
          trans.site.clockInitialTime(),
          " ",
          form3.select(form("clockInit"), FormHelpers.clockInits, "".some)
        ),
        div(trans.site.clockIncrement(), " ", form3.select(form("clockInc"), FormHelpers.clockIncs, "".some))
      )
    )

  def status =
    tr(
      th(label(`for` := form3.id(form("status")))(trs.result())),
      td(form3.select(form("status"), FormHelpers.statuses, "".some))
    )

  def winnerColor =
    tr(
      th(label(`for` := form3.id(form("winnerColor")))(trans.search.winnerColor())),
      td(form3.select(form("winnerColor"), FormHelpers.winnerColors, "".some))
    )

  def date =
    tr(cls := "date")(
      th(label(trans.search.date())),
      td(cls := "two-columns")(
        div(trs.from(), " ", form3.input(form("dateMin"), "date")(dateMinMax*)),
        div(trs.to(), " ", form3.input(form("dateMax"), "date")(dateMinMax*))
      )
    )

  def sort =
    tr(
      th(label(trans.search.sortBy())),
      td(cls := "two-columns")(
        div(form3.select(form("sort")("field"), Sorting.fields)),
        div(form3.select(form("sort")("order"), Sorting.orders))
      )
    )

  def analysed =
    val field = form("analysed")
    tr(
      th(
        label(`for` := form3.id(field))(
          trans.search.analysis(),
          " ",
          span(cls := "help", title := trs.onlyAnalysed.txt())("(?)")
        )
      ),
      td(
        form3.cmnToggle(form3.id(field), field.name, checked = field.value.has("1"), value = "1")
      )
    )
