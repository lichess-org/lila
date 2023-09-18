package lila.gathering

import scalatags.Text.all.*

class PrizeTest extends munit.FunSuite:

  test("richText prize regex not find btc >> url") {
    assertEquals(looksLikePrize("HqVrbTcy"), false)
    assertEquals(looksLikePrize("10btc"), true)
    assertEquals(looksLikePrize("ten btc"), true)
  }

  test("richText prize regex for TA description") {
    assertEquals(
      looksLikePrize("""Blitz Titled Arena July '23 Prizes: $500/$250/$125/$75/$50

  [Warm-up event](https://lichess.org/tournament/jul23bua) """),
      true
    )
  }
