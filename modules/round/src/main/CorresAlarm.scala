package lila.round

import akka.stream.scaladsl.*
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson.*

import lila.common.{ Bus, LilaScheduler, LilaStream }
import lila.db.dsl.{ *, given }
import lila.game.{ Game, Pov }

final private class CorresAlarm(
    coll: Coll,
    hasUserId: (Game, UserId) => Fu[Boolean],
    proxyGame: GameId => Fu[Option[Game]]
)(using
    Executor,
    Scheduler,
    akka.stream.Materializer
):

  private case class Alarm(
      _id: GameId,
      ringsAt: Instant, // when to notify the player
      expiresAt: Instant
  )

  private given BSONDocumentHandler[Alarm] = Macros.handler

  Bus.subscribeFun("finishGame") { case lila.game.actorApi.FinishGame(game, _, _) =>
    if (game.hasCorrespondenceClock && !game.hasAi) coll.delete.one($id(game.id)).unit
  }

  Bus.subscribeFun("moveEventCorres") { case lila.hub.actorApi.round.CorresMoveEvent(move, _, _, true, _) =>
    proxyGame(move.gameId) foreach {
      _ foreach { game =>
        game.playableCorrespondenceClock.ifTrue(game.bothPlayersHaveMoved) so { clock =>
          val remainingSeconds = clock remainingTime game.turnColor
          val ringsAt          = nowInstant.plusSeconds(remainingSeconds.toInt * 8 / 10)
          coll.update
            .one(
              $id(game.id),
              Alarm(
                _id = game.id,
                ringsAt = ringsAt,
                expiresAt = nowInstant.plusSeconds(remainingSeconds.toInt * 2)
              ),
              upsert = true
            )
            .void
        }
      }
    }
  }

  LilaScheduler("CorresAlarm", _.Every(10 seconds), _.AtMost(10 seconds), _.Delay(2 minutes)) {
    def deleteAlarm(id: GameId) = coll.delete.one($id(id)).void
    coll
      .find($doc("ringsAt" $lt nowInstant))
      .cursor[Alarm]()
      .documentSource(200)
      .mapAsyncUnordered(4)(alarm => proxyGame(alarm._id).map(alarm -> _))
      .mapAsyncUnordered(4) {
        case (_, Some(game)) =>
          val pov = Pov.ofCurrentTurn(game)
          deleteAlarm(game.id) zip
            pov.player.userId.fold(fuccess(true))(u => hasUserId(pov.game, u)).addEffect {
              if _ then () // already looking at the game
              else Bus.publish(lila.game.actorApi.CorresAlarmEvent(pov), "notify")
            }
        case (alarm, None) => deleteAlarm(alarm._id)
      }
      .toMat(LilaStream.sinkCount)(Keep.right)
      .run()
      .mon(_.round.alarm.time)
      .void
  }
