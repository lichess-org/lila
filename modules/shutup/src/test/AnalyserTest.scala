package lila.shutup

/* Some of the texts in this file are horrible.
 * We reject and condemn them. They are here so we can verify
 * that Lichess detects them, to help moderators keeping the site nice.
 */
class AnalyserTest extends munit.FunSuite:

  private def find(t: String) = Analyser(t).badWords
  private def dirty(t: String) = Analyser(t).dirty
  private def ratio(t: String) = Analyser(t).ratio

  test("find one bad word"):
    assertEquals(find("fuck"), List("fuck"))
    assertEquals(find("well fuck me"), List("fuck"))

  test("find one bad word with punctuation"):
    assertEquals(find("fuck."), List("fuck"))
    assertEquals(find("well fuck! me"), List("fuck"))

  test("find many bad words"):
    assertEquals(find("fucked that shit"), List("fucked", "shit"))
    assertEquals(
      find("Beat them cunting nigger faggots with a communist dick"),
      List("cunting", "nigger", "faggots", "dick")
    )

  test("find many bad words with punctuation"):
    assertEquals(find("fucked? that shit!"), List("fucked", "shit"))

  test("find no bad words"):
    assertEquals(find(""), Nil)
    assertEquals(find("hello there"), Nil)
    assertEquals(
      find:
        "A sonnet is a poetic form which originated in Italy; Giacomo Da Lentini is credited with its invention."
      ,
      Nil
    )
    assertEquals(find("computer analysis"), Nil)

  test("find badly spelled words"):
    assertEquals(find("fuk"), List("fuk"))
    assertEquals(find("well fuk me"), List("fuk"))
    assertEquals(find("foo ashole bar fukd"), List("ashole", "fukd"))
    assertEquals(find("faaaaaaaaagg faaaagot fag"), List("faaaaaaaaagg", "faaaagot", "fag"))

  test("find variants"):
    assertEquals(find("cunt kunt cunting kawa kunting"), List("cunt", "kunt", "cunting", "kunting"))
    assertEquals(find("ass as ashole"), List("ass", "ashole"))

  test("find plurals"):
    assertEquals(
      find("asses cunts kunts cuntings kawas kuntings"),
      List("asses", "cunts", "kunts", "cuntings", "kuntings")
    )

  test("find phrases"):
    assertEquals(find("I think you suck"), List("you suck"))
    assertEquals(find("you should suck my"), List("suck my"))

  test("50 shades of fuck"):
    assertEquals(find("fuck fffuuk fektard feak fak phuk"), List("fuck", "fffuuk", "fektard", "fak", "phuk"))

  test("compute ratio"):
    assertEquals(ratio("fuck that shit"), 2d / 3)
    assertEquals(ratio("Beat them cunting nigger faggots with a communist dick"), 12d / 9)
    assertEquals(ratio("hello there"), 0d)
    assertEquals(ratio(""), 0d)

  test("chessbot variations"):
    assertEquals(find("foo chessbot bar"), List("chessbot"))
    assertEquals(find("foo chess-bot bar"), List("chess-bot"))
    assertEquals(find("foo chess_bot bar"), List("chess_bot"))
    assertEquals(find("foo chess-bot.com bar"), List("chess-bot.com"))
    assertEquals(find("foo chess_bot_com bar"), List("chess_bot_com"))
    assertEquals(find("foo chessbotcom bar"), List("chessbotcom"))
    assertEquals(find("foo http://chess-bot.com bar"), List("chess-bot.com"))

  test("rat false positives"):
    assertEquals(find("test rat is rate some rates what rated"), List("rat"))

  test("russian chars"):
    assertEquals(find("sеx"), List("sex"))

  test("russian"):
    assertEquals(find("сука пизда"), List("сука", "пизда"))

  test("russian with punctuation"):
    assertEquals(find("сука! ?пизда"), List("сука", "пизда"))

  test("with punctuation"):
    assertEquals(find("nigger?"), List("nigger"))

  test("prod msg"):
    assert:
      dirty:
        """Hello fucking arab. It's morning here I am getting ready to fuck your smelly mom and sister together today. Just wanna inform you ;"""
