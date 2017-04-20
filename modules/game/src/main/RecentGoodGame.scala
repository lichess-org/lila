package lila.game

import akka.actor._

private final class RecentGoodGame extends Actor {

  val maxIds = 500
  var ids = List.empty[Game.ID]

  def matches(g: lila.game.Game) =
    g.variant.standard &&
      g.rated &&
      g.turns >= 10 && {
        g.averageUsersRating.??(1900 <=) ||
          (g.averageUsersRating.??(1700 <=) && ids.size < 100)
      } &&
      g.clock.??(_.estimateTotalSeconds >= 5 * 60)

  def receive = {
    case lila.game.actorApi.FinishGame(g, _, _) if matches(g) =>
      ids = g.id :: ids.take(maxIds - 1)
    case true => ids match {
      case head :: tail =>
        sender ! head.some
        ids = tail
      case _ => sender ! none
    }
  }
}
