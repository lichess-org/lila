package chess

import scala.math.{ abs, max, min }

sealed case class Pos private (x: Int, y: Int, piotr: Char) {

  import Pos.posAt

  val down: Option[Pos]         = posAt(x, y - 1)
  val left: Option[Pos]         = posAt(x - 1, y)
  val downLeft: Option[Pos]     = posAt(x - 1, y - 1)
  val downRight: Option[Pos]    = posAt(x + 1, y - 1)
  lazy val up: Option[Pos]      = posAt(x, y + 1)
  lazy val right: Option[Pos]   = posAt(x + 1, y)
  lazy val upLeft: Option[Pos]  = posAt(x - 1, y + 1)
  lazy val upRight: Option[Pos] = posAt(x + 1, y + 1)

  def >|(stop: Pos => Boolean): List[Pos] = |<>|(stop, _.right)
  def |<(stop: Pos => Boolean): List[Pos] = |<>|(stop, _.left)
  def |<>|(stop: Pos => Boolean, dir: Direction): List[Pos] =
    dir(this) map { p =>
      p :: (if (stop(p)) Nil else p.|<>|(stop, dir))
    } getOrElse Nil

  def ?<(other: Pos): Boolean = x < other.x
  def ?>(other: Pos): Boolean = x > other.x
  def ?+(other: Pos): Boolean = y < other.y
  def ?^(other: Pos): Boolean = y > other.y
  def ?|(other: Pos): Boolean = x == other.x
  def ?-(other: Pos): Boolean = y == other.y

  def <->(other: Pos): Iterable[Pos] =
    min(x, other.x) to max(x, other.x) flatMap { posAt(_, y) }

  def touches(other: Pos): Boolean = xDist(other) <= 1 && yDist(other) <= 1

  def onSameDiagonal(other: Pos): Boolean = color == other.color && xDist(other) == yDist(other)
  def onSameLine(other: Pos): Boolean     = ?-(other) || ?|(other)

  def xDist(other: Pos) = abs(x - other.x)
  def yDist(other: Pos) = abs(y - other.y)

  val file     = Pos xToString x
  val rank     = y.toString
  val key      = file + rank
  val color    = Color((x % 2 == 0) ^ (y % 2 == 0))
  val piotrStr = piotr.toString

  override val toString = key

  override val hashCode = 9 * (y - 1) + (x - 1)
}

object Pos {
  val posCache = new Array[Some[Pos]](81)

  def posAt(x: Int, y: Int): Option[Pos] =
    if (x < 1 || x > 9 || y < 1 || y > 9) None
    else posCache(x + 9 * y - 10)

  def posAt(key: String): Option[Pos] = allKeys get key

  def xToString(x: Int) = (96 + x).toChar.toString

  def piotr(c: Char): Option[Pos] = allPiotrs get c

  def keyToPiotr(key: String) = posAt(key) map (_.piotr)
  def doubleKeyToPiotr(key: String) =
    for {
      a <- keyToPiotr(key take 2)
      b <- keyToPiotr(key drop 2)
    } yield s"$a$b"
  def doublePiotrToKey(piotrs: String) =
    for {
      a <- piotr(piotrs.head)
      b <- piotr(piotrs(1))
    } yield s"${a.key}${b.key}"

  private[this] def createPos(x: Int, y: Int, piotr: Char): Pos = {
    val pos = new Pos(x, y, piotr)
    posCache(x + 9 * y - 10) = Some(pos)
    pos
  }

