package lila.analyse

import lila.game.{ Game, GameRepo, PgnRepo }
import lila.db.api._
import tube.analysisTube
import lila.game.tube.gameTube
import makeTimeout.veryLarge

import akka.pattern.ask

final class Analyser(ai: lila.hub.ActorLazyRef) {

  def get(id: String): Fu[Option[Analysis]] = $find.byId[Analysis](id)

  def has(id: String): Fu[Boolean] = AnalysisRepo isDone id

  def hasMany(ids: Seq[String]): Fu[Set[String]] =
    $primitive[Analysis, String]($select byIds ids, "_id")(_.asOpt[String]) map (_.toSet) 

  def getOrGenerate(id: String, userId: String, admin: Boolean): Fu[Analysis] = {

    def generate: Fu[Analysis] =
      admin.fold(fuccess(none), AnalysisRepo userInProgress userId) flatMap {
        _.fold(doGenerate) { progressId ⇒
          fufail("[analysis] %s already analyses %s, won't process %s".format(userId, progressId, id))
        }
      }

    def doGenerate: Fu[Analysis] =
      $find.byId[Game](id) zip (PgnRepo getNonEmpty id) flatMap {
        case (Some(game), Some(pgn)) ⇒ (for {
          _ ← AnalysisRepo.progress(id, userId)
          initialFen ← GameRepo initialFen id
          analysis ← {
            ai ? lila.hub.actorApi.ai.Analyse(id, pgn, initialFen)
          } mapTo manifest[Analysis]
        } yield analysis) flatFold (
          e ⇒ AnalysisRepo.fail(id, e).mapTo[Analysis],
          a ⇒ AnalysisRepo.done(id, a) >> fuccess(a)
        )
        case _ ⇒ fufail[Analysis]("[analysis] %s no game or pgn found" format (id))
      }

    AnalysisRepo doneById id flatMap {
      _.fold(generate)(fuccess(_))
    }
  }
}
