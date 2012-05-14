package lila
package round

import play.api.libs.json._

import chess.{ PromotableRole, Pos, Color, Situation, Move ⇒ ChessMove, Clock ⇒ ChessClock }
import Pos.{ piotr, allPiotrs }

sealed trait Event {
  def typ: String
  def data: JsValue
  def only: Option[Color] = None
  def owner: Boolean = false
}

object Event {

  def fromMove(move: ChessMove): List[Event] = Move(move) :: List(
    if (move.enpassant) move.capture map Event.Enpassant.apply else None,
    move.promotion map { Promotion(_, move.dest) },
    move.castle map {
      case (king, rook) ⇒ Castling(king, rook, move.color)
    }
  ).flatten

  def fromSituation(situation: Situation): List[Event] = List(
    if (situation.check) situation.kingPos map Check.apply else None,
    if (situation.threefoldRepetition) Some(Threefold()) else None,
    Some(Premove(situation.color))
  ).flatten

  def possibleMoves(situation: Situation, color: Color): Event =
    PossibleMoves(
      color,
      if (color == situation.color) situation.destinations else Map.empty
    )

  sealed trait Empty extends Event {
    def data = JsNull
  }

  case class Start() extends Empty {
    def typ = "start"
  }

  case class Move(orig: Pos, dest: Pos, color: Color) extends Event {
    def typ = "move"
    def data = JsObject(Seq(
      "type" -> JsString("move"),
      "from" -> JsString(orig.key),
      "to" -> JsString(dest.key),
      "color" -> JsString(color.name)
    ))
  }
  object Move {
    def apply(move: ChessMove): Move =
      Move(move.orig, move.dest, move.piece.color)
  }

  case class PossibleMoves(
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

  case class Enpassant(killed: Pos) extends Event {
    def typ = "enpassant"
    def data = JsString(killed.key)
  }

  case class Castling(king: (Pos, Pos), rook: (Pos, Pos), color: Color) extends Event {
    def typ = "castling"
    def data = JsObject(Seq(
      "king" -> jsArray(king._1.key, king._2.key),
      "rook" -> jsArray(rook._1.key, rook._2.key),
      "color" -> JsString(color.name)
    ))

    def jsArray(a: String, b: String) = JsArray(List(JsString(a), JsString(b)))
  }

  sealed trait Redirect extends Event {
    def url: String
    def typ = "redirect"
    def data = JsString(url)
  }

  case class RedirectOwner(color: Color, url: String) extends Redirect {
    override def only = Some(color)
    override def owner = true
  }

  case class Reload() extends Empty {
    def typ = "reload"
  }

  case class Promotion(role: PromotableRole, pos: Pos) extends Event {
    def typ = "promotion"
    def data = JsObject(Seq(
      "key" -> JsString(pos.key),
      "pieceClass" -> JsString(role.toString.toLowerCase)
    ))
  }

  case class Check(pos: Pos) extends Event {
    def typ = "check"
    def data = JsString(pos.key)
  }

  case class Message(author: String, message: String) extends Event {
    def typ = "message"
    def data = JsString(Room render (author, message))
    override def owner = true
  }

  case class End() extends Empty {
    def typ = "end"
  }

  case class Threefold() extends Empty {
    def typ = "threefold_repetition"
  }

  case class ReloadTable(color: Color) extends Event {
    def typ = "reload_table"
    def data = JsNull
    override def only = Some(color)
  }

  case class Premove(color: Color) extends Empty {
    def typ = "premove"
    override def only = Some(color)
  }

  case class Clock(white: Float, black: Float) extends Event {
    def typ = "clock"
    def data = JsObject(Seq(
      "white" -> JsNumber(white),
      "black" -> JsNumber(black)
    ))
  }
  object Clock {
    def apply(clock: ChessClock): Clock = Clock(
      clock remainingTime Color.White,
      clock remainingTime Color.Black)
  }

  case class State(color: Color, turns: Int) extends Event {
    def typ = "state"
    def data = JsObject(Seq(
      "color" -> JsString(color.name),
      "turns" -> JsNumber(turns)
    ))
  }

  case class Crowd(
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
}
