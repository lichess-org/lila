package shogi
package format
package csa

import scala.util.parsing.combinator._
import cats.data.Validated
import cats.data.Validated.{ invalid, valid }
import cats.implicits._

import shogi.variant.Standard

// https://gist.github.com/Marken-Foo/b1047990ee0c65537582ebe591e2b6d7
object CsaParser {

  // Helper strings for regex, so we don't have to repeat ourselves that much
  val colorsS     = """\+|-"""
  val positionS   = """[1-9][1-9]"""
  val dropOriginS = """00"""
  val piecesS     = """OU|HI|RY|KA|UM|KI|GI|NG|KE|NK|KY|NY|FU|TO"""
  val handPiecesS = """HI|KA|KI|GI|KE|KY|FU"""

  val moveOrDropRegex =
    raw"""($colorsS)?($positionS|$dropOriginS)($positionS)($piecesS)""".r

  case class StrMove(
      move: String,
      comments: List[String],
      timeSpent: Option[Centis] = None
  )

  def full(csa: String): Validated[String, ParsedNotation] =
    try {
      val preprocessed = augmentString(cleanCsa(csa)).linesIterator
        .collect {
          case l if !l.trim.startsWith("'") => l.replace(",", "\n").trim
          case l                            => l.trim // try to keep ',' in comments
        }
        .filterNot(l => l.isEmpty || l == "'" || l.startsWith("V")) // remove empty comments and version
        .mkString("\n")
      for {
        splitted <- splitHeaderAndMoves(preprocessed)
        (headerStr, movesStr) = splitted
        splitted3 <- splitMetaAndBoard(headerStr)
        (metaStr, boardStr) = splitted3
        preTags     <- TagParser(metaStr)
        parsedMoves <- MovesParser(movesStr)
        (strMoves, terminationOption) = parsedMoves
        init      <- getComments(headerStr)
        situation <- CsaParserHelper.parseSituation(boardStr)
        tags = createTags(preTags, situation, strMoves.size, terminationOption)
        parsedMoves <- objMoves(strMoves)
      } yield ParsedNotation(init, tags, parsedMoves)
    } catch {
      case _: StackOverflowError =>
        sys error "### StackOverflowError ### in CSA parser"
    }

  def objMoves(strMoves: List[StrMove]): Validated[String, ParsedMoves] = {
    strMoves.map { case StrMove(moveStr, comments, timeSpent) =>
      (
        MoveParser(moveStr) map { m =>
          m withComments comments withTimeSpent timeSpent
        }
      ): Validated[String, ParsedMove]
    }.sequence map { ParsedMoves.apply(_) }
  }

  def createTags(
      tags: Tags,
      sit: Situation,
      nbMoves: Int,
      moveTermTag: Option[Tag]
  ): Tags = {
    val sfenTag = sit.toSfen.some.collect {
      case sfen if sfen.truncate != sit.variant.initialSfen.truncate => Tag(_.Sfen, sfen.truncate)
    }
    val termTag = (tags(_.Termination) orElse moveTermTag.map(_.value)).map(t => Tag(_.Termination, t))
    val resultTag = CsaParserHelper
      .createResult(
        termTag,
        Color.fromSente((nbMoves + { if (sit.color.gote) 1 else 0 }) % 2 == 0)
      )

    List(sfenTag, resultTag, termTag).flatten.foldLeft(tags)(_ + _)
  }

  trait Logging { self: Parsers =>
    protected val loggingEnabled = false
    protected def as[T](msg: String)(p: => Parser[T]): Parser[T] =
      if (loggingEnabled) log(p)(msg) else p
  }

  object MovesParser extends RegexParsers with Logging {

    override val whiteSpace = """(\s|\t|\r?\n)+""".r

    def apply(csaMoves: String): Validated[String, (List[StrMove], Option[Tag])] = {
      parseAll(strMoves, csaMoves) match {
        case Success((moves, termination), _) =>
          valid(
            (
              moves,
              termination map { r =>
                Tag(_.Termination, r)
              }
            )
          )
        case err => invalid("Cannot parse moves: %s\n%s".format(err.toString, csaMoves))
      }
    }

    def strMoves: Parser[(List[StrMove], Option[String])] =
      as("moves") {
        (strMove *) ~ (termination *) ~ (commentary *) ^^ { case parsedMoves ~ term ~ coms =>
          (updateLastComments(parsedMoves, cleanComments(coms)), term.headOption)
        }
      }

    def strMove: Parser[StrMove] =
      as("move") {
        (commentary *) ~>
          (moveOrDropRegex ~ opt(clock) ~ rep(commentary)) <~
          (moveExtras *) ^^ { case move ~ clk ~ comments =>
            StrMove(move, cleanComments(comments), clk.flatten)
          }
      }

    private val clockSecondsRegex = """(\d++)""".r

    private def readCentis(seconds: String): Option[Centis] =
      seconds.toDoubleOption match {
        case Some(s) => Centis(BigDecimal(s * 100).setScale(0, BigDecimal.RoundingMode.HALF_UP).toInt).some
        case _       => none
      }

    private def parseClock(str: String): Option[Centis] = {
      str match {
        case clockSecondsRegex(seconds) => readCentis(seconds)
        case _                          => None
      }
    }

    private def updateLastComments(moves: List[StrMove], comments: List[String]): List[StrMove] = {
      val index = moves.size - 1
      (moves lift index).fold(moves) { move =>
        moves.updated(index, move.copy(comments = move.comments ::: comments))
      }
    }

