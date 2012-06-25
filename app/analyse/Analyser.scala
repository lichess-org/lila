package lila
package analyse

import game.{ DbGame, GameRepo }

import scalaz.effects._
import play.api.libs.concurrent.Akka
import play.api.Play.current
import akka.dispatch.Future
import akka.util.duration._
import akka.util.Timeout

final class Analyser(
    analysisRepo: AnalysisRepo,
    gameRepo: GameRepo,
    generator: () ⇒ (DbGame, Option[String]) ⇒ IO[Valid[Analysis]]) {

  private implicit val executor = Akka.system.dispatcher
  private implicit val timeout = Timeout(5 minutes)

  def get(id: String): IO[Option[Analysis]] = analysisRepo byId id

  def getOrGenerate(id: String, userId: String): Future[Valid[Analysis]] = Future {
    getOrGenerateIO(id, userId).unsafePerformIO
  }

  private def getOrGenerateIO(id: String, userId: String): IO[Valid[Analysis]] = for {
    a ← analysisRepo byId id
    b ← a.fold(
      x ⇒ io(success(x)),
      for {
        gameOption ← gameRepo game id
        result ← gameOption.fold(
          game ⇒ for {
            _ ← analysisRepo.progress(id, userId)
            initialFen ← gameRepo initialFen id
            analysis ← generator()(game, initialFen)
            _ ← analysis.fold(
              analysisRepo.fail(id, _),
              analysisRepo.done(id, _)
            )
          } yield analysis,
          io(!!("No such game " + id): Valid[Analysis])
        )
      } yield result
    )
  } yield b

}
