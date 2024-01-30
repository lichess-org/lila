package lila.study

import chess.format.pgn.PgnStr
import lila.common.config.Max

case class MultiPgn(value: List[PgnStr]) extends AnyVal:

  def toPgnStr = PgnStr(value mkString "\n\n")

object MultiPgn:

  private[this] val splitPat = """\R\R(?=\[)""".r.pattern

  def split(str: PgnStr, max: Max) = MultiPgn:
    PgnStr from splitPat
      .split(str.value)
      .view
      .filter(_.nonEmpty)
      .take(max.value)
      .toList
