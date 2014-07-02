package lila.game

import chess.{ Pos, Piece, Color }

import lila.user.User

case class Player(
    id: String,
    color: Color,
    aiLevel: Option[Int],
    isWinner: Option[Boolean] = None,
    isOfferingDraw: Boolean = false,
    isOfferingRematch: Boolean = false,
    lastDrawOffer: Option[Int] = None,
    isProposingTakeback: Boolean = false,
    userId: Option[String] = None,
    rating: Option[Int] = None,
    ratingDiff: Option[Int] = None,
    blurs: Int = 0,
    holdAlert: Option[Player.HoldAlert] = None,
    name: Option[String] = None) {

  def encodePieces(allPieces: Iterable[(Pos, Piece, Boolean)]): String =
    allPieces collect {
      case (pos, piece, dead) if piece.color == color => pos.piotrStr + {
        if (dead) piece.role.forsyth.toUpper
        else piece.role.forsyth
      }
    } mkString ""

  def withUser(user: User): Player = copy(
    userId = user.id.some,
    rating = user.rating.some)

  def withRating(rating: Int) = copy(rating = rating.some)

  def isAi = aiLevel.isDefined

  def isHuman = !isAi

  def hasUser = userId.isDefined

  def isUser(u: User) = userId.fold(false)(_ == u.id)

  def userInfos: Option[(String, Int)] = (userId |@| rating).tupled

  def wins = isWinner getOrElse false

  def hasHoldAlert = holdAlert.isDefined
  def hasSuspiciousHoldAlert = holdAlert ?? (_.suspicious)

  def finish(winner: Boolean) = copy(
    isWinner = if (winner) Some(true) else None
  )

  def offerDraw(turn: Int) = copy(
    isOfferingDraw = true,
    lastDrawOffer = Some(turn)
  )

  def removeDrawOffer = copy(isOfferingDraw = false)

  def offerRematch = copy(isOfferingRematch = true)

  def removeRematchOffer = copy(isOfferingRematch = false)

  def proposeTakeback = copy(isProposingTakeback = true)

  def removeTakebackProposition = copy(isProposingTakeback = false)

  def withName(name: String) = copy(name = name.some)

  def before(other: Player) = ((rating, id), (other.rating, other.id)) match {
    case ((Some(a), _), (Some(b), _)) if a != b => a < b
    case ((Some(_), _), (None, _))              => true
    case ((None, _), (Some(_), _))              => false
    case ((_, a), (_, b))                       => a < b
  }
}

object Player {

  def make(
    color: Color,
    aiLevel: Option[Int]): Player = Player(
    id = IdGenerator.player,
    color = color,
    aiLevel = aiLevel)

  def white = make(Color.White, None)

  def black = make(Color.Black, None)

  case class HoldAlert(ply: Int, mean: Int, sd: Int) {

    def suspicious = ply >= 20 && ply <= 30
  }

  import reactivemongo.bson.Macros
  implicit val holdAlertBSONHandler = Macros.handler[HoldAlert]

  object BSONFields {

    val aiLevel = "ai"
    val isOfferingDraw = "od"
    val isOfferingRematch = "or"
    val lastDrawOffer = "ld"
    val isProposingTakeback = "ot"
    val rating = "e"
    val ratingDiff = "d"
    val blurs = "b"
    val holdAlert = "h"
    val name = "na"
  }

  import reactivemongo.bson._
  import lila.db.BSON

  type Id = String
  type UserId = Option[String]
  type Win = Option[Boolean]
  type Builder = Color => Id => UserId => Win => Player

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
      isProposingTakeback = r boolD isProposingTakeback,
      userId = userId,
      rating = r intO rating,
      ratingDiff = r intO ratingDiff,
      blurs = r intD blurs,
      holdAlert = r.getO[HoldAlert](holdAlert),
      name = r strO name)

    def writes(w: BSON.Writer, o: Builder) =
      o(chess.White)("0000")(none)(none) |> { p =>
        BSONDocument(
          aiLevel -> p.aiLevel,
          isOfferingDraw -> w.boolO(p.isOfferingDraw),
          isOfferingRematch -> w.boolO(p.isOfferingRematch),
          lastDrawOffer -> p.lastDrawOffer,
          isProposingTakeback -> w.boolO(p.isProposingTakeback),
          rating -> p.rating,
          ratingDiff -> p.ratingDiff,
          blurs -> w.intO(p.blurs),
          holdAlert -> p.holdAlert,
          name -> p.name)
      }
  }
}
