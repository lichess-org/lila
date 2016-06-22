package lila.game

import chess.{ Pos, Piece, Color }

import lila.user.User

case class PlayerUser(id: String, rating: Int, ratingDiff: Option[Int])

case class Player(
    id: String,
    color: Color,
    aiLevel: Option[Int],
    isWinner: Option[Boolean] = None,
    isOfferingDraw: Boolean = false,
    isOfferingRematch: Boolean = false,
    lastDrawOffer: Option[Int] = None,
    proposeTakebackAt: Int = 0, // ply when takeback was proposed
    userId: Option[String] = None,
    rating: Option[Int] = None,
    ratingDiff: Option[Int] = None,
    provisional: Boolean = false,
    blurs: Int = 0,
    holdAlert: Option[Player.HoldAlert] = None,
    berserk: Boolean = false,
    name: Option[String] = None) {

  def playerUser = userId flatMap { uid =>
    rating map { PlayerUser(uid, _, ratingDiff) }
  }

  def withUser(id: User.ID, perf: lila.rating.Perf): Player = copy(
    userId = id.some,
    rating = perf.intRating.some,
    provisional = perf.glicko.provisional)

  def isAi = aiLevel.isDefined

  def isHuman = !isAi

  def hasUser = userId.isDefined

  def isUser(u: User) = userId.fold(false)(_ == u.id)

  def userInfos: Option[Player.UserInfo] = (userId |@| rating) {
    case (id, ra) => Player.UserInfo(id, ra, provisional)
  }

  def wins = isWinner getOrElse false

  def hasHoldAlert = holdAlert.isDefined
  def hasSuspiciousHoldAlert = holdAlert ?? (_.suspicious)

  def goBerserk = copy(berserk = true)

  def finish(winner: Boolean) = copy(isWinner = winner option true)

  def offerDraw(turn: Int) = copy(
    isOfferingDraw = true,
    lastDrawOffer = Some(turn)
  )

  def removeDrawOffer = copy(isOfferingDraw = false)

  def offerRematch = copy(isOfferingRematch = true)

  def removeRematchOffer = copy(isOfferingRematch = false)

  def proposeTakeback(ply: Int) = copy(proposeTakebackAt = ply)

  def removeTakebackProposition = copy(proposeTakebackAt = 0)

  def isProposingTakeback = proposeTakebackAt != 0

  def withName(name: String) = copy(name = name.some)

  def nameSplit: Option[(String, Option[Int])] = name map {
    case Player.nameSplitRegex(n, r) => n -> parseIntOption(r)
    case n                           => n -> none
  }

  def before(other: Player) = ((rating, id), (other.rating, other.id)) match {
    case ((Some(a), _), (Some(b), _)) if a != b => a > b
    case ((Some(_), _), (None, _))              => true
    case ((None, _), (Some(_), _))              => false
    case ((_, a), (_, b))                       => a < b
  }

  def ratingAfter = rating map (_ + ~ratingDiff)

  def stableRating = rating ifFalse provisional

  def stableRatingAfter = stableRating map (_ + ~ratingDiff)
}

object Player {

  private val nameSplitRegex = """^([^\(]+)\((.+)\)$""".r

  def make(
    color: Color,
    aiLevel: Option[Int]): Player = Player(
    id = IdGenerator.player,
    color = color,
    aiLevel = aiLevel)

  def white = make(Color.White, None)

  def black = make(Color.Black, None)

  case class HoldAlert(ply: Int, mean: Int, sd: Int) {

    def suspicious = ply >= 16 && ply <= 40
  }

  case class UserInfo(id: String, rating: Int, provisional: Boolean)

  import reactivemongo.bson.Macros
  implicit val holdAlertBSONHandler = Macros.handler[HoldAlert]

  object BSONFields {

    val aiLevel = "ai"
    val isOfferingDraw = "od"
    val isOfferingRematch = "or"
    val lastDrawOffer = "ld"
    val proposeTakebackAt = "ta"
    val rating = "e"
    val ratingDiff = "d"
    val provisional = "p"
    val blurs = "b"
    val holdAlert = "h"
    val berserk = "be"
    val name = "na"
  }

  import reactivemongo.bson._
  import lila.db.BSON

  type Id = String
  type UserId = Option[String]
  type Win = Option[Boolean]
  type Builder = Color => Id => UserId => Win => Player

  private def safeRange(range: Range, name: String)(userId: Option[String])(v: Int): Option[Int] =
    if (range contains v) Some(v)
    else {
      logger.warn(s"Player $userId $name=$v (range: ${range.min}-${range.max})")
      None
    }

  private val ratingRange = safeRange(0 to 4000, "rating") _
  private val ratingDiffRange = safeRange(-1000 to 1000, "ratingDiff") _

  implicit val playerBSONHandler = new BSON[Builder] {

    import BSONFields._

    def reads(r: BSON.Reader) = color => id => userId => win => Player(
      id = id,
      color = color,
      aiLevel = r intO aiLevel,
      isWinner = win,
      isOfferingDraw = r boolD isOfferingDraw,
      isOfferingRematch = r boolD isOfferingRematch,
      lastDrawOffer = r intO lastDrawOffer,
      proposeTakebackAt = r intD proposeTakebackAt,
      userId = userId,
      rating = r intO rating flatMap ratingRange(userId),
      ratingDiff = r intO ratingDiff flatMap ratingDiffRange(userId),
      provisional = r boolD provisional,
      blurs = r intD blurs,
      holdAlert = r.getO[HoldAlert](holdAlert),
      berserk = r boolD berserk,
      name = r strO name)

    def writes(w: BSON.Writer, o: Builder) =
      o(chess.White)("0000")(none)(none) |> { p =>
        BSONDocument(
          aiLevel -> p.aiLevel,
          isOfferingDraw -> w.boolO(p.isOfferingDraw),
          isOfferingRematch -> w.boolO(p.isOfferingRematch),
          lastDrawOffer -> p.lastDrawOffer,
          proposeTakebackAt -> w.intO(p.proposeTakebackAt),
          rating -> p.rating,
          ratingDiff -> p.ratingDiff,
          provisional -> w.boolO(p.provisional),
          blurs -> w.intO(p.blurs),
          holdAlert -> p.holdAlert,
          name -> p.name)
      }
  }
}
