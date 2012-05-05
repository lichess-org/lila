package lila.chess
package format
import scala.util.parsing.combinator._

object PgnReader {

  // Standard Algebraic Notation
  sealed trait San

  case class Std(
      dest: Pos,
      capture: Boolean = false,
      role: Option[Role] = None,
      file: Option[Char] = None,
      rank: Option[Char] = None,
      check: Boolean = false,
      checkmate: Boolean = false,
      promotion: Option[PromotableRole] = None) extends San {

    def withSuffixes(s: Suffixes) = copy(
      check = s.check,
      checkmate = s.checkmate,
      promotion = s.promotion)
  }

  case class Suffixes(
    check: Boolean,
    checkmate: Boolean,
    promotion: Option[PromotableRole])

  case object KingSideCastle extends San

  case object QueenSideCastle extends San

  def apply(pgn: String): Valid[List[Move]] = SanParser(pgn) flatMap { sans ⇒
    println(sans)
    success(List[Move]())
  }

  object SanParser extends RegexParsers {

    def apply(pattern: String): Valid[List[San]] =
      parseAll(moves, pattern) match {
        case f: Failure       ⇒ f.toString.failNel
        case Success(sans, _) ⇒ scalaz.Scalaz.success(sans)
      }

    def moves: Parser[List[San]] = rep(log(move)("move"))

    def move: Parser[San] = kCastle | qCastle | std

    def kCastle: Parser[San] = "O-O" ^^^ KingSideCastle

    def qCastle: Parser[San] = "O-O-O" ^^^ QueenSideCastle

    def std: Parser[Std] =
      (simple | disambiguated) ~ suffixes ^^ {
        case std ~ suf ⇒ std withSuffixes suf
      }

    def simple: Parser[Std] = role ~ x ~ dest ^^ {
      case ro ~ ca ~ de ⇒ Std(dest = de, role = ro, capture = ca)
    }

    def disambiguated: Parser[Std] = role ~ file ~ rank ~ x ~ dest ^^ {
      case ro ~ fi ~ ra ~ ca ~ de ⇒ Std(
        dest = de, role = ro, capture = ca, file = fi, rank = ra)
    }

    def suffixes: Parser[Suffixes] = opt(promotion) ~ check ~ checkmate ^^ {
      case p ~ c ~ cm ⇒ Suffixes(c, cm, p)
    }

    def x = exists("x")

    def check = exists("+")

    def checkmate = exists("#")

    def exists(c: String): Parser[Boolean] = (c | "") ^^ (_.nonEmpty)

    def promotion: Parser[PromotableRole] =
      "=" ~> promotable

    val promotable: Parser[PromotableRole] =
      Role.allPromotableByPgn.foldLeft(failure("Invalid promotion"): Parser[PromotableRole]) {
        case (acc, (pgn, role)) ⇒ pgn.toString ^^^ role | acc
      }

    val file = rangeParser('a' to 'h')

    val rank = rangeParser('1' to '8')

    def rangeParser(range: Iterable[Char]): Parser[Option[Char]] =
      range.foldLeft(("" ^^^ None): Parser[Option[Char]]) { (acc, c) ⇒
        (c.toString ^^^ Some(c)) | acc
      }

    val role: Parser[Option[Role]] =
      Role.allByPgn.foldLeft(("" ^^^ None): Parser[Option[Role]]) {
        case (acc, (pgn, role)) ⇒ pgn.toString ^^^ Some(role) | acc
      }

    val dest: Parser[Pos] =
      Pos.allKeys.foldLeft(failure("Invalid position"): Parser[Pos]) {
        case (acc, (str, pos)) ⇒ str ^^^ pos | acc
      }
  }
}
