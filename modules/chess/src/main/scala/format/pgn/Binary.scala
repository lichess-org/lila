package chess
package format.pgn

import scala.util.Try

object Binary {

  def writeMove(m: String)             = Try(Writer move m)
  def writeMoves(ms: Iterable[String]) = Try(Writer moves ms)

  def readMoves(bs: List[Byte])          = Try(Reader moves bs)
  def readMoves(bs: List[Byte], nb: Int) = Try(Reader.moves(bs, nb))

  private object MoveType {
    val SimplePiece = 0
    val FullPiece   = 1
  }

  private object Encoding {
    val pieceInts: Map[String, Int] =
      Map("P" -> 0, "K" -> 1, "G" -> 2, "S" -> 3, "N" -> 4, "L" -> 5, "B" -> 6, "R" -> 7, "T" -> 8, "U" -> 9, "M" -> 10, "A" -> 11, "H" -> 12, "D" -> 13)
    val pieceStrs: Map[Int, String]     = pieceInts map { case (k, v) => v -> k }
    val dropPieceInts: Map[String, Int] = Map("P" -> 1, "G" -> 2, "S" -> 3, "N" -> 4, "L" -> 5, "B" -> 6, "R" -> 7)
    val dropPieceStrs: Map[Int, String] = dropPieceInts map { case (k, v) => v -> k }
    val promotionInts: Map[String, Int] = Map("" -> 0, "T" -> 1, "U" -> 2, "M" -> 3, "A" -> 4, "H" -> 6, "D" -> 7)
    val promotionStrs: Map[Int, String] = promotionInts map { case (k, v) => v -> k }
    val checkInts: Map[String, Int]     = Map("" -> 0, "+" -> 1, "=" -> 2)
    // Dont's question this, mistakes were made
    val checkStrs: Map[Int, String]     = (checkInts map { case (k, v) => v -> k }) ++ Map(3 -> "+")
  }

  private object Reader {

    import Encoding._

    private val maxPlies = 600

    def moves(bs: List[Byte]): List[String] = moves(bs, maxPlies)
    def moves(bs: List[Byte], nb: Int): List[String] = intMoves(bs map toInt, nb)

    def intMoves(bs: List[Int], pliesToGo: Int): List[String] = {
      bs match {
        case _ if pliesToGo <= 0 => Nil
        case Nil                 => Nil
        case b1 :: b2 :: rest if (moveType(b1) == MoveType.SimplePiece) =>
          simplePiece(b1, b2) :: intMoves(rest, pliesToGo - 1)
        case b1 :: b2 :: b3 :: rest if (moveType(b1) == MoveType.FullPiece) =>
          fullPiece(b1, b2, b3) :: intMoves(rest, pliesToGo - 1)
        case x => !!(x map showByte mkString ",")
      }
    }

    // 1 movetype
    // 7 pos
    // ----
    // 4 role
    // 2 check
    // 1 capture
    // 1 drop
    def simplePiece(b1: Int, b2: Int): String = {
      if (bitAt(b2, 1)) drop(b1, b2)
      else
        pieceStrs(b2 >> 4) match {
          case piece => {
            val pos     = posString(right(b1, 7))
            val capture = if (bitAt(b2, 2)) "x" else ""
            val check   = checkStrs(cut(b2, 4, 2))
            s"$piece$capture$pos$check"
          }
        }
    }
    def drop(b1: Int, b2: Int): String = {
      val piece = dropPieceStrs(b2 >> 5)
      val pos   = posString(right(b1, 7))
      s"$piece*$pos"
    }

    def fullPiece(b1: Int, b2: Int, b3: Int): String = {
      pieceStrs(b2 >> 4) match {
        case piece => {
          val from    = posString(right(b3, 7))
          val pos     = posString(right(b1, 7))
          val check   = checkStrs(cut(b2, 4, 2))
          // Dont's question this, mistakes were made
          if(!bitAt(b2, 1)){
            val capture = if (bitAt(b2, 3)) "x" else ""
            s"$piece$from$capture$pos$check"
          }
          else{
            val capture = if (bitAt(b2, 2)) "x" else ""
            s"$piece$from$capture$pos$check"
          }
        }
      }
    }

