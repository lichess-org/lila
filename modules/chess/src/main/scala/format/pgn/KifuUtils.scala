package chess
package format
package pgn

import scala._

object KifuUtils {
  val tagParse = Map[TagType, String](
    Tag.Event -> "棋戦",
    Tag.Site -> "場所",
    Tag.TimeControl -> "持ち時間",
    Tag.Handicap -> "手合割",
    Tag.White -> "先手",
    Tag.Black -> "後手",
    Tag.Opening -> "戦型"
  )

  val tagIndex = (List(
    Tag.Event,
    Tag.Site,
    Tag.TimeControl,
    Tag.Handicap,
    Tag.White,
    Tag.Black,
    Tag.Opening
  ) map { _.name }).zipWithIndex.toMap

  val destSymbols = Map(
    "9" -> "一",
    "8" -> "二",
    "7" -> "三",
    "6" -> "四",
    "5" -> "五",
    "4" -> "六",
    "3" -> "七",
    "2" -> "八",
    "1" -> "九",
    "a" ->  "９",
    "b" ->  "８",
    "c" ->  "７",
    "d" ->  "６",
    "e" ->  "５",
    "f" ->  "４",
    "g" ->  "３",
    "h" ->  "２",
    "i" ->  "１"
  )
  val origSymbols = Map(
    "9" -> "1",
    "8" -> "2",
    "7" -> "3",
    "6" -> "4",
    "5" -> "5",
    "4" -> "6",
    "3" -> "7",
    "2" -> "8",
    "1" -> "9",
    "a" ->  "9",
    "b" ->  "8",
    "c" ->  "7",
    "d" ->  "6",
    "e" ->  "5",
    "f" ->  "4",
    "g" ->  "3",
    "h" ->  "2",
    "i" ->  "1"
  )
  val pieceSymbols = Map(
    "P" ->  "歩",
    "L" ->  "香",
    "N" ->  "桂",
    "S" ->  "銀",
    "B" ->  "角",
    "R" ->  "飛",
    "G" ->  "金",
    "U" ->  "成香",
    "M" ->  "成桂",
    "A" ->  "成銀",
    "T" ->  "と",
    "H" ->  "馬",
    "D" ->  "龍",
    "K" ->  "玉"
  )
  val kifuSymbols = Map(
    "+" ->  "成",
    "same" -> "同　",
    "*" -> "打"
  )

  def tagsAsKifu(tags: Tags): Vector[String] = {
    val tagsWithHc = tags + Tag(Tag.Handicap, "平手")
    val preprocessedTags = Tags(value = tagsWithHc.value sortBy { tag =>
      tagIndex.getOrElse(tag.name.name, 9999)
    })
    preprocessedTags.value.toVector map { tag =>
      tagParse.get(tag.name).fold("") { x: String =>
        val timeControlPattern = "(\\d+)\\+(\\d+)\\+(\\d+)\\((\\d+)\\)".r

        val preprocessedValue = tag.name match {
          case Tag.TimeControl => tag.value match {
            case timeControlPattern(init, inc, byo, periods) => {
              val realInit = if (init.toInt % 60 == 0) s"${init.toInt / 60}分" else s"${init}秒"
              s"$realInit+${byo}秒 # $init seconds initial time, $inc seconds increment, $byo seconds byoyomi with $periods periods"
            }
            case _ => "PGN error"
          }
          case _ => tag.value
        }
        if (tag.value != "?") x + "：" +  preprocessedValue else ""
      }
    } filter { _ != "" }
  }

  def movesAsKifu(uciPgn: Vector[(String, String)]): Vector[String] = {
    uciPgn.foldLeft(Vector[String]()) { (prev, t) =>
      // t is a tuple of (uci, pgn)
      val movePattern = "([a-i])([1-9])([a-i])([1-9])(\\+?)".r
      val dropPattern = "([A-Z])\\*([a-i])([1-9])".r
      val pgnPattern = "([A-Z]).*".r
      val kifuMove = t match {
        case (movePattern(o1, o2, d1, d2, pro), pgnPattern(piece)) => {
          val lastMovePattern = s"(.*)${origSymbols(o1)}${origSymbols(o2)}".r
          (prev.lastOption match {
            // check if 同 is needed
            case Some(lastMovePattern(_)) => kifuSymbols("same")
            // else use dest coords
            case _ => destSymbols(d1) + destSymbols(d2)
          }) + pieceSymbols(piece) + (if (pro == "+") kifuSymbols("+") else "") + "(" + origSymbols(o1) + origSymbols(o2) + ")"
        }
        case (dropPattern(piece, d1, d2), _) => destSymbols(d1) + destSymbols(d2) + pieceSymbols(piece) + kifuSymbols("*")
        case _ => "UCI/PGN parse error"
      }
      prev :+ kifuMove
    }
  }
}
//Result & Termination <-> saigo no te
