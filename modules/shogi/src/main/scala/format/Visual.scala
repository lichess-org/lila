package shogi
package format

/** r bqkb r
  * p ppp pp
  * pr
  *    P p
  *    QnB
  *  PP  N
  * P    PPP
  * RN  K  R
  */
object Visual {

  def <<(source: String): Board = {
    val lines = augmentString(source).linesIterator.to(List)
    val filtered = lines.size match {
      case 9          => lines
      case n if n > 9 => lines.slice(1, 10)
      case n          => (List.fill(9 - n)("")) ::: lines
    }
    Board(
      pieces = (for {
        (l, y) <- (filtered zipWithIndex)
        (c, x) <- (l zipWithIndex)
        role   <- Role forsyth c.toLower
      } yield {
        Pos.at(9 - x, y + 1) map { pos =>
          pos -> (Color.fromSente(c isUpper) - role)
        }
      }) flatten,
      variant = shogi.variant.Variant.default
    )
  }

  def >>(board: Board): String = >>|(board, Map.empty)

  def >>|(board: Board, marks: Map[Iterable[Pos], Char]): String = {
    val markedPoss: Map[Pos, Char] = marks.foldLeft(Map[Pos, Char]()) { case (marks, (poss, char)) =>
      marks ++ (poss.toList map { pos =>
        (pos, char)
      })
    }
    for (y <- 1 to 9) yield {
      for (x <- 9 to 1 by -1) yield {
        Pos.at(x, y) flatMap markedPoss.get getOrElse board(x, y).fold(' ')(_ forsyth)
      }
    } mkString
  } map { """\s*$""".r.replaceFirstIn(_, "") } mkString "\n"

  def addNewLines(str: String) = "\n" + str + "\n"
}
