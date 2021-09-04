package shogi
package format.pgn

import variant.Variant

import scala.util.parsing.combinator._
import scalaz.Validation.FlatMap._
import scalaz.Validation.{ success => succezz }

// We are keeping the original interface of the parser
// Instead of being strict with what is a valid kif
// we are gonna try to be rather benevolent.
// Both half-width and full-width numbers are accepted (not in clock times)
// All reasonable position formats (1一, 1a, 11) will be accepted
// Pieces can either be kanji(1 or 2) or english letters (sfen)

// https://gist.github.com/Marken-Foo/7694548af1f562ecd01fba6b60a9c96a
object KifParser extends scalaz.syntax.ToTraverseOps {

  // Helper strings for regex, so we don't have to repeat ourselves that much
  val colorsS        = """▲|△|☗|☖"""
  val numbersS       = """[1-9１-９一二三四五六七八九十百][0-9０-９一二三四五六七八九十百]*"""
  val positionS      = """[1-9１-９一二三四五六七八九][1-9a-i１-９一二三四五六七八九]|同"""
  val piecesJPS      = """玉|王|飛|龍|角|馬|金|銀|成銀|桂|成桂|香|成香|歩|と|竜|全|圭|今|杏|仝|个"""
  val handPiecesJPS  = """飛|角|金|銀|桂|香|歩"""
  val piecesENGS     = """K|R|\+R|B|\+B|G|S|\+G|N|\+N|L|\+L|P|\+P"""
  val handPiecesENGS = """R|B|G|S|N|L|P"""
  val promotionS     = """不成|成|\+"""
  val dropS          = """打"""
  val parsS          = """\(|（|\)|）"""

  val moveOrDropRegex =
    raw"""(${colorsS})?(${positionS})\s?(${piecesJPS}|${piecesENGS})(((${promotionS})?(${parsS})?(${positionS})(${parsS})?)|(${dropS}))""".r

  val moveOrDropLineRegex =
    raw"""(${numbersS}[\s\.。]{1,})?(${colorsS})?(${positionS})\s?(${piecesJPS}|${piecesENGS})(((${promotionS})?(${parsS})?${positionS}(${parsS})?)|${dropS})""".r.unanchored

  case class StrMove(
      turnNumber: Option[Int],
      san: String,
      comments: List[String],
      timeSpent: Option[Centis] = None,
      timeTotal: Option[Centis] = None
  )

  case class StrVariation(
      variationStart: Int,
      moves: List[StrMove],
      variations: List[StrVariation]
  )

  def full(kif: String): Valid[ParsedPgn] =
    try {
      val preprocessed = augmentString(kif).linesIterator
        .map(_.split("#|&").head.trim) // remove # and & comments and trim
        .filter(l => l.nonEmpty && !(l.startsWith("まで")))
        .mkString("\n")
        .replace("‑", "-")
        .replace("–", "-")
        .replace('　', ' ')
        .replace("：", ":")
      for {
        splitted <- splitHeaderAndRest(preprocessed)
        headerStr = splitted._1
        restStr   = splitted._2
        splitted2 <- splitMovesAndVariations(restStr)
        movesStr     = splitted2._1
        variationStr = splitted2._2
        splitted3 <- splitMetaAndBoard(headerStr)
        metaStr  = splitted3._1
        boardStr = splitted3._2
        preTags     <- TagParser(metaStr)
        parsedMoves <- MovesParser(movesStr)
        init              = parsedMoves._1
        strMoves          = parsedMoves._2
        terminationOption = parsedMoves._3
        parsedVariations <- VariationParser(variationStr)
        variations  = createVariations(parsedVariations)
        boardOption = KifUtils readBoard boardStr orElse preTags(_.Handicap).flatMap(KifUtils handicapToFen _)
        termTags    = terminationOption.filterNot(_ => preTags.exists(_.Termination)).foldLeft(preTags)(_ + _)
        boardTags   = boardOption.filterNot(_ => termTags.exists(_.FEN)).foldLeft(termTags)(_ + _)
        tags = KifUtils
          .createResult(
            boardTags,
            Color((strMoves.size + { if (boardOption.fold(false)(_.value contains " w")) 1 else 0 }) % 2 == 0)
          )
          .filterNot(_ => preTags.exists(_.Result))
          .foldLeft(boardTags)(_ + _)
        sans <- objMoves(strMoves, tags.variant | Variant.default, variations)
      } yield ParsedPgn(init, tags, sans)
    } catch {
      case _: StackOverflowError =>
        println(kif)
        sys error "### StackOverflowError ### in KIF parser"
    }

