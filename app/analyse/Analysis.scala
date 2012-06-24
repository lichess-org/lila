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

  def builder = new AnalysisBuilder(Vector.empty)
}

case class AnalysisBuilder(infos: Vector[Info]) {

  def size = infos.size

  def +(info: Info) = copy(infos = infos :+ info)

  def done = new Analysis(infos.toList.reverse)
}

case class Info(
    cp: Option[Int],
    mate: Option[Int],
    best: Option[(Pos, Pos)]) {

  def encode = List(cp, mate, best map {
    case (orig, dest) ⇒ orig.key + dest.key
  }) mkString Info.separator
}

object Info {

  private val separator = ","

  def decode(str: String): Valid[Info] = str.split(separator).toList match {
    case cpString :: mateString :: bestString :: Nil ⇒ Info(
      cp = parseIntOption(cpString) ,
      mate = parseIntOption(mateString),
      best = Uci parseMove bestString
    ).success
    case _ ⇒ !!("Invalid encoded info " + str)
  }
}
