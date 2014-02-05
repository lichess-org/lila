package lila.puzzle

import play.api.libs.json._

sealed trait Line

case class Node(move: String, lines: Lines) extends Line
case class Win(move: String) extends Line
case class Retry(move: String) extends Line

object Line {

  def minPlyDepth(lines: Lines, depth: Int = 0): Int = lines match {
    case Nil                   ⇒ Int.MaxValue
    case Retry(_) :: rest      ⇒ minPlyDepth(rest)
    case Win(_) :: rest        ⇒ 1
    case Node(_, more) :: rest ⇒ 1 + minPlyDepth(rest ::: more)
  }

  def toString(lines: Lines, level: Int = 0): String = {
    val indent = "  " * level
    lines map {
      case Win(move)        ⇒ s"$indent$move win"
      case Retry(move)      ⇒ s"$indent$move retry"
      case Node(move, more) ⇒ s"$indent$move\n${toString(more, level + 1)}"
    } mkString "\n"
  }

  def toJson(lines: Lines): JsObject = JsObject(lines map {
    case Win(move)        ⇒ move -> JsString("win")
    case Retry(move)      ⇒ move -> JsString("retry")
    case Node(move, more) ⇒ move -> toJson(more)
  })

  def toJsonString(lines: Lines) = Json stringify toJson(lines)
}
