package lila.system
package model

import lila.chess._
import Pos.posAt
import Piotr._

sealed trait Event {

  def encode: Option[String]
}

trait EventDecoder {

  def decode(str: String): Option[Event]
}
object EventDecoder {

  val all: Map[Char, EventDecoder] = Map(
    's' -> StartEvent,
    'm' -> MoveEvent,
    'p' -> PossibleMovesEvent,
    'E' -> EnpassantEvent
  )
}

case class StartEvent() extends Event {

  def encode = Some("s")
}
object StartEvent extends EventDecoder {

  def decode(str: String) = Some(StartEvent())
}

case class MoveEvent(orig: Pos, dest: Pos, color: Color) extends Event {

  def encode = for {
    o ← encodePos get orig
    d ← encodePos get dest
  } yield "m" + o + d + color.letter
}
object MoveEvent extends EventDecoder {

  def decode(str: String) = str.toList match {
    case List(o, d, c) ⇒ for {
      orig ← decodePos get o
      dest ← decodePos get d
      color ← Color(c)
    } yield MoveEvent(orig, dest, color)
    case _ ⇒ None
  }
}

case class PossibleMovesEvent(moves: Map[Pos, List[Pos]]) extends Event {

  def encode = Some("p" + ((moves map {
    case (orig, dests) ⇒ for {
      o ← encodePos get orig
      ds = dests collect encodePos
    } yield o.toString + (ds mkString "")
  }).flatten mkString ","))
}
object PossibleMovesEvent extends EventDecoder {

  def decode(str: String) = Some(PossibleMovesEvent(
    (str.split(",") map { line ⇒
      line.toList match {
        case Nil      ⇒ None
        case o :: Nil ⇒ None
        case o :: ds ⇒ for {
          orig ← decodePos get o
          dests = ds collect decodePos
        } yield (orig, dests)
      }
    }).flatten toMap
  ))
}

case class EnpassantEvent(killed: Pos) extends Event {

  def encode = for {
    k ← encodePos get killed
  } yield "E" + k
}
object EnpassantEvent extends EventDecoder {

  def decode(str: String) = for {
    k ← str.headOption
    killed ← decodePos get k
  } yield EnpassantEvent(killed)
}
