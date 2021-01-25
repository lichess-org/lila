package lila.storm

import org.joda.time.DateTime

import lila.common.Day
import lila.user.User
import org.joda.time.Days

case class StormDay(
    _id: StormDay.Id,
    runs: Int,
    score: Int,
    moves: Int,
    combo: Int,
    time: Int,
    highest: Int
) {

  def add(run: StormForm.RunData) = copy(
    runs = runs + 1,
    score = score atLeast run.score,
    moves = moves atLeast run.moves,
    combo = combo atLeast run.combo,
    time = time atLeast run.time,
    highest = highest atLeast run.highest
  )
}

object StormDay {

  case class Id(userId: User.ID, day: Day)
  object Id {
    def today(userId: User.ID) = Id(userId, Day.today)
  }

  def empty(id: Id) = StormDay(id, 0, 0, 0, 0, 0, 0)
}
