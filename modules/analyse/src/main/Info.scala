package lila.analyse

import chess.Color
import chess.format.Uci

case class Info(
    ply: Int,
    score: Option[Score] = None,
    mate: Option[Int] = None,
    // variation is first in UCI, then converted to PGN before storage
    variation: List[String] = Nil,
    // best is always in UCI (used for hilight)
    best: Option[Uci.Move] = None) {

  def turn = 1 + (ply - 1) / 2

  def color = Color(ply % 2 == 1)

  def encode: String = List(
    best ?? (_.keysPiotr),
    variation take Info.LineMaxPlies mkString " ",
    mate ?? (_.toString),
    score ?? (_.centipawns.toString)
  ).dropWhile(_.isEmpty).reverse mkString Info.separator

  def hasVariation = variation.nonEmpty
  def dropVariation = copy(variation = Nil, best = None)

  def invert = copy(score = score.map(_.invert), mate = mate.map(-_))

  def scoreComment: Option[String] = score map (_.showPawns)
  def mateComment: Option[String] = mate map { m => s"Mate in ${math.abs(m)}" }
  def evalComment: Option[String] = scoreComment orElse mateComment

  def isEmpty = score.isEmpty && mate.isEmpty

  def forceCentipawns: Option[Int] = mate match {
    case None             => score.map(_.centipawns)
    case Some(m) if m < 0 => Some(Int.MinValue - m)
    case Some(m)          => Some(Int.MaxValue - m)
  }

  override def toString = s"Info $color [$ply] ${score.fold("?")(_.showPawns)} ${mate | 0} ${variation.mkString(" ")}"
}

object Info {

  val LineMaxPlies = 16

  private val separator = ","
  private val listSeparator = ";"

  def start(ply: Int) = Info(ply, Evaluation.start.score, none, Nil)

  def decode(ply: Int, str: String): Option[Info] = str.split(separator) match {
    case Array()               => Info(ply).some
    case Array(cp)             => Info(ply, Score(cp)).some
    case Array(cp, ma)         => Info(ply, Score(cp), parseIntOption(ma)).some
    case Array(cp, ma, va)     => Info(ply, Score(cp), parseIntOption(ma), va.split(' ').toList).some
    case Array(cp, ma, va, be) => Info(ply, Score(cp), parseIntOption(ma), va.split(' ').toList, Uci.Move piotr be).some
    case _                     => none
  }

  def decodeList(str: String, fromPly: Int): Option[List[Info]] = {
    str.split(listSeparator).toList.zipWithIndex map {
      case (infoStr, index) => decode(index + 1 + fromPly, infoStr)
    }
  }.sequence

  def encodeList(infos: List[Info]): String = infos map (_.encode) mkString listSeparator

  def apply(score: Option[Int], mate: Option[Int], variation: List[String]): Int => Info =
    ply => Info(ply, score map Score.apply, mate, variation)
}
