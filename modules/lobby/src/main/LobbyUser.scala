package lila.lobby

import chess.IntRating
import chess.rating.RatingProvisional

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

private opaque type LobbyPerf = Int
private object LobbyPerf extends OpaqueInt[LobbyPerf]:
  def apply(rating: IntRating, provisional: RatingProvisional): LobbyPerf =
    LobbyPerf(rating.value * (if provisional.yes then -1 else 1))
  extension (lp: LobbyPerf)
    def rating: IntRating              = IntRating(math.abs(lp))
    def provisional: RatingProvisional = RatingProvisional(lp < 0)
  val default = LobbyPerf(-Glicko.default.intRating.value)
