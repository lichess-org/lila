package lila.round

import scala.concurrent.duration._

import actorApi.round.Cheat
import lila.game.Game
import lila.hub.actorApi.map.Tell

private[round] final class FenUrlWatch(
    roundMap: akka.actor.ActorRef,
    reporter: akka.actor.ActorSelection,
    scheduler: akka.actor.Scheduler) {

  private val stack = lila.memo.Builder.expiry[String, Int](30.seconds)

  def apply(game: Game) {
    if (game.playable) {
      val id = game.id + game.turnColor.name.head
      ~Option(stack getIfPresent id) match {
        case nb if nb < 5 => stack.put(id, nb + 1)
        case nb =>
          play.api.Logger("FenUrlWatch").warn(s"Detected http://lichess.org/${game.id} ${game.turnColor}")
          stack.invalidate(id)
          scheduler.scheduleOnce(
            (1 + scala.util.Random.nextInt(15)).seconds,
            roundMap,
            Tell(game.id, Cheat(game.turnColor))
          )
          game.player.userId foreach { userId =>
            reporter ! lila.hub.actorApi.report.Cheater(userId,
              s"Cheat detected on http://lichess.org/${game.id}, using a FEN bot.")
          }
      }
    }
  }
}
