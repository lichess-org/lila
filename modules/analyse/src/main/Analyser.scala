package lila.analyse

import lila.game.{ Game, GameRepo, PgnRepo }
import lila.db.api._
import tube.analysisTube
import lila.game.tube.gameTube

import akka.actor.ActorRef
import akka.pattern.ask

private[analyse] final class Analyser(ai: lila.hub.ActorLazyRef) {

  private implicit val timeout = makeTimeout minutes 5

  def get(id: String): Fu[Option[Analysis]] = $find.byId[Analysis](id)

  def has(id: String): Fu[Boolean] = AnalysisRepo isDone id

  def getOrGenerate(id: String, userId: String, admin: Boolean): Fu[Valid[Analysis]] = for {
    a ← AnalysisRepo doneById id
    b ← a.fold(for {
      userInProgress ← admin.fold(
        fuccess(false),
        AnalysisRepo userInProgress userId)
      gameOption ← $find.byId[Game](id)
      pgnString ← PgnRepo get id
      result ← gameOption.filterNot(_ ⇒ userInProgress).fold(
        fufail("No such game " + id): Fu[Valid[Analysis]]
      ) { game ⇒
          for {
            _ ← AnalysisRepo.progress(id, userId)
            initialFen ← GameRepo initialFen id
            analysis ← ai ? lila.hub.actorApi.ai.Analyse(pgnString, initialFen) mapTo manifest[Valid[Analysis]]
            _ ← analysis.prefixFailuresWith("[analysis] ").fold(
              fail ⇒ AnalysisRepo.fail(id, fail) >>- fail.foreach(logwarn),
              AnalysisRepo.done(id, _)
            )
          } yield analysis
        }
    } yield result) { x ⇒ fuccess(success(x)) }
  } yield b
}
