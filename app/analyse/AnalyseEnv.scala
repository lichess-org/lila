package lila
package analyse

import game.GameRepo
import user.UserRepo
import core.Settings

final class AnalyseEnv(
    settings: Settings,
    gameRepo: GameRepo,
    userRepo: UserRepo) {

  import settings._

  lazy val pgnDump = new PgnDump(
    userRepo = userRepo,
    gameRepo = gameRepo)

  lazy val gameInfo = GameInfo(pgnDump) _
}
