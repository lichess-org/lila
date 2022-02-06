package shogi

import scala.math.{ abs, max, min }

// Coordinate system starts at top right
// Directions are given from sente POV
case class Pos private (index: Int) extends AnyVal {

  def down: Option[Pos]      = Pos.at(file.index, rank.index + 1)
  def left: Option[Pos]      = Pos.at(file.index + 1, rank.index)
  def downLeft: Option[Pos]  = Pos.at(file.index + 1, rank.index + 1)
  def downRight: Option[Pos] = Pos.at(file.index - 1, rank.index + 1)
  def up: Option[Pos]        = Pos.at(file.index, rank.index - 1)
  def right: Option[Pos]     = Pos.at(file.index - 1, rank.index)
  def upLeft: Option[Pos]    = Pos.at(file.index + 1, rank.index - 1)
  def upRight: Option[Pos]   = Pos.at(file.index - 1, rank.index - 1)

  def >|(stop: Pos => Boolean): List[Pos] = |<>|(stop, _.right)
  def |<(stop: Pos => Boolean): List[Pos] = |<>|(stop, _.left)
  def |<>|(stop: Pos => Boolean, dir: Direction): List[Pos] =
    dir(this) map { p =>
      p :: (if (stop(p)) Nil else p.|<>|(stop, dir))
    } getOrElse Nil

  def ?<(other: Pos): Boolean = file > other.file
  def ?>(other: Pos): Boolean = file < other.file
  def ?+(other: Pos): Boolean = rank > other.rank
  def ?^(other: Pos): Boolean = rank < other.rank
  def ?|(other: Pos): Boolean = file == other.file
  def ?-(other: Pos): Boolean = rank == other.rank

  def <->(other: Pos): Seq[Pos] =
    min(file.index, other.file.index) to max(file.index, other.file.index) flatMap { Pos.at(_, rank.index) }

  // from down left corner to top right corner
  def upTo(other: Pos): Seq[Pos] =
    min(rank.index, other.rank.index) to max(rank.index, other.rank.index) flatMap {
      Pos.at(file.index, _)
    } flatMap { _ <-> other }

  def touches(other: Pos): Boolean = xDist(other) <= 1 && yDist(other) <= 1

  def onSameDiagonal(other: Pos): Boolean =
    file.index - rank.index == other.file.index - other.rank.index || file.index + rank.index == other.file.index + other.rank.index
  def onSameLine(other: Pos): Boolean = ?-(other) || ?|(other)

  def xDist(other: Pos) = abs(file - other.file)
  def yDist(other: Pos) = abs(rank - other.rank)

  @inline def file = File of this
  @inline def rank = Rank of this

  def usiKey    = file.toString + rank.toString
  def uciKey    = ((9 - file.index) + 96).toChar.toString + (9 - rank.index).toString // remove
  def numberKey = (file.index + 1).toString + (rank.index + 1).toString

  override def toString = usiKey
}

object Pos {

  val MaxFiles = 9
  val MaxRanks = 9

  def apply(index: Int): Option[Pos] =
    if (0 <= index && index < MaxFiles * MaxRanks) Some(new Pos(index))
    else None

  def apply(file: File, rank: Rank): Pos =
    new Pos(file.index + MaxFiles * rank.index)

  def at(x: Int, y: Int): Option[Pos] =
    if (0 <= x && x < MaxFiles && 0 <= y && y < MaxRanks)
      Some(new Pos(y * MaxFiles + x))
    else None

  def fromKey(key: String): Option[Pos] =
    allUsiKeys.get(key) orElse allUciKeys.get(key) orElse allNumberKeys.get(key)

  // 9a 8a 7a 6a 5a 4a 3a 2a 1a
  // 9b 8b 7b 6b 5b 4b 3b 2b 1b
  // 9c 8c 7c 6c 5c 4c 3c 2c 1c
  // 9d 8d 7d 6d 5d 4d 3d 2d 1d
  // 9e 8e 7e 6e 5e 4e 3e 2e 1e
  // 9f 8f 7f 6f 5f 4f 3f 2f 1f
  // 9g 8g 7g 6g 5g 4g 3g 2g 1g
  // 9h 8h 7h 6h 5h 4h 3h 2h 1h
  // 9i 8i 7i 6i 5i 4i 3i 2i 1i

