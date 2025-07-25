package lila.user
package ui

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class UserGamesDownload(helpers: Helpers):
  import helpers.{ *, given }

  def apply(user: User)(using ctx: Context) =
    Page(s"${user.username} • ${trans.site.exportGames.txt()}")
      .css("bits.search")
      .js(Esm("user.gamesDownload")):
        main(cls := "box page-small search")(
          boxTop(h1(userLink(user), s" • ${trans.site.exportGames.txt()}")),
          form(
            id := "dl-form",
            cls := "box__pad search__form"
          )(
            table(
              color,
              date,
              opponent,
              mode,
              analysis,
              perfToggles,
              includeToggles,
              amount,
              tr(cls := "output")(
                th(label(`for` := "dl-api-url")("API URL")),
                td(
                  copyMeInput("")(
                    id := "dl-api-url",
                    attr("data-api-path") := routes.Game.apiExportByUser(user.username)
                  )
                )
              ),
              tr(
                td(cls := "action", colspan := "2")(
                  a(
                    id := "dl-button",
                    cls := "button",
                    href := routes.Game.exportByUser(user.username),
                    downloadAttr
                  )(trans.site.download())
                )
              )
            ),
            br,
            br,
            ctx
              .is(user)
              .option(
                p(style := "text-align: right")(
                  a(href := routes.Game.apiExportByUserImportedGames())(
                    "Or download imported games as PGN"
                  )
                )
              )
          )
        )

  private def color(using Context): Frag = tr(
    th(label(`for` := "dl-color")(trans.search.color())),
    td(cls := "single"):
      select(id := "dl-color", name := "color")(
        st.option(value := ""),
        st.option(value := "white")(trans.site.white()),
        st.option(value := "black")(trans.site.black())
      )
  )

  private def date(using Context): Frag = tr(
    th(label(trans.search.date())),
    td(cls := "two-columns")(
      div(
        trans.search.from(),
        " ",
        input(tpe := "date", id := "dl-date-since"),
        input(tpe := "time", id := "dl-time-since", step := "1")
      ),
      div(
        trans.search.to(),
        " ",
        input(tpe := "date", id := "dl-date-until"),
        input(tpe := "time", id := "dl-time-until", step := "1")
      )
    )
  )

  private def opponent(using Context): Frag = tr(
    th(label(`for` := "dl-opponent")(trans.search.opponentName())),
    td(input(tpe := "text", id := "dl-opponent", name := "vs"))
  )

  private def mode(using Context): Frag = tr(
    th(label(`for` := "dl-rated")(trans.site.mode())),
    td(cls := "single")(
      select(id := "dl-rated", name := "rated")(
        st.option(value := ""),
        st.option(value := "false")(trans.site.casual()),
        st.option(value := "true")(trans.site.rated())
      )
    )
  )

  private def analysis(using Context): Frag = tr(
    th(
      label(`for` := "dl-analysis")(
        trans.search.analysis(),
        " ",
        span(cls := "help", title := trans.search.onlyAnalysed.txt())("(?)")
      )
    ),
    td(cls := "single")(
      select(id := "dl-analysis", name := "analysed")(
        st.option(value := ""),
        st.option(value := "true")(trans.site.yes()),
        st.option(value := "false")(trans.site.no())
      )
    )
  )

  private def perfToggles(using Context): Frag =
    tr(
      th(cls := "top")(label(`for` := "dl-perfs")(trans.site.variants())),
      td(
        div(id := "dl-perfs", cls := "toggle-columns")(
          lila.rating.PerfType.nonPuzzle.map(_.key).map(perfToggle)
        )
      )
    )

  private def perfToggle(pk: PerfKey)(using Context): Frag = div(
    form3.cmnToggle(
      s"dl-perf-${pk}",
      "",
      true,
      value = pk
    ),
    label(`for` := s"dl-perf-${pk}")(pk.perfTrans)
  )

  private def includeToggles(using Context): Frag = tr(
    th(cls := "top")(
      label(`for` := "dl-includes")(trans.search.include())
    ),
    td(
      div(id := "dl-includes", cls := "toggle-columns")(
        div(form3.cmnToggle("dl-tags", "tags", true), label(`for` := "dl-tags")(trans.study.pgnTags())),
        div(
          form3.cmnToggle("dl-clocks", "clocks", false),
          label(`for` := "dl-clocks")(trans.site.moveTimes())
        ),
        div(
          form3.cmnToggle("dl-evals", "evals", false),
          label(`for` := "dl-evals")(trans.search.evaluation())
        ),
        div(
          form3.cmnToggle("dl-opening", "opening", false),
          label(`for` := "dl-opening")(trans.site.opening())
        ),
        div(
          form3.cmnToggle("dl-literate", "literate", false),
          label(`for` := "dl-literate")("Textual annotations")
        )
      )
    )
  )

  private def amount(using Context): Frag = tr(
    th(
      label(`for` := "dl-amount")(
        trans.search.maxNumber(),
        " ",
        span(cls := "help", title := trans.search.maxNumberExplanation.txt())("(?)")
      )
    ),
    td(input(tpe := "number", id := "dl-amount", name := "max", min := "1", step := "1"))
  )
