package lila.storm

import org.joda.time.DateTime
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.user.User

final class StormRunApi(coll: Coll)(implicit ctx: ExecutionContext) {

  def record(data: StormForm.RunData, user: Option[User]): Funit = {
    lila.mon.storm.run.score(user.isDefined).record(data.score).unit
    user ?? { u =>
      coll.insert
        .one(
          StormRun(
            _id = StormRun.randomId,
            user = u.id,
            date = DateTime.now,
            puzzles = data.puzzles,
            score = data.score,
            moves = data.moves,
            combo = data.combo,
            time = data.time,
            highest = data.highest
          )
        )
        .void
    }
  }
}
