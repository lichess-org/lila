package lila.game

import scala.concurrent.duration._

import lila.db.api._
import lila.user.UserRepo
import tube.gameTube

private[game] final class Cli(computeElos: ComputeElos) extends lila.common.Cli {

  def process = {

    case "game" :: "per" :: "day" :: days ⇒
      GameRepo nbPerDay {
        (days.headOption flatMap parseIntOption) | 30
      } map (_ mkString " ")

    case "game" :: "typecheck" :: Nil ⇒ {
      loginfo("Counting games...")
      val size = $count($select.all).await
      var nb = 0
      val bulkSize = 1000
      ($enumerate.bulk[Option[Game]]($query.all, bulkSize) { gameOptions ⇒
        val nbGames = gameOptions.flatten.size
        if (nbGames != bulkSize)
          logwarn("Built %d of %d games".format(nbGames, bulkSize))
        nb = nb + nbGames
        loginfo("Typechecked %d of %d games".format(nb, size))
        funit
      }).await(2.hours)
      fuccess("Done")
    }

    case "game" :: "compute" :: "elos" :: names ⇒ {
      loginfo("Computing elo of %s".format(names.isEmpty ? "All users" | names.mkString(", ")))
      UserRepo nameds names flatMap { 
        _.map(computeElos.apply).sequenceFu
      } inject "Done"
    }
  }

}
