package shogi
package format

import shogi.variant._

import scala.util.chaining._

// Gote:
//
// l n s g k g s n l
// . r . . . . .+B .
// p p p p p p . p p
// . . . . . . p . .
// . . . . . . . . .
// . . P . . . . . .
// P P . P P P P P P
// . . . . . . . R .
// L N S G K G S N L
//
// Sente: B

object Visual {

  def <<@(source: String, variant: Variant = Standard): Option[Board] = {
    val clean = source.replaceAll("^\n|\n$| ", "")
    val lines = augmentString(clean).linesIterator.to(List).map(_.trim).filter(_.nonEmpty)
    val hands = lines.filter(_ contains ":").flatMap(_.split(':').lift(1)).take(2).mkString("")
    val sfenReversed = lines
      .filterNot(_ contains ":")
      .mkString("/")
      .foldLeft(List.empty[(Int, Char)]) {
        case (fp :: lp, cur) if fp._2 == cur && fp._2 == '.' =>
          (fp._1 + 1, cur) :: lp
        case (fp, cur) =>
          (1, cur) :: fp
      }
      .map(ic => s"${if (ic._2 == '.') ic._1 else ""}${ic._2}")
      .mkString("")
      .filter(_ != '.')
    val pad        = sfenReversed.size + variant.numberOfRanks - sfenReversed.count(_ == '/') - 1
    val sfenPadded = sfenReversed.padTo(pad, "9/").reverse.mkString
    Forsyth.makeBoard(variant, sfenPadded).map(_.withHandData(Forsyth.readHands(variant, hands)))
  }

  def <<(source: String): Option[Board] =
    <<@(source, Standard)

  def >>(board: Board): String = >>|(board, Map.empty)

  def >>|(board: Board, marks: Map[Iterable[Pos], Char]): String = {
    val markedPoss: Map[Pos, Char] = marks.foldLeft(Map[Pos, Char]()) { case (marks, (poss, char)) =>
      marks ++ (poss.toList map { pos =>
        (pos, char)
      })
    }
    for (y <- 1 to board.variant.numberOfRanks) yield {
      for (x <- board.variant.numberOfFiles to 1 by -1) yield {
        "%2s" format (Pos.at(x, y) flatMap markedPoss.get getOrElse board(x, y).fold(".")(_ forsyth))
      }
    } mkString
  } map (_.trim) mkString "\n" pipe { b =>
    board.handData.filter(_.size > 0).fold(b) { h =>
      List(
        s"Gote:${Forsyth.exportHand(board.variant, h(Gote))}",
        b,
        s"Sente:${Forsyth.exportHand(board.variant, h(Sente)).toUpperCase}"
      ) mkString "\n"
    }
  }

  def addNewLines(str: String) = "\n" + str + "\n"
}
