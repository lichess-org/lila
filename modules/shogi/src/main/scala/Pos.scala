package shogi

import scala.math.{ abs, max, min }

sealed case class Pos private (x: Int, y: Int, piotr: Char) {

  val down: Option[Pos]         = Pos.at(x, y + 1)
  val left: Option[Pos]         = Pos.at(x + 1, y)
  val downLeft: Option[Pos]     = Pos.at(x + 1, y + 1)
  val downRight: Option[Pos]    = Pos.at(x - 1, y + 1)
  lazy val up: Option[Pos]      = Pos.at(x, y - 1)
  lazy val right: Option[Pos]   = Pos.at(x - 1, y)
  lazy val upLeft: Option[Pos]  = Pos.at(x + 1, y - 1)
  lazy val upRight: Option[Pos] = Pos.at(x - 1, y - 1)

  def >|(stop: Pos => Boolean): List[Pos] = |<>|(stop, _.right)
  def |<(stop: Pos => Boolean): List[Pos] = |<>|(stop, _.left)
  def |<>|(stop: Pos => Boolean, dir: Direction): List[Pos] =
    dir(this) map { p =>
      p :: (if (stop(p)) Nil else p.|<>|(stop, dir))
    } getOrElse Nil

  def ?<(other: Pos): Boolean = x > other.x
  def ?>(other: Pos): Boolean = x < other.x
  def ?+(other: Pos): Boolean = y > other.y
  def ?^(other: Pos): Boolean = y < other.y
  def ?|(other: Pos): Boolean = x == other.x
  def ?-(other: Pos): Boolean = y == other.y

  def <->(other: Pos): Seq[Pos] =
    min(x, other.x) to max(x, other.x) flatMap { Pos.at(_, y) }

  // from down left corner to top right corner
  def upTo(other: Pos): Seq[Pos] =
    min(y, other.y) to max(y, other.y) flatMap { Pos.at(x, _) } flatMap { _ <-> other }

  def touches(other: Pos): Boolean = xDist(other) <= 1 && yDist(other) <= 1

  def onSameDiagonal(other: Pos): Boolean = x - y == other.x - other.y || x + y == other.x + other.y
  def onSameLine(other: Pos): Boolean     = ?-(other) || ?|(other)

  def xDist(other: Pos) = abs(x - other.x)
  def yDist(other: Pos) = abs(y - other.y)

  val uciFile = ((10 - x) + 96).toChar.toString
  val uciRank = (10 - y).toString
  val uciKey  = uciFile + uciRank

  val numberKey = x.toString + y.toString

  val usiKey   = x.toString + (96 + y).toChar.toString
  val piotrStr = piotr.toString

  override val toString = usiKey

  override val hashCode = 9 * ((10 - y) - 1) + ((10 - x) - 1)
}

object Pos {
  val posCache = new Array[Pos](81)

  def at(x: Int, y: Int): Option[Pos] =
    if (x < 1 || x > 9 || y < 1 || y > 9) None
    else posCache.lift(x + 9 * y - 10)

  def fromKey(key: String): Option[Pos] =
    allUsiKeys.get(key) orElse allUciKeys.get(key) orElse allNumberKeys.get(key)

  def piotr(c: Char): Option[Pos] = allPiotrs get c

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

  val SQ9I = createPos(9, 9, 'a')
  val SQ8I = createPos(8, 9, 'b')
  val SQ7I = createPos(7, 9, 'c')
  val SQ6I = createPos(6, 9, 'd')
  val SQ5I = createPos(5, 9, 'e')
  val SQ4I = createPos(4, 9, 'f')
  val SQ3I = createPos(3, 9, 'g')
  val SQ2I = createPos(2, 9, 'h')
  val SQ1I = createPos(1, 9, 'i')

  val SQ9H = createPos(9, 8, 'j')
  val SQ8H = createPos(8, 8, 'k')
  val SQ7H = createPos(7, 8, 'l')
  val SQ6H = createPos(6, 8, 'm')
  val SQ5H = createPos(5, 8, 'n')
  val SQ4H = createPos(4, 8, 'o')
  val SQ3H = createPos(3, 8, 'p')
  val SQ2H = createPos(2, 8, 'q')
  val SQ1H = createPos(1, 8, 'r')

