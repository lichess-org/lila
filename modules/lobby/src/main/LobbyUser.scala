package lila.lobby

import lila.rating.{ Glicko, Perf, PerfType }
import lila.user.User
import lila.pool.Blocking

private[lobby] case class LobbyUser(
    id: UserId,
    username: UserName,
    lame: Boolean,
    bot: Boolean,
    perfMap: LobbyUser.PerfMap,
    blocking: Blocking
):

  def perfAt(pt: PerfType): LobbyPerf = perfMap.get(pt.key) | LobbyPerf.default

  def ratingAt(pt: PerfType): IntRating = perfAt(pt).rating

private[lobby] object LobbyUser:

  given UserIdOf[LobbyUser] = _.id

  type PerfMap = Map[Perf.Key, LobbyPerf]

  def make(user: User.WithPerfs, blocking: Blocking) =
    LobbyUser(
      id = user.id,
      username = user.username,
      lame = user.lame,
      bot = user.isBot,
      perfMap = perfMapOf(user.perfs),
      blocking = blocking
    )

  private def perfMapOf(perfs: lila.user.UserPerfs): PerfMap =
    perfs.perfs.view.collect {
      case (pt, perf) if pt != PerfType.Puzzle && perf.nonEmpty =>
        pt.key -> LobbyPerf(perf.intRating, perf.provisional)
    }.toMap

// TODO opaque type Int (minus for provisional)
case class LobbyPerf(rating: IntRating, provisional: RatingProvisional)

object LobbyPerf:

  val default = LobbyPerf(Glicko.default.intRating, provisional = RatingProvisional.Yes)
