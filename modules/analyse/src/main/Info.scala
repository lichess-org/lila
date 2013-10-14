package lila.analyse

import chess.Color

// variation is first in UCI, then converted to PGN before storage
case class Info(
    ply: Int,
    score: Option[Score] = None,
    mate: Option[Int] = None,
    variation: List[String] = Nil) {

  def turn = 1 + (ply - 1) / 2

  def color = Color(ply % 2 == 1)

  def encode: String = List(
    score ?? (_.centipawns.toString),
    mate ?? (_.toString),
    variation mkString " "
  ) mkString Info.separator

  def hasVariation = variation.nonEmpty
  def dropVariation = copy(variation = Nil)

  def reverse = copy(score = score map (-_), mate = mate map (-_))

  override def toString = s"Info [$ply] ${score.fold("?")(_.showPawns)} ${mate | 0} ${variation.mkString(" ")}"
}

object Info {

  private val separator = ","
  private val listSeparator = ";"

  lazy val start = Info(0, Evaluation.start.score, none, Nil)

  def decode(ply: Int, str: String): Option[Info] = str.split(separator).toList match {
    case cp :: Nil             ⇒ Info(ply, Score(cp)).some
    case cp :: ma :: Nil       ⇒ Info(ply, Score(cp), parseIntOption(ma)).some
    case cp :: ma :: va :: Nil ⇒ Info(ply, Score(cp), parseIntOption(ma), va.split(' ').toList).some
    case _                     ⇒ none
  }

  def decodeList(str: String): Option[List[Info]] = {
    str.split(listSeparator).toList.zipWithIndex map {
      case (infoStr, index) ⇒ decode(index + 1, infoStr)
    }
  }.sequence

  def encodeList(infos: List[Info]): String = infos map (_.encode) mkString listSeparator

  def apply(score: Option[Int], mate: Option[Int], variation: List[String]): Int ⇒ Info =
    ply ⇒ Info(ply, score map Score.apply, mate, variation)
}
