package lila
package importer

import game.GameRepo
import round.{ Hand, Finisher }

final class ImporterEnv(
  gameRepo: GameRepo,
  hand: Hand,
  finisher: Finisher) {

  lazy val forms = new DataForm

  lazy val importer = new Importer(gameRepo, hand, finisher)
}
