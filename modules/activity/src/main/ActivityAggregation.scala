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
      rd = RatingDiff(~player.ratingDiff)
    )
  } yield a.copy(games = a.games.add(pt, score))

  def addAnalysis(analysis: Analysis)(a: Activity): Option[Activity] =
    a.copy(comps = a.comps + GameId(analysis.id)).some

  def addForumPost(post: lila.forum.Post, topic: lila.forum.Topic)(a: Activity) =
    post.userId map { userId =>
      a.copy(posts = a.posts.+(Posts.PostId(post.id), Posts.TopicId(topic.id)))
    }
}
