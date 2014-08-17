package lila.round

import scala.concurrent.duration._

import lila.db.api._
import lila.user.UserRepo
import lila.game.Game
import lila.game.tube.gameTube

private[round] final class Cli(
    db: lila.db.Env,
    roundMap: akka.actor.ActorRef,
    system: akka.actor.ActorSystem) extends lila.common.Cli {

  def process = {

    case "round" :: "abort" :: "clock" :: Nil =>
      $enumerate[Game]($query(play.api.libs.json.Json.obj(
        Game.BSONFields.status -> chess.Status.Started.id,
        Game.BSONFields.clock -> $exists(true)
      ))) { game =>
        roundMap ! lila.hub.actorApi.map.Tell(game.id, actorApi.round.AbortForMaintenance)
      } inject "done"
  }

}
