package shogi
package format
package kif

import variant._

object KifUtils {
  def toDigit(c: Char): Char = {
    c match {
      case '１' | '一' | 'a' =>
        return '1'
      case '２' | '二' | 'b' =>
        return '2'
      case '３' | '三' | 'c' =>
        return '3'
      case '４' | '四' | 'd' =>
        return '4'
      case '５' | '五' | 'e' =>
        return '5'
      case '６' | '六' | 'f' =>
        return '6'
      case '７' | '七' | 'g' =>
        return '7'
      case '８' | '八' | 'h' =>
        return '8'
      case '９' | '九' | 'i' =>
        return '9'
      case _ =>
        return c
    }
  }

  def toKanjiDigit(c: Char): Char = {
    c match {
      case '1' =>
        return '一'
      case '2' =>
        return '二'
      case '3' =>
        return '三'
      case '4' =>
        return '四'
      case '5' =>
        return '五'
      case '6' =>
        return '六'
      case '7' =>
        return '七'
      case '8' =>
        return '八'
      case '9' =>
        return '九'
      case _ =>
        return c
    }
  }

  // supporting max 999
  def kanjiToInt(str: String): Int = {
    def orderHelper(ord: String): Int = {
      if (ord == "") 1
      else if (ord.contains('十')) 10 * orderHelper(ord.filterNot(_ == '十'))
      else if (ord.contains('百')) 100 * orderHelper(ord.filterNot(_ == '百'))
      else ord.map(toDigit _).toIntOption.getOrElse(0)
    }
    str.split("""(?<=(百|十))""").foldLeft(0) { (acc, cur) =>
      acc + orderHelper(cur)
    }
  }

  // supporting max 999
  def intToKanji(num: Int): String = {
    if (num >= 200) intToKanji(num / 100) + "百" + intToKanji(num % 100)
    else if (num >= 100) "百" + intToKanji(num % 100)
    else if (num >= 20) intToKanji(num / 10) + "十" + intToKanji(num % 10)
    else if (num >= 10) "十" + intToKanji(num % 10)
    else num.toString.map(toKanjiDigit _)
  }

  val defaultHandicaps: Map[Variant, String] = Map(
    Minishogi -> "五々将棋",
    Standard  -> "平手"
  )

}
