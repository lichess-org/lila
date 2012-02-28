package lila.chess

import scala.math.{ abs, min, max }

sealed case class Pos private (x: Int, y: Int) {

  import Pos.makePos

  lazy val up: Option[Pos] = this ^ 1
  lazy val down: Option[Pos] = this v 1
  lazy val right: Option[Pos] = this > 1
  lazy val left: Option[Pos] = this < 1
  lazy val upLeft: Option[Pos] = up flatMap (_ left)
  lazy val upRight: Option[Pos] = up flatMap (_ right)
  lazy val downLeft: Option[Pos] = down flatMap (_ left)
  lazy val downRight: Option[Pos] = down flatMap (_ right)

  def ^(n: Int): Option[Pos] = makePos(x, y + n)
  def v(n: Int): Option[Pos] = makePos(x, y - n)
  def >(n: Int): Option[Pos] = makePos(x + n, y)
  def <(n: Int): Option[Pos] = makePos(x - n, y)
  def >|(stop: Pos ⇒ Boolean): List[Pos] = |<>|(stop, _.right)
  def |<(stop: Pos ⇒ Boolean): List[Pos] = |<>|(stop, _.left)
  def |<>|(stop: Pos ⇒ Boolean, dir: Direction): List[Pos] = dir(this) map { p ⇒
    p :: (if (stop(p)) Nil else p.|<>|(stop, dir))
  } getOrElse Nil

  def ?<(other: Pos) = x < other.x
  def ?>(other: Pos) = x > other.x

  def <->(other: Pos): Iterable[Pos] =
    min(x, other.x) to max(x, other.x) map { makePos(_, y) } flatten

  override def toString = (Pos xToString x) + y.toString
}

object Pos {

  private val bounds: Set[Int] = (1 to 8) toSet

  def makePos(x: Int, y: Int): Option[Pos] =
    if (bounds(x) && bounds(y)) Some(Pos(x, y)) else None

  def unsafe(x: Int, y: Int): Pos = Pos(x, y)

  def xToString(x: Int) = (96 + x).toChar.toString

  val A1 = Pos(1, 1)
  val A2 = Pos(1, 2)
  val A3 = Pos(1, 3)
  val A4 = Pos(1, 4)
  val A5 = Pos(1, 5)
  val A6 = Pos(1, 6)
  val A7 = Pos(1, 7)
  val A8 = Pos(1, 8)
  val B1 = Pos(2, 1)
  val B2 = Pos(2, 2)
  val B3 = Pos(2, 3)
  val B4 = Pos(2, 4)
  val B5 = Pos(2, 5)
  val B6 = Pos(2, 6)
  val B7 = Pos(2, 7)
  val B8 = Pos(2, 8)
  val C1 = Pos(3, 1)
  val C2 = Pos(3, 2)
  val C3 = Pos(3, 3)
  val C4 = Pos(3, 4)
  val C5 = Pos(3, 5)
  val C6 = Pos(3, 6)
  val C7 = Pos(3, 7)
  val C8 = Pos(3, 8)
  val D1 = Pos(4, 1)
  val D2 = Pos(4, 2)
  val D3 = Pos(4, 3)
  val D4 = Pos(4, 4)
  val D5 = Pos(4, 5)
  val D6 = Pos(4, 6)
  val D7 = Pos(4, 7)
  val D8 = Pos(4, 8)
  val E1 = Pos(5, 1)
  val E2 = Pos(5, 2)
  val E3 = Pos(5, 3)
  val E4 = Pos(5, 4)
  val E5 = Pos(5, 5)
  val E6 = Pos(5, 6)
  val E7 = Pos(5, 7)
  val E8 = Pos(5, 8)
  val F1 = Pos(6, 1)
  val F2 = Pos(6, 2)
  val F3 = Pos(6, 3)
  val F4 = Pos(6, 4)
  val F5 = Pos(6, 5)
  val F6 = Pos(6, 6)
  val F7 = Pos(6, 7)
  val F8 = Pos(6, 8)
  val G1 = Pos(7, 1)
  val G2 = Pos(7, 2)
  val G3 = Pos(7, 3)
  val G4 = Pos(7, 4)
  val G5 = Pos(7, 5)
  val G6 = Pos(7, 6)
  val G7 = Pos(7, 7)
  val G8 = Pos(7, 8)
  val H1 = Pos(8, 1)
  val H2 = Pos(8, 2)
  val H3 = Pos(8, 3)
  val H4 = Pos(8, 4)
  val H5 = Pos(8, 5)
  val H6 = Pos(8, 6)
  val H7 = Pos(8, 7)
  val H8 = Pos(8, 8)

  val allKeys: Map[String, Pos] = Map("a1" -> A1, "a2" -> A2, "a3" -> A3, "a4" -> A4, "a5" -> A5, "a6" -> A6, "a7" -> A7, "a8" -> A8, "b1" -> B1, "b2" -> B2, "b3" -> B3, "b4" -> B4, "b5" -> B5, "b6" -> B6, "b7" -> B7, "b8" -> B8, "c1" -> C1, "c2" -> C2, "c3" -> C3, "c4" -> C4, "c5" -> C5, "c6" -> C6, "c7" -> C7, "c8" -> C8, "d1" -> D1, "d2" -> D2, "d3" -> D3, "d4" -> D4, "d5" -> D5, "d6" -> D6, "d7" -> D7, "d8" -> D8, "e1" -> E1, "e2" -> E2, "e3" -> E3, "e4" -> E4, "e5" -> E5, "e6" -> E6, "e7" -> E7, "e8" -> E8, "f1" -> F1, "f2" -> F2, "f3" -> F3, "f4" -> F4, "f5" -> F5, "f6" -> F6, "f7" -> F7, "f8" -> F8, "g1" -> G1, "g2" -> G2, "g3" -> G3, "g4" -> G4, "g5" -> G5, "g6" -> G6, "g7" -> G7, "g8" -> G8, "h1" -> H1, "h2" -> H2, "h3" -> H3, "h4" -> H4, "h5" -> H5, "h6" -> H6, "h7" -> H7, "h8" -> H8)

  def all = allKeys.values
}
