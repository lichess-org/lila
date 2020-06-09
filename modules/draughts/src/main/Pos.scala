package draughts

import scala.collection.breakOut

sealed abstract class Pos(val fieldNumber: Int) {

  val piotr = Piotr.byField(fieldNumber)
  val key = f"${fieldNumber}%02d"
  val shortKey = fieldNumber.toString
  val piotrStr = piotr.toString

  override val toString = key
  override val hashCode = fieldNumber - 1
  override def equals(other: Any) = other match {
    case u: Pos => fieldNumber == u.fieldNumber
    case _ => false
  }
}

sealed abstract class PosMotion(field: Int) extends Pos(field) {
  val x: Int
  val y: Int
  val moveDownLeft: Option[PosMotion]
  val moveDownRight: Option[PosMotion]
  val moveUpLeft: Option[PosMotion]
  val moveUpRight: Option[PosMotion]
  val moveDown: Option[PosMotion]
  val moveUp: Option[PosMotion]
  val moveLeft: Option[PosMotion]
  val moveRight: Option[PosMotion]
}

sealed case class Pos100 private (x: Int, y: Int) extends PosMotion(5 * (y - 1) + x) {

  import Pos100.{ posAt, movesDown, movesUp, movesHorizontal }

  lazy val moveDownLeft = movesDown.get(fieldNumber).map(_(0)).filter(_ > 0) flatMap posAt
  lazy val moveDownRight = movesDown.get(fieldNumber).map(_(1)).filter(_ > 0) flatMap posAt
  lazy val moveUpLeft = movesUp.get(fieldNumber).map(_(0)).filter(_ > 0) flatMap posAt
  lazy val moveUpRight = movesUp.get(fieldNumber).map(_(1)).filter(_ > 0) flatMap posAt

  lazy val moveDown = movesDown.get(fieldNumber).map(_(2)).filter(_ > 0) flatMap posAt
  lazy val moveUp = movesUp.get(fieldNumber).map(_(2)).filter(_ > 0) flatMap posAt
  lazy val moveLeft = movesHorizontal.get(fieldNumber).map(_(0)).filter(_ > 0) flatMap posAt
  lazy val moveRight = movesHorizontal.get(fieldNumber).map(_(1)).filter(_ > 0) flatMap posAt
}

sealed case class Pos64 private (x: Int, y: Int) extends PosMotion(4 * (y - 1) + x) {

  import Pos64.{ posAt, movesDown, movesUp, movesHorizontal }

  lazy val moveDownLeft = movesDown.get(fieldNumber).map(_(0)).filter(_ > 0) flatMap posAt
  lazy val moveDownRight = movesDown.get(fieldNumber).map(_(1)).filter(_ > 0) flatMap posAt
  lazy val moveUpLeft = movesUp.get(fieldNumber).map(_(0)).filter(_ > 0) flatMap posAt
  lazy val moveUpRight = movesUp.get(fieldNumber).map(_(1)).filter(_ > 0) flatMap posAt

  lazy val moveDown = movesDown.get(fieldNumber).map(_(2)).filter(_ > 0) flatMap posAt
  lazy val moveUp = movesUp.get(fieldNumber).map(_(2)).filter(_ > 0) flatMap posAt
  lazy val moveLeft = movesHorizontal.get(fieldNumber).map(_(0)).filter(_ > 0) flatMap posAt
  lazy val moveRight = movesHorizontal.get(fieldNumber).map(_(1)).filter(_ > 0) flatMap posAt
}

sealed trait BoardPos {
  val all: List[PosMotion]
  def posAt(x: Int, y: Int): Option[PosMotion]
  def posAt(field: Int): Option[PosMotion]
  def posAt(field: String): Option[PosMotion]
  def piotr(c: Char): Option[PosMotion]
  val hasAlgebraic: Boolean
  def algebraic(field: Int): Option[String]
  def algebraic(field: String): Option[String] = parseIntOption(field) flatMap algebraic
}

object Pos100 extends BoardPos {

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

  val posCache = new Array[Some[PosMotion]](50)

  def posAt(x: Int, y: Int): Option[PosMotion] =
    if (x < 1 || x > 5 || y < 1 || y > 10) None
    else posCache(x + 5 * y - 6)

  def posAt(field: Int): Option[PosMotion] =
    if (field < 1 || field > 50) None
    else posCache(field - 1)

  def posAt(field: String): Option[PosMotion] = parseIntOption(field) flatMap posAt

  def piotr(c: Char): Option[PosMotion] = allPiotrs get c

  val hasAlgebraic = false
  def algebraic(field: Int) = posAt(field) map { _.toString }

