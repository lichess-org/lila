package draughts
package format.pdn

import variant.Variant

import scala.util.parsing.combinator._
import scalaz.Validation.FlatMap._
import scalaz.Validation.{ success => succezz }

import scala.collection.breakOut

// http://www.saremba.de/chessgml/standards/pgn/pgn-complete.htm
// https://pdn.fmjd.org/index.html
object Parser extends scalaz.syntax.ToTraverseOps {

  case class StrMove(
      san: String,
      glyphs: Glyphs,
      comments: List[String],
      variations: List[List[StrMove]]
  )

  def full(pdn: String): Valid[ParsedPdn] = try {
    val preprocessed = pdn.lines.map(_.trim).filter {
      _.headOption != Some('%')
    }.mkString("\n")
      .replace("[pgn]", "").replace("[pdn]", "")
      .replace("[/pgn]", "").replace("[/pdn]", "")
      .replace("‑", "-")
      .replace("–", "-")
    for {
      splitted ← splitTagAndMoves(preprocessed)
      tagStr = splitted._1
      moveStr = splitted._2
      preTags ← TagParser(tagStr)
      parsedMoves ← MovesParser(moveStr)
      init = parsedMoves._1
      strMoves = parsedMoves._2
      resultOption = parsedMoves._3
      tags = resultOption.filterNot(_ => preTags.exists(_.Result)).fold(preTags)(t => preTags + t)
      sans ← objMoves(strMoves, tags.variant | Variant.default)
    } yield ParsedPdn(init, tags, sans)
  } catch {
    case _: StackOverflowError =>
      println(pdn)
      sys error "### StackOverflowError ### in PDN parser"
  }

  def moves(str: String, variant: Variant): Valid[Sans] = moves(
    str.split(' ').toList,
    variant
  )
  def moves(strMoves: Traversable[String], variant: Variant): Valid[Sans] = objMoves(
    strMoves.map { StrMove(_, Glyphs.empty, Nil, Nil) }(breakOut),
    variant
  )
  def objMoves(strMoves: List[StrMove], variant: Variant): Valid[Sans] =
    strMoves.map {
      case StrMove(san, glyphs, comments, variations) => (
        MoveParser(MovesParser.collapsedSan(san), variant) map { m =>
          m withComments comments withVariations {
            variations.map { v =>
              objMoves(v, variant) | Sans.empty
            }.filter(_.value.nonEmpty)
          } mergeGlyphs glyphs
        }
      ): Valid[San]
    }.sequence map Sans.apply

  trait Logging { self: Parsers =>
    protected val loggingEnabled = false
    protected def as[T](msg: String)(p: => Parser[T]): Parser[T] =
      if (loggingEnabled) log(p)(msg) else p
  }

  object MovesParser extends RegexParsers with Logging {

    override val whiteSpace = """(\s|\t|\r?\n)+""".r

    private def cleanComments(comments: List[String]) = comments.map(_.trim).filter(_.nonEmpty)

    def apply(pdn: String): Valid[(InitialPosition, List[StrMove], Option[Tag])] =
      parseAll(strMoves, pdn) match {
        case Success((init, moves, result), _) =>
          succezz(init, moves, result map { r => Tag(_.Result, r) })
        case err =>
          "Cannot parse moves: %s\n%s".format(err.toString, pdn).failureNel
      }

    def strMoves: Parser[(InitialPosition, List[StrMove], Option[String])] = as("moves") {
      (commentary*) ~ (strMove*) ~ (result?) ~ (commentary*) ^^ {
        case coms ~ sans ~ res ~ _ => (InitialPosition(cleanComments(coms)), sans, res)
      }
    }

    //val moveRegex = """0\-0\-0|0\-0|[PQKRBNOoa-h@][QKRBNa-h1-8xOo\-=\+\#\@]{1,6}[\?!□]{0,2}""".r
    val moveRegex = """(50|[1-4][0-9]|0?[1-9])[\-x](50|[1-4][0-9]|0?[1-9])(x(50|[1-4][0-9]|0?[1-9]))*[\?!□⨀]{0,2}""".r

    def strMove: Parser[StrMove] = as("move") {
      ((number | commentary)*) ~>
        (moveRegex ~ nagGlyphs ~ rep(commentary) ~ nagGlyphs ~ rep(variation)) <~
        (moveExtras*) ^^ {
          case san ~ glyphs ~ comments ~ glyphs2 ~ variations =>
            StrMove(collapsedSan(san.trim()), glyphs merge glyphs2, cleanComments(comments), variations)
        }
    }

    def collapsedSan(san: String) = {
      val capts = san.split('x');
      if (capts.length > 2)
        s"${capts.head}x${capts.last}"
      else san
    }

    def number: Parser[String] = """[1-9]\d*\.+\s*""".r

    def moveExtras: Parser[Unit] = as("moveExtras") {
      (commentary).^^^(())
    }

    def nagGlyphs: Parser[Glyphs] = as("nagGlyphs") {
      rep(nag) ^^ { nags =>
        Glyphs fromList nags.flatMap { n =>
          parseIntOption(n drop 1) flatMap Glyph.find
        }
      }
    }

