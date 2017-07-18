package lila.activity

import lila.analyse.Analysis
import lila.game.Game

private object ActivityAggregation {

  import activities._
  import model._

  def game(game: Game, userId: String)(a: Activity): Option[Activity] = for {
    pt <- game.perfType
    player <- game playerByUserId userId
    score = Score.make(
      res = game.winnerColor map (player.color ==),
      rp = player.rating map { before =>
      RatingProg(Rating(before), Rating(before + ~player.ratingDiff))
    }
    )
  } yield a.copy(games = a.games.add(pt, score))

  def analysis(analysis: Analysis)(a: Activity): Option[Activity] =
    a.copy(comps = a.comps + GameId(analysis.id)).some

  def forumPost(post: lila.forum.Post, topic: lila.forum.Topic)(a: Activity) =
    post.userId map { userId =>
      a.copy(posts = a.posts.+(Posts.PostId(post.id), Posts.TopicId(topic.id)))
    }

  def puzzle(res: lila.puzzle.Puzzle.UserResult)(a: Activity) =
    a.copy(puzzles = a.puzzles + Score.make(
      res = res.result.win.some,
      rp = RatingProg(Rating(res.rating._1), Rating(res.rating._2)).some
    )).some
}
