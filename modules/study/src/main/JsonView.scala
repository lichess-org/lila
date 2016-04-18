package lila.study

import chess.format.{ Uci, UciCharPair, Forsyth }
import play.api.libs.json._

import lila.common.LightUser
import lila.common.PimpedJson._

final class JsonView(
    getLightUser: String => Option[LightUser]) {

  def study(s: Study) = Json.obj(
    "study" -> s,
    "analysis" -> Json.obj())

  private implicit val lightUserWrites = OWrites[LightUser] { u =>
    Json.obj(
      "id" -> u.id,
      "name" -> u.name,
      "title" -> u.title)
  }

  private implicit val uciWrites = Writes[Uci] { u =>
    JsString(u.uci)
  }
  private implicit val uciCharPairWrites = Writes[UciCharPair] { u =>
    JsString(u.toString)
  }
  private implicit val posWrites = Writes[chess.Pos] { p =>
    JsString(p.key)
  }
  private implicit val pathWrites = Writes[Path] { p =>
    JsString(p.value)
  }
  private implicit val colorWriter = Writes[chess.Color] { c =>
    JsString(c.name)
  }
  private implicit val moveWrites = Json.writes[Node.Move]
  private implicit val nodeWrites = Json.writes[Node]
  private implicit val rootWrites = Json.writes[Node.Root]

  import Chapter.Shape
  private implicit val shapeCircleWrites = Json.writes[Shape.Circle]
  private implicit val shapeArrowWrites = Json.writes[Shape.Arrow]
  private implicit val shapeWrites = Writes[Shape] {
    case s: Shape.Circle => Json toJson s
    case s: Shape.Arrow  => Json toJson s
  }
  private implicit val fenWrites = Writes[chess.format.FEN] { f =>
    JsString(f.value)
  }

  private implicit val variantWrites = Writes[chess.variant.Variant] { v => JsString(v.key) }
  private implicit val chapterSetupWrites = Json.writes[Chapter.Setup]
  private implicit val chapterWrites = Json.writes[Chapter]

  private implicit val studyWrites = OWrites[Study] { s =>
    Json.obj(
      "id" -> s.id,
      "owner" -> getLightUser(s.owner),
      "chapters" -> s.chapters,
      "createdAt" -> s.createdAt)
  }
}

object JsonView {

  case class BiData(study: JsObject, analysis: JsObject)
}
