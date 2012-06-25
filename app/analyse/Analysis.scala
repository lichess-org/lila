package lila
package analyse

import chess.{ Pos, Color }
import ai.stockfish.Uci

case class Analysis(infos: List[Info], done: Boolean) {

  def encode: String = infos map (_.encode) mkString Analysis.separator

  lazy val richInfos = {
    var prevCp = 0
    infos.zipWithIndex map {
      case (info, index) ⇒ {
        RichInfo(index, 0, info)
      }
    }
  }

  def of(color: Color) = PlayerAnalysis(
    color = color,
    richInfos filter (_.color == color))
}

case class PlayerAnalysis(color: Color, richInfos: List[RichInfo]) {

  def cps = richInfos map { ri ⇒ ri.turn -> ri.cp }
}

object Analysis {

  private val separator = " "

  def apply(str: String, done: Boolean) = decode(str) map { infos =>
    new Analysis(infos, done)
  }

  def decode(str: String): Valid[List[Info]] =
    (str.split(separator).toList map Info.decode).sequence 

  def builder = new AnalysisBuilder(Nil)
}

final class AnalysisBuilder(infos: List[Info]) {

  def size = infos.size

  def +(info: Info) = new AnalysisBuilder(info :: infos)

  def done = new Analysis(infos.reverse, true)
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

case class RichInfo(turn: Int, delta: Int, info: Info) {

  def color = Color(turn % 2 == 0)

  def move = info.move
  def best = info.best
  def cp = info.cp
  def mate = info.mate
}
