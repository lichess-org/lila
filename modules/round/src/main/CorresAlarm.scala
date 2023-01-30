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
    akka.actor.Scheduler,
    akka.stream.Materializer
):

  private case class Alarm(
      _id: GameId,
      ringsAt: DateTime, // when to notify the player
      expiresAt: DateTime
  )

  private given BSONDocumentHandler[Alarm] = Macros.handler

  Bus.subscribeFun("finishGame") { case lila.game.actorApi.FinishGame(game, _, _) =>
    if (game.hasCorrespondenceClock && !game.hasAi) coll.delete.one($id(game.id)).unit
  }

  Bus.subscribeFun("moveEventCorres") { case lila.hub.actorApi.round.CorresMoveEvent(move, _, _, true, _) =>
    proxyGame(move.gameId) foreach {
      _ foreach { game =>
        game.bothPlayersHaveMoved ?? {
          game.playableCorrespondenceClock ?? { clock =>
            val remainingTime = clock remainingTime game.turnColor
            val ringsAt       = nowDate.plusSeconds(remainingTime.toInt * 8 / 10)
            coll.update
              .one(
                $id(game.id),
                Alarm(
                  _id = game.id,
                  ringsAt = ringsAt,
                  expiresAt = nowDate.plusSeconds(remainingTime.toInt * 2)
                ),
                upsert = true
              )
              .void
          }
        }
      }
    }
  }

  LilaScheduler("CorresAlarm", _.Every(10 seconds), _.AtMost(10 seconds), _.Delay(2 minutes)) {
    coll
      .find($doc("ringsAt" $lt nowDate))
      .cursor[Alarm]()
      .documentSource(200)
      .mapAsyncUnordered(4)(alarm => proxyGame(alarm._id))
      .via(LilaStream.collect)
      .mapAsyncUnordered(4) { game =>
        val pov = Pov(game, game.turnColor)
        pov.player.userId.fold(fuccess(true))(u => hasUserId(pov.game, u)).addEffect {
          case true  => // already looking at the game
          case false => Bus.publish(lila.game.actorApi.CorresAlarmEvent(pov), "notify")
        } >> coll.delete.one($id(game.id))
      }
      .toMat(LilaStream.sinkCount)(Keep.right)
      .run()
      .mon(_.round.alarm.time)
      .void
  }
