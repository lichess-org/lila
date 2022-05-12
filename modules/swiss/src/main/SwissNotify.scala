package lila.swiss

import akka.actor.ActorSystem
import org.joda.time.DateTime
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.Bus
import lila.common.{ AtMost, Every, ResilientScheduler }
import lila.db.dsl._
import lila.hub.actorApi.push.TourSoon
import lila.user.User

final private class SwissNotify(colls: SwissColls)(implicit
    ec: ExecutionContext,
    system: ActorSystem
) {
  import BsonHandlers._

  private val doneMemo = new lila.memo.ExpireSetMemo(10 minutes)

  ResilientScheduler(every = Every(20 seconds), timeout = AtMost(10 seconds), initialDelay = 1 minute) {
    colls.swiss
      .find(
        $doc(
          "featurable" -> true,
          "settings.i" $lte 600 // hits the partial index
        ) ++ $doc(
          "startsAt" $gt DateTime.now.plusMinutes(10) $lt DateTime.now.plusMinutes(11),
          "_id" $nin doneMemo.keys
        )
      )
      .cursor[Swiss]()
      .list(5)
      .flatMap {
        _.map { swiss =>
          doneMemo put swiss.id.value
          SwissPlayer.fields { f =>
            colls.player
              .distinctEasy[User.ID, List](f.userId, $doc(f.swissId -> swiss.id))
              .map { userIds =>
                lila.common.Bus.publish(
                  TourSoon(tourId = swiss.id.value, tourName = swiss.name, userIds, swiss = true),
                  "tourSoon"
                )
              }
          }
        }.sequenceFu.void
      }
  }
}
