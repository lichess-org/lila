package lila
package importer

import game.GameRepo
import round.Hand

final class ImporterEnv(
  gameRepo: GameRepo,
  hand: Hand) {

  lazy val forms = new DataForm

  lazy val importer = new Importer(gameRepo, hand)
}
