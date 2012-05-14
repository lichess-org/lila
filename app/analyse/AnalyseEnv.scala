package lila
package analyse

import com.mongodb.casbah.MongoCollection

import game.GameRepo
import user.UserRepo

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
