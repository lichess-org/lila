package lila
package analyse

import game.{ DbGame, GameRepo, PgnRepo }

import scalaz.effects._
import play.api.libs.concurrent.Akka
import play.api.Play.current
import akka.dispatch.Future
import akka.util.duration._
import akka.util.Timeout

final class Analyser(
    analysisRepo: AnalysisRepo,
    gameRepo: GameRepo,
    pgnRepo: PgnRepo,
    generator: () ⇒ (String, Option[String]) ⇒ Future[Valid[Analysis]]) {

  private implicit val executor = Akka.system.dispatcher
  private implicit val timeout = Timeout(5 minutes)

  def get(id: String): IO[Option[Analysis]] = analysisRepo byId id

  def has(id: String): IO[Boolean] = analysisRepo isDone id

  def getOrGenerate(id: String, userId: String, admin: Boolean): Future[Valid[Analysis]] =
    getOrGenerateIO(id, userId, admin)

  private def getOrGenerateIO(id: String, userId: String, admin: Boolean): Future[Valid[Analysis]] = for {
    a ← ioToFuture(analysisRepo doneById id)
    b ← a.fold(
      x ⇒ Future(success(x)),
      for {
        userInProgress ← ioToFuture(admin.fold(
          io(false),
          analysisRepo userInProgress userId
        ))
        gameOption ← ioToFuture(gameRepo game id)
        pgnString ← ioToFuture(pgnRepo get id)
        result ← gameOption.filterNot(_ ⇒ userInProgress).fold(
          game ⇒ for {
            _ ← ioToFuture(analysisRepo.progress(id, userId))
            initialFen ← ioToFuture(gameRepo initialFen id)
            analysis ← generator()(pgnString, initialFen)
            _ ← ioToFuture(analysis.prefixFailuresWith("Analysis").fold(
              fail ⇒ for {
                _ ← putFailures(fail)
                _ ← analysisRepo.fail(id, fail)
              } yield (),
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
