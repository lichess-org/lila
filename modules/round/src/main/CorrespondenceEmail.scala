package lila.round

import akka.stream.scaladsl._
import org.joda.time.{ LocalTime, Period }
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.ReadPreference

import lila.common.Bus
import lila.common.LilaStream
import lila.db.dsl._
import lila.game.{ Game, GameRepo, Pov }
import lila.hub.actorApi.mailer._
import lila.pref.PrefApi
import lila.user.UserRepo

final private class CorrespondenceEmail(gameRepo: GameRepo, userRepo: UserRepo, prefApi: PrefApi)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  private val (runAfter, runBefore) = (LocalTime parse "05:00", LocalTime parse "05:11")

  def tick(): Unit = {
    val now = LocalTime.now
    if (now.isAfter(runAfter) && now.isBefore(runBefore)) run()
  }.unit

  private def run() =
    opponentStream
      .map { Bus.publish(_, "dailyCorrespondenceNotif") }
      .toMat(LilaStream.sinkCount)(Keep.right)
      .run()
      .addEffect(lila.mon.round.correspondenceEmail.emails.record(_).unit)
      .monSuccess(_.round.correspondenceEmail.time)

  private def opponentStream =
    prefApi.coll
      .aggregateWith(readPreference = ReadPreference.secondaryPreferred) { framework =>
        import framework._
        // hit partial index
        List(
          Match($doc("corresEmailNotif" -> true)),
          Project($id(true)),
          PipelineOperator(
            $lookup.pipeline(
              from = userRepo.coll,
              as = "user",
              local = "_id",
              foreign = "_id",
              pipe = List(
                $doc("$match"   -> $doc("enabled" -> true)),
                $doc("$project" -> $id(true))
              )
            )
          ),
          Unwind("user"),
          PipelineOperator(
            $lookup.simple(
              from = gameRepo.coll,
              as = "games",
              local = "_id",
              foreign = Game.BSONFields.playingUids // hit index
            )
          )
        )
      }
      .documentSource()
      .mapConcat { doc =>
        import lila.game.BSONHandlers._
        (for {
          userId <- doc string "_id"
          games  <- doc.getAsOpt[List[Game]]("games")
          povs = games
            .flatMap(Pov.ofUserId(_, userId))
            .filter(pov => pov.game.isCorrespondence && pov.game.nonAi && pov.isMyTurn)
            .sortBy(_.remainingSeconds getOrElse Int.MaxValue)
          if !povs.isEmpty
          opponents = povs map { pov =>
            CorrespondenceOpponent(
              pov.opponent.userId,
              pov.remainingSeconds.map(remainingSeconds => new Period(remainingSeconds * 1000L)),
              pov.game.id
            )
          }
        } yield CorrespondenceOpponents(userId, opponents)).toList
      }
}
