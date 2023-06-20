package lila.swiss

import lila.common.{ Bus, LilaScheduler }
import lila.db.dsl.{ *, given }
import lila.hub.actorApi.push.TourSoon

final private class SwissNotify(mongo: SwissMongo)(using Executor, Scheduler):
  import BsonHandlers.given

  private val doneMemo = lila.memo.ExpireSetMemo[SwissId](10 minutes)

  LilaScheduler("SwissNotify", _.Every(20 seconds), _.AtMost(10 seconds), _.Delay(1 minute)) {
    mongo.swiss
      .find(
        $doc(
          "featurable" -> true,
          "settings.i" $lte 600 // hits the partial index
        ) ++ $doc(
          "startsAt" $gt nowInstant.plusMinutes(10) $lt nowInstant.plusMinutes(11),
          "_id" $nin doneMemo.keys
        )
      )
      .cursor[Swiss]()
      .list(5)
      .flatMap {
        _.map { swiss =>
          doneMemo put swiss.id
          SwissPlayer.fields { f =>
            mongo.player
              .distinctEasy[UserId, List](f.userId, $doc(f.swissId -> swiss.id))
              .map { userIds =>
                lila.common.Bus.publish(
                  TourSoon(tourId = swiss.id.value, tourName = swiss.name, userIds, swiss = true),
                  "tourSoon"
                )
              }
          }
        }.parallel.void
      }
  }
