package lila.study

import chess.format.pgn.PgnStr

opaque type MultiPgn = List[PgnStr]

object MultiPgn extends TotalWrapper[MultiPgn, List[PgnStr]]:

  private val splitPat = """\n\n(?=\[)""".r.pattern

  def split(str: PgnStr, max: Max): MultiPgn =
    PgnStr.from:
      splitPat
        .split(str.value.replaceIf('\r', ""), max.value + 1)
        .view
        .filter(_.nonEmpty)
        .take(max.value)
        .toList

  extension (pgns: MultiPgn) def toPgnStr = PgnStr(pgns.mkString("\n\n"))
