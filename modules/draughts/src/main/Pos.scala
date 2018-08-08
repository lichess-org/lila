package draughts

import scala.collection.breakOut

sealed case class Pos private (x: Int, y: Int, piotr: Char) {

  import Pos.{ posAt, movesDown, movesUp, movesHorizontal }

  val fieldNumber = x + 5 * (y - 1)

  lazy val moveDownLeft: Option[Pos] = movesDown.get(fieldNumber).map(_(0)).filter(_ > 0) flatMap posAt
  lazy val moveDownRight: Option[Pos] = movesDown.get(fieldNumber).map(_(1)).filter(_ > 0) flatMap posAt
  lazy val moveUpLeft: Option[Pos] = movesUp.get(fieldNumber).map(_(0)).filter(_ > 0) flatMap posAt
  lazy val moveUpRight: Option[Pos] = movesUp.get(fieldNumber).map(_(1)).filter(_ > 0) flatMap posAt

  lazy val moveDown: Option[Pos] = movesDown.get(fieldNumber).map(_(2)).filter(_ > 0) flatMap posAt
  lazy val moveUp: Option[Pos] = movesUp.get(fieldNumber).map(_(2)).filter(_ > 0) flatMap posAt
  lazy val moveLeft: Option[Pos] = movesHorizontal.get(fieldNumber).map(_(0)).filter(_ > 0) flatMap posAt
  lazy val moveRight: Option[Pos] = movesHorizontal.get(fieldNumber).map(_(1)).filter(_ > 0) flatMap posAt

  val key = f"${5 * (y - 1) + x}%02d"
  val shortKey = (5 * (y - 1) + x).toString
  val piotrStr = piotr.toString

  override val toString = key
  override val hashCode = 5 * (y - 1) + (x - 1)

}

object Pos {

  //Possible destinations: (left, right, straight)
  val movesDown = Map(
    1 -> Array(6, 7, 11),
    2 -> Array(7, 8, 12),
    3 -> Array(8, 9, 13),
    4 -> Array(9, 10, 14),
    5 -> Array(10, -1, 15),
    6 -> Array(-1, 11, 16),
    7 -> Array(11, 12, 17),
    8 -> Array(12, 13, 18),
    9 -> Array(13, 14, 19),
    10 -> Array(14, 15, 20),
    11 -> Array(16, 17, 21),
    12 -> Array(17, 18, 22),
    13 -> Array(18, 19, 23),
    14 -> Array(19, 20, 24),
    15 -> Array(20, -1, 25),
    16 -> Array(-1, 21, 26),
    17 -> Array(21, 22, 27),
    18 -> Array(22, 23, 28),
    19 -> Array(23, 24, 29),
    20 -> Array(24, 25, 30),
    21 -> Array(26, 27, 31),
    22 -> Array(27, 28, 32),
    23 -> Array(28, 29, 33),
    24 -> Array(29, 30, 34),
    25 -> Array(30, -1, 35),
    26 -> Array(-1, 31, 36),
    27 -> Array(31, 32, 37),
    28 -> Array(32, 33, 38),
    29 -> Array(33, 34, 39),
    30 -> Array(34, 35, 40),
    31 -> Array(36, 37, 41),
    32 -> Array(37, 38, 42),
    33 -> Array(38, 39, 43),
    34 -> Array(39, 40, 44),
    35 -> Array(40, -1, 45),
    36 -> Array(-1, 41, 46),
    37 -> Array(41, 42, 47),
    38 -> Array(42, 43, 48),
    39 -> Array(43, 44, 49),
    40 -> Array(44, 45, 50),
    41 -> Array(46, 47, -1),
    42 -> Array(47, 48, -1),
    43 -> Array(48, 49, -1),
    44 -> Array(49, 50, -1),
    45 -> Array(50, -1, -1)
  )

