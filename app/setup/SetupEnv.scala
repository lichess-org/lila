package lila
package setup

import com.mongodb.casbah.MongoCollection
import scalaz.effects._

import core.Settings
import game.{ DbGame, GameRepo }
import ai.Ai

final class SetupEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    gameRepo: GameRepo,
    timelinePush: DbGame ⇒ IO[Unit],
    ai: () ⇒ Ai) {

  import settings._

  lazy val configRepo = new UserConfigRepo(mongodb(MongoCollectionConfig))

  lazy val formFactory = new FormFactory(
    configRepo = configRepo)

  lazy val processor = new Processor(
    configRepo = configRepo,
    gameRepo = gameRepo,
    timelinePush = timelinePush,
    ai = ai)
}
