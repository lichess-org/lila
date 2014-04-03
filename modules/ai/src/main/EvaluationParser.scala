package lila.ai

import java.util.regex.Matcher.quoteReplacement

import lila.analyse.{ Evaluation, Score }

object EvaluationParser {

  private val LineMaxPlies = 20

  private val cpRegex = """^info.*\scp\s(\-?\d+).*$""".r
  private val mateRegex = """^info.*\smate\s(\-?\d+).*$""".r
  private val lineRegex = """^.+\spv\s([\w\s]+)$""".r

  // info depth 4 seldepth 5 score cp -3309 nodes 1665 nps 43815 time 38 multipv 1 pv f2e3 d4c5 c1d1 c5g5 d1d2 g5g2 d2c1 e8e3
  def apply(output: String): Evaluation = {

    val score = parseIntOption {
      cpRegex.replaceAllIn(output, m => quoteReplacement(m group 1))
    } map Score.apply

    val mate = parseIntOption {
      mateRegex.replaceAllIn(output, m => quoteReplacement(m group 1))
    }

    val line = output match {
      case lineRegex(line) => line.split(' ').toList take LineMaxPlies
      case _               => Nil
    }

    Evaluation(score, mate, line)
  }
}
