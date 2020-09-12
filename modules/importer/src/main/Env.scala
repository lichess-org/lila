package lila.importer

import com.softwaremill.macwire._

@Module
final class Env(gameRepo: lila.game.GameRepo)(implicit ec: scala.concurrent.ExecutionContext) {

  lazy val forms = wire[ImporterForm]

  lazy val importer = wire[Importer]
}
