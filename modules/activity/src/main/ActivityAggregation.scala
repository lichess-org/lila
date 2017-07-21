package lila.activity

import lila.analyse.Analysis
import lila.game.{ Game, Player }
import lila.user.User

private object ActivityAggregation {

  import activities._
  import model._

  def game(game: Game, userId: User.ID)(a: Activity): Option[Activity] = for {
    pt <- game.perfType
    player <- game playerByUserId userId
    score = Score.make(game wonBy player.color, RatingProg make player)
  } yield a.copy(
    games = if (game.isCorrespondence) a.games else a.games.orDefault.add(pt, score).some,
    corres =
    if (game.hasCorrespondenceClock) Some { ~a.corres + (GameId(game.id), false, true) }
    else a.corres
  )

  def analysis(analysis: Analysis)(a: Activity): Option[Activity] =
    a.copy(comps = Some(~a.comps + GameId(analysis.id))).some

  def forumPost(post: lila.forum.Post, topic: lila.forum.Topic)(a: Activity) =
    post.userId map { userId =>
      a.copy(posts = Some(~a.posts + PostId(post.id)))
    }

  def puzzle(res: lila.puzzle.Puzzle.UserResult)(a: Activity) =
    a.copy(puzzles = Some(~a.puzzles + (
      s = Score.make(
        res = res.result.win.some,
        rp = RatingProg(Rating(res.rating._1), Rating(res.rating._2)).some
      ),
      id = PuzzleId(res.puzzleId)
    ))).some
}
