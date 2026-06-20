package lila.shutup

/* Some of the texts in this file are horrible.
 * We reject and condemn them. They are here so we can verify
 * that Lichess detects them, to help moderators keeping the site nice.
 */
class AnalyserTest extends munit.FunSuite:

  private def find(t: String) = Analyser(t).badWords
  private def grave(t: String) = Analyser(t).critical
  private def ratio(t: String) = Analyser(t).ratio

  test("critical"):
    assert(grave("gets cancer"))
    assert(grave("kys"))
    assert(grave("kill your father"))
    assert(grave("murder you"))

  test("find one bad word"):
    assertEquals(find("cheater"), List("cheater"))
    assertEquals(find("well fuck me"), List("fuck"))
    assertEquals(find("you chickens"), List("chickens"))

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
    assertEquals(find("press f for respects"), Nil)

  test("find badly spelled words"):
    assertEquals(find("cheatedd cheaterr"), List("cheatedd", "cheaterr"))
    assertEquals(find("pnis pusy quer"), List("pnis", "pusy", "quer"))
    assertEquals(find("foo ashole bar fuks"), List("ashole", "fuks"))
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
    assertEquals(find("neck your mom"), List("neck your mom"))

  test("50 shades of fuck"):
    assertEquals(
      find("fck fk fuck fuckk fuk fffuuk fucko fuckeds fektard feak fak phuk"),
      List("fck", "fk", "fuck", "fuckk", "fuk", "fffuuk", "fucko", "fuckeds", "fektard", "fak", "phuk")
    )

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

  test("with slash and plural"):
    assertEquals(find("/Vigger"), List("vigger"))
    assertEquals(find("V/igger"), List("vigger"))
    assertEquals(find("/vigger"), List("vigger"))
    assertEquals(find("I like /Viggers"), List("viggers"))

  test("i workaround with https://www.codetable.net/decimal/775"):
    assertEquals(find("ni̇gger"), List("nigger"))