  private[this] def createPos(x: Int, y: Int): Pos100 = {
    val pos = new Pos100(x, y)
    posCache(pos.hashCode) = Some(pos)
    pos
  }

  val A1 = createPos(1, 1)
  val B1 = createPos(2, 1)
  val C1 = createPos(3, 1)
  val D1 = createPos(4, 1)
  val E1 = createPos(5, 1)
  val A2 = createPos(1, 2)
  val B2 = createPos(2, 2)
  val C2 = createPos(3, 2)
  val D2 = createPos(4, 2)
  val E2 = createPos(5, 2)
  val A3 = createPos(1, 3)
  val B3 = createPos(2, 3)
  val C3 = createPos(3, 3)
  val D3 = createPos(4, 3)
  val E3 = createPos(5, 3)
  val A4 = createPos(1, 4)
  val B4 = createPos(2, 4)
  val C4 = createPos(3, 4)
  val D4 = createPos(4, 4)
  val E4 = createPos(5, 4)
  val A5 = createPos(1, 5)
  val B5 = createPos(2, 5)
  val C5 = createPos(3, 5)
  val D5 = createPos(4, 5)
  val E5 = createPos(5, 5)
  val A6 = createPos(1, 6)
  val B6 = createPos(2, 6)
  val C6 = createPos(3, 6)
  val D6 = createPos(4, 6)
  val E6 = createPos(5, 6)
  val A7 = createPos(1, 7)
  val B7 = createPos(2, 7)
  val C7 = createPos(3, 7)
  val D7 = createPos(4, 7)
  val E7 = createPos(5, 7)
  val A8 = createPos(1, 8)
  val B8 = createPos(2, 8)
  val C8 = createPos(3, 8)
  val D8 = createPos(4, 8)
  val E8 = createPos(5, 8)
  val A9 = createPos(1, 9)
  val B9 = createPos(2, 9)
  val C9 = createPos(3, 9)
  val D9 = createPos(4, 9)
  val E9 = createPos(5, 9)
  val A10 = createPos(1, 10)
  val B10 = createPos(2, 10)
  val C10 = createPos(3, 10)
  val D10 = createPos(4, 10)
  val E10 = createPos(5, 10)

  val all = posCache.toList.flatten

  val allKeys: Map[String, PosMotion] = all.map { pos => pos.key -> pos }(breakOut)

  val allPiotrs: Map[Char, PosMotion] = all.map { pos => pos.piotr -> pos }(breakOut)

}

object Pos64 extends BoardPos {

  //Possible destinations: (left, right, straight)
  val movesDown = Map(
    1 -> Array(5, 6, 9),
    2 -> Array(6, 7, 10),
    3 -> Array(7, 8, 11),
    4 -> Array(8, -1, 12),
    5 -> Array(-1, 9, 13),
    6 -> Array(9, 10, 14),
    7 -> Array(10, 11, 15),
    8 -> Array(11, 12, 16),
    9 -> Array(13, 14, 17),
    10 -> Array(14, 15, 18),
    11 -> Array(15, 16, 19),
    12 -> Array(16, -1, 20),
    13 -> Array(-1, 17, 21),
    14 -> Array(17, 18, 22),
    15 -> Array(18, 19, 23),
    16 -> Array(19, 20, 24),
    17 -> Array(21, 22, 25),
    18 -> Array(22, 23, 26),
    19 -> Array(23, 24, 27),
    20 -> Array(24, -1, 28),
    21 -> Array(-1, 25, 29),
    22 -> Array(25, 26, 30),
    23 -> Array(26, 27, 31),
    24 -> Array(27, 28, 32),
    25 -> Array(29, 30, -1),
    26 -> Array(30, 31, -1),
    27 -> Array(31, 32, -1),
    28 -> Array(32, -1, -1)
  )

  //Possible destinations: (left, right, straight)
  val movesUp = Map(
    5 -> Array(-1, 1, -1),
    6 -> Array(1, 2, -1),
    7 -> Array(2, 3, -1),
    8 -> Array(3, 4, -1),
    9 -> Array(5, 6, 1),
    10 -> Array(6, 7, 2),
    11 -> Array(7, 8, 3),
    12 -> Array(8, -1, 4),
    13 -> Array(-1, 9, 5),
    14 -> Array(9, 10, 6),
    15 -> Array(10, 11, 7),
    16 -> Array(11, 12, 8),
    17 -> Array(13, 14, 9),
    18 -> Array(14, 15, 10),
    19 -> Array(15, 16, 11),
    20 -> Array(16, -1, 12),
    21 -> Array(-1, 17, 13),
    22 -> Array(17, 18, 14),
    23 -> Array(18, 19, 15),
    24 -> Array(19, 20, 16),
    25 -> Array(21, 22, 17),
    26 -> Array(22, 23, 18),
    27 -> Array(23, 24, 19),
    28 -> Array(24, -1, 20),
    29 -> Array(-1, 25, 21),
    30 -> Array(25, 26, 22),
    31 -> Array(26, 27, 23),
    32 -> Array(27, 28, 24)
  )

