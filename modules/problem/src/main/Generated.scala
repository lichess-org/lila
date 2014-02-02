package lila.problem

import scala.util.{ Try, Success, Failure }

import chess.Color
import org.joda.time.DateTime
import play.api.libs.json._

case class Generated(
    tags: List[String],
    color: String,
    position: String,
    solution: JsObject,
    id: String) {

  def toProblem: Try[Problem] = for {
    trueColor ← Color(color).fold[Try[Color]](Failure(new Exception(s"Invalid color $color")))(Success.apply)
    lines ← Generated readLines solution
  } yield Problem.make(
    gameId = id.some,
    tags = tags,
    color = trueColor,
    position = position.trim.split(' ').toList,
    lines = lines)
}

object Generated {

  def readLines(obj: JsObject): Try[Lines] = (obj.fields.toList map {
    case (move, JsString("end")) ⇒ Success(End(move))
    case (move, more: JsObject)  ⇒ readLines(more) map { Node(move, _) }
    case (move, value)           ⇒ Failure(new Exception(s"Invalid line $move $value"))
  }).sequence

  implicit val generatedJSONRead = Json.reads[Generated]
}
