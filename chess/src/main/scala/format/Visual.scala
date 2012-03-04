package lila.chess
package format

import Pos.posAt

/**
 * r bqkb r
 * p ppp pp
 * pr
 *    P p
 *    QnB
 *  PP  N
 * P    PPP
 * RN  K  R
 */
object Visual extends Format[Board] {

  def <<(source: String): Board = {
    val lines = source.lines.toList
    val filtered = lines.size match {
      case 8          ⇒ lines
      case n if n > 8 ⇒ lines drop 1 take 8
      case n          ⇒ (List.fill(8 - n)("")) ::: lines
    }
    Board(
      (for {
        line ← (filtered.zipWithIndex)
        (l, y) = line
        char ← (l zipWithIndex)
        (c, x) = char
        role ← Role forsyth c.toLower
      } yield {
        posAt(x + 1, 8 - y) map { pos ⇒ pos -> (Color(c isUpper) - role) }
      }) flatten
    ) withHistory History.noCastle
  }

  def >>(board: Board): String = >>|(board, Map.empty)

  def >>|(board: Board, marks: Map[Iterable[Pos], Char]): String = {
    val markedPoss: Map[Pos, Char] = marks.foldLeft(Map[Pos, Char]()) {
      case (marks, (poss, char)) ⇒ marks ++ (poss.toList map { pos ⇒ (pos, char) })
    }
    for (y ← 8 to 1 by -1) yield {
      for (x ← 1 to 8) yield {
        posAt(x, y) flatMap markedPoss.get getOrElse board(x, y).fold(_ forsyth, ' ')
      }
    } mkString
  } map { """\s*$""".r.replaceFirstIn(_, "") } mkString "\n"

  def addNewLines(str: String) = "\n" + str + "\n"
}
