package lila
package model

import play.api.libs.json._

import chess._
import Pos.{ piotr, allPiotrs }

sealed trait Event {
  def typ: String
  def data: JsValue
  def only: Option[Color] = None
}
object Event {

  def fromMove(move: Move): List[Event] = MoveEvent(move) :: List(
    if (move.enpassant) move.capture map EnpassantEvent.apply else None,
    move.promotion map { PromotionEvent(_, move.dest) },
    move.castle map {
      case (king, rook) ⇒ CastlingEvent(king, rook, move.color)
    }
  ).flatten

  def fromSituation(situation: Situation): List[Event] = List(
    if (situation.check) situation.kingPos map CheckEvent.apply else None,
    if (situation.threefoldRepetition) Some(ThreefoldEvent()) else None
  ).flatten

  def possibleMoves(situation: Situation, color: Color): Event =
    PossibleMovesEvent(
      color,
      if (color == situation.color) situation.destinations else Map.empty
    )
}

case class StartEvent() extends Event {
  def typ = "start"
  def data = JsNull
}

case class MoveEvent(orig: Pos, dest: Pos, color: Color) extends Event {
  def typ = "move"
  def data = JsObject(Seq(
    "type" -> JsString("move"),
    "from" -> JsString(orig.key),
    "to" -> JsString(dest.key),
    "color" -> JsString(color.name)
  ))
}
object MoveEvent {
  def apply(move: Move): MoveEvent =
    MoveEvent(move.orig, move.dest, move.piece.color)
}

case class PossibleMovesEvent(
  color: Color,
  moves: Map[Pos, List[Pos]]) extends Event {
  def typ = "possible_moves"
  def data =
    if (moves.isEmpty) JsNull
    else JsObject(moves map {
      case (o, d) ⇒ o.key -> JsString(d map (_.key) mkString)
    } toList)
  override def only = Some(color)
}

case class EnpassantEvent(killed: Pos) extends Event {
  def typ = "enpassant"
  def data = JsString(killed.key)
}

case class CastlingEvent(king: (Pos, Pos), rook: (Pos, Pos), color: Color) extends Event {
  def typ = "castling"
  def data = JsObject(Seq(
    "king" -> jsArray(king._1.key, king._2.key),
    "rook" -> jsArray(rook._1.key, rook._2.key),
    "color" -> JsString(color.name)
  ))

  def jsArray(a: String, b: String) = JsArray(List(JsString(a), JsString(b)))
}

case class RedirectEvent(color: Color, url: String) extends Event {
  def typ = "redirect"
  def data = JsString(url)
  override def only = Some(color)
}

case class PromotionEvent(role: PromotableRole, pos: Pos) extends Event {
  def typ = "promotion"
  def data = JsObject(Seq(
    "key" -> JsString(pos.key),
    "pieceClass" -> JsString(role.toString.toLowerCase)
  ))
}

case class CheckEvent(pos: Pos) extends Event {
  def typ = "check"
  def data = JsString(pos.key)
}

case class MessageEvent(author: String, message: String) extends Event {
  def typ = "message"
  def data = JsNull
}

case class EndEvent() extends Event {
  def typ = "end"
  def data = JsNull
}

case class ThreefoldEvent() extends Event {
  def typ = "threefold_repetition"
  def data = JsNull
}

case class ReloadTableEvent(color: Color) extends Event {
  def typ = "reload_table"
  def data = JsNull
  override def only = Some(color)
}

case class MoretimeEvent(color: Color, seconds: Int) extends Event {
  def typ = "moretime"
  def data = JsObject(Seq(
    "type" -> JsString("moretime"),
    "color" -> JsString(color.name),
    "seconds" -> JsNumber(seconds)
  ))
}