  //Possible destinations: (left, right, straight)
  val movesUp = Map(
    6 -> Array(-1, 1, -1),
    7 -> Array(1, 2, -1),
    8 -> Array(2, 3, -1),
    9 -> Array(3, 4, -1),
    10 -> Array(4, 5, -1),
    11 -> Array(6, 7, 1),
    12 -> Array(7, 8, 2),
    13 -> Array(8, 9, 3),
    14 -> Array(9, 10, 4),
    15 -> Array(10, -1, 5),
    16 -> Array(-1, 11, 6),
    17 -> Array(11, 12, 7),
    18 -> Array(12, 13, 8),
    19 -> Array(13, 14, 9),
    20 -> Array(14, 15, 10),
    21 -> Array(16, 17, 11),
    22 -> Array(17, 18, 12),
    23 -> Array(18, 19, 13),
    24 -> Array(19, 20, 14),
    25 -> Array(20, -1, 15),
    26 -> Array(-1, 21, 16),
    27 -> Array(21, 22, 17),
    28 -> Array(22, 23, 18),
    29 -> Array(23, 24, 19),
    30 -> Array(24, 25, 20),
    31 -> Array(26, 27, 21),
    32 -> Array(27, 28, 22),
    33 -> Array(28, 29, 23),
    34 -> Array(29, 30, 24),
    35 -> Array(30, -1, 25),
    36 -> Array(-1, 31, 26),
    37 -> Array(31, 32, 27),
    38 -> Array(32, 33, 28),
    39 -> Array(33, 34, 29),
    40 -> Array(34, 35, 30),
    41 -> Array(36, 37, 31),
    42 -> Array(37, 38, 32),
    43 -> Array(38, 39, 33),
    44 -> Array(39, 40, 34),
    45 -> Array(40, -1, 35),
    46 -> Array(-1, 41, 36),
    47 -> Array(41, 42, 37),
    48 -> Array(42, 43, 38),
    49 -> Array(43, 44, 39),
    50 -> Array(44, 45, 40)
  )

  //Possible destinations: (straight-left, straight-right)
  val movesHorizontal = Map(
    1 -> Array(-1, 2),
    2 -> Array(1, 3),
    3 -> Array(2, 4),
    4 -> Array(3, 5),
    5 -> Array(4, -1),
    6 -> Array(-1, 7),
    7 -> Array(6, 8),
    8 -> Array(7, 9),
    9 -> Array(8, 10),
    10 -> Array(9, -1),
    11 -> Array(-1, 12),
    12 -> Array(11, 13),
    13 -> Array(12, 14),
    14 -> Array(13, 15),
    15 -> Array(14, -1),
    16 -> Array(-1, 17),
    17 -> Array(16, 18),
    18 -> Array(17, 19),
    19 -> Array(18, 20),
    20 -> Array(19, -1),
    21 -> Array(-1, 22),
    22 -> Array(21, 23),
    23 -> Array(22, 24),
    24 -> Array(23, 25),
    25 -> Array(24, -1),
    26 -> Array(-1, 27),
    27 -> Array(26, 28),
    28 -> Array(27, 29),
    29 -> Array(28, 30),
    30 -> Array(29, -1),
    31 -> Array(-1, 32),
    32 -> Array(31, 33),
    33 -> Array(32, 34),
    34 -> Array(33, 35),
    35 -> Array(34, -1),
    36 -> Array(-1, 37),
    37 -> Array(36, 38),
    38 -> Array(37, 39),
    39 -> Array(38, 40),
    40 -> Array(39, -1),
    41 -> Array(-1, 42),
    42 -> Array(41, 43),
    43 -> Array(42, 44),
    44 -> Array(43, 45),
    45 -> Array(44, -1),
    46 -> Array(-1, 47),
    47 -> Array(46, 48),
    48 -> Array(47, 49),
    49 -> Array(48, 50),
    50 -> Array(49, -1)
  )

  val posCache = new Array[Some[Pos]](50)

