package draughts
package format.pdn

import scala.util.Try
import scala.collection.breakOut

object Binary {

  def writeMove(m: String) = Try(Writer move m)
  def writeMoves(ms: Traversable[String]) = Try(Writer moves ms)

  def readMoves(bs: List[Byte]) = Try(Reader moves bs)
  def readMoves(bs: List[Byte], nb: Int) = Try(Reader.moves(bs, nb))

  private object MoveType {
    val IsMove = 0
    val IsCapture = 1
  }

  /*private object Encoding {
    val pieceInts: Map[String, Int] = Map("K" -> 1, "Q" -> 2, "R" -> 3, "N" -> 4, "B" -> 5, "O-O" -> 6, "O-O-O" -> 7)
    val pieceStrs: Map[Int, String] = (pieceInts map { case (k, v) => v -> k })(breakOut)
    val dropPieceInts: Map[String, Int] = Map("P" -> 1, "Q" -> 2, "R" -> 3, "N" -> 4, "B" -> 5)
    val dropPieceStrs: Map[Int, String] = (dropPieceInts map { case (k, v) => v -> k })(breakOut)
    val promotionInts: Map[String, Int] = Map("" -> 0, "Q" -> 1, "R" -> 2, "N" -> 3, "B" -> 4, "K" -> 6)
    val promotionStrs: Map[Int, String] = (promotionInts map { case (k, v) => v -> k })(breakOut)
    val checkInts: Map[String, Int] = Map("" -> 0, "+" -> 1, "#" -> 2)
    val checkStrs: Map[Int, String] = (checkInts map { case (k, v) => v -> k })(breakOut)
  }*/

  private object Reader {

    private val maxPlies = 600

    def moves(bs: List[Byte]): List[String] = moves(bs, maxPlies)
    def moves(bs: List[Byte], nb: Int): List[String] = intMoves(bs map toInt, nb, "x00")

    private def intMoves(bs: List[Int], pliesToGo: Int, lastUci: String): List[String] = bs match {
      case _ if pliesToGo < 0 => Nil
      case Nil => Nil
      case b1 :: b2 :: rest if moveType(b1) == MoveType.IsMove =>
        if (pliesToGo == 0)
          Nil
        else
          moveUci(b1, b2) :: intMoves(rest, pliesToGo - 1, "x00")
      case b1 :: b2 :: rest if moveType(b1) == MoveType.IsCapture =>
        val newUci = captureUci(b1, b2)
        if (lastUci.endsWith("x" + newUci.substring(0, newUci.indexOf('x'))))
          newUci :: intMoves(rest, pliesToGo, newUci)
        else if (pliesToGo == 0)
          Nil
        else
          newUci :: intMoves(rest, pliesToGo - 1, newUci)
      case x => !!(x map showByte mkString ",")
    }

    // 2 movetype
    // 6 srcPos
    // ----
    // 2 NOTHING
    // 6 dstPos
    def moveUci(b1: Int, b2: Int): String = s"${right(b1, 6)}-${right(b2, 6)}"
    def captureUci(b1: Int, b2: Int): String = s"${right(b1, 6)}x${right(b2, 6)}"

    private def moveType(i: Int) = i >> 6

    private def right(i: Int, x: Int): Int = i & lengthMasks(x)
    private val lengthMasks = Map(1 -> 0x01, 2 -> 0x03, 3 -> 0x07, 4 -> 0x0F, 5 -> 0x1F, 6 -> 0x3F, 7 -> 0x7F, 8 -> 0xFF)
    private def !!(msg: String) = throw new Exception("Binary reader failed: " + msg)

    //private def cut(i: Int, from: Int, to: Int): Int = right(i, from) >> to
    //private def bitAt(i: Int, p: Int): Boolean = cut(i, p, p - 1) != 0

  }

  private object Writer {

    def move(str: String): List[Byte] = (str match {
      case MoveUciR(src, dst) => moveUci(src, dst)
      case CaptureUciR(src, dst) => captureUci(src, dst)
      case _ =>
        draughtsLog("ERROR: Binary").info(s"Cannot encode $str")
        Nil
    }) map (_.toByte)

    def moves(strs: Traversable[String]): Array[Byte] = strs.flatMap(move)(breakOut)

    def moveUci(src: String, dst: String) = List(
      (MoveType.IsMove << 6) + src.toInt,
      dst.toInt
    )

    def captureUci(src: String, dst: String) = List(
      (MoveType.IsCapture << 6) + src.toInt,
      dst.toInt
    )

    val fieldR = "(\\d+)"
    val MoveUciR = s"$fieldR-$fieldR$$".r
    val CaptureUciR = s"${fieldR}x$fieldR$$".r

  }

  @inline private def toInt(b: Byte): Int = b & 0xff
  private def showByte(b: Int): String = "%08d" format (b.toBinaryString.toInt)

}
