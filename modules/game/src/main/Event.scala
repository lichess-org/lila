package lila.game

import play.api.libs.json._

import lila.chat.{ Line, UserLine, PlayerLine }
import chess.Pos.{ piotr, allPiotrs }
import chess.{ PromotableRole, Pos, Color, Situation, Move ⇒ ChessMove, Clock ⇒ ChessClock }

sealed trait Event {
  def typ: String
  def data: JsValue
  def only: Option[Color] = None
  def owner: Boolean = false
  def watcher: Boolean = false
  def troll: Boolean = false
}

object Event {

  def fromMove(move: ChessMove): List[Event] = Move(move) :: List(
    (move.capture ifTrue move.enpassant) map Event.Enpassant.apply,
    move.promotion map { Promotion(_, move.dest) },
    move.castle map { case (king, rook) ⇒ Castling(king, rook, move.color) }
  ).flatten

  def fromSituation(situation: Situation): List[Event] = List(
    situation.check ?? situation.kingPos map Check.apply,
    situation.threefoldRepetition option Threefold,
    Some(Premove(situation.color))
  ).flatten

  def possibleMoves(situation: Situation, color: Color): Event =
    PossibleMoves(color, (color == situation.color) ?? situation.destinations)

  sealed trait Empty extends Event {
    def data = JsNull
  }

  object Start extends Empty {
    def typ = "start"
  }

  case class Move(orig: Pos, dest: Pos, color: Color) extends Event {
    def typ = "move"
    def data = Json.obj(
      "type" -> "move",
      "from" -> orig.key,
      "to" -> dest.key,
      "color" -> color.name)
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
    def data = Json.obj(
      "king" -> Json.arr(king._1.key, king._2.key),
      "rook" -> Json.arr(rook._1.key, rook._2.key),
      "color" -> color.name
    )
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

  object Reload extends Empty {
    def typ = "resync"
  }

  case class Promotion(role: PromotableRole, pos: Pos) extends Event {
    def typ = "promotion"
    def data = Json.obj(
      "key" -> pos.key,
      "pieceClass" -> role.toString.toLowerCase
    )
  }

  case class Check(pos: Pos) extends Event {
    def typ = "check"
    def data = JsString(pos.key)
  }

  case class PlayerMessage(line: PlayerLine) extends Event {
    def typ = "message"
    def data = Line toJson line
    override def owner = true
    override def troll = false
  }

  // it *IS* a username, and not a user ID
  // immediately used for rendering
  case class UserMessage(line: UserLine, w: Boolean) extends Event {
    def typ = "message"
    def data = Line toJson line
    override def troll = line.troll
    override def watcher = w
  }

  object End extends Empty {
    def typ = "end"
  }

  object Threefold extends Empty {
    def typ = "threefold_repetition"
  }

  case class ReloadTable(color: Color) extends Event {
    def typ = "reload_table"
    def data = JsNull
    override def only = Some(color)
  }

  case object ReloadTables extends Event {
    def typ = "reload_table"
    def data = JsNull
  }
  case object ReloadTablesOwner extends Event {
    def typ = "reload_table"
    def data = JsNull
    override def owner = true
  }

  case class Premove(color: Color) extends Empty {
    def typ = "premove"
    override def only = Some(color)
    override def owner = true
  }

  case class Clock(white: Float, black: Float) extends Event {
    def typ = "clock"
    def data = Json.obj(
      "white" -> white,
      "black" -> black
    )
  }
  object Clock {
    def apply(clock: ChessClock): Clock = Clock(
      clock remainingTime Color.White,
      clock remainingTime Color.Black)
  }

  case class State(color: Color, turns: Int) extends Event {
    def typ = "state"
    def data = Json.obj(
      "color" -> color.name,
      "turns" -> turns
    )
  }

  case class Crowd(
      white: Boolean,
      black: Boolean,
      watchers: List[String]) extends Event {
    def typ = "crowd"
    def data = Json.obj(
      "white" -> white,
      "black" -> black,
      "watchers" -> watchers)
  }
}
