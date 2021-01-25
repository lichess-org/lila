package lila.storm

import org.joda.time.DateTime
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.user.User

final class StormRunApi(coll: Coll)(implicit ctx: ExecutionContext) {

  def record(data: StormForm.RunData, user: Option[User]): Funit = {
    monitor(data)
    user ?? { u =>
      coll.insert
        .one(
          StormRun(
            _id = StormRun.randomId,
            user = u.id,
            date = DateTime.now,
            puzzles = data.puzzles,
            wins = data.wins,
            moves = data.moves,
            combo = data.combo,
            time = data.time,
            highest = data.highest
          )
        )
        .void
    }
  }

  private def monitor(run: StormForm.RunData): Unit = {}
}
