package lila.app
package importer

import game.GameRepo
import round.{ Hand, Finisher }

import scalaz.effects.IO

final class ImporterEnv(
    gameRepo: GameRepo,
    hand: Hand,
    finisher: Finisher,
    bookmark: (String, String) ⇒ IO[Unit]) {

  lazy val forms = new DataForm

  lazy val importer = new Importer(gameRepo, hand, finisher, bookmark)
}
