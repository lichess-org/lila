package lila.game

import scala.concurrent.duration._

import lila.db.api._
import lila.user.UserRepo
import tube.gameTube

private[game] final class Cli(
    db: lila.db.Env,
    system: akka.actor.ActorSystem) extends lila.common.Cli {

  def process = {

    case "game" :: "per" :: "day" :: days =>
      GameRepo nbPerDay {
        (days.headOption flatMap parseIntOption) | 30
      } map (_ mkString " ")

    case "game" :: "typecheck" :: Nil =>
      logger.info("Counting games...")
      val size = $count($select.all).await
      var nb = 0
      val bulkSize = 1000
      ($enumerate.bulk[Option[Game]]($query.all, bulkSize) { gameOptions =>
        val nbGames = gameOptions.flatten.size
        if (nbGames != bulkSize)
          logger.warn("Built %d of %d games".format(nbGames, bulkSize))
        nb = nb + nbGames
        logger.info("Typechecked %d of %d games".format(nb, size))
        funit
      }).await(2.hours)
      fuccess("Done")
  }

}
