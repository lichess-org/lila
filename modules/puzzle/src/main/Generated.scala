package lila.puzzle

import scala.util.{ Try, Success, Failure }

import chess.format.{ Forsyth, UciMove }
import chess.{ Color, Game, Variant }
import org.joda.time.DateTime
import play.api.libs.json._

case class Generated(
    tags: List[String],
    color: String,
    position: String,
    solution: JsObject,
    id: String) {

  def toPuzzle: Try[Puzzle] = for {
    trueColor ← Color(color).fold[Try[Color]](Failure(new Exception(s"Invalid color $color")))(Success.apply)
    lines ← Generated readLines solution
    fen ← Generated.fenOf(position split ' ')
  } yield Puzzle.make(
    gameId = id.some,
    tags = tags,
    color = trueColor,
    history = position.trim.split(' ').toList,
    fen = fen,
    lines = lines)
}

object Generated {

  def readLines(obj: JsObject): Try[Lines] = (obj.fields.toList map {
    case (move, JsString("end")) ⇒ Success(End(move))
    case (move, more: JsObject)  ⇒ readLines(more) map { Node(move, _) }
    case (move, value)           ⇒ Failure(new Exception(s"Invalid line $move $value"))
  }).sequence

  private[puzzle] def fenOf(moves: Seq[String]): Try[String] =
    (moves.foldLeft(Try(Game(Variant.Standard))) {
      case (game, moveStr) ⇒ game flatMap { g ⇒
        (UciMove(moveStr) toValid s"Invalid UCI move $moveStr" flatMap {
          case UciMove(orig, dest, prom) ⇒ g(orig, dest, prom) map (_._1)
        }).fold(errs ⇒ Failure(new Exception(errs.shows)), Success.apply)
      }
    }) map { game ⇒
      Forsyth >> Forsyth.SituationPlus(game.situation, moves.size / 2)
    }

  implicit val generatedJSONRead = Json.reads[Generated]
}