  val SQ1A = new Pos(0)
  val SQ2A = new Pos(1)
  val SQ3A = new Pos(2)
  val SQ4A = new Pos(3)
  val SQ5A = new Pos(4)
  val SQ6A = new Pos(5)
  val SQ7A = new Pos(6)
  val SQ8A = new Pos(7)
  val SQ9A = new Pos(8)

  val SQ1B = new Pos(9)
  val SQ2B = new Pos(10)
  val SQ3B = new Pos(11)
  val SQ4B = new Pos(12)
  val SQ5B = new Pos(13)
  val SQ6B = new Pos(14)
  val SQ7B = new Pos(15)
  val SQ8B = new Pos(16)
  val SQ9B = new Pos(17)

  val SQ1C = new Pos(18)
  val SQ2C = new Pos(19)
  val SQ3C = new Pos(20)
  val SQ4C = new Pos(21)
  val SQ5C = new Pos(22)
  val SQ6C = new Pos(23)
  val SQ7C = new Pos(24)
  val SQ8C = new Pos(25)
  val SQ9C = new Pos(26)

  val SQ1D = new Pos(27)
  val SQ2D = new Pos(28)
  val SQ3D = new Pos(29)
  val SQ4D = new Pos(30)
  val SQ5D = new Pos(31)
  val SQ6D = new Pos(32)
  val SQ7D = new Pos(33)
  val SQ8D = new Pos(34)
  val SQ9D = new Pos(35)

  val SQ1E = new Pos(36)
  val SQ2E = new Pos(37)
  val SQ3E = new Pos(38)
  val SQ4E = new Pos(39)
  val SQ5E = new Pos(40)
  val SQ6E = new Pos(41)
  val SQ7E = new Pos(42)
  val SQ8E = new Pos(43)
  val SQ9E = new Pos(44)

  val SQ1F = new Pos(45)
  val SQ2F = new Pos(46)
  val SQ3F = new Pos(47)
  val SQ4F = new Pos(48)
  val SQ5F = new Pos(49)
  val SQ6F = new Pos(50)
  val SQ7F = new Pos(51)
  val SQ8F = new Pos(52)
  val SQ9F = new Pos(53)

  val SQ1G = new Pos(54)
  val SQ2G = new Pos(55)
  val SQ3G = new Pos(56)
  val SQ4G = new Pos(57)
  val SQ5G = new Pos(58)
  val SQ6G = new Pos(59)
  val SQ7G = new Pos(60)
  val SQ8G = new Pos(61)
  val SQ9G = new Pos(62)

  val SQ1H = new Pos(63)
  val SQ2H = new Pos(64)
  val SQ3H = new Pos(65)
  val SQ4H = new Pos(66)
  val SQ5H = new Pos(67)
  val SQ6H = new Pos(68)
  val SQ7H = new Pos(69)
  val SQ8H = new Pos(70)
  val SQ9H = new Pos(71)

  val SQ1I = new Pos(72)
  val SQ2I = new Pos(73)
  val SQ3I = new Pos(74)
  val SQ4I = new Pos(75)
  val SQ5I = new Pos(76)
  val SQ6I = new Pos(77)
  val SQ7I = new Pos(78)
  val SQ8I = new Pos(79)
  val SQ9I = new Pos(80)

  val all: List[Pos] = (0 until (MaxFiles * MaxRanks)).map(new Pos(_)).toList

  val allDirections: Directions =
    List(_.up, _.down, _.left, _.right, _.upLeft, _.upRight, _.downLeft, _.downRight)

  val allUsiKeys: Map[String, Pos] = all.map { pos =>
    pos.usiKey -> pos
  }.toMap

  val allUciKeys: Map[String, Pos] = all.map { pos =>
    pos.uciKey -> pos
  }.toMap

  val allNumberKeys: Map[String, Pos] = all.map { pos =>
    pos.numberKey -> pos
  }.toMap

}
