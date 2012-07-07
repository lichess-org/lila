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
    generator: () ⇒ (DbGame, Option[String]) ⇒ Future[Valid[Analysis]]) {

  private implicit val executor = Akka.system.dispatcher
  private implicit val timeout = Timeout(5 minutes)

  def get(id: String): IO[Option[Analysis]] = analysisRepo byId id

  def has(id: String): IO[Boolean] = analysisRepo isDone id

  def getOrGenerate(id: String, userId: String): Future[Valid[Analysis]] = 
    getOrGenerateIO(id, userId)

  private def getOrGenerateIO(id: String, userId: String): Future[Valid[Analysis]] = for {
    a ← ioToFuture(analysisRepo byId id)
    b ← a.fold(
      x ⇒ Future(success(x)),
      for {
        gameOption ← ioToFuture(gameRepo game id)
        result ← gameOption.fold(
          game ⇒ for {
            _ ← ioToFuture(analysisRepo.progress(id, userId))
            initialFen ← ioToFuture(gameRepo initialFen id)
            analysis ← generator()(game, initialFen)
            _ ← ioToFuture(analysis.fold(
              analysisRepo.fail(id, _),
              analysisRepo.done(id, _)
            ))
          } yield analysis,
          Future(!!("No such game " + id): Valid[Analysis])
        )
      } yield result
    )
  } yield b

  private def ioToFuture[A](ioa: IO[A]) = Future {
    ioa.unsafePerformIO
  }
}
