package lila.storm

import org.joda.time.DateTime
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.user.User

final class StormDayApi(coll: Coll)(implicit ctx: ExecutionContext) {

  import StormDay._
  import StormBsonHandlers._

  def addRun(data: StormForm.RunData, user: Option[User]): Funit = {
    lila.mon.storm.run.score(user.isDefined).record(data.score).unit
    user ?? { u =>
      val todayId = Id today u.id
      coll
        .one[StormDay]($id(todayId))
        .map {
          _.getOrElse(StormDay empty todayId) add data
        }
        .flatMap { day =>
          coll.update.one($id(day._id), day, upsert = true)
        }
        .void
    }
  }
}
