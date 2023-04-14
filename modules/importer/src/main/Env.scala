package lila.importer

import com.softwaremill.macwire.*

@Module
@annotation.nowarn("msg=unused")
final class Env(gameRepo: lila.game.GameRepo)(using Executor):

  lazy val forms = wire[ImporterForm]

  lazy val importer = wire[Importer]
