package lila
package command

import game.GameRepo
import round.Finisher

import scalaz.effects._

final class GameFinish(gameRepo: GameRepo, finisher: Finisher) {

  def apply(): IO[Unit] =
    for {
      games ← gameRepo.candidatesToAutofinish
      _ ← (finisher outoftimes games).sequence
    } yield ()
}
