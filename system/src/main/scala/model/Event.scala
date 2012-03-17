package lila.system
package model

import lila.chess._
import Pos.{ piotr, allPiotrs }

sealed trait Event {
  def encode: String
  def export: Map[String, Any]
}
object Event {

  def fromMove(move: Move): List[Event] = MoveEvent(move) :: List(
    if (move.enpassant) move.capture map EnpassantEvent.apply else None,
    move.promotion map { role ⇒ PromotionEvent(role, move.dest) },
    move.castle map { rook ⇒ CastlingEvent((move.orig, move.dest), rook, move.color) }
  ).flatten

  def fromSituation(situation: Situation): List[Event] = List(
    if (situation.check) situation.kingPos map CheckEvent.apply else None,
    if (situation.end) Some(EndEvent()) else None,
    if (situation.threefoldRepetition) Some(ThreefoldEvent()) else None
  ).flatten

  def possibleMoves(situation: Situation, color: Color): Event = PossibleMovesEvent(
    if (color == situation.color) situation.destinations else Map.empty
  )
}

sealed trait EventDecoder {
  def decode(str: String): Option[Event]
}
object EventDecoder {
  val all: Map[Char, EventDecoder] = Map(
    's' -> StartEvent,
    'p' -> PossibleMovesEvent,
    'P' -> PromotionEvent,
    'r' -> RedirectEvent,
    'R' -> ReloadTableEvent,
    'm' -> MoveEvent,
    'M' -> MessageEvent,
    'c' -> CastlingEvent,
    'C' -> CheckEvent,
    't' -> ThreefoldEvent,
    'T' -> MoretimeEvent,
    'e' -> EndEvent,
    'E' -> EnpassantEvent)
}

case class StartEvent() extends Event {
  def encode = "s"
  def export = Map(
    "type" -> "start")
}
object StartEvent extends EventDecoder {
  def decode(str: String) = Some(StartEvent())
}

case class MoveEvent(orig: Pos, dest: Pos, color: Color) extends Event {
  def encode = "m" + orig.piotr + dest.piotr + color.letter
  def export = Map(
    "type" -> "move",
    "from" -> orig.key,
    "to" -> dest.key,
    "color" -> color.name)
}
object MoveEvent extends EventDecoder {
  def apply(move: Move): MoveEvent = MoveEvent(move.orig, move.dest, move.piece.color)
  def decode(str: String) = str.toList match {
    case List(o, d, c) ⇒ for {
      orig ← piotr(o)
      dest ← piotr(d)
      color ← Color(c)
    } yield MoveEvent(orig, dest, color)
    case _ ⇒ None
  }
}

case class PossibleMovesEvent(moves: Map[Pos, List[Pos]]) extends Event {
  def encode = "p" + (moves map {
    case (orig, dests) ⇒ (orig :: dests) map (_.piotr) mkString
  } mkString ",")
  def export = Map(
    "type" -> "possible_moves",
    "possible_moves" -> (moves map {
      case (o, d) ⇒ o.key -> (d map (_.key) mkString)
    }))
}
object PossibleMovesEvent extends EventDecoder {
  def decode(str: String) = Some(PossibleMovesEvent(
    (str.split(",") map { line ⇒
      line.toList match {
        case Nil      ⇒ None
        case o :: Nil ⇒ None
        case o :: ds ⇒ for {
          orig ← piotr(o)
          dests = ds collect allPiotrs
        } yield (orig, dests)
      }
    }).flatten toMap
  ))
}

case class EnpassantEvent(killed: Pos) extends Event {
  def encode = "E" + killed.piotr
  def export = Map(
    "type" -> "enpassant",
    "killed" -> killed.key)
}
object EnpassantEvent extends EventDecoder {
  def decode(str: String) = for {
    k ← str.headOption
    killed ← piotr(k)
  } yield EnpassantEvent(killed)
}

case class CastlingEvent(king: (Pos, Pos), rook: (Pos, Pos), color: Color) extends Event {
  def encode = "c" + king._1.piotr + king._2.piotr + rook._1.piotr + rook._2.piotr + color.letter
  def export = Map(
    "type" -> "castling",
    "king" -> List(king._1.key, king._2.key),
    "rook" -> List(rook._1.key, rook._2.key),
    "color" -> color.name)
}
object CastlingEvent extends EventDecoder {
  def decode(str: String) = str.toList match {
    case List(k1, k2, r1, r2, c) ⇒ for {
      king1 ← piotr(k1)
      king2 ← piotr(k2)
      king = (king1, king2)
      rook1 ← piotr(r1)
      rook2 ← piotr(r2)
      rook = (rook1, rook2)
      color ← Color(c)
    } yield CastlingEvent(king, rook, color)
    case _ ⇒ None
  }
}

case class RedirectEvent(url: String) extends Event {
  def encode = "r" + url
  def export = Map(
    "type" -> "redirect",
    "url" -> url)
}
object RedirectEvent extends EventDecoder {
  def decode(str: String) = Some(RedirectEvent(str))
}

case class PromotionEvent(role: PromotableRole, pos: Pos) extends Event {
  def encode = "P" + pos.piotr + role.forsyth
  def export = Map(
    "type" -> "promotion",
    "key" -> pos.key,
    "pieceClass" -> role.toString.toLowerCase)
}
object PromotionEvent extends EventDecoder {
  def decode(str: String) = str.toList match {
    case List(p, r) ⇒ for {
      pos ← piotr(p)
      role ← Role promotable r
    } yield PromotionEvent(role, pos)
    case _ ⇒ None
  }
}

case class CheckEvent(pos: Pos) extends Event {
  def encode = "C" + pos.piotr
  def export = Map(
    "type" -> "check",
    "key" -> pos.key)
}
object CheckEvent extends EventDecoder {
  def decode(str: String) = for {
    p ← str.headOption
    pos ← piotr(p)
  } yield CheckEvent(pos)
}

case class MessageEvent(author: String, message: String) extends Event {
  def encode = "M" + author + " " + message.replace("|", "(pipe)")
  def export = Map(
    "type" -> "message",
    "message" -> List(author, message))
}
object MessageEvent extends EventDecoder {
  def decode(str: String) = str.split(' ').toList match {
    case author :: words ⇒ Some(MessageEvent(
      author, (words mkString " ").replace("(pipe)", "|")
    ))
    case _ ⇒ None
  }
}

case class EndEvent() extends Event {
  def encode = "e"
  def export = Map(
    "type" -> "end")
}
object EndEvent extends EventDecoder {
  def decode(str: String) = Some(EndEvent())
}

case class ThreefoldEvent() extends Event {
  def encode = "t"
  def export = Map(
    "type" -> "threefold_repetition")
}
object ThreefoldEvent extends EventDecoder {
  def decode(str: String) = Some(ThreefoldEvent())
}

case class ReloadTableEvent() extends Event {
  def encode = "R"
  def export = Map(
    "type" -> "reload_table")
}
object ReloadTableEvent extends EventDecoder {
  def decode(str: String) = Some(ReloadTableEvent())
}

case class MoretimeEvent(color: Color, seconds: Int) extends Event {
  def encode = "T" + color.letter + seconds
  def export = Map(
    "type" -> "moretime",
    "color" -> color.name,
    "seconds" -> seconds)
}
object MoretimeEvent extends EventDecoder {
  def decode(str: String) = for {
    c ← str.headOption
    color ← Color(c)
    seconds ← parseIntOption(str drop 1)
  } yield MoretimeEvent(color, seconds)
}
