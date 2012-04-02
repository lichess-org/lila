package lila.system
package command

import db.GameRepo
import scalaz.effects._

final class GameFinishCommand(gameRepo: GameRepo, finisher: Finisher) {

  def apply(): IO[Unit] =
    for {
      games ← gameRepo.candidatesToAutofinish
      _ ← putStrLn("[cron] finish %d games (%s)".format(
        games.size, games take 3 map (_.id) mkString ", "))
      _ ← (finisher outoftimes games).sequence
    } yield ()
}
