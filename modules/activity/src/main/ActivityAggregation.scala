package lila.activity

import lila.analyse.Analysis
import lila.game.{ Game, Player }
import lila.user.User

private object ActivityAggregation {

  import activities._
  import model._

  def puzzle(res: lila.puzzle.Puzzle.UserResult)(a: Activity) =
    a.copy(puzzles = Some(~a.puzzles + Score.make(
      res = res.result.win.some,
      rp = RatingProg(Rating(res.rating._1), Rating(res.rating._2)).some
    ))).some
}
