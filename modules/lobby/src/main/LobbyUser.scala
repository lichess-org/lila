package lila.lobby

import lila.rating.{ Glicko, Perf, PerfType }
import lila.user.User

private[lobby] case class LobbyUser(
    id: User.ID,
    username: String,
    lame: Boolean,
    bot: Boolean,
    perfMap: LobbyUser.PerfMap,
    blocking: Set[User.ID]
) {

  def perfAt(pt: PerfType): LobbyPerf = perfMap.get(pt.key) | LobbyPerf.default

  def ratingAt(pt: PerfType): Int = perfAt(pt).rating
}

private[lobby] object LobbyUser {

  type PerfMap = Map[Perf.Key, LobbyPerf]

  def make(user: User, blocking: Set[User.ID]) =
    LobbyUser(
      id = user.id,
      username = user.username,
      lame = user.lame,
      bot = user.isBot,
      perfMap = perfMapOf(user.perfs),
      blocking = blocking
    )

  private def perfMapOf(perfs: lila.user.Perfs): PerfMap =
    perfs.perfs.view.collect {
      case (key, perf) if key != PerfType.Puzzle.key && perf.nonEmpty =>
        key -> LobbyPerf(perf.intRating, perf.provisional)
    }.toMap
}

case class LobbyPerf(rating: Int, provisional: Boolean)

object LobbyPerf {

  val default = LobbyPerf(Glicko.defaultIntRating, provisional = true)
}
