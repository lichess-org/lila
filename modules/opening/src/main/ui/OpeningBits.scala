package lila.opening
package ui

import chess.opening.{ Opening, OpeningKey }
import play.api.libs.json.*

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class OpeningBits(helpers: Helpers):
  import helpers.{ *, given }

  def pageModule(page: Option[OpeningPage])(using Context) =
    PageModule(
      "opening",
      page.fold(JsNull): p =>
        import lila.common.Json.given
        Json.obj("history" -> p.exploredOption.so[List[Float]](_.history), "sans" -> p.query.sans)
    )

  def whatsNext(page: OpeningPage): Option[Tag] =
    page.exploredOption.map: explored =>
      div(cls := "opening__nexts")(
        explored.next.map: next =>
          val canFollow = page.query.uci.isEmpty || page.wiki.exists(_.hasMarkup)
          a(cls := "opening__next", href := queryUrl(next.query), (!canFollow).option(noFollow))(
            span(cls := "opening__next__popularity"):
              span(
                style := s"width:${percentNumber(Math.max(next.percent, 10))}%",
                title := "Popularity"
              ):
                s"${Math.round(next.percent)}%"
            ,
            span(cls := "opening__next__title")(
              span(cls := "opening__next__name")(next.shortName.fold(nbsp)(frag(_))),
              span(cls := "opening__next__san")(next.san)
            ),
            span(cls := "opening__next__result-board")(
              span(cls := "opening__next__result result-bar"):
                resultSegments(next.result)
              ,
              span(cls := "opening__next__board"):
                chessgroundMini(next.fen.board, lastMove = next.uci.some)(span)
            )
          )
      )

  def configForm(config: OpeningConfig, thenTo: String)(using Context) =
    import OpeningConfig.*
    details(cls := "opening__config")(
      summary(cls := "opening__config__summary")(
        div(cls := "opening__config__summary__short")(
          iconTag(Icon.Gear)
        ),
        div(cls := "opening__config__summary__large")(
          "Speed: ",
          span(cls := "opening__config__summary__speed")(config.showSpeeds),
          nbsp,
          nbsp,
          "Rating: ",
          span(cls := "opening__config__summary__rating")(config.showRatings)
        )
      ),
      postForm(
        cls    := "opening__config__form",
        action := routes.Opening.config(thenTo.some.filter(_.nonEmpty) | "index")
      )(
        checkboxes(form("speeds"), speedChoices, config.speeds.map(_.id)),
        checkboxes(form("ratings"), ratingChoices, config.ratings),
        div(cls := "opening__config__form__submit")(
          form3.submit(trans.site.apply())(cls := "button-empty")
        )
      )
    )

  def splitName(op: Opening) =
    NameSection.sectionsOf(op.name) match
      case NonEmptyList(family, variations) =>
        frag(
          span(cls := "opening-name__family")(family),
          variations.nonEmpty.option(": "),
          fragList(
            variations.map: variation =>
              span(cls := "opening-name__variation")(variation),
            ", "
          )
        )

  def queryUrl(q: OpeningQuery): Call = queryUrl(q.query)
  def queryUrl(q: OpeningQuery.Query): Call =
    routes.Opening.byKeyAndMoves(q.key, q.moves.so(_.value.replace(" ", "_")))
  def openingUrl(o: Opening)         = openingKeyUrl(o.key)
  def openingKeyUrl(key: OpeningKey) = routes.Opening.byKeyAndMoves(key.value, "")

  val lpvPreload = div(cls := "lpv__board")(div(cls := "cg-wrap")(cgWrapContent))

  def percentNumber(v: Double) = f"${v}%1.2f"
  def percentFrag(v: Double)   = frag(strong(percentNumber(v)), "%")

  def resultSegments(result: ResultCounts) = (result.sum > 0).option:
    import result.*
    val (blackV, drawsV, whiteV) = exaggerateResults(result)
    frag(
      resultSegment("black", "Black wins", blackPercent, blackV),
      resultSegment("draws", "Draws", drawsPercent, drawsV),
      resultSegment("white", "White wins", whitePercent, whiteV)
    )

  def resultSegment(key: String, help: String, percent: Double, visualPercent: Double) =
    val visible = visualPercent > 7
    val text    = s"${Math.round(percent)}%"
    span(
      cls   := key,
      style := s"height:${percentNumber(visualPercent)}%",
      title := s"$text $help"
    )(visible.option(text))

  def showMissing(ops: List[Opening]) = div(cls := "opening__wiki__missing")(
    h2("Openings to explain"),
    p("Sorted by popularity"),
    ul(
      ops.map: op =>
        li(a(href := openingUrl(op))(op.name), " ", op.pgn)
    )
  )

  private def exaggerateResults(result: ResultCounts) =
    import result.*
    val (lower, upper)   = (30d, 70d)
    val factor           = 100d / (upper - lower)
    val drawSquishing    = 50d / 100d
    val drawHalfSquished = drawsPercent * factor * drawSquishing * 50d / 100d
    val drawTransformed  = drawsPercent * factor - 2 * drawHalfSquished
    val blackTransformed = (blackPercent - lower) * factor + drawHalfSquished
    val whiteTransformed = (whitePercent - lower) * factor + drawHalfSquished
    (blackTransformed, drawTransformed, whiteTransformed)
