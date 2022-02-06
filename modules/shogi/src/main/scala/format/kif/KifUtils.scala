package shogi
package format
package kif

import shogi.variant._

object KifUtils {
  def toDigit(c: Char): Char = {
    c match {
      case '１' | '一' | 'a' => '1'
      case '２' | '二' | 'b' => '2'
      case '３' | '三' | 'c' => '3'
      case '４' | '四' | 'd' => '4'
      case '５' | '五' | 'e' => '5'
      case '６' | '六' | 'f' => '6'
      case '７' | '七' | 'g' => '7'
      case '８' | '八' | 'h' => '8'
      case '９' | '九' | 'i' => '9'
      case _               => c
    }
  }

  def toKanjiDigit(c: Char): Char = {
    c match {
      case '1' => '一'
      case '2' => '二'
      case '3' => '三'
      case '4' => '四'
      case '5' => '五'
      case '6' => '六'
      case '7' => '七'
      case '8' => '八'
      case '9' => '九'
      case _   => c
    }
  }

  // supporting max 999
  def kanjiToInt(str: String): Int = {
    def orderHelper(ord: String): Int = {
      if (ord == "") 1
      else if (ord.contains('十')) 10 * orderHelper(ord.filterNot(_ == '十'))
      else if (ord.contains('百')) 100 * orderHelper(ord.filterNot(_ == '百'))
      else ord.map(toDigit _).toIntOption | 0
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

  // head used in kif model
  val defaultHandicaps: Map[Variant, List[String]] = Map(
    Minishogi -> List("5五将棋", "五々将棋", "５五将棋"),
    Standard  -> List("平手")
  )

}