  val A1 = createPos(1, 1, 'a')
  val B1 = createPos(2, 1, 'b')
  val C1 = createPos(3, 1, 'c')
  val D1 = createPos(4, 1, 'd')
  val E1 = createPos(5, 1, 'e')
  val F1 = createPos(6, 1, 'f')
  val G1 = createPos(7, 1, 'g')
  val H1 = createPos(8, 1, 'h')
  val I1 = createPos(9, 1, 'i')
  val A2 = createPos(1, 2, 'j')
  val B2 = createPos(2, 2, 'k')
  val C2 = createPos(3, 2, 'l')
  val D2 = createPos(4, 2, 'm')
  val E2 = createPos(5, 2, 'n')
  val F2 = createPos(6, 2, 'o')
  val G2 = createPos(7, 2, 'p')
  val H2 = createPos(8, 2, 'q')
  val I2 = createPos(9, 2, 'r')
  val A3 = createPos(1, 3, 's')
  val B3 = createPos(2, 3, 't')
  val C3 = createPos(3, 3, 'u')
  val D3 = createPos(4, 3, 'v')
  val E3 = createPos(5, 3, 'w')
  val F3 = createPos(6, 3, 'x')
  val G3 = createPos(7, 3, 'y')
  val H3 = createPos(8, 3, 'z')
  val I3 = createPos(9, 3, 'A')
  val A4 = createPos(1, 4, 'B')
  val B4 = createPos(2, 4, 'C')
  val C4 = createPos(3, 4, 'D')
  val D4 = createPos(4, 4, 'E')
  val E4 = createPos(5, 4, 'F')
  val F4 = createPos(6, 4, 'G')
  val G4 = createPos(7, 4, 'H')
  val H4 = createPos(8, 4, 'I')
  val I4 = createPos(9, 4, 'J')
  val A5 = createPos(1, 5, 'K')
  val B5 = createPos(2, 5, 'L')
  val C5 = createPos(3, 5, 'M')
  val D5 = createPos(4, 5, 'N')
  val E5 = createPos(5, 5, 'O')
  val F5 = createPos(6, 5, 'P')
  val G5 = createPos(7, 5, 'Q')
  val H5 = createPos(8, 5, 'R')
  val I5 = createPos(9, 5, 'S')
  val A6 = createPos(1, 6, 'T')
  val B6 = createPos(2, 6, 'U')
  val C6 = createPos(3, 6, 'V')
  val D6 = createPos(4, 6, 'W')
  val E6 = createPos(5, 6, 'X')
  val F6 = createPos(6, 6, 'Y')
  val G6 = createPos(7, 6, 'Z')
  val H6 = createPos(8, 6, '0')
  val I6 = createPos(9, 6, '1')
  val A7 = createPos(1, 7, '2')
  val B7 = createPos(2, 7, '3')
  val C7 = createPos(3, 7, '4')
  val D7 = createPos(4, 7, '5')
  val E7 = createPos(5, 7, '6')
  val F7 = createPos(6, 7, '7')
  val G7 = createPos(7, 7, '8')
  val H7 = createPos(8, 7, '9')
  val I7 = createPos(9, 7, ':')
  val A8 = createPos(1, 8, 34)
  val B8 = createPos(2, 8, 35)
  val C8 = createPos(3, 8, 36)
  val D8 = createPos(4, 8, 37)
  val E8 = createPos(5, 8, 38)
  val F8 = createPos(6, 8, 39)
  val G8 = createPos(7, 8, 40)
  val H8 = createPos(8, 8, 41)
  val I8 = createPos(9, 8, 42)
  val A9 = createPos(1, 9, 43)
  val B9 = createPos(2, 9, 44)
  val C9 = createPos(3, 9, 45)
  val D9 = createPos(4, 9, 46)
  val E9 = createPos(5, 9, 47)
  val F9 = createPos(6, 9, 123)
  val G9 = createPos(7, 9, 124)
  val H9 = createPos(8, 9, 125)
  val I9 = createPos(9, 9, 126)

  val all = posCache.toList.flatten

  val whiteBackrank = (A1 <-> I1).toList
  val blackBackrank = (A9 <-> I9).toList

  val allKeys: Map[String, Pos] = all
    .map { pos =>
      pos.key -> pos
    }
    .to(Map)

  val allPiotrs: Map[Char, Pos] = all
    .map { pos =>
      pos.piotr -> pos
    }
    .to(Map)
}
