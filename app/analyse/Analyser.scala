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
    generator: (DbGame, Option[String]) ⇒ IO[Valid[Analysis]]) {

  private implicit val executor = Akka.system.dispatcher
  private implicit val timeout = Timeout(5 minutes)

  def apply(id: String): Future[Valid[Analysis]] = Future {
    applyIO(id).unsafePerformIO
  }

  private def applyIO(id: String): IO[Valid[Analysis]] = for {
    a ← analysisRepo byId id
    b ← a.fold(
      x ⇒ io(success(x)),
      for {
        gameOption ← gameRepo game id
        result ← gameOption.fold(
          game ⇒ for {
            initialFen ← gameRepo initialFen id
            analysis ← generator(game, initialFen)
          } yield analysis,
          io(!!("No such game " + id): Valid[Analysis])
        )
      } yield result
    )
  } yield b

}
