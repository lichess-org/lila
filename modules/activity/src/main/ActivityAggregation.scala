package lila.activity

import lila.analyse.Analysis
import lila.game.Game

private object ActivityAggregation {

  import Activity._

  def addGame(game: Game, userId: String)(a: Activity): Option[Activity] = for {
    pt <- game.perfType
    player <- game playerByUserId userId
    won = game.winnerColor map (player.color ==)
    score = Score(
      win = won.has(true) ?? 1,
      loss = won.has(false) ?? 1,
      draw = won.isEmpty ?? 1,
      rp = player.rating map { before =>
      RatingProg(Rating(before), Rating(before + ~player.ratingDiff))
    }
    )
  } yield a.copy(games = a.games.add(pt, score))

  def addAnalysis(analysis: Analysis)(a: Activity): Option[Activity] =
    a.copy(comps = a.comps + GameId(analysis.id)).some

  def addForumPost(post: lila.forum.Post, topic: lila.forum.Topic)(a: Activity) =
    post.userId map { userId =>
      a.copy(posts = a.posts.+(Posts.PostId(post.id), Posts.TopicId(topic.id)))
    }

  def addPuzzle(res: lila.puzzle.Puzzle.UserResult)(a: Activity) = a.copy(puzzles = {
    val p = a.puzzles
    val id = PuzzleId(res.puzzleId)
    p.copy(
      win = if (res.result.win) p.win + id else p.win,
      loss = if (res.result.loss) p.loss + id else p.loss,
      ratingProg = p.ratingProg.fold(RatingProg(Rating(res.rating._1), Rating(res.rating._2))) { rp =>
      rp.copy(after = Rating(res.rating._2))
    } some
    )
  }).some
}
