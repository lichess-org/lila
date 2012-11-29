package lila
package analyse

import game.{ DbGame, GameRepo, PgnRepo }
import user.UserRepo
import core.Settings

import com.mongodb.casbah.MongoCollection
import scalaz.effects._
import scala.concurrent.Future

final class AnalyseEnv(
    settings: Settings,
    gameRepo: GameRepo,
    pgnRepo: PgnRepo,
    userRepo: UserRepo,
    mongodb: String ⇒ MongoCollection,
    generator: () ⇒ (String, Option[String]) ⇒ Future[Valid[Analysis]]) {

  import settings._

  lazy val pgnDump = new PgnDump(
    gameRepo = gameRepo,
    analyser = analyser,
    userRepo = userRepo)

  lazy val analysisRepo = new AnalysisRepo(
    mongodb(AnalyseCollectionAnalysis))

  lazy val analyser = new Analyser(
    analysisRepo = analysisRepo,
    gameRepo = gameRepo,
    pgnRepo = pgnRepo,
    generator = generator)

  lazy val paginator = new PaginatorBuilder(
    analysisRepo = analysisRepo,
    cached = cached,
    gameRepo = gameRepo,
    maxPerPage = GamePaginatorMaxPerPage)

  lazy val cached = new Cached(
    analysisRepo = analysisRepo,
    nbTtl = AnalyseCachedNbTtl)
}
