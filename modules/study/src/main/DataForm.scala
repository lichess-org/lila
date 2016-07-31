package lila.study

import play.api.data._
import play.api.data.Forms._

object DataForm {

  lazy val form = Form(mapping(
    "gameId" -> optional(nonEmptyText),
    "orientation" -> optional(nonEmptyText),
    "fen" -> optional(nonEmptyText),
    "variant" -> optional(nonEmptyText)
  )(Data.apply)(Data.unapply))

  case class Data(
      gameId: Option[String] = None,
      orientationStr: Option[String] = None,
      fenStr: Option[String] = None,
      variantStr: Option[String] = None) {

    def orientation = orientationStr.flatMap(chess.Color.apply) | chess.White
  }
}
