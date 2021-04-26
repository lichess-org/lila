package views.html.user

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object download {
  def apply(user: lila.user.User)(implicit ctx: Context): Frag = {
    val title = s"${user.username} • ${trans.exportGames.txt()}"
    views.html.base.layout(
      title = title,
      moreCss = cssTag("search"),
      moreJs = jsModule("download")
    ) {
      main(cls := "box page-small search")(
        h1(title),
        form(cls := "box__pad search__form")(
          input(tpe := "hidden", id := "dl-username", value := user.username),
          table(
            color,
            date,
            opponent,
            mode,
            analysis,
            ongoing,
            perfToggles,
            includeToggles,
            amount,
            btn,
            output
          )
        )
      )
    }
  }

  private def color(implicit ctx: Context): Frag = tr(
    th(label(`for` := "dl-color")(trans.search.color())),
    td(cls := "single")(
      select(id := "dl-color")(
        option(value := ""),
        option(value := "white")(trans.white()),
        option(value := "black")(trans.black())
      )
    )
  )

  private def date(implicit ctx: Context): Frag = tr(
    th(label(trans.search.date())),
    td(
      div(cls := "half")(
        trans.search.from(),
        " ",
        input(tpe := "date", id := "dl-dateMin"),
        input(tpe := "time", id := "dl-timeMin", step := "1")
      ),
      div(cls := "half")(
        trans.search.to(),
        " ",
        input(tpe := "date", id := "dl-dateMax"),
        input(tpe := "time", id := "dl-timeMax", step := "1")
      )
    )
  )

  private def opponent(implicit ctx: Context): Frag = tr(
    th(label(`for` := "dl-opponent")(trans.search.opponentName())),
    td(input(tpe := "text", id := "dl-opponent"))
  )

  private def mode(implicit ctx: Context): Frag = tr(
    th(label(`for` := "dl-rated")(trans.mode())),
    td(cls := "single")(
      select(id := "dl-rated")(
        option(value := ""),
        option(value := "false")(trans.casual()),
        option(value := "true")(trans.rated())
      )
    )
  )

  private def analysis(implicit ctx: Context): Frag = tr(
    th(label(`for` := "dl-analysis")(trans.search.analysis())),
    td(cls := "single")(
      select(id := "dl-analysis")(
        option(value := ""),
        option(value := "true")(trans.yes()),
        option(value := "false")(trans.no())
      )
    )
  )

  private def ongoing(implicit ctx: Context): Frag = tr(
    th(label(`for` := "dl-ongoing")(trans.currentGames())),
    td(cmnToggle("dl-ongoing"))
  )

  private def perfToggles(implicit ctx: Context): Frag = {
    val perfTypes = lila.rating.PerfType.nonPuzzle
    tr(
      th(cls := "top")(label(`for` := "dl-perf-tbl")(trans.variants())),
      td(
        table(id := "dl-perf-tbl")(
          for (i <- perfTypes.indices by 2)
            yield tr(perfToggle(perfTypes(i)), (i + 1 < perfTypes.length) option perfToggle(perfTypes(i + 1)))
        )
      )
    )
  }

  private def includeToggles(implicit ctx: Context): Frag = tr(
    th(cls := "top")(
      label(`for` := "dl-include-tbl")(trans.search.include())
    ),
    td(
      table(id := "dl-include-tbl")(
        tr(
          th(cls := "quarter")(label(`for` := "dl-tags")(trans.study.pgnTags())),
          td(cls := "quarter")(cmnToggle("dl-tags", defaultOn = true)),
          th(cls := "quarter")(label(`for` := "dl-clocks")(trans.moveTimes())),
          td(cls := "quarter")(cmnToggle("dl-clocks"))
        ),
        tr(
          th(cls := "quarter")(label(`for` := "dl-evals")(trans.search.evaluation())),
          td(cls := "quarter")(cmnToggle("dl-evals")),
          th(cls := "quarter")(label(`for` := "dl-opening")(trans.opening())),
          td(cls := "quarter")(cmnToggle("dl-opening"))
        )
      )
    )
  )

  private def amount(implicit ctx: Context): Frag = tr(
    th(
      label(`for` := "dl-amount")(trans.search.maxNumber()),
      " ",
      span(cls := "help", title := trans.search.maxNumberExplanation.txt())("(?)")
    ),
    td(input(tpe := "number", id := "dl-amount", min := "1", step := "1"))
  )

  private def btn(implicit ctx: Context): Frag = tr(
    th,
    td(cls := "action")(
      button(cls := "button", tpe := "button", id := "dl-button")(
        trans.search.generateURL()
      )
    )
  )

  private def output(implicit ctx: Context): Frag = tr(
    th,
    td(
      input(
        id := "dl-output",
        cls := "copyable autoselect output",
        readonly,
        spellcheck := "false"
      )
    )
  )

  private def cmnToggle(id: String, v: String = "on", defaultOn: Boolean = false): Frag = frag(
    input(attr("id") := id, tpe := "checkbox", cls := "cmn-toggle", value := v, defaultOn option checked),
    label(`for` := id)
  )

  private def perfToggle(perfType: lila.rating.PerfType)(implicit ctx: Context): Frag = frag(
    th(cls := "quarter")(label(`for` := s"dl-perf-${perfType.key}")(perfType.trans)),
    td(cls := "quarter")(cmnToggle(id = s"dl-perf-${perfType.key}", v = perfType.key, defaultOn = true))
  )
}
