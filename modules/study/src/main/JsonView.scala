package lila.study

import chess.format.{ Uci, UciCharPair, Forsyth, FEN }
import play.api.libs.json._

import lila.common.LightUser
import lila.common.PimpedJson._

final class JsonView(
    getLightUser: String => Option[LightUser]) {

  def study(s: Study) = studyWrites writes s

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
    JsString(p.toString)
  }
  private implicit val colorWriter = Writes[chess.Color] { c =>
    JsString(c.name)
  }
  private implicit val fenWriter = Writes[FEN] { f =>
    JsString(f.value)
  }
  private implicit val moveWrites = Json.writes[Uci.WithSan]

  implicit lazy val nodeWrites: Writes[Node] = Writes[Node] { n =>
    Json.obj(
      "id" -> n.id,
      "ply" -> n.ply,
      "move" -> n.move,
      "fen" -> fenWriter.writes(n.fen),
      "check" -> n.check,
      "by" -> n.by,
      "children" -> n.children.nodes)
  }

  private implicit val rootWrites = Writes[Node.Root] { n =>
    Json.obj(
      "ply" -> n.ply,
      "fen" -> n.fen,
      "check" -> n.check,
      "children" -> n.children.nodes)
  }

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
      "ownerChapterId" -> s.ownerChapterId,
      "createdAt" -> s.createdAt)
  }
}

object JsonView {

  case class BiData(study: JsObject, analysis: JsObject)
}