  //Possible destinations: (straight-left, straight-right)
  val movesHorizontal = Map(
    1 -> Array(-1, 2),
    2 -> Array(1, 3),
    3 -> Array(2, 4),
    4 -> Array(3, -1),
    5 -> Array(-1, 6),
    6 -> Array(5, 7),
    7 -> Array(6, 8),
    8 -> Array(7, -1),
    9 -> Array(-1, 10),
    10 -> Array(9, 11),
    11 -> Array(10, 12),
    12 -> Array(11, -1),
    13 -> Array(-1, 14),
    14 -> Array(13, 15),
    15 -> Array(14, 16),
    16 -> Array(15, -1),
    17 -> Array(-1, 18),
    18 -> Array(17, 19),
    19 -> Array(18, 20),
    20 -> Array(19, -1),
    21 -> Array(-1, 22),
    22 -> Array(21, 23),
    23 -> Array(22, 24),
    24 -> Array(23, -1),
    25 -> Array(-1, 26),
    26 -> Array(25, 27),
    27 -> Array(26, 28),
    28 -> Array(27, -1),
    29 -> Array(-1, 30),
    30 -> Array(29, 31),
    31 -> Array(30, 32),
    32 -> Array(31, -1)
  )

  val posCache = new Array[Some[PosMotion]](32)

  private lazy val alg2pos: Map[String, PosMotion] = posCache.map { p =>
    val pos = p.get
    val algY = 9 - pos.y
    val algX = pos.x * 2 - algY % 2
    s"${(96 + algX).toChar}$algY" -> pos
  }(scala.collection.breakOut)

  private lazy val field2alg: Map[Int, String] = posCache.map { p =>
    val pos = p.get
    val algY = 9 - pos.y
    val algX = pos.x * 2 - algY % 2
    pos.fieldNumber -> s"${(96 + algX).toChar}$algY"
  }(scala.collection.breakOut)

  def posAt(x: Int, y: Int): Option[PosMotion] =
    if (x < 1 || x > 4 || y < 1 || y > 8) None
    else posCache(x + 4 * y - 5)

  def posAt(field: Int): Option[PosMotion] =
    if (field < 1 || field > 32) None
    else posCache(field - 1)

  def posAt(field: String): Option[PosMotion] =
    parseIntOption(field).fold(alg2pos get field)(posAt)

  def piotr(c: Char): Option[PosMotion] = allPiotrs get c

  val hasAlgebraic = true
  def algebraic(field: Int) = field2alg get field

  private[this] def createPos(x: Int, y: Int): Pos64 = {
    val pos = new Pos64(x, y)
    posCache(pos.hashCode) = Some(pos)
    pos
  }

  val A1 = createPos(1, 1)
  val B1 = createPos(2, 1)
  val C1 = createPos(3, 1)
  val D1 = createPos(4, 1)
  val A2 = createPos(1, 2)
  val B2 = createPos(2, 2)
  val C2 = createPos(3, 2)
  val D2 = createPos(4, 2)
  val A3 = createPos(1, 3)
  val B3 = createPos(2, 3)
  val C3 = createPos(3, 3)
  val D3 = createPos(4, 3)
  val A4 = createPos(1, 4)
  val B4 = createPos(2, 4)
  val C4 = createPos(3, 4)
  val D4 = createPos(4, 4)
  val A5 = createPos(1, 5)
  val B5 = createPos(2, 5)
  val C5 = createPos(3, 5)
  val D5 = createPos(4, 5)
  val A6 = createPos(1, 6)
  val B6 = createPos(2, 6)
  val C6 = createPos(3, 6)
  val D6 = createPos(4, 6)
  val A7 = createPos(1, 7)
  val B7 = createPos(2, 7)
  val C7 = createPos(3, 7)
  val D7 = createPos(4, 7)
  val A8 = createPos(1, 8)
  val B8 = createPos(2, 8)
  val C8 = createPos(3, 8)
  val D8 = createPos(4, 8)

  val all = posCache.toList.flatten

  val allKeys: Map[String, PosMotion] = all.map { pos => pos.key -> pos }(breakOut)

  val allPiotrs: Map[Char, PosMotion] = all.map { pos => pos.piotr -> pos }(breakOut)

}