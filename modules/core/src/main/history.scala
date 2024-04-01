package lila.core
package history

import lila.core.rating.Perf
import lila.core.rating.PerfKey
import lila.core.user.User
import lila.core.user.WithPerf

trait HistoryApi:
  def addPuzzle: (user: User, completedAt: Instant, perf: Perf) => Funit
  def progresses: (users: List[WithPerf], perfKey: PerfKey, days: Days) => Fu[List[PairOf[IntRating]]]