  val SQ9G = createPos(9, 7, 's')
  val SQ8G = createPos(8, 7, 't')
  val SQ7G = createPos(7, 7, 'u')
  val SQ6G = createPos(6, 7, 'v')
  val SQ5G = createPos(5, 7, 'w')
  val SQ4G = createPos(4, 7, 'x')
  val SQ3G = createPos(3, 7, 'y')
  val SQ2G = createPos(2, 7, 'z')
  val SQ1G = createPos(1, 7, 'A')

  val SQ9F = createPos(9, 6, 'B')
  val SQ8F = createPos(8, 6, 'C')
  val SQ7F = createPos(7, 6, 'D')
  val SQ6F = createPos(6, 6, 'E')
  val SQ5F = createPos(5, 6, 'F')
  val SQ4F = createPos(4, 6, 'G')
  val SQ3F = createPos(3, 6, 'H')
  val SQ2F = createPos(2, 6, 'I')
  val SQ1F = createPos(1, 6, 'J')

  val SQ9E = createPos(9, 5, 'K')
  val SQ8E = createPos(8, 5, 'L')
  val SQ7E = createPos(7, 5, 'M')
  val SQ6E = createPos(6, 5, 'N')
  val SQ5E = createPos(5, 5, 'O')
  val SQ4E = createPos(4, 5, 'P')
  val SQ3E = createPos(3, 5, 'Q')
  val SQ2E = createPos(2, 5, 'R')
  val SQ1E = createPos(1, 5, 'S')

  val SQ9D = createPos(9, 4, 'T')
  val SQ8D = createPos(8, 4, 'U')
  val SQ7D = createPos(7, 4, 'V')
  val SQ6D = createPos(6, 4, 'W')
  val SQ5D = createPos(5, 4, 'X')
  val SQ4D = createPos(4, 4, 'Y')
  val SQ3D = createPos(3, 4, 'Z')
  val SQ2D = createPos(2, 4, '0')
  val SQ1D = createPos(1, 4, '1')

  val SQ9C = createPos(9, 3, '2')
  val SQ8C = createPos(8, 3, '3')
  val SQ7C = createPos(7, 3, '4')
  val SQ6C = createPos(6, 3, '5')
  val SQ5C = createPos(5, 3, '6')
  val SQ4C = createPos(4, 3, '7')
  val SQ3C = createPos(3, 3, '8')
  val SQ2C = createPos(2, 3, '9')
  val SQ1C = createPos(1, 3, ':')

  val SQ9B = createPos(9, 2, '"')
  val SQ8B = createPos(8, 2, '#')
  val SQ7B = createPos(7, 2, '$')
  val SQ6B = createPos(6, 2, '%')
  val SQ5B = createPos(5, 2, '&')
  val SQ4B = createPos(4, 2, '\'')
  val SQ3B = createPos(3, 2, '(')
  val SQ2B = createPos(2, 2, ')')
  val SQ1B = createPos(1, 2, '<')

  val SQ9A = createPos(9, 1, '+')
  val SQ8A = createPos(8, 1, ',')
  val SQ7A = createPos(7, 1, '-')
  val SQ6A = createPos(6, 1, '.')
  val SQ5A = createPos(5, 1, '/')
  val SQ4A = createPos(4, 1, '{')
  val SQ3A = createPos(3, 1, '|')
  val SQ2A = createPos(2, 1, '}')
  val SQ1A = createPos(1, 1, '~')

  val all9x9 = (SQ9I upTo SQ1A).toList
  val all5x5 = (SQ5E upTo SQ1A).toList

  val reversedAll9x9 = all9x9.reverse
  val reversedAll5x5 = all5x5.reverse

  val allDirections =
    List(_.up, _.down, _.left, _.right, _.upLeft, _.upRight, _.downLeft, _.downRight): Directions

  val allUsiKeys: Map[String, Pos] = all9x9
    .map { pos =>
      pos.usiKey -> pos
    }
    .to(Map)

  val allUciKeys: Map[String, Pos] = all9x9
    .map { pos =>
      pos.uciKey -> pos
    }
    .to(Map)

  val allNumberKeys: Map[String, Pos] = all9x9
    .map { pos =>
      pos.numberKey -> pos
    }
    .to(Map)

  val allPiotrs: Map[Char, Pos] = all9x9
    .map { pos =>
      pos.piotr -> pos
    }
    .to(Map)

}
