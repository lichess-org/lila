package lila.coordinate

import play.api.data.*
import play.api.data.Forms.*

object CoordinateForm:

  val color = Form(
    single(
      "color" -> number(min = 1, max = 3)
    )
  )

  val score = Form(
    mapping(
      "mode" -> lila.common.Form
        .trim(text)
        .verifying(m => CoordMode.find(m).isDefined)
        .transform[CoordMode](m => CoordMode.find(m).get, _.toString),
      "color" -> lila.common.Form.color.mapping,
      "score" -> number(min = 0, max = 100)
    )(ScoreData.apply)(unapply)
  )

  case class ScoreData(mode: CoordMode, color: Color, score: Int)
