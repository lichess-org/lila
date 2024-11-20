package lila.core
package history

import scalalib.model.Days
import _root_.chess.IntRating

import lila.core.perf.{ Perf, PerfKey }
import lila.core.user.{ User, WithPerf }
import lila.core.userId.UserId

trait HistoryApi:
  def addPuzzle(user: User, completedAt: Instant, perf: Perf): Funit
  def progresses(users: List[WithPerf], perfKey: PerfKey, days: Days): Fu[List[PairOf[IntRating]]]
  def lastWeekTopRating(user: UserId, perf: PerfKey): Fu[IntRating]
  def setPerfRating(user: User, perf: PerfKey, rating: IntRating): Funit
