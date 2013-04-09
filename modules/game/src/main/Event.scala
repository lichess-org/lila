package lila.game

import play.api.libs.json._

import chess.{ PromotableRole, Pos, Color, Situation, Move ⇒ ChessMove, Clock ⇒ ChessClock }
import Pos.{ piotr, allPiotrs }
import org.apache.commons.lang3.StringEscapeUtils.escapeXml

sealed trait Event {
  def typ: String
  def data: JsValue
  def only: Option[Color] = None
  def owner: Boolean = false
  def watcher: Boolean = false
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
    if (situation.threefoldRepetition) Some(Threefold) else None,
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

  object Start extends Empty {
    def typ = "start"
  }

  case class Move(orig: Pos, dest: Pos, color: Color) extends Event {
    def typ = "move"
    def data = Json.obj(
      "type" -> "move",
      "from" -> orig.key,
      "to" -> dest.key,
      "color" -> color.name
    )
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

  case class Message(author: String, message: String) extends Event {
    def typ = "message"
    def data = JsString(renderRoom(author, message))
    override def owner = true
  }

  private def renderRoom(author: String, text: String): String =
    """<li class="%s%s">%s</li>""".format(
      author,
      (author == "system") ?? " trans_me",
      escapeXml(text))

  case class WatcherMessage(author: Option[String], text: String) extends Event {
    def typ = "message"
    def data = JsString(renderWatcherRoom(author, text))
    override def watcher = true
  }

  private def renderWatcherRoom(author: Option[String], text: String): String =
    """<li><span>%s</span>%s</li>""".format(
      author.fold("Anonymous")("@" + _),
      escapeXml(text))

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

  case class Premove(color: Color) extends Empty {
    def typ = "premove"
    override def only = Some(color)
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
      "watchers" -> Json.arr(watchers map JsString)
    )
  }
}
