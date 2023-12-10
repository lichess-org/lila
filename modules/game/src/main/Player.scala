package lila.game

import cats.derived.*
import chess.{ Ply, Color, ByColor }

import lila.user.User

case class PlayerUser(id: UserId, rating: IntRating, ratingDiff: Option[IntRatingDiff]) derives Eq

case class Player(
    id: GamePlayerId,
    color: Color,
    aiLevel: Option[Int],
    isWinner: Option[Boolean] = None,
    isOfferingDraw: Boolean = false,
    proposeTakebackAt: Ply = Ply.initial, // ply when takeback was proposed
    userId: Option[UserId] = None,
    rating: Option[IntRating] = None,
    ratingDiff: Option[IntRatingDiff] = None,
    provisional: RatingProvisional = RatingProvisional.No,
    blurs: Blurs = Blurs.zeroBlurs.zero,
    berserk: Boolean = false,
    name: Option[String] = None
) derives Eq:

  def playerUser =
    userId.flatMap: uid =>
      rating map { PlayerUser(uid, _, ratingDiff) }

  def isAi = aiLevel.isDefined

  def isHuman = !isAi

  def hasUser = userId.isDefined

  def isUser[U: UserIdOf](u: U) = userId.has(u.id)

  def userInfos: Option[Player.UserInfo] =
    (userId, rating).mapN: (id, ra) =>
      Player.UserInfo(id, ra, provisional)

  def wins = ~isWinner

  def goBerserk = copy(berserk = true)

  def finish(winner: Boolean) = copy(isWinner = winner option true)

  def offerDraw = copy(isOfferingDraw = true)

  def removeDrawOffer = copy(isOfferingDraw = false)

  def proposeTakeback(ply: Ply) = copy(proposeTakebackAt = ply)

  def removeTakebackProposition = copy(proposeTakebackAt = Ply.initial)

  def isProposingTakeback = proposeTakebackAt > 0

  def nameSplit: Option[(String, Option[Int])] =
    name map:
      case Player.nameSplitRegex(n, r) => n.trim -> r.toIntOption
      case n                           => n      -> none

  def before(other: Player) =
    ((rating, id), (other.rating, other.id)) match
      case ((Some(a), _), (Some(b), _)) if a != b => a.value > b.value
      case ((Some(_), _), (None, _))              => true
      case ((None, _), (Some(_), _))              => false
      case ((_, a), (_, b))                       => a.value < b.value

  def ratingAfter = rating.map(_.applyDiff(~ratingDiff))

  def stableRating = rating ifFalse provisional.value

  def stableRatingAfter = stableRating.map(_.applyDiff(~ratingDiff))

  def light = LightPlayer(color, aiLevel, userId, rating, ratingDiff, provisional, berserk)

object Player:

  private val nameSplitRegex = """([^(]++)\((\d++)\)""".r

  def makeAnon(color: Color, aiLevel: Option[Int] = None): Player = Player(
    id = IdGenerator.player(color),
    color = color,
    aiLevel = aiLevel
  )

  def make(color: Color, userPerf: (UserId, lila.rating.Perf)): Player = make(
    color = color,
    userId = userPerf._1,
    rating = userPerf._2.intRating,
    provisional = userPerf._2.glicko.provisional
  )

  def make(
      color: Color,
      userId: UserId,
      rating: IntRating,
      provisional: RatingProvisional
  ): Player =
    Player(
      id = IdGenerator.player(color),
      color = color,
      aiLevel = none,
      userId = userId.some,
      rating = rating.some,
      provisional = provisional
    )

  def make(color: Color, user: Option[User.WithPerf]): Player =
    user.fold(makeAnon(color))(u => make(color, u.user.id -> u.perf))

  def makeImported(
      color: Color,
      name: Option[String],
      rating: Option[IntRating]
  ): Player =
    Player(
      id = IdGenerator.player(color),
      color = color,
      aiLevel = none,
      name = name orElse "?".some,
      rating = rating
    )

  case class HoldAlert(ply: Ply, mean: Int, sd: Int):
    def suspicious = HoldAlert.suspicious(ply)
  object HoldAlert:
    type Map = ByColor[Option[HoldAlert]]
    val emptyMap: Map                 = ByColor(none, none)
    def suspicious(ply: Ply): Boolean = ply >= 16 && ply <= 40
    def suspicious(m: Map): Boolean   = m exists { _ exists (_.suspicious) }

  case class UserInfo(id: UserId, rating: IntRating, provisional: RatingProvisional)

  import reactivemongo.api.bson.*
  import lila.db.dsl.{ *, given }

  given BSONDocumentHandler[HoldAlert] = Macros.handler

  object BSONFields:

    val aiLevel           = "ai"
    val isOfferingDraw    = "od"
    val proposeTakebackAt = "ta"
    val rating            = "e"
    val ratingDiff        = "d"
    val provisional       = "p"
    val blursBits         = "l"
    val holdAlert         = "h"
    val berserk           = "be"
    val name              = "na"

  def from(light: LightGame, color: Color, ids: String, doc: Bdoc): Player =
    import BSONFields.*
    val p = light.player(color)
    Player(
      id = GamePlayerId(color.fold(ids take 4, ids drop 4)),
      color = p.color,
      aiLevel = p.aiLevel,
      isWinner = light.win.map(_ == color),
      isOfferingDraw = doc booleanLike isOfferingDraw getOrElse false,
      proposeTakebackAt = Ply(doc int proposeTakebackAt getOrElse 0),
      userId = p.userId,
      rating = p.rating,
      ratingDiff = p.ratingDiff,
      provisional = p.provisional,
      blurs = doc.getAsOpt[Blurs](blursBits) getOrElse Blurs.zeroBlurs.zero,
      berserk = p.berserk,
      name = doc string name
    )

  def playerWrite(p: Player) =
    import BSONFields.*
    import Blurs.*
    $doc(
      aiLevel           -> p.aiLevel,
      isOfferingDraw    -> p.isOfferingDraw.option(true),
      proposeTakebackAt -> p.proposeTakebackAt.some.filter(_ > 0),
      rating            -> p.rating,
      ratingDiff        -> p.ratingDiff,
      provisional       -> p.provisional.yes.option(true),
      blursBits         -> p.blurs.nonEmpty.so(p.blurs),
      name              -> p.name
    )
