package lila.swiss

import lila.common.LightUser
import lila.rating.PerfType
import lila.user.User

case class SwissPlayer(
    id: SwissPlayer.Id, // swissId:userId
    swissId: SwissId,
    userId: UserId,
    rating: IntRating,
    provisional: Boolean,
    points: SwissPoints,
    tieBreak: Swiss.TieBreak,
    performance: Option[Swiss.Performance],
    score: Swiss.Score,
    absent: Boolean,
    byes: Set[SwissRoundNumber] // byes granted by the pairing system - the player was here
):
  def is(uid: UserId): Boolean       = uid == userId
  def is(user: User): Boolean         = is(user.id)
  def is(other: SwissPlayer): Boolean = is(other.userId)
  def present                         = !absent

  def recomputeScore =
    copy(
      score = Swiss.makeScore(points, tieBreak, performance | Swiss.Performance(rating.value.toFloat))
    )

object SwissPlayer:

  opaque type Id = String
  object Id extends OpaqueString[Id]

  def makeId(swissId: SwissId, userId: UserId) = Id(s"$swissId:$userId")

  private[swiss] def make(
      swissId: SwissId,
      user: User,
      perf: PerfType
  ): SwissPlayer =
    new SwissPlayer(
      id = makeId(swissId, user.id),
      swissId = swissId,
      userId = user.id,
      rating = user.perfs(perf).intRating,
      provisional = user.perfs(perf).provisional,
      points = SwissPoints fromDouble 0,
      tieBreak = Swiss.TieBreak(0),
      performance = none,
      score = Swiss.Score(0),
      absent = false,
      byes = Set.empty
    ).recomputeScore

  case class WithRank(player: SwissPlayer, rank: Int):
    def is(other: WithRank)       = player is other.player
    def withUser(user: LightUser) = WithUserAndRank(player, user, rank)
    override def toString         = s"$rank. ${player.userId}[${player.rating}]"

  case class WithUser(player: SwissPlayer, user: LightUser)

  case class WithUserAndRank(player: SwissPlayer, user: LightUser, rank: Int)

  sealed private[swiss] trait Viewish:
    val player: SwissPlayer
    val rank: Int
    val user: lila.common.LightUser
    val sheet: SwissSheet

  private[swiss] case class View(
      player: SwissPlayer,
      rank: Int,
      user: lila.common.LightUser,
      pairings: Map[SwissRoundNumber, SwissPairing],
      sheet: SwissSheet
  ) extends Viewish

  private[swiss] case class ViewExt(
      player: SwissPlayer,
      rank: Int,
      user: lila.common.LightUser,
      pairings: Map[SwissRoundNumber, SwissPairing.View],
      sheet: SwissSheet
  ) extends Viewish

  type PlayerMap = Map[UserId, SwissPlayer]

  def toMap(players: List[SwissPlayer]): PlayerMap =
    players.view.map(p => p.userId -> p).toMap

  object Fields:
    val id          = "_id"
    val swissId     = "s"
    val userId      = "u"
    val rating      = "r"
    val provisional = "pr"
    val points      = "p"
    val tieBreak    = "t"
    val performance = "e"
    val score       = "c"
    val absent      = "a"
    val byes        = "b"
  def fields[A](f: Fields.type => A): A = f(Fields)
