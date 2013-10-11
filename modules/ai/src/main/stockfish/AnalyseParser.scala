package lila.ai
package stockfish

import java.util.regex.Matcher.quoteReplacement

import lila.analyse.Info

object AnalyseParser {

  private val cpRegex = """^info.*\scp\s(\-?\d+).*$""".r
  private val mateRegex = """^info.*\smate\s(\-?\d+).*$""".r
  private val lineRegex = """^.+\spv\s([\w\s]+)$""".r

  // info depth 4 seldepth 5 score cp -3309 nodes 1665 nps 43815 time 38 multipv 1 pv f2e3 d4c5 c1d1 c5g5 d1d2 g5g2 d2c1 e8e3
  def apply(line: String, move: String): Int ⇒ Info = {

    val cp = parseIntOption { 
      cpRegex.replaceAllIn(line, m ⇒ quoteReplacement(m group 1))
    } 

    val mate = parseIntOption {
      mateRegex.replaceAllIn(line, m ⇒ quoteReplacement(m group 1))
    } 

    val continuation = 
      lineRegex.replaceAllIn(line, m ⇒ quoteReplacement(m group 1)).split(' ').toList

    val variation = 
      if (continuation.headOption == Some(move)) continuation else Nil

    Info(cp, mate, variation)
  }
}
