package lila.study

import chess.format.pgn.PgnStr
import lila.common.config.Max

case class MultiPgn(value: List[PgnStr]) extends AnyVal:

  def toPgnStr = PgnStr(value mkString "\n\n")

object MultiPgn:

  private[this] val splitPat = """\n\n(?=\[)""".r.pattern

  def split(str: PgnStr, max: Max) = MultiPgn:
    PgnStr from splitPat
      .split(str.value.replaceIf('\r', ""), max.value + 1)
      .view
      .filter(_.nonEmpty)
      .take(max.value)
      .toList
