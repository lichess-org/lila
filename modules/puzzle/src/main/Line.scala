package lila.puzzle

import play.api.libs.json._

sealed trait Line

case class Node(move: String, lines: Lines) extends Line

case class End(move: String) extends Line

object Line {

  def toString(lines: Lines, level: Int = 0): String = {
    val indent = "  " * level
    lines map {
      case End(move) => s"$indent$move end"
      case Node(move, more) => s"$indent$move\n${toString(more, level + 1)}"
    } mkString "\n"
  }

  def toJson(lines: Lines): JsObject = JsObject(lines map {
    case End(move)        ⇒ move -> JsString("end")
    case Node(move, more) ⇒ move -> toJson(more)
  })

  def toJsonString(lines: Lines) = Json stringify toJson(lines)
}
