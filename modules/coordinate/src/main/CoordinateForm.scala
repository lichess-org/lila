package lila.coordinate

import play.api.data._
import play.api.data.Forms._

object CoordinateForm {

  val color = Form(
    single(
      "color" -> number(min = 1, max = 3)
    )
  )

  val score = Form(
    mapping(
      "mode"  -> text.verifying(Set("findSquare", "nameSquare") contains _),
      "color" -> text.verifying(Set("white", "black") contains _),
      "score" -> number(min = 0, max = 100)
    )(ScoreData.apply)(ScoreData.unapply)
  )

  case class ScoreData(mode: String, color: String, score: Int) {
    def isFindSquareMode = mode == "findSquare"
    def isWhite          = color == "white"
  }
}
