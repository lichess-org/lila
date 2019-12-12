package lila.importer

import com.softwaremill.macwire._

@Module
final class Env(gameRepo: lila.game.GameRepo) {

  lazy val forms = wire[DataForm]

  lazy val importer = wire[Importer]
}
