package lila.chess
package format

import scala.util.parsing.combinator._

object PgnParser extends RegexParsers {

  def apply(pgn: String): Valid[ParsedPgn] =
    parseAll(all, pgn) match {
      case f: Failure       ⇒ f.toString.failNel
      case Success(sans, _) ⇒ scalaz.Scalaz.success(sans)
    }

  def all: Parser[ParsedPgn] = tags ~ moves <~ (result?) ^^ {
    case ts ~ ms ⇒ ParsedPgn(tags = ts, sans = ms)
  }

  def tags: Parser[List[Tag]] = rep(tag) <~ (CRLF?)

  def tag: Parser[Tag] = tagName ~ tagValue <~ (CRLF?) ^^ {
    case "FEN" ~ value ⇒ Fen(value)
    case name ~ value  ⇒ Unknown(name, value)
  }

  val tagName: Parser[String] = "[" ~> """[a-zA-Z]+""".r

  val tagValue: Parser[String] = "\"" ~> """[^"]+""".r <~ "\"]"

  def moves: Parser[List[San]] = rep(move)

  val result: Parser[String] = "*" | "1/2-1/2" | "0-1" | "1-0"

  def move: Parser[San] = (number?) ~> (qCastle | kCastle | std) <~ (comment?)

  val comment: Parser[String] = "{" ~> """[^\}]+""".r <~ "}"

  val qCastle: Parser[San] = "O-O-O" ^^^ Castle(QueenSide)

  val kCastle: Parser[San] = "O-O" ^^^ Castle(KingSide)

  def std: Parser[Std] = (simple | disambiguated) ~ suffixes ^^ {
    case std ~ suf ⇒ std withSuffixes suf
  }

  val number: Parser[String] = """\d+\.+ """.r

  def simple: Parser[Std] = role ~ x ~ dest ^^ {
    case ro ~ ca ~ de ⇒ Std(dest = de, role = ro, capture = ca)
  }

  def disambiguated: Parser[Std] = role ~ opt(file) ~ opt(rank) ~ x ~ dest ^^ {
    case ro ~ fi ~ ra ~ ca ~ de ⇒ Std(
      dest = de, role = ro, capture = ca, file = fi, rank = ra)
  }

  def suffixes: Parser[Suffixes] = opt(promotion) ~ check ~ checkmate ^^ {
    case p ~ c ~ cm ⇒ Suffixes(c, cm, p)
  }

  val x = exists("x")

  val check = exists("+")

  val checkmate = exists("#")

  val role = mapParser(Role.allByPgn, "role") | success(Pawn)

  val file = mapParser(rangeToMap('a' to 'h'), "file")

  val rank = mapParser(rangeToMap('1' to '8'), "rank")

  val promotion = "=" ~> mapParser(Role.allPromotableByPgn, "promotion")

  val dest = mapParser(Pos.allKeys, "dest")

  val CRLF = "\r\n" | "\n"

  def exists(c: String): Parser[Boolean] = c ^^^ true | success(false)

  def rangeToMap(r: Iterable[Char]) = r.zipWithIndex.toMap mapValues (_ + 1)

  def mapParser[A, B](map: Map[A, B], name: String): Parser[B] =
    map.foldLeft(failure(name + " not found"): Parser[B]) {
      case (acc, (a, b)) ⇒ a.toString ^^^ b | acc
    }
}
