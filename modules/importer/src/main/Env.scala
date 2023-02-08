package lila.importer

import com.softwaremill.macwire.*

@Module
final class Env(gameRepo: lila.game.GameRepo)(using ec: scala.concurrent.ExecutionContext):

  lazy val forms = wire[ImporterForm]

  lazy val importer = wire[Importer]