    def clock: Parser[Option[Centis]] =
      as("clock") {
        """T""".r ~>
          clockSecondsRegex ^^ { case spent =>
            parseClock(spent)
          }
      }

    def moveExtras: Parser[Unit] =
      as("moveExtras") {
        commentary.^^^(())
      }

    def commentary: Parser[String] =
      as("commentary") {
        """'""" ~> """.+""".r
      }

    def termination: Parser[String] =
      as("termination") {
        "%" ~> termValue ~ opt(clock) ^^ { case term ~ _ =>
          term
        }
      }

    val termValue: Parser[String] =
      "CHUDAN" | "TORYO" | "JISHOGI" | "SENNICHITE" | "TSUMI" | "TIME_UP" | "ILLEGAL_MOVE" | "+ILLEGAL_ACTION" | "-ILLEGAL_ACTION" | "KACHI" | "HIKIWAKE" | "FUZUMI" | "MATTA" | "ERROR"
  }

  object MoveParser extends RegexParsers with Logging {

    val MoveRegex =
      raw"""^(?:${colorsS})?($positionS)($positionS)($piecesS)""".r
    val DropRegex = raw"""^(?:${colorsS})?(?:$dropOriginS)($positionS)($handPiecesS)""".r

    override def skipWhitespace = false

    def apply(str: String): Validated[String, ParsedMove] = {
      str match {
        case MoveRegex(origS, destS, roleS) => {
          for {
            role <- Role.allByCsa get roleS toValid s"Uknown role in move: $str"
            _ <-
              if (Standard.allRoles contains role) valid(role)
              else invalid(s"$role not supported in standard shogi")
            dest <- Pos.allNumberKeys get destS toValid s"Cannot parse destination sqaure in move: $str"
            orig <- Pos.allNumberKeys get origS toValid s"Cannot parse origin sqaure in move: $str"
          } yield CsaMove(
            dest = dest,
            role = role,
            orig = orig,
            metas = Metas(
              comments = Nil,
              glyphs = Glyphs.empty,
              variations = Nil,
              timeSpent = None,
              timeTotal = None
            )
          )
        }
        case DropRegex(posS, roleS) =>
          for {
            role <- Role.allByCsa get roleS toValid s"Uknown role in drop: $str"
            _ <-
              if (Standard.handRoles contains role) valid(role)
              else invalid(s"$role can't be dropped in standard shogi")
            pos <- Pos.allNumberKeys get posS toValid s"Cannot parse destination sqaure in drop: $str"
          } yield Drop(
            role = role,
            pos = pos,
            metas = Metas(
              comments = Nil,
              glyphs = Glyphs.empty,
              variations = Nil,
              timeSpent = None,
              timeTotal = None
            )
          )
        case _ => invalid("Cannot parse move/drop: %s\n".format(str))
      }
    }
  }

  object TagParser extends RegexParsers with Logging {

    def apply(csa: String): Validated[String, Tags] =
      parseAll(all, csa) match {
        case f: Failure       => invalid("Cannot parse CSA tags: %s\n%s".format(f.toString, csa))
        case Success(tags, _) => valid(Tags(tags.filter(_.value.nonEmpty)))
        case err              => invalid("Cannot parse CSA tags: %s\n%s".format(err.toString, csa))
      }

    def all: Parser[List[Tag]] =
      as("all") {
        rep(tags) <~ """(.|\n)*""".r
      }

    def tags: Parser[Tag] = tag | playerTag

    def tag: Parser[Tag] =
      "$" ~>
        """\w+""".r ~ ":" ~ """.*""".r ^^ { case name ~ _ ~ value =>
          Tag(normalizeCsaName(name), value)
        }

    def playerTag: Parser[Tag] =
      """N""" ~>
        """[\+|-].*""".r ^^ { case line =>
          Tag(normalizeCsaName(line.slice(0, 1)), line.drop(1))
        }
  }

  private def cleanCsa(csa: String): String =
    csa
      .replace("‑", "-")
      .replace("–", "-")
      .replace('　', ' ')
      .replace("：", ":")
      .replace(s"\ufeff", "")

  private def cleanComments(comments: List[String]) =
    comments.map(_.trim.take(2000)).filter(_.nonEmpty)

  private def normalizeCsaName(str: String): String =
    Tag.csaNameToTag.get(str).fold(str)(_.lowercase)

  private def getComments(csa: String): Validated[String, InitialPosition] =
    augmentString(csa).linesIterator.toList.map(_.trim).filter(_.nonEmpty) filter { line =>
      line.startsWith("'")
    } match {
      case (comms) => valid(InitialPosition(comms.map(_.drop(1).trim)))
    }

  private def splitHeaderAndMoves(csa: String): Validated[String, (String, String)] =
    augmentString(csa).linesIterator.toList.map(_.trim).filter(_.nonEmpty) span { line =>
      !(moveOrDropRegex.matches(line))
    } match {
      case (headerLines, moveLines) => valid(headerLines.mkString("\n") -> moveLines.mkString("\n"))
    }

  private def splitMetaAndBoard(csa: String): Validated[String, (String, String)] =
    augmentString(csa).linesIterator.toList
      .map(_.trim)
      .filter(l => l.nonEmpty && !l.startsWith("'")) partition { line =>
      !((line startsWith "P") || (line == "+") || (line == "-"))
    } match {
      case (metaLines, boardLines) => valid(metaLines.mkString("\n") -> boardLines.mkString("\n"))
    }
}