    private def moveType(i: Int)  = i >> 7
    private def posString(i: Int) = fileChar(i / 9).toString + rankChar(i % 9)
    private def fileChar(i: Int)  = (i + 97).toChar
    private def rankChar(i: Int)  = (i + 49).toChar

    // Returns x right bits from i
    private def right(i: Int, x: Int): Int           = i & lengthMasks(x)
    // Shifts i from from bit to to bit
    private def cut(i: Int, from: Int, to: Int): Int = right(i, from) >> to
    // Return the p bit from i
    private def bitAt(i: Int, p: Int): Boolean       = cut(i, p, p - 1) != 0
    private val lengthMasks =
      Map(1 -> 0x01, 2 -> 0x03, 3 -> 0x07, 4 -> 0x0f, 5 -> 0x1f, 6 -> 0x3f, 7 -> 0x7f, 8 -> 0xff)
    private def !!(msg: String) = throw new Exception("Binary reader failed: " + msg)
  }

  private object Writer {

    import Encoding._

    def move(str: String): List[Byte] = {
      (str match {
        case SimplePieceR(piece, capture, pos, check) =>
          simplePiece(piece, pos, capture, check)
        case FullPieceR(piece, orig, capture, pos, check) =>
          fullPiece(piece, orig, pos, capture, check)
        case DropR(role, pos) => drop(role, pos)
      }) map (_.toByte)
    }

    def moves(strs: Iterable[String]): Array[Byte] = strs.flatMap(move).to(Array)

    // first bit of the first byte set to 0 means the moves takes two bytes - SimplePice,
    // first bit set to 1 means the moves takes 3 bytes - FullPiece

    // 1. 0(1), position(7); 2. pieceType(4), check(2), capture(1), dropFlag-0(1)
    def simplePiece(piece: String, pos: String, capture: String, check: String) =
      List(
        posInt(pos),
        (pieceInts(piece) << 4) + (checkInts(check) << 2) + (boolInt(capture) << 1)
      )

    // 1. 0(1), position(7); 2. pieceType(3), check(2), 0(2), dropFlag-1(1)
    def drop(piece: String, pos: String) =
      List(
        posInt(pos),
        (dropPieceInts(piece) << 5) + 1
      )

    // 1. 1(1), positionTo(7); 2. pieceType(4), check(2), capture(1), 0(0); 3. positionFrom(7)
    def fullPiece(piece: String, orig: String, pos: String, capture: String, check: String) =
      List(
        (1 << 7) + posInt(pos),
        (pieceInts(piece) << 4) + (checkInts(check) << 2) + (boolInt(capture) << 1) + 1,
        posInt(orig)
      )

    def boolInt(s: String): Int  = if (s.nonEmpty) 1 else 0
    def boolInt(b: Boolean): Int = if (b) 1 else 0

    def posInt(pos: String): Int    = posInt(fileInt(pos.head), rankInt(pos(1)))
    def posInt(x: Int, y: Int): Int = (9 * x) + y

    def fileInt(c: Char): Int = c.toInt - 97
    def rankInt(c: Char): Int = c.toInt - 49

    def shiftOptionInt(fileOption: Option[String], pos: String): Int =
      fileOption.fold(0) { file =>
        if (file.head < pos.head) 1 else 2
      }

    val pieceR       = "([KRNBSLAMUDHTGP])"
    //val fileR        = "(?:([a-i])x)?"
    val posR         = "([a-i][1-9])"
    val captureR     = "(x?)"
    val checkR       = "([\\+=]?)"
    //val promotionR   = "(?:\\=?([AMUDHT]))?"
    val origR        = "([a-i][1-9])".r

    // todo - checkR will be promotion
    //val SimplePieceR = s"^$pieceR$captureR$posR$checkR$promotionR$$".r
    val SimplePieceR = s"^$pieceR$captureR$posR$checkR$$".r
    //val FullPieceR   = s"^$pieceR$origR$captureR$posR$checkR$promotionR$$".r
    val FullPieceR   = s"^$pieceR$origR$captureR$posR$checkR$$".r
    val DropR        = s"^([RNBSGLP])\\*$posR$$".r
  }

  @inline private def toInt(b: Byte): Int = b & 0xff
  private def showByte(b: Int): String    = "%08d" format (b.toBinaryString.toInt)
}