  def objMoves(
      strMoves: List[StrMove],
      variant: Variant,
      variations: List[StrVariation],
      startDest: Option[Pos] = None,
      startNum: Int = 1
  ): Valid[Sans] = {
    // No need to store 0s that mean nothing
    val uselessTimes = strMoves.forall(m => m.timeSpent.fold(true)(_ == Centis(0)) && m.timeTotal.fold(true)(_ == Centis(0)))
    
    var lastDest: Option[Pos] = startDest
    var bMoveNumber           = startNum
    var res: List[Valid[San]] = List()

    for (StrMove(moveNumber, san, comments, timeSpent, timeTotal) <- strMoves) {
      val move: Valid[San] = MoveParser(san, lastDest, variant) map { m =>
        val m1 = m withComments comments withVariations {
          variations
            .filter(_.variationStart == moveNumber.getOrElse(bMoveNumber))
            .map { v =>
              objMoves(v.moves, variant, v.variations, lastDest, bMoveNumber + 1) | Sans.empty
            }
            .filter(_.value.nonEmpty)
        }
        if(uselessTimes) m1
        else 
          m1 withTimeSpent timeSpent withTimeTotal timeTotal
      }
      move map { m: San =>
        lastDest = m.getDest.some
      }
      bMoveNumber += 1
      res = move :: res
    }
    res.reverse.sequence map Sans.apply
  }

  def createVariations(vs: List[StrVariation]): List[StrVariation] = {
    def getChildren(parent: StrVariation, rest: List[StrVariation]): StrVariation = {
      val ch = rest
        .takeWhile(_.variationStart > parent.variationStart)
        .zipWithIndex
        .foldLeft(List[(StrVariation, Int)]()) { case (acc, o) =>
          if (acc.headOption.fold(false)(_._1.variationStart < o._1.variationStart)) acc
          else o :: acc
        }
        .reverse
      val res = ch.map { case (k, i) =>
        getChildren(k, rest.drop(i + 1))
      }
      StrVariation(parent.variationStart, parent.moves, res)
    }
    val ch = vs.zipWithIndex
      .foldLeft(List[(StrVariation, Int)]()) { case (acc, o) =>
        if (acc.headOption.fold(false)(_._1.variationStart < o._1.variationStart)) acc
        else o :: acc
      }
      .reverse

    ch.map { case (k, i) =>
      getChildren(k, vs.drop(i + 1))
    }
  }

  def cleanComments(comments: List[String]) = comments.map(_.trim.take(2000)).filter(_.nonEmpty)

  object VariationParser extends RegexParsers with Logging {

    override val whiteSpace = """(\s|\t|\r?\n)+""".r

    def apply(kifVariations: String): Valid[List[StrVariation]] = {
      parseAll(variations, kifVariations.trim) match {
        case Success(vars, _) => succezz(vars)
        case _                => "Cannot parse variations, man".failureNel
      }
    }

    def variations: Parser[List[StrVariation]] = rep(variation)

    def variation: Parser[StrVariation] =
      as("variation") {
        header ~ rep(move) ^^ { case h ~ m =>
          StrVariation(h, m, Nil)
        }
      }

    def header: Parser[Int] =
      """.+""".r ^^ { case num =>
        (raw"""${numbersS}""".r findFirstIn num).map(KifUtils.kanjiToInt _).getOrElse(0)
      }

    // todo - don't repeat this just use MovesParser
    def move: Parser[StrMove] =
      as("move") {
        (commentary *) ~>
          (opt(number) ~ moveOrDropRegex ~ opt(clock) ~ rep(commentary)) <~
          (moveExtras *) ^^ { case num ~ san ~ _ ~ comments =>
            StrMove(num, san, cleanComments(comments))
          }
      }

