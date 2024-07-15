package lila.lobby

import lila.core.perf.{ UserPerfs, UserWithPerfs }
import lila.core.pool.Blocking
import lila.rating.UserPerfsExt.perfsList
import lila.rating.{ Glicko, PerfType }

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

  type PerfMap = Map[PerfKey, LobbyPerf]

  def make(user: UserWithPerfs, blocking: Blocking) =
    LobbyUser(
      id = user.id,
      username = user.username,
      lame = user.lame,
      bot = user.isBot,
      perfMap = perfMapOf(user.perfs),
      blocking = blocking
    )

  private def perfMapOf(perfs: UserPerfs): PerfMap =
    perfs.perfsList.view.collect {
      case (pk, perf) if pk != PerfKey.puzzle && perf.nonEmpty =>
        pk -> LobbyPerf(perf.intRating, perf.provisional)
    }.toMap

// TODO opaque type Int (minus for provisional)
case class LobbyPerf(rating: IntRating, provisional: RatingProvisional)

object LobbyPerf:

  val default = LobbyPerf(Glicko.default.intRating, provisional = RatingProvisional.Yes)
