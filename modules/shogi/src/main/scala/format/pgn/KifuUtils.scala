package shogi
package format
package pgn

import scala._

object KifUtils {
  val tagParse = Map[TagType, String](
    Tag.Event       -> "棋戦",
    Tag.Site        -> "場所",
    Tag.TimeControl -> "持ち時間",
    Tag.Handicap    -> "手合割",
    Tag.Sente       -> "先手",
    Tag.Gote        -> "後手",
    Tag.Opening     -> "戦型"
  )

  val tagIndex = (List(
    Tag.Event,
    Tag.Site,
    Tag.TimeControl,
    Tag.Handicap,
    Tag.Sente,
    Tag.Gote,
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
    "a" -> "９",
    "b" -> "８",
    "c" -> "７",
    "d" -> "６",
    "e" -> "５",
    "f" -> "４",
    "g" -> "３",
    "h" -> "２",
    "i" -> "１"
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
    "a" -> "9",
    "b" -> "8",
    "c" -> "7",
    "d" -> "6",
    "e" -> "5",
    "f" -> "4",
    "g" -> "3",
    "h" -> "2",
    "i" -> "1"
  )
  val pieceSymbols = Map(
    "P" -> "歩",
    "L" -> "香",
    "N" -> "桂",
    "S" -> "銀",
    "B" -> "角",
    "R" -> "飛",
    "G" -> "金",
    "U" -> "成香",
    "M" -> "成桂",
    "A" -> "成銀",
    "T" -> "と",
    "H" -> "馬",
    "D" -> "龍",
    "K" -> "玉"
  )
  val kifuSymbols = Map(
    "+"    -> "成",
    "same" -> "同　",
    "*"    -> "打"
  )

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

  // supporting max 999
  def kanjiToInt(str: String): Int = {
    def orderHelper(ord: String): Int = {
      if (ord == "") 1
      else if (ord.contains('十')) 10 * orderHelper(ord.filterNot(_ == '十'))
      else if (ord.contains('百')) 100 * orderHelper(ord.filterNot(_ == '百'))
      else parseIntOption(ord.map(toDigit _)).getOrElse(0)
    }
    str.split("""(?<=(百|十))""").foldLeft(0) { (acc, cur) =>
      acc + orderHelper(cur)
    }
  }

  // 後手の持駒:金四 銀二 香三 歩十三
  def readHands(str: String = ""): String = {
    val values = str.split(":").lastOption.getOrElse("").trim
    if (values == "なし" || values == "") return ""
    var hand = Hand.init
    for (p <- values.split(" ")) {
      val role = p.headOption
      val num  = p.tail
      Role.allByEverything.get(role.fold("")(_.toString)) map { r =>
        hand = hand.store(r, KifUtils kanjiToInt num)
      }
    }
    hand.exportHand
  }

  def handicapToFen(str: String): Option[Tag] = {
    StartingPosition.searchByEco(str) map { h =>
      Tag("FEN", h.fen)
    }
  }

  def readBoard(str: String): Option[Tag] = {
    val lines = augmentString(str).linesIterator.to(List).map(_.trim)
    val ranks = lines.map(_.trim).filter(l => (l lift 0 contains '|') && (l.length <= 42))
    val senteHandStr =
      lines.map(_.trim).filter(l => l.startsWith("先手の持駒:") || l.startsWith("下手の持駒:"))
    val goteHandStr =
      lines.map(_.trim).filter(l => l.startsWith("後手の持駒:") || l.startsWith("上手の持駒:"))
    val goteBan = lines.map(_.trim).exists(l => l.startsWith("後手番") || l.startsWith("上手番"))

    if (ranks.length != 9) return None

    var rank = 0
    val sfen = new scala.collection.mutable.StringBuilder(256)
    for (r <- ranks) {
      r.replace(".", "・")
      var empty    = 0
      var gote     = false
      var pieceStr = ""
      for (sq <- r) {
        sq match {
          case '・' => {
            empty = empty + 1
          }
          case 'v' | 'V' => {
            gote = true
          }
          case '成' | '+' =>
            pieceStr = sq.toString
          case _ => {
            Role.allByEverything.get(pieceStr + sq) map { r =>
              if (empty > 0) sfen append empty.toString
              sfen append Piece(Color(!gote), r).forsythFull
              empty = 0
            }
            pieceStr = ""
            gote = false
          }
        }
      }
      rank += 1
      if (empty > 0) sfen append empty.toString
      if (rank != 9) sfen append '/'
    }
    if (goteBan) sfen.append(" w ") else sfen.append(" b ")
    val senteHand = readHands(senteHandStr.headOption.getOrElse("")).toUpperCase
    val goteHand  = readHands(goteHandStr.headOption.getOrElse(""))

    if (senteHand == "" && senteHandStr == goteHand) sfen.append("-")
    else sfen.append(senteHand + goteHand)

    Tag(_.FEN, sfen).some
  }

  def createResult(tags: Tags, color: Color): Option[Tag] = {
    tags(_.Termination).map(_.toLowerCase) match {
      case Some("投了") | Some("反則負け") | Some("切れ負け") | Some("Time-up") =>
        Tag(_.Result, color.fold("0-1", "1-0")).some
      case Some("入玉勝ち") | Some("詰み") | Some("反則勝ち") => Tag(_.Result, color.fold("1-0", "0-1")).some
      case Some("持将棋") | Some("千日手")                => Tag(_.Result, "1/2-1/2").some
      case _                                        => None
    }
  }

  def tagsAsKifu(tags: Tags): Vector[String] = {
    val tagsWithHc = tags + Tag(Tag.Handicap, "平手")
    val preprocessedTags = Tags(value = tagsWithHc.value sortBy { tag =>
      tagIndex.getOrElse(tag.name.name, 9999)
    })
    preprocessedTags.value.toVector map { tag =>
      tagParse.get(tag.name).fold("") { x: String =>
        val timeControlPattern = "(\\d+)\\+(\\d+)\\+(\\d+)\\((\\d+)\\)".r

        val preprocessedValue = tag.name match {
          case Tag.TimeControl =>
            tag.value match {
              case timeControlPattern(init, inc, byo, periods) => {
                val realInit = if (init.toInt % 60 == 0) s"${init.toInt / 60}分" else s"${init}秒"
                s"$realInit+${byo}秒 # $init seconds initial time, $inc seconds increment, $byo seconds byoyomi with $periods periods"
              }
              case _ => "-"
            }
          case _ => tag.value
        }
        if (tag.value != "?") x + "：" + preprocessedValue else ""
      }
    } filter { _ != "" }
  }

  def movesAsKifu(uciPgn: Vector[(String, String)]): Vector[String] = {
    uciPgn.foldLeft(Vector[String]()) { (prev, t) =>
      // t is a tuple of (uci, pgn)
      val movePattern = "([a-i])([1-9])([a-i])([1-9])(\\+?)".r
      val dropPattern = "([A-Z])\\*([a-i])([1-9])".r
      val pgnPattern  = "([A-Z]).*".r
      val kifuMove = t match {
        case (movePattern(o1, o2, d1, d2, pro), pgnPattern(piece)) => {
          val lastMovePattern = s"(.*)${origSymbols(o1)}${origSymbols(o2)}".r
          (prev.lastOption match {
            // check if 同 is needed
            case Some(lastMovePattern(_)) => kifuSymbols("same")
            // else use dest coords
            case _ => destSymbols(d1) + destSymbols(d2)
          }) + pieceSymbols(piece) + (if (pro == "+") kifuSymbols("+") else "") + "(" + origSymbols(
            o1
          ) + origSymbols(o2) + ")"
        }
        case (dropPattern(piece, d1, d2), _) =>
          destSymbols(d1) + destSymbols(d2) + pieceSymbols(piece) + kifuSymbols("*")
        case _ => "UCI/PGN parse error"
      }
      prev :+ kifuMove
    }
  }
}
//Result & Termination <-> saigo no te
