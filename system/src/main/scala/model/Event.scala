package lila.system
package model

import lila.chess._
import Pos.{ piotr, allPiotrs }

sealed trait Event {
  def encode: Option[String]
}
object Event {
  def fromMove(move: Move): List[Event] = MoveEvent(move) :: List(
    if (move.enpassant) move.capture map EnpassantEvent.apply else None,
    move.promotion map { role ⇒ PromotionEvent(role, move.dest) },
    move.castle map { rook ⇒ CastlingEvent((move.orig, move.dest), rook, move.color) }
  ).flatten
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
  def encode = Some("s")
}
object StartEvent extends EventDecoder {
  def decode(str: String) = Some(StartEvent())
}

case class MoveEvent(orig: Pos, dest: Pos, color: Color) extends Event {
  def encode = Some("m" + orig.piotr + dest.piotr + color.letter)
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
  def encode = Some("p" + (moves map {
    case (orig, dests) ⇒ (orig :: dests) map (_.piotr) mkString
  } mkString ","))
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
  def encode = Some("E" + killed.piotr)
}
object EnpassantEvent extends EventDecoder {
  def decode(str: String) = for {
    k ← str.headOption
    killed ← piotr(k)
  } yield EnpassantEvent(killed)
}

case class CastlingEvent(king: (Pos, Pos), rook: (Pos, Pos), color: Color) extends Event {
  def encode = Some(
    "c" + king._1.piotr + king._2.piotr + rook._1.piotr + rook._2.piotr + color.letter
  )
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
  def encode = Some("r" + url)
}
object RedirectEvent extends EventDecoder {
  def decode(str: String) = Some(RedirectEvent(str))
}

case class PromotionEvent(role: PromotableRole, pos: Pos) extends Event {
  def encode = Some("P" + role.forsyth + pos.piotr)
}
object PromotionEvent extends EventDecoder {
  def decode(str: String) = str.toList match {
    case List(r, p) ⇒ for {
      role ← Role promotable r
      pos ← piotr(p)
    } yield PromotionEvent(role, pos)
    case _ ⇒ None
  }
}

case class CheckEvent(pos: Pos) extends Event {
  def encode = Some("C" + pos.piotr)
}
object CheckEvent extends EventDecoder {
  def decode(str: String) = for {
    p ← str.headOption
    pos ← piotr(p)
  } yield CheckEvent(pos)
}

case class MessageEvent(author: String, message: String) extends Event {
  def encode = Some("M" + author + " " + message.replace("|", "(pipe)"))
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
  def encode = Some("e")
}
object EndEvent extends EventDecoder {
  def decode(str: String) = Some(EndEvent())
}

case class ThreefoldEvent() extends Event {
  def encode = Some("t")
}
object ThreefoldEvent extends EventDecoder {
  def decode(str: String) = Some(ThreefoldEvent())
}

case class ReloadTableEvent() extends Event {
  def encode = Some("R")
}
object ReloadTableEvent extends EventDecoder {
  def decode(str: String) = Some(ReloadTableEvent())
}

case class MoretimeEvent(color: Color, seconds: Int) extends Event {
  def encode = Some("T" + color.letter + seconds)
}
object MoretimeEvent extends EventDecoder {
  def decode(str: String) = for {
    c ← str.headOption
    color ← Color(c)
    seconds ← parseIntOption(str drop 1)
  } yield MoretimeEvent(color, seconds)
}
