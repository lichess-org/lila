package lila.system
package model

object Status extends Enumeration {

  val created, started, aborted, mate, resign, stalemate, timeout, draw, outoftime, cheat = Value

  def toInt(s: Value) = s match {
    case x if x == created ⇒ 10
    case x if x == started ⇒ 20
    case x if x == aborted ⇒ 25
    case x if x == mate ⇒ 30
    case x if x == resign ⇒ 31
    case x if x == stalemate ⇒ 32
    case x if x == timeout ⇒ 33
    case x if x == draw ⇒ 34
    case x if x == outoftime ⇒ 35
    case x if x == cheat ⇒ 36
  }

  val indexed: Map[Int, Value] = values map { v =>
    toInt(v) -> v
  } toMap

  def fromInt(i: Int): Option[Value] = indexed get i
}
