package lila.analyse

import chess.Color
import chess.format.UciMove

case class Info(
    ply: Int,
    score: Option[Score] = None,
    mate: Option[Int] = None,
    // variation is first in UCI, then converted to PGN before storage
    variation: List[String] = Nil,
    // best is always in UCI (used for hilight)
    best: Option[UciMove] = None) {

  def turn = 1 + (ply - 1) / 2

  def color = Color(ply % 2 == 1)

  def encode: String = List(
    best ?? (_.keysPiotr),
    variation mkString " ",
    mate ?? (_.toString),
    score ?? (_.centipawns.toString)
  ).dropWhile(_.isEmpty).reverse mkString Info.separator

  def hasVariation = variation.nonEmpty
  def dropVariation = copy(variation = Nil, best = None)

  def reverse = copy(score = score map (-_), mate = mate map (-_))

  def scoreComment: Option[String] = score map (_.showPawns)
  def mateComment: Option[String] = mate map { m => s"Mate in ${math.abs(m)}" }
  def evalComment: Option[String] = scoreComment orElse mateComment

  override def toString = s"Info [$ply] ${score.fold("?")(_.showPawns)} ${mate | 0} ${variation.mkString(" ")}"
}

object Info {

  private val separator = ","
  private val listSeparator = ";"

  lazy val start = Info(0, Evaluation.start.score, none, Nil)

  def decode(ply: Int, str: String): Option[Info] = str.split(separator).toList match {
    case Nil                  => Info(ply).some
    case List(cp)             => Info(ply, Score(cp)).some
    case List(cp, ma)         => Info(ply, Score(cp), parseIntOption(ma)).some
    case List(cp, ma, va)     => Info(ply, Score(cp), parseIntOption(ma), va.split(' ').toList).some
    case List(cp, ma, va, be) => Info(ply, Score(cp), parseIntOption(ma), va.split(' ').toList, UciMove piotr be).some
    case _                    => none
  }

  def decodeList(str: String): Option[List[Info]] = {
    str.split(listSeparator).toList.zipWithIndex map {
      case (infoStr, index) => decode(index + 1, infoStr)
    }
  }.sequence

  def encodeList(infos: List[Info]): String = infos map (_.encode) mkString listSeparator

  def apply(score: Option[Int], mate: Option[Int], variation: List[String]): Int => Info =
    ply => Info(ply, score map Score.apply, mate, variation)
}
