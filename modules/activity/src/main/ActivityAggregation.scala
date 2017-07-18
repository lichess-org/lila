package lila.activity

import lila.game.Game

private object ActivityAggregation {

  import Activity._

  def addGame(ua: Activity.WithUserId, game: Game): Option[Activity] = for {
    pt <- game.perfType
    player <- game playerByUserId ua.userId
    won = game.winnerUserId map (ua.userId ==)
    score = Score(
      win = won.has(true) ?? 1,
      loss = won.has(false) ?? 1,
      draw = won.isEmpty ?? 1,
      rd = RatingDiff(~player.ratingDiff * 100)
    )
  } yield ua.activity.copy(
    games = ua.activity.games.add(pt, score)
  )
}