    def number: Parser[Int] = raw"""${numbersS}[\s\.。]{1,}""".r ^^ { case n =>
      KifUtils.kanjiToInt(n.filterNot(c => (c == '.' || c == '。')).trim)
    }

    def clock: Parser[String] =
      as("clock") {
        """[\(（][0-9０-９\/\s:／]{1,}[\)）]\+?""".r
      }

    def moveExtras: Parser[Unit] =
      as("moveExtras") {
        commentary.^^^(())
      }

    def commentary: Parser[String] =
      as("commentary") {
        """\*|＊""".r ~> """.+""".r
      }

  }

  trait Logging { self: Parsers =>
    protected val loggingEnabled = false
    protected def as[T](msg: String)(p: => Parser[T]): Parser[T] =
      if (loggingEnabled) log(p)(msg) else p
  }

  object MovesParser extends RegexParsers with Logging {

    override val whiteSpace = """(\s|\t|\r?\n)+""".r

    def apply(kifMoves: String): Valid[(InitialPosition, List[StrMove], Option[Tag])] = {
      parseAll(strMoves, kifMoves) match {
        case Success((init, moves, termination), _) =>
          succezz(
            (
              init,
              moves,
              termination map { r =>
                Tag(_.Termination, r)
              }
            )
          )
        case err => "Cannot parse moves from kif: %s\n%s".format(err.toString, kifMoves).failureNel
      }
    }

    def strMoves: Parser[(InitialPosition, List[StrMove], Option[String])] =
      as("moves") {
        (commentary *) ~ (strMove *) ~ (termination *) ~ (commentary *) ^^ { case coms ~ sans ~ term ~ _ =>
          (InitialPosition(cleanComments(coms)), sans, term.headOption)
        }
      }

    def strMove: Parser[StrMove] =
      as("move") {
        (commentary *) ~>
          (opt(number) ~ moveOrDropRegex ~ opt(clock) ~ rep(commentary)) <~
          (moveExtras *) ^^ { case num ~ san ~ clk ~ comments =>
            StrMove(num, san, cleanComments(comments), clk.flatMap(_._1), clk.flatMap(_._2))
          }
      }

    def number: Parser[Int] = raw"""${numbersS}[\s\.。]{1,}""".r ^^ { case n =>
      KifUtils.kanjiToInt(n.filterNot(c => (c == '.' || c == '。')).trim)
    }

    private val clockMinuteSecondRegex     = """(\d++):(\d+(?:\.\d+)?)""".r
    private val clockHourMinuteSecondRegex = """(\d++):(\d++)[:\.](\d+(?:\.\d+)?)""".r

    private def readCentis(hours: String, minutes: String, seconds: String): Option[Centis] =
      for {
        h <- hours.toIntOption
        m <- minutes.toIntOption
        cs <- seconds.toDoubleOption match {
          case Some(s) => Some(BigDecimal(s * 100).setScale(0, BigDecimal.RoundingMode.HALF_UP).toInt)
          case _       => none
        }
      } yield Centis(h * 360000 + m * 6000 + cs)

    private def parseClock(str: String): Option[Centis] = {
      str match {
        case clockMinuteSecondRegex(minutes, seconds)            => readCentis("0", minutes, seconds)
        case clockHourMinuteSecondRegex(hours, minutes, seconds) => readCentis(hours, minutes, seconds)
        case _                                                   => None
      }
    }

    def clock: Parser[(Option[Centis], Option[Centis])] =
      as("clock") {
        """[\(（]\s*""".r ~>
          clockMinuteSecondRegex ~ opt("/") ~ opt(clockHourMinuteSecondRegex) <~
          """\s*[\)）]\+?""".r ^^ { case spent ~ _ ~ total =>
            (parseClock(spent), total.flatMap(parseClock(_)))
          }
      }

    def moveExtras: Parser[Unit] =
      as("moveExtras") {
        commentary.^^^(())
      }

    def commentary: Parser[String] =
      as("commentary") {
        """\*|＊""".r ~> """.+""".r
      }

    def termination: Parser[String] =
      as("termination") {
        opt(number) ~ termValue ~ opt(clock) ^^ { case _ ~ term ~ _ =>
          term
        }
      }

    val termValue: Parser[String] = "中断" | "投了" | "持将棋" | "千日手" | "詰み" | "切れ負け" | "反則勝ち" | "入玉勝ち" | "Time-up"
  }

