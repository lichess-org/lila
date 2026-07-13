package lila.user
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.i18n.I18nKey

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
              user.enabled.yes.option:
                tr(cls := "output")(
                  th(label(`for` := "dl-api-url")("API URL")),
                  td(
                    copyMeInput("")(
                      id := "dl-api-url",
                      attr("data-api-path") := routes.Game.apiExportByUser(user.username)
                    )
                  )
                )
              ,
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

  private def selectTr(name: String, title: Frag, options: List[(String, I18nKey)])(using Context) =
    tr(
      th(label(`for` := s"dl-$name")(title)),
      td(cls := "single"):
        form3.selectLowLevel(
          name,
          options.map((value, t) => value -> t.txt()),
          default = "".some
        )(id := s"dl-$name")
    )

  private def color(using Context): Frag =
    selectTr("color", trans.search.color(), List("white" -> trans.site.white, "black" -> trans.site.black))

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

  private def mode(using Context): Frag =
    selectTr("rated", trans.site.mode(), List("false" -> trans.site.casual, "true" -> trans.site.rated))

  private def analysis(using Context): Frag =
    val label = frag(
      trans.search.analysis(),
      " ",
      span(cls := "help", title := trans.search.onlyAnalysed.txt())("(?)")
    )
    selectTr("analysed", label, List("true" -> trans.site.yes, ("false" -> trans.site.no)))

  private def perfToggles(using Context): Frag =
    tr(
      th(cls := "top")(label(`for` := "dl-perfs")(trans.site.variants())),
      td(
        div(id := "dl-perfs", cls := "toggle-columns")(
          lila.rating.PerfType.nonPuzzle.map(_.key).map(perfToggle)
        )
      )
    )

  private def perfToggle(pk: PerfKey)(using Context): Frag = div(cls := "form-check__container")(
    form3.nativeCheckbox(
      s"dl-perf-${pk}",
      "",
      true,
      value = pk
    ),
    label(`for` := s"dl-perf-${pk}", cls := "form-label")(pk.perfTrans)
  )

  private def includeToggle(name: String, checked: Boolean, text: Frag): Frag =
    div(cls := "form-check__container")(
      form3.nativeCheckbox(s"dl-include-$name", name, checked),
      label(`for` := s"dl-include-$name", cls := "form-label")(text)
    )

  private def includeToggles(using Context): Frag = tr(
    th(cls := "top")(
      label(`for` := "dl-includes")(trans.search.include())
    ),
    td(
      div(id := "dl-includes", cls := "toggle-columns")(
        includeToggle("tags", true, trans.study.pgnTags()),
        includeToggle("clocks", false, trans.site.moveTimes()),
        includeToggle("evals", false, trans.search.evaluation()),
        includeToggle("opening", false, trans.site.opening()),
        includeToggle("literate", false, "Textual annotations")
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