  def posAt(x: Int, y: Int): Option[Pos] =
    if (x < 1 || x > 5 || y < 1 || y > 10) None
    else posCache(x + 5 * y - 6)

  def posAt(field: Int): Option[Pos] =
    if (field < 1 || field > 50) None
    else posCache(field - 1)

  def posAt(field: String): Option[Pos] = parseIntOption(field).flatMap(posAt)

  def piotr(c: Char): Option[Pos] = allPiotrs get c

  def keyToPiotr(key: String) = posAt(key) map (_.piotr)
  def doubleKeyToPiotr(key: String) = for {
    a ← keyToPiotr(key take 2)
    b ← keyToPiotr(key drop 2)
  } yield s"$a$b"
  def doublePiotrToKey(piotrs: String) = for {
    a ← piotr(piotrs.head)
    b ← piotr(piotrs(1))
  } yield s"${a.key}${b.key}"

  private[this] def createPos(x: Int, y: Int, piotr: Char): Pos = {
    val pos = new Pos(x, y, piotr)
    posCache(x + 5 * y - 6) = Some(pos)
    pos
  }

  val A1 = createPos(1, 1, 'a')
  val B1 = createPos(2, 1, 'b')
  val C1 = createPos(3, 1, 'c')
  val D1 = createPos(4, 1, 'd')
  val E1 = createPos(5, 1, 'e')
  val A2 = createPos(1, 2, 'f')
  val B2 = createPos(2, 2, 'g')
  val C2 = createPos(3, 2, 'h')
  val D2 = createPos(4, 2, 'i')
  val E2 = createPos(5, 2, 'j')
  val A3 = createPos(1, 3, 'k')
  val B3 = createPos(2, 3, 'l')
  val C3 = createPos(3, 3, 'm')
  val D3 = createPos(4, 3, 'n')
  val E3 = createPos(5, 3, 'o')
  val A4 = createPos(1, 4, 'p')
  val B4 = createPos(2, 4, 'q')
  val C4 = createPos(3, 4, 'r')
  val D4 = createPos(4, 4, 's')
  val E4 = createPos(5, 4, 't')
  val A5 = createPos(1, 5, 'u')
  val B5 = createPos(2, 5, 'v')
  val C5 = createPos(3, 5, 'w')
  val D5 = createPos(4, 5, 'x')
  val E5 = createPos(5, 5, 'y')
  val A6 = createPos(1, 6, 'z')
  val B6 = createPos(2, 6, 'A')
  val C6 = createPos(3, 6, 'B')
  val D6 = createPos(4, 6, 'C')
  val E6 = createPos(5, 6, 'D')
  val A7 = createPos(1, 7, 'E')
  val B7 = createPos(2, 7, 'F')
  val C7 = createPos(3, 7, 'G')
  val D7 = createPos(4, 7, 'H')
  val E7 = createPos(5, 7, 'I')
  val A8 = createPos(1, 8, 'J')
  val B8 = createPos(2, 8, 'K')
  val C8 = createPos(3, 8, 'L')
  val D8 = createPos(4, 8, 'M')
  val E8 = createPos(5, 8, 'N')
  val A9 = createPos(1, 9, 'O')
  val B9 = createPos(2, 9, 'P')
  val C9 = createPos(3, 9, 'Q')
  val D9 = createPos(4, 9, 'R')
  val E9 = createPos(5, 9, 'S')
  val A10 = createPos(1, 10, 'T')
  val B10 = createPos(2, 10, 'U')
  val C10 = createPos(3, 10, 'V')
  val D10 = createPos(4, 10, 'W')
  val E10 = createPos(5, 10, 'X')

  val all = posCache.toList.flatten

  val allKeys: Map[String, Pos] = all.map { pos => pos.key -> pos }(breakOut)

  val allPiotrs: Map[Char, Pos] = all.map { pos => pos.piotr -> pos }(breakOut)

}