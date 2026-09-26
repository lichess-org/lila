package lila.fide

import java.time.YearMonth
import chess.rating.Elo

class FideRatingHistoryTest extends munit.FunSuite:

  test("FideRatingPoint.decode"):
    assertEquals(FideRatingPoint(YearMonth.parse("2021-03"), Elo(1800)), FideRatingPoint(2021031800))
    assertEquals(FideRatingPoint(YearMonth.parse("1991-12"), Elo(777)), FideRatingPoint(1991120777))

  test("FideRatingPoint.encode"):
    assertEquals(FideRatingPoint(2021031800).date, YearMonth.parse("2021-03"))
    assertEquals(FideRatingPoint(2021031800).elo, Elo(1800))
    assertEquals(FideRatingPoint(1991120777).date, YearMonth.parse("1991-12"))
    assertEquals(FideRatingPoint(1991120777).elo, Elo(777))