    def nag: Parser[String] = as("nag") {
      """\$\d+""".r
    }

    def variation: Parser[List[StrMove]] = as("variation") {
      "(" ~> strMoves <~ ")" ^^ { case (_, sms, _) => sms }
    }

    def commentary: Parser[String] = blockCommentary | inlineCommentary | fenCommentary

    def blockCommentary: Parser[String] = as("block comment") {
      "{" ~> """[^\}]*""".r <~ "}"
    }

    def inlineCommentary: Parser[String] = as("inline comment") {
      ";" ~> """.+""".r
    }

    def fenCommentary: Parser[String] = as("fen comment") {
      "/FEN \"" ~> """[\w:,]*""".r <~ "\"/"
    }

    val result: Parser[String] = "*" | "1/2-1/2" | "½-½" | "0.5-0.5" | "1-1" | "0-1" | "0-2" | "1-0" | "2-0"
  }

  object MoveParser extends RegexParsers with Logging {

    private val MoveR = """^(50|[1-4][0-9]|0?[1-9])(-|x)(50|[1-4][0-9]|0?[1-9])([\?!□⨀]{0,2})$""".r

    def apply(str: String, variant: Variant): Valid[San] = {
      str match {
        case MoveR(srcR, cptR, dstR, suff) => {
          val src = parseIntOption(srcR) flatMap Pos.posAt
          val dst = parseIntOption(dstR) flatMap Pos.posAt
          (src, dst) match {
            case (Some(srcP), Some(dstP)) =>
              val parseGlyphs = if (suff.isEmpty) Glyphs.empty
              else parseAll(glyphs, suff) match {
                case Success(glphs, _) => glphs
                case err => Glyphs.empty
              }
              succezz(Std(
                src = srcP,
                dest = dstP,
                capture = cptR == "x",
                metas = Metas(
                  checkmate = false,
                  comments = Nil,
                  glyphs = parseGlyphs,
                  variations = Nil
                )
              ))
            case _ => s"Cannot parse fields: $srcR($cptR)$dstR".failureNel
          }
        }
        case _ => s"Cannot parse move: $str".failureNel
      }
    }

    def glyphs: Parser[Glyphs] = as("glyphs") {
      rep(glyph) ^^ Glyphs.fromList
    }

    def glyph: Parser[Glyph] = as("glyph") {
      mapParser(
        Glyph.MoveAssessment.all.sortBy(_.symbol.size).map { g => g.symbol -> g },
        "glyph"
      )
    }

    def exists(c: String): Parser[Boolean] = c ^^^ true | success(false)

    def mapParser[A, B](pairs: Iterable[(A, B)], name: String): Parser[B] =
      pairs.foldLeft(failure(name + " not found"): Parser[B]) {
        case (acc, (a, b)) => a.toString ^^^ b | acc
      }
  }

  object TagParser extends RegexParsers with Logging {

    def apply(pdn: String): Valid[Tags] = parseAll(all, pdn) match {
      case f: Failure => "Cannot parse tags: %s\n%s".format(f.toString, pdn).failureNel
      case Success(tags, _) => succezz(Tags(tags))
      case err => "Cannot parse tags: %s\n%s".format(err.toString, pdn).failureNel
    }

    def fromFullPdn(pdn: String): Valid[Tags] =
      splitTagAndMoves(pdn) flatMap {
        case (tags, _) => apply(tags)
      }

    def all: Parser[List[Tag]] = as("all") {
      tags <~ """(.|\n)*""".r
    }

    def tags: Parser[List[Tag]] = rep(tag)

    def tag: Parser[Tag] = as("tag") {
      tagName ~ tagValue ^^ {
        case name ~ value => Tag(name, value)
      }
    }

    val tagName: Parser[String] = "[" ~> """[a-zA-Z]+""".r

    val tagValue: Parser[String] = """[^\]]+""".r <~ "]" ^^ {
      _.replace("\"", "")
    }
  }

  // there must be a newline between the tags and the first move/comment
  private def ensureTagsNewline(pdn: String): String =
    """"\]\s*(\{|\d+\.)""".r.replaceAllIn(pdn, m => "\"]\n" + m.group(1))

  //To accomodate some common PDN source that always adds a [FILENAME ""] tag on the bottom of every file
  private def ensureTagsNewlineReverse(pdn: String): String =
    """\[(FILENAME\s+")""".r.replaceAllIn(pdn, m => "\n[" + m.group(1))

  private def splitTagAndMoves(pdn: String): Valid[(String, String)] =
    ensureTagsNewlineReverse(ensureTagsNewline(pdn)).lines.toList.map(_.trim).filter(_.nonEmpty) span { line =>
      line lift 0 contains '['
    } match {
      case (tagLines, moveLines) => //Drop any tag in last line (accomodate [FILENAME)
        if (moveLines.lastOption.fold(false)(line => line.nonEmpty && line.head == '[' && line.last == ']')) succezz(tagLines.mkString("\n") -> moveLines.dropRight(1).mkString("\n"))
        else succezz(tagLines.mkString("\n") -> moveLines.mkString("\n"))
    }

}
