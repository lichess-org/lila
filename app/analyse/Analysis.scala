package lila
package analyse

import chess.Pos
import ai.stockfish.Uci

case class Analysis(infos: List[Info]) {

  def encode: String = infos map (_.encode) mkString Analysis.separator
}

object Analysis {

  private val separator = " "

  def decode(str: String): Valid[Analysis] =
    (str.split(separator).toList map Info.decode).sequence map { infos ⇒
      Analysis(infos)
    }

  def builder = new AnalysisBuilder(Nil)
}

final class AnalysisBuilder(infos: List[Info]) {

  def size = infos.size

  def +(info: Info) = new AnalysisBuilder(info :: infos)

  def done = new Analysis(infos.reverse)
}

case class Info(
    move: (Pos, Pos),
    best: (Pos, Pos),
    cp: Option[Int],
    mate: Option[Int]) {

  def encode: String = List(
    Uci makeMove move,
    Uci makeMove best,
    encode(cp),
    encode(mate)
  ) mkString Info.separator

  private def encode(oa: Option[Any]): String = oa.fold(_.toString, "_")
}

object Info {

  private val separator = ","

  def decode(str: String): Valid[Info] = str.split(separator).toList match {
    case moveString :: bestString :: cpString :: mateString :: Nil ⇒ for {
      move ← Uci parseMove moveString toValid "Invalid move " + moveString
      best ← Uci parseMove bestString toValid "Invalid best " + bestString
    } yield Info(
      move = move,
      best = best,
      cp = parseIntOption(cpString),
      mate = parseIntOption(mateString)
    )
    case _ ⇒ !!("Invalid encoded info " + str)
  }
}
