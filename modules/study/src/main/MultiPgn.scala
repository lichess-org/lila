package lila.study

import chess.format.pgn.PgnStr

case class MultiPgn(value: List[PgnStr]) extends AnyVal:

  def toPgnStr = PgnStr(value mkString "\n\n")

object MultiPgn:

  private[this] val splitPat = """\n\n(?=\[)""".r.pattern
  def split(str: PgnStr, max: Int) =
    MultiPgn {
      PgnStr from splitPat.split(str.value.replaceIf('\r', ""), max + 1).take(max).toList
    }
