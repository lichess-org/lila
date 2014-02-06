package lila.puzzle

import play.api.libs.json._

sealed trait Line

case class Node(move: String, lines: Lines) extends Line
case class Win(move: String) extends Line
case class Retry(move: String) extends Line

object Line {

  def minDepth(lines: Lines): Int = {
    def walk(subs: Vector[(Lines, Int)]): Option[Int] = subs match {
      case Vector() ⇒ none
      case (lines, depth) +: rest ⇒ lines match {
        case Nil                  ⇒ walk(rest)
        case Win(_) :: _          ⇒ depth.some
        case Retry(_) :: siblings ⇒ walk(rest :+ (siblings -> depth))
        case Node(_, children) :: siblings ⇒
          walk(rest :+ (siblings -> depth) :+ (children -> (depth + 1)))
      }
    }
    (1 + ~(walk(Vector(lines -> 1)))) / 2
  }

  def flatLine(line: Line): List[String] = line match {
    case Win(move) ⇒ List(move)
    case Retry(_)  ⇒ Nil
    case Node(move, lines) ⇒ move :: ~(lines.find {
      case Retry(_)            ⇒ false
      case Win(_) | Node(_, _) ⇒ true
    }).map(flatLine)
  }

  def toString(lines: Lines, level: Int = 0): String = {
    val indent = ". " * level
    lines map {
      case Win(move)        ⇒ s"$indent$move win"
      case Retry(move)      ⇒ s"$indent$move retry"
      case Node(move, more) ⇒ s"$indent$move\n${toString(more, level + 1)}"
    } mkString "\n"
  }

  def toJson(lines: Lines): JsObject = JsObject(lines map {
    case Win(move)        ⇒ dropPromotion(move) -> JsString("win")
    case Retry(move)      ⇒ dropPromotion(move) -> JsString("retry")
    case Node(move, more) ⇒ dropPromotion(move) -> toJson(more)
  })

  private def dropPromotion(move: String) = move take 4

  def toJsonString(lines: Lines) = Json stringify toJson(lines)
}
