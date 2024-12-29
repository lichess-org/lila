package lila.relay

import chess.format.pgn.{ Tag, Tags }
import scalalib.model.Seconds

class GameJsonTest extends munit.FunSuite:

  test("clock"):
    val clock =
      DgtJson.ClockJson(white = Some(Seconds(4468)), black = Some(Seconds(30)), time = 1734688185870L)

    assertEquals(
      DgtJson.GameJson(Nil, None, Some(clock)).clockTags,
      Tags(
        List(
          Tag(_.WhiteClock, "4468"),
          Tag(_.BlackClock, "30")
          // Tag(_.ReferenceTime, "2024-12-20T09:49:45.870Z")
        )
      )
    )

  test("toPgn mini"):

    val moves = List(
      "d4",
      "e6 1818",
      "Nf3 +19",
      "Nc6 1821+28"
    )

    val expected = """d4 e6 { [%clk 0:30:18] } Nf3 { [%emt 0:00:19] } Nc6 { [%clk 0:30:21] [%emt 0:00:28] }"""

    val game = DgtJson.GameJson(moves, None)
    assertEquals(game.toPgn(Tags.empty).value.trim, expected)
