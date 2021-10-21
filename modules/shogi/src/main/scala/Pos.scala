package shogi

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
  val usiKey   = (10 - x).toString + (96 + 10 - y).toChar.toString
  val color    = Color((x % 2 == 0) ^ (y % 2 == 0))
  val piotrStr = piotr.toString

  override val toString = key

  override val hashCode = 9 * (y - 1) + (x - 1)
}

object Pos {
  val posCache = new Array[Pos](81)

  def posAt(x: Int, y: Int): Option[Pos] =
    if (x < 1 || x > 9 || y < 1 || y > 9) None
    else posCache.lift(x + 9 * y - 10)

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
    posCache(x + 9 * y - 10) = pos
    pos
  }

  //
  // 9a 8a 7a 6a /5a 4a 3a 2a 1a
  // 9b 8b 7b 6b /5b 4b 3b 2b 1b
  // 9c 8c 7c 6c /5c 4c 3c 2c 1c
  // 9d 8d 7d 6d /5d 4d 3d 2d 1d
  // 9e 8e 7e 6e /5e 4e 3e 2e 1e
  // 9f 8f 7f 6f 5f 4f 3f 2f 1f
  // 9g 8g 7g 6g 5g 4g 3g 2g 1g
  // 9h 8h 7h 6h 5h 4h 3h 2h 1h
  // 9i 8i 7i 6i 5i 4i 3i 2i 1i
  //

  val SQ9I = createPos(1, 1, 'a')
  val SQ8I = createPos(2, 1, 'b')
  val SQ7I = createPos(3, 1, 'c')
  val SQ6I = createPos(4, 1, 'd')
  val SQ5I = createPos(5, 1, 'e')
  val SQ4I = createPos(6, 1, 'f')
  val SQ3I = createPos(7, 1, 'g')
  val SQ2I = createPos(8, 1, 'h')
  val SQ1I = createPos(9, 1, 'i')

  val SQ9H = createPos(1, 2, 'j')
  val SQ8H = createPos(2, 2, 'k')
  val SQ7H = createPos(3, 2, 'l')
  val SQ6H = createPos(4, 2, 'm')
  val SQ5H = createPos(5, 2, 'n')
  val SQ4H = createPos(6, 2, 'o')
  val SQ3H = createPos(7, 2, 'p')
  val SQ2H = createPos(8, 2, 'q')
  val SQ1H = createPos(9, 2, 'r')
  
  val SQ9G = createPos(1, 3, 's')
  val SQ8G = createPos(2, 3, 't')
  val SQ7G = createPos(3, 3, 'u')
  val SQ6G = createPos(4, 3, 'v')
  val SQ5G = createPos(5, 3, 'w')
  val SQ4G = createPos(6, 3, 'x')
  val SQ3G = createPos(7, 3, 'y')
  val SQ2G = createPos(8, 3, 'z')
  val SQ1G = createPos(9, 3, 'A')
  
  val SQ9F = createPos(1, 4, 'B')
  val SQ8F = createPos(2, 4, 'C')
  val SQ7F = createPos(3, 4, 'D')
  val SQ6F = createPos(4, 4, 'E')
  val SQ5F = createPos(5, 4, 'F')
  val SQ4F = createPos(6, 4, 'G')
  val SQ3F = createPos(7, 4, 'H')
  val SQ2F = createPos(8, 4, 'I')
  val SQ1F = createPos(9, 4, 'J')
  
  val SQ9E = createPos(1, 5, 'K')
  val SQ8E = createPos(2, 5, 'L')
  val SQ7E = createPos(3, 5, 'M')
  val SQ6E = createPos(4, 5, 'N')
  val SQ5E = createPos(5, 5, 'O')
  val SQ4E = createPos(6, 5, 'P')
  val SQ3E = createPos(7, 5, 'Q')
  val SQ2E = createPos(8, 5, 'R')
  val SQ1E = createPos(9, 5, 'S')

  val SQ9D = createPos(1, 6, 'T')
  val SQ8D = createPos(2, 6, 'U')
  val SQ7D = createPos(3, 6, 'V')
  val SQ6D = createPos(4, 6, 'W')
  val SQ5D = createPos(5, 6, 'X')
  val SQ4D = createPos(6, 6, 'Y')
  val SQ3D = createPos(7, 6, 'Z')
  val SQ2D = createPos(8, 6, '0')
  val SQ1D = createPos(9, 6, '1')

  val SQ9C = createPos(1, 7, '2')
  val SQ8C = createPos(2, 7, '3')
  val SQ7C = createPos(3, 7, '4')
  val SQ6C = createPos(4, 7, '5')
  val SQ5C = createPos(5, 7, '6')
  val SQ4C = createPos(6, 7, '7')
  val SQ3C = createPos(7, 7, '8')
  val SQ2C = createPos(8, 7, '9')
  val SQ1C = createPos(9, 7, ':')

  val SQ9B = createPos(1, 8, '"')
  val SQ8B = createPos(2, 8, '#')
  val SQ7B = createPos(3, 8, '$')
  val SQ6B = createPos(4, 8, '%')
  val SQ5B = createPos(5, 8, '&')
  val SQ4B = createPos(6, 8, '\'')
  val SQ3B = createPos(7, 8, '(')
  val SQ2B = createPos(8, 8, ')')
  val SQ1B = createPos(9, 8, '<')
  
  val SQ9A = createPos(1, 9, '+')
  val SQ8A = createPos(2, 9, ',')
  val SQ7A = createPos(3, 9, '-')
  val SQ6A = createPos(4, 9, '.')
  val SQ5A = createPos(5, 9, '/')
  val SQ4A = createPos(6, 9, '{')
  val SQ3A = createPos(7, 9, '|')
  val SQ2A = createPos(8, 9, '}')
  val SQ1A = createPos(9, 9, '~')

  val all = posCache.toList

  val senteBackrank = (SQ9I <-> SQ1I).toList
  val goteBackrank  = (SQ9A <-> SQ1A).toList

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

  val numberAllKeys = Pos.allKeys map { case (_, v) => ((10 - v.x).toString + (10 - v.y)) -> v }
  val usiAllKeys    = Pos.allKeys map { case (_, v) => ((10 - v.x).toString + (10 - v.y + 96).toChar) -> v }

}
