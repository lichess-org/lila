package lila
package game

import play.api.libs.json._

import chess._
import Pos.{ piotr, allPiotrs }

sealed trait Event {
  def typ: String
  def data: JsValue
  def only: Option[Color] = None
  def owner: Boolean = false
}
sealed trait EmptyEvent extends Event {
  def data = JsNull
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
    if (situation.threefoldRepetition) Some(ThreefoldEvent()) else None,
    Some(PremoveEvent(situation.color))
  ).flatten

  def possibleMoves(situation: Situation, color: Color): Event =
    PossibleMovesEvent(
      color,
      if (color == situation.color) situation.destinations else Map.empty
    )
}

case class StartEvent() extends EmptyEvent {
  def typ = "start"
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

sealed trait RedirectEvent extends Event {
  def url: String
  def typ = "redirect"
  def data = JsString(url)
}

case class RedirectOwnerEvent(color: Color, url: String) extends RedirectEvent {
  override def only = Some(color)
  override def owner = true
}

case class ReloadEvent() extends EmptyEvent {
  def typ = "reload"
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
  def data = JsString(Room render (author, message))
  override def owner = true
}

case class EndEvent() extends EmptyEvent {
  def typ = "end"
}

case class ThreefoldEvent() extends EmptyEvent {
  def typ = "threefold_repetition"
}

case class ReloadTableEvent(color: Color) extends Event {
  def typ = "reload_table"
  def data = JsNull
  override def only = Some(color)
}

case class PremoveEvent(color: Color) extends EmptyEvent {
  def typ = "premove"
  override def only = Some(color)
}

case class ClockEvent(white: Float, black: Float) extends Event {
  def typ = "clock"
  def data = JsObject(Seq(
    "white" -> JsNumber(white),
    "black" -> JsNumber(black)
  ))
}
object ClockEvent {
  def apply(clock: Clock): ClockEvent = ClockEvent(
    clock remainingTime White,
    clock remainingTime Black)
}

case class StateEvent(color: Color, turns: Int) extends Event {
  def typ = "state"
  def data = JsObject(Seq(
    "color" -> JsString(color.name),
    "turns" -> JsNumber(turns)
  ))
}

case class CrowdEvent(
  white: Boolean,
  black: Boolean,
  watchers: Int) extends Event {
  def typ = "crowd"
  def data = JsObject(Seq(
    "white" -> JsBoolean(white),
    "black" -> JsBoolean(black),
    "watchers" -> JsNumber(watchers)
  ))
  def incWatchers = copy(watchers = watchers + 1)
}
