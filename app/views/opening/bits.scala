package views.html.opening

import controllers.routes
import play.api.libs.json.{ JsArray, Json }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.opening.{ OpeningConfig, OpeningPage, OpeningQuery, ResultCounts }

private object bits {

  def whatsNext(page: OpeningPage)(implicit ctx: Context) =
    div(cls := "opening__nexts")(
      page.explored.next.map { next =>
        a(cls := "opening__next", href := queryUrl(next.query))(
          span(cls := "opening__next__popularity")(
            span(style := s"width:${percentNumber(next.percent)}%")(
              s"${Math.round(next.percent)}%"
            )
          ),
          span(cls := "opening__next__title")(
            span(cls := "opening__next__name")(next.shortName.fold(nbsp)(frag(_))),
            span(cls := "opening__next__san")(next.san)
          ),
          span(cls := "opening__next__result-board")(
            span(cls := "opening__next__result") {
              import next.result._
              val (blackV, drawsV, whiteV) = exaggerateResults(next.result)
              frag(
                resultSegment("black", blackPercent, blackV),
                resultSegment("draws", drawsPercent, drawsV),
                resultSegment("white", whitePercent, whiteV)
              )
            },
            span(cls := "opening__next__board")(
              views.html.board.bits.mini(next.fen, lastMove = next.uci.uci)(span)
            )
          )
        )
      }
    )

  def winRate(page: OpeningPage)(implicit ctx: Context) =
    div(cls := "opening__win-rate")(
      h2(
        strong(page.explored.result.sum.localize),
        " games",
        span(cls := "title-stats")(
          em("White: ", percentFrag(page.explored.result.whitePercent)),
          em("Black: ", percentFrag(page.explored.result.blackPercent)),
          em("Draws: ", percentFrag(page.explored.result.drawsPercent))
        )
      )
    )

  def config(page: OpeningPage)(implicit ctx: Context) = {
    import OpeningConfig._
    details(cls := "opening__config")( // , attr("open") := true)(
      summary(cls := "opening__config__summary")(page.query.config.toString),
      postForm(
        cls    := "opening__config__form",
        action := routes.Opening.config(page.query.key.some.filter(_.nonEmpty) | "index")
      )(
        checkboxes(form("speeds"), speedChoices, page.query.config.speeds.map(_.id)),
        checkboxes(form("ratings"), ratingChoices, page.query.config.ratings),
        div(cls                           := "opening__config__form__submit")(
          form3.submit(trans.apply())(cls := "button-empty")
        )
      )
    )
  }

  def moreJs(page: OpeningPage)(implicit ctx: Context) = frag(
    jsModule("opening"),
    embedJsUnsafeLoadThen {
      s"""LichessOpening.page(${safeJsonValue(
          Json.obj("history" -> page.explored.history)
        )})"""
    }
  )

  def queryUrl(q: OpeningQuery) = routes.Opening.query(q.key)

  val lpvPreload = div(cls := "lpv__board")(div(cls := "cg-wrap")(cgWrapContent))

  def percentNumber(v: Double) = f"${v}%1.2f"
  def percentFrag(v: Double)   = frag(strong(percentNumber(v)), "%")

  def resultSegment(key: String, percent: Double, visualPercent: Double) = {
    val visible = visualPercent > 7
    val text    = s"${Math.round(percent)}%"
    span(cls := key, style := s"height:${percentNumber(visualPercent)}%", title := (!visible).option(text))(
      visible option text
    )
  }

  private def exaggerateResults(result: ResultCounts) = {
    import result._
    val (lower, upper)   = (30d, 70d)
    val factor           = 100d / (upper - lower)
    val drawSquishing    = 50d / 100d
    val drawHalfSquished = drawsPercent * factor * drawSquishing * 50d / 100d
    val drawTransformed  = drawsPercent * factor - 2 * drawHalfSquished
    val blackTransformed = (blackPercent - lower) * factor + drawHalfSquished
    val whiteTransformed = (whitePercent - lower) * factor + drawHalfSquished
    (blackTransformed, drawTransformed, whiteTransformed)
  }

}
