package views.html.user

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes
import lila.user.User

object download {
  def apply(user: lila.user.User)(implicit ctx: Context): Frag = {
    views.html.base.layout(
      title = s"${user.username} • ${trans.exportGames.txt()}",
      moreCss = cssTag("search"),
      moreJs = jsModule("userGamesDownload")
    ) {
      main(cls := "box page-small search")(
        h1(userLink(user), s" • ${trans.exportGames.txt()}"),
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
                input(
                  id := "dl-api-url",
                  cls := "copyable autoselect",
                  tpe := "text",
                  readonly,
                  spellcheck := "false",
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
                )(trans.download())
              )
            )
          )
        )
      )
    }
  }

  private def color(implicit ctx: Context): Frag = tr(
    th(label(`for` := "dl-color")(trans.search.color())),
    td(cls := "single")(
      select(id := "dl-color", name := "color")(
        option(value := ""),
        option(value := "white")(trans.white()),
        option(value := "black")(trans.black())
      )
    )
  )

  private def date(implicit ctx: Context): Frag = tr(
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

  private def opponent(implicit ctx: Context): Frag = tr(
    th(label(`for` := "dl-opponent")(trans.search.opponentName())),
    td(input(tpe := "text", id := "dl-opponent", name := "vs"))
  )

  private def mode(implicit ctx: Context): Frag = tr(
    th(label(`for` := "dl-rated")(trans.mode())),
    td(cls := "single")(
      select(id := "dl-rated", name := "rated")(
        option(value := ""),
        option(value := "false")(trans.casual()),
        option(value := "true")(trans.rated())
      )
    )
  )

  private def analysis(implicit ctx: Context): Frag = tr(
    th(
      label(`for` := "dl-analysis")(
        trans.search.analysis(),
        " ",
        span(cls := "help", title := trans.search.onlyAnalysed.txt())("(?)")
      )
    ),
    td(cls := "single")(
      select(id := "dl-analysis", name := "analysed")(
        option(value := ""),
        option(value := "true")(trans.yes()),
        option(value := "false")(trans.no())
      )
    )
  )

  private def perfToggles(implicit ctx: Context): Frag = {
    val perfTypes = lila.rating.PerfType.nonPuzzle
    tr(
      th(cls := "top")(label(`for` := "dl-perfs")(trans.variants())),
      td(
        div(id := "dl-perfs", cls := "toggle-columns")(
          perfTypes map perfToggle
        )
      )
    )
  }

  private def perfToggle(perfType: lila.rating.PerfType)(implicit ctx: Context): Frag = div(
    form3.cmnToggle(
      s"dl-perf-${perfType.key}",
      "",
      true,
      value = perfType.key
    ),
    label(`for` := s"dl-perf-${perfType.key}")(perfType.trans)
  )

  private def includeToggles(implicit ctx: Context): Frag = tr(
    th(cls := "top")(
      label(`for` := "dl-includes")(trans.search.include())
    ),
    td(
      div(id := "dl-includes", cls := "toggle-columns")(
        div(form3.cmnToggle("dl-tags", "tags", true), label(`for` := "dl-tags")(trans.study.pgnTags())),
        div(form3.cmnToggle("dl-clocks", "clocks", false), label(`for` := "dl-clocks")(trans.moveTimes())),
        div(
          form3.cmnToggle("dl-evals", "evals", false),
          label(`for` := "dl-evals")(trans.search.evaluation())
        ),
        div(form3.cmnToggle("dl-opening", "opening", false), label(`for` := "dl-opening")(trans.opening()))
      )
    )
  )

  private def amount(implicit ctx: Context): Frag = tr(
    th(
      label(`for` := "dl-amount")(
        trans.search.maxNumber(),
        " ",
        span(cls := "help", title := trans.search.maxNumberExplanation.txt())("(?)")
      )
    ),
    td(input(tpe := "number", id := "dl-amount", name := "max", min := "1", step := "1"))
  )
}
