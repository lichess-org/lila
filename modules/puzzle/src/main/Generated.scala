package lila.puzzle

import scala.util.{ Try, Success, Failure }

import chess.format.{ Forsyth, UciMove }
import chess.{ Game, Variant }
import org.joda.time.DateTime
import play.api.libs.json._

case class Generated(
    tags: List[String],
    position: String,
    solution: JsObject,
    id: String) {

  def toPuzzle: Try[PuzzleId => Puzzle] = for {
    lines ← Generated readLines solution
    history = position split ' '
    _ ← if (history.isEmpty) Failure(new Exception("Empty history")) else Success(true)
    fen ← Generated fenOf history
  } yield Puzzle.make(
    gameId = id.some,
    tags = tags,
    history = position.trim.split(' ').toList,
    fen = fen,
    lines = lines)
}

object Generated {

  def readLines(obj: JsObject): Try[Lines] = (obj.fields.toList map {
    case (move, JsString("win"))   => Success(Win(move))
    case (move, JsString("retry")) => Success(Retry(move))
    case (move, more: JsObject)    => readLines(more) map { Node(move, _) }
    case (move, value)             => Failure(new Exception(s"Invalid line $move $value"))
  }).sequence

  private[puzzle] def fenOf(moves: Seq[String]): Try[String] =
    (moves.init.foldLeft(Try(Game(Variant.Standard))) {
      case (game, moveStr) => game flatMap { g =>
        (UciMove(moveStr) toValid s"Invalid UCI move $moveStr" flatMap {
          case UciMove(orig, dest, prom) => g(orig, dest, prom) map (_._1)
        }).fold(errs => Failure(new Exception(errs.shows)), Success.apply)
      }
    }) map { game =>
      Forsyth >> Forsyth.SituationPlus(game.situation, moves.size / 2)
    }

  implicit val generatedJSONRead = Json.reads[Generated]
}
