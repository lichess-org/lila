package lila
package game

import chess._
import user.User

case class DbPlayer(
    id: String,
    color: Color,
    aiLevel: Option[Int],
    ps: String = "",
    isWinner: Option[Boolean] = None,
    isOfferingDraw: Boolean = false,
    isOfferingRematch: Boolean = false,
    lastDrawOffer: Option[Int] = None,
    isProposingTakeback: Boolean = false,
    userId: Option[String] = None,
    elo: Option[Int] = None,
    eloDiff: Option[Int] = None,
    moveTimes: String = "",
    blurs: Int = 0) {

  def encodePieces(allPieces: Iterable[(Pos, Piece, Boolean)]): String =
    allPieces withFilter (_._2.color == color) map {
      case (pos, piece, dead) ⇒ pos.piotr.toString + {
        if (dead) piece.role.forsyth.toUpper
        else piece.role.forsyth
      }
    } mkString ""

  def withEncodedPieces(allPieces: Iterable[(Pos, Piece, Boolean)]) = copy(
    ps = encodePieces(allPieces)
  )

  def withUser(user: User): DbPlayer = copy(
    userId = user.id.some,
    elo = user.elo.some)

  def isAi = aiLevel.isDefined

  def isHuman = !isAi

  def hasUser = userId.isDefined

  def isUser(u: User) = userId.fold(_ == u.id, false)

  def wins = isWinner getOrElse false

  def hasMoveTimes = moveTimes.size > 4

  def moveTimeList: List[Int] = MoveTime decode moveTimes

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

  def encode: RawDbPlayer = RawDbPlayer(
    id = id,
    ps = ps,
    ai = aiLevel,
    w = isWinner,
    elo = elo,
    ed = eloDiff,
    isOfferingDraw = if (isOfferingDraw) Some(true) else None,
    isOfferingRematch = if (isOfferingRematch) Some(true) else None,
    lastDrawOffer = lastDrawOffer,
    isProposingTakeback = if (isProposingTakeback) Some(true) else None,
    uid = userId,
    mts = Some(moveTimes) filter ("" !=),
    bs = Some(blurs) filter (0 !=)
  )
}

object DbPlayer {

  def apply(
    color: Color,
    aiLevel: Option[Int]): DbPlayer = DbPlayer(
    id = IdGenerator.player,
    color = color,
    aiLevel = aiLevel)

  def white = apply(Color.White, None)

  def black = apply(Color.Black, None)
}

case class RawDbPlayer(
    id: String,
    ps: String,
    ai: Option[Int],
    w: Option[Boolean],
    elo: Option[Int],
    ed: Option[Int],
    isOfferingDraw: Option[Boolean],
    isOfferingRematch: Option[Boolean],
    lastDrawOffer: Option[Int],
    isProposingTakeback: Option[Boolean],
    uid: Option[String],
    mts: Option[String],
    bs: Option[Int]) {

  def decode(color: Color): DbPlayer = DbPlayer(
    id = id,
    color = color,
    ps = ps,
    aiLevel = ai,
    isWinner = w,
    elo = elo,
    eloDiff = ed,
    isOfferingDraw = isOfferingDraw | false,
    isOfferingRematch = isOfferingRematch | false,
    lastDrawOffer = lastDrawOffer,
    isProposingTakeback = isProposingTakeback | false,
    userId = uid,
    moveTimes = mts | "",
    blurs = bs | 0
  )
}
