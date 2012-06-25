package lila
package analyse

import game.{ DbGame, GameRepo }
import user.UserRepo
import core.Settings

import com.mongodb.casbah.MongoCollection
import scalaz.effects._

final class AnalyseEnv(
    settings: Settings,
    gameRepo: GameRepo,
    userRepo: UserRepo,
    mongodb: String ⇒ MongoCollection,
    generator: () ⇒ (DbGame, Option[String]) ⇒ IO[Valid[Analysis]]) {

  import settings._

  lazy val pgnDump = new PgnDump(
    userRepo = userRepo,
    gameRepo = gameRepo)

  lazy val analysisRepo = new AnalysisRepo(
    mongodb(MongoCollectionAnalysis))

  lazy val analyser = new Analyser(
    analysisRepo = analysisRepo,
    gameRepo = gameRepo,
    generator = generator)
}
