package lila.importer

import com.softwaremill.macwire._

final class Env(
    gameRepo: lila.game.GameRepo,
    scheduler: akka.actor.Scheduler
) {

  lazy val forms = wire[DataForm]

  lazy val importer = wire[Importer]
}