  object MoveParser extends RegexParsers with Logging {

    val MoveRegex =
      raw"""(?:${colorsS})?(${positionS})\s?(${piecesJPS}|${piecesENGS})(${promotionS})?(?:(?:${parsS})?(${positionS})(?:${parsS})?)?""".r
    val DropRegex = raw"""(?:${colorsS})?(${positionS})\s?(${handPiecesJPS}|${handPiecesENGS})${dropS}""".r

    override def skipWhitespace = false

    def apply(str: String, lastDest: Option[Pos], variant: Variant): Valid[San] = {
      str.map(KifUtils toDigit _) match {
        case MoveRegex(pos, role, prom, orig) => {
          (variant.rolesByEverything get role) flatMap { role =>
            (if (pos == "同") lastDest else (Pos.numberAllKeys get pos)) map { dest =>
              succezz(
                Std(
                  dest = dest,
                  role = role,
                  file = Pos.numberAllKeys.get(orig).map(_.x),
                  rank = Pos.numberAllKeys.get(orig).map(_.y),
                  promotion = if (prom == "成" || prom == "+") true else false,
                  metas = Metas(
                    check = false,
                    checkmate = false,
                    comments = Nil,
                    glyphs = Glyphs.empty,
                    variations = Nil,
                    timeSpent = None,
                    timeTotal = None
                  )
                )
              )
            }
          } getOrElse "Cannot parse move: %s\n".format(str).failureNel
        }
        case DropRegex(posS, roleS) =>
          (variant.rolesByEverything get roleS) flatMap { role =>
            Pos.numberAllKeys get posS map { pos =>
              succezz(
                Drop(
                  role = role,
                  pos = pos,
                  metas = Metas(
                    check = false,
                    checkmate = false,
                    comments = Nil,
                    glyphs = Glyphs.empty,
                    variations = Nil,
                    timeSpent = None,
                    timeTotal = None
                  )
                )
              )
            }
          } getOrElse "Cannot parse move: %s\n".format(str).failureNel
        case _ => "Cannot parse move: %s\n".format(str).failureNel
      }
    }
  }

  object TagParser extends RegexParsers with Logging {

    def apply(kif: String): Valid[Tags] =
      parseAll(all, kif) match {
        case f: Failure       => "Cannot parse kif tags: %s\n%s".format(f.toString, kif).failureNel
        case Success(tags, _) => succezz(Tags(tags.filter(_.value.nonEmpty)))
        case err              => "Cannot parse kif tags: %s\n%s".format(err.toString, kif).failureNel
      }

    def all: Parser[List[Tag]] =
      as("all") {
        rep(tag) <~ """(.|\n)*""".r
      }

    def tag: Parser[Tag] =
      """.+(:).*""".r ^^ { case line =>
        val s = line.split(":", 2).map(_.trim).toList
        Tag(KifUtils.normalizeKifName(s.head), s.lift(1).getOrElse(""))
      }
  }

  private def splitHeaderAndRest(kif: String): Valid[(String, String)] =
    augmentString(kif).linesIterator.to(List).map(_.trim).filter(_.nonEmpty) span { line =>
      !((moveOrDropLineRegex.matches(line)) || (line lift 0 contains '*')) // Matches first move or comment
    } match {
      case (headerLines, moveLines) => succezz(headerLines.mkString("\n") -> moveLines.mkString("\n"))
    }

  private def splitMovesAndVariations(kif: String): Valid[(String, String)] =
    augmentString(kif).linesIterator.to(List).map(_.trim).filter(_.nonEmpty) span { line =>
      !line.startsWith("変化")
    } match {
      case (moveLines, variantsLines) => succezz(moveLines.mkString("\n") -> variantsLines.mkString("\n"))
    }

  private def splitMetaAndBoard(kif: String): Valid[(String, String)] =
    augmentString(kif).linesIterator.to(List).map(_.trim).filter(_.nonEmpty) partition { line =>
      (line contains ":") && !(line.tail startsWith "手の持駒")
    } match {
      case (metaLines, boardLines) => succezz(metaLines.mkString("\n") -> boardLines.mkString("\n"))
    }
}
