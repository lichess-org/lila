package lila.round

import akka.stream.scaladsl.*
import java.time.{ LocalTime, Duration }
import reactivemongo.akkastream.cursorProducer

import lila.common.Bus
import lila.common.LilaStream
import lila.db.dsl.{ *, given }
import lila.game.{ Game, GameRepo, Pov }
import lila.hub.actorApi.mailer.*
import lila.notify.NotifyColls
import lila.user.UserRepo

final private class CorrespondenceEmail(gameRepo: GameRepo, userRepo: UserRepo, notifyColls: NotifyColls)(
    using
    Executor,
    akka.stream.Materializer
):

  private val (runAfter, runBefore) = (LocalTime parse "05:00", LocalTime parse "05:10")

  def tick(): Unit =
    val now = LocalTime.now
    if now.isAfter(runAfter) && now.isBefore(runBefore) then run()

  private def run() =
    opponentStream
      .map { Bus.publish(_, "dailyCorrespondenceNotif") }
      .runWith(LilaStream.sinkCount)
      .addEffect(lila.mon.round.correspondenceEmail.emails.record(_))
      .monSuccess(_.round.correspondenceEmail.time)

  private def opponentStream =
    notifyColls.pref
      .aggregateWith(readPreference = ReadPref.priTemp): framework =>
        import framework.*
        // hit partial index
        List(
          Match($doc("correspondenceEmail" -> true)),
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
      .documentSource()
      .mapConcat: doc =>
        import lila.game.BSONHandlers.given
        (for
          userId <- doc.getAsOpt[UserId]("_id")
          games  <- doc.getAsOpt[List[Game]]("games")
          povs = games
            .flatMap(Pov(_, userId))
            .filter(pov => pov.game.isCorrespondence && pov.game.nonAi && pov.isMyTurn)
            .sortBy(_.remainingSeconds getOrElse Int.MaxValue)
          if !povs.isEmpty
          opponents = povs.map: pov =>
            CorrespondenceOpponent(
              pov.opponent.userId,
              pov.remainingSeconds.map(s => Duration.ofSeconds(s.toLong)),
              pov.game.id
            )
        yield CorrespondenceOpponents(userId, opponents)).toList
