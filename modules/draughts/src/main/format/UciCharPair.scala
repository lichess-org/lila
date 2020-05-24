package draughts
package format

import scala.collection.breakOut

case class UciCharPair(a: Char, b: Char) {
  override def toString = s"$a$b"
}

object UciCharPair {

  import implementation._
  import Board.BoardSize

  def apply(uci: Uci, boardSize: BoardSize): UciCharPair = UciCharPair(toChar(uci.origDest._1, boardSize), toChar(uci.origDest._2, boardSize))
  def apply(uci: Uci, ambiguity: Int, boardSize: BoardSize): UciCharPair = UciCharPair(toChar(uci.origDest._1, boardSize), ambiguity2charMap(boardSize).getOrElse(ambiguity, voidChar))
  def apply(orig: Char, ambiguity: Int, boardSize: BoardSize): UciCharPair = UciCharPair(orig, ambiguity2charMap(boardSize).getOrElse(ambiguity, voidChar))

  def combine(uci1: Uci, uci2: Uci, boardSize: BoardSize): UciCharPair = UciCharPair(toChar(uci1.origDest._1, boardSize), toChar(uci2.origDest._2, boardSize))

  private[format] object implementation {

    type File = Int

    val charShift = 35 // Start at Char(35) == '#'
    val voidChar = 33.toChar // '!'. We skipped Char(34) == '"'.

    val pos2charMaps: Map[String, Map[Pos, Char]] = Board.boardSizes.map { size =>
      size.key -> getPos2charMap(size.pos)
    }(breakOut)

    def getPos2charMap(bp: BoardPos): Map[Pos, Char] = bp.all.map { pos =>
      pos -> (pos.hashCode + charShift).toChar
    }(breakOut)

    def toChar(pos: Pos, boardSize: BoardSize) = pos2charMaps(boardSize.key).getOrElse(pos, voidChar)

    /**
     * Allow for 50 ambiguities per destination, should be enough
     */
    val ambiguity2charMaps: Map[String, Map[Int, Char]] = Board.boardSizes.map { size =>
      size.key -> getAmbiguity2charMap(size)
    }(breakOut)
    def getAmbiguity2charMap(bs: BoardSize): Map[Int, Char] = (for {
      ambNr <- 1 to 50
    } yield ambNr -> (charShift + pos2charMaps(bs.key).size + ambNr).toChar)(breakOut)

    def ambiguity2charMap(bs: BoardSize) = ambiguity2charMaps(bs.key)

  }
}
