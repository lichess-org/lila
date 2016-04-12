package lila.game

import scala.concurrent.duration._

import lila.db.dsl._
import lila.user.UserRepo

private[game] final class Cli(coll: Coll) extends lila.common.Cli {

  def process = {

    case "game" :: "per" :: "day" :: days =>
      GameRepo nbPerDay {
        (days.headOption flatMap parseIntOption) | 30
      } map (_ mkString " ")
  }
}
