package views.html.opening

import chess.opening.{ OpeningKey, Opening }
import controllers.routes
import play.api.libs.json.Json
import play.api.mvc.Call

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.safeJsonValue
import lila.opening.OpeningQuery.Query
import lila.opening.{ NameSection, OpeningConfig, OpeningPage, OpeningQuery, ResultCounts }

object bits:

  def beta = span(cls := "opening__beta")("BETA")

  def whatsNext(page: OpeningPage): Option[Tag] =
    page.explored.map: explored =>
      div(cls := "opening__nexts")(
        explored.next.map: next =>
          val canFollow = page.query.uci.isEmpty || page.wiki.exists(_.hasMarkup)
          a(cls := "opening__next", href := queryUrl(next.query), (!canFollow).option(noFollow))(
            span(cls := "opening__next__popularity"):
              span(style := s"width:${percentNumber(next.percent)}%", title := "Popularity"):
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
                views.html.board.bits.mini(next.fen.board, lastMove = next.uci.some)(span)
            )
          )
      )

  def configForm(config: OpeningConfig, thenTo: String)(using PageContext) =
    import OpeningConfig.*
    details(cls := "opening__config")(
      summary(cls := "opening__config__summary")(
        div(cls := "opening__config__summary__short")(
          iconTag(licon.Gear)
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
          form3.submit(trans.apply())(cls := "button-empty")
        )
      )
    )

  def moreJs(page: Option[OpeningPage])(using PageContext) =
    page match
      case Some(p) =>
        import lila.common.Json.given
        jsModuleInit(
          "opening",
          Json.obj("history" -> p.explored.so[List[Float]](_.history), "sans" -> p.query.sans)
        )
      case None =>
        jsModule("opening")

  def splitName(op: Opening) =
    NameSection.sectionsOf(op.name) match
      case NonEmptyList(family, variations) =>
        frag(
          span(cls := "opening-name__family")(family),
          variations.nonEmpty option ": ",
          fragList(
            variations.map: variation =>
              span(cls := "opening-name__variation")(variation),
            ", "
          )
        )

  def queryUrl(q: OpeningQuery): Call = queryUrl(q.query)
  def queryUrl(q: Query): Call =
    routes.Opening.byKeyAndMoves(q.key, q.moves.so(_.value.replace(" ", "_")))
  def openingUrl(o: Opening)  = keyUrl(o.key)
  def keyUrl(key: OpeningKey) = routes.Opening.byKeyAndMoves(key, "")

  val lpvPreload = div(cls := "lpv__board")(div(cls := "cg-wrap")(cgWrapContent))

  def percentNumber(v: Double) = f"${v}%1.2f"
  def percentFrag(v: Double)   = frag(strong(percentNumber(v)), "%")

  def resultSegments(result: ResultCounts) = result.sum > 0 option {
    import result.*
    val (blackV, drawsV, whiteV) = exaggerateResults(result)
    frag(
      resultSegment("black", "Black wins", blackPercent, blackV),
      resultSegment("draws", "Draws", drawsPercent, drawsV),
      resultSegment("white", "White wins", whitePercent, whiteV)
    )
  }

  def resultSegment(key: String, help: String, percent: Double, visualPercent: Double) =
    val visible = visualPercent > 7
    val text    = s"${Math.round(percent)}%"
    span(
      cls   := key,
      style := s"height:${percentNumber(visualPercent)}%",
      title := s"$text $help"
    )(visible option text)

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
