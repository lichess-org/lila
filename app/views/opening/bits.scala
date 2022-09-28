package views.html.opening

import controllers.routes
import play.api.libs.json.{ JsArray, Json }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.opening.{ OpeningConfig, OpeningPage, OpeningQuery }

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
            span(cls := "opening__next__san")(next.san),
            next.shortName map { sn =>
              span(cls := "opening__next__name")(sn)
            }
          ),
          span(cls := "opening__next__board")(
            views.html.board.bits.mini(next.fen, lastMove = next.uci.uci)(span)
          ),
          span(cls := "opening__next__result")(
            resultSegment("white", next.result.whitePercent),
            resultSegment("draws", next.result.drawsPercent),
            resultSegment("black", next.result.blackPercent)
          )
        )
      }
    )

  def winRate(page: OpeningPage)(implicit ctx: Context) =
    div(cls := "opening__win-rate")(
      h2(
        "Lichess win rate",
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
      postForm(cls := "opening__config__form", action := routes.Opening.config(page.query.key))(
        checkboxes(form("speeds"), speedChoices, page.query.config.speeds.map(_.id)),
        checkboxes(form("ratings"), ratingChoices, page.query.config.ratings),
        div(cls                           := "opening__config__form__submit")(
          form3.submit(trans.apply())(cls := "button-empty")
        )
      )
    )
  }

  def moreJs(implicit ctx: Context) = frag(
    jsModule("opening"),
    embedJsUnsafeLoadThen {
      import lila.opening.OpeningHistory.segmentJsonWrite
      s"""LichessOpening.page(${safeJsonValue(
          Json.obj("history" -> JsArray())
        )})"""
    }
  )

  def queryUrl(q: OpeningQuery) = routes.Opening.query(q.key)

  val lpvPreload = div(cls := "lpv__board")(div(cls := "cg-wrap")(cgWrapContent))

  def percentNumber(v: Double) = f"${v}%1.2f"
  def percentFrag(v: Double)   = frag(strong(percentNumber(v)), "%")

  def resultSegment(key: String, percent: Double) =
    span(cls := key, style := s"width:${percentNumber(percent)}%")(
      percent > 20 option s"${Math.round(percent)}%"
    )
}
