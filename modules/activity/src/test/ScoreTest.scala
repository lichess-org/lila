package lila.activity

import chess.{ Color, IntRating, Status }
import chess.rating.IntRatingDiff
import chess.variant.Standard

import lila.core.game.{ LightGame, LightPlayer, LightPov }

class ScoreTest extends munit.FunSuite:

  def pov(id: String, rating: Int, diff: Int) =
    val me =
      LightPlayer(Color.White, none, UserId("me").some, IntRating(rating).some, IntRatingDiff(diff).some)
    val opponent = LightPlayer(Color.Black, none, UserId("opponent").some)
    val winner = if diff > 0 then Color.White else Color.Black
    LightPov(LightGame(GameId(id), me, opponent, Status.Resign, winner.some, Standard), Color.White)

  test("correspondence games ended on the same day, newest first"):
    val povs = List(
      pov("game0004", 2164, 6),
      pov("game0003", 2157, 7),
      pov("game0002", 2165, -8),
      pov("game0001", 2158, 7)
    )
    assertEquals(
      Score.make(povs),
      lila.core.rating.Score(3, 1, 0, lila.core.rating.RatingProg(IntRating(2158), IntRating(2170)).some)
    )
