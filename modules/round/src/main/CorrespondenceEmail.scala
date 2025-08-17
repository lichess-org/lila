package lila.round
import reactivemongo.akkastream.cursorProducer

import java.time.{ Duration, LocalTime }

import lila.common.{ Bus, LilaStream }
import lila.core.game.GameRepo
import lila.core.misc.mailer.*
import lila.core.notify.NotifyApi
import lila.db.dsl.{ *, given }
import lila.user.UserRepo

final private class CorrespondenceEmail(gameRepo: GameRepo, userRepo: UserRepo, notifyApi: NotifyApi)(using
    Executor,
    akka.stream.Materializer
):

  private val (runAfter, runBefore) = (LocalTime.parse("05:00"), LocalTime.parse("05:10"))

  def tick(): Unit =
    val now = LocalTime.now
    if now.isAfter(runAfter) && now.isBefore(runBefore) then run()

  private def run() =
    opponentStream
      .map { Bus.pub(_) }
      .runWith(LilaStream.sinkCount)
      .addEffect(lila.mon.round.correspondenceEmail.emails.record(_))
      .monSuccess(_.round.correspondenceEmail.time)

  private def opponentStream =
    notifyApi.prefColl
      .aggregateWith(readPreference = ReadPref.sec): framework =>
        import framework.*
        // hit partial index
        List(
          Match($doc("correspondenceEmail" -> true)),
          Project($id(true)),
          PipelineOperator(
            $lookup.simple(
              from = userRepo.coll,
              as = "user",
              local = "_id",
              foreign = "_id",
              pipe = List(
                $doc("$match" -> $doc("enabled" -> true)),
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
              foreign = lila.game.Game.BSONFields.playingUids // hit index
            )
          )
        )
      .documentSource()
      .mapConcat: doc =>
        import lila.game.BSONHandlers.given
        (for
          userId <- doc.getAsOpt[UserId]("_id")
          games <- doc.getAsOpt[List[Game]]("games")
          povs = games
            .flatMap(Pov(_, userId))
            .filter(pov => pov.game.isCorrespondence && pov.game.nonAi && pov.isMyTurn)
            .sortBy(_.remainingSeconds.fold(Int.MaxValue)(_.value))
          if !povs.isEmpty
          opponents = povs.map: pov =>
            CorrespondenceOpponent(
              pov.opponent.userId,
              pov.remainingSeconds.map(s => Duration.ofSeconds(s.value)),
              pov.game.id
            )
        yield CorrespondenceOpponents(userId, opponents)).toList
