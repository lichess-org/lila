package lila
package game

import chess._
import user.User

import com.mongodb.DBRef

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
    user: Option[DBRef] = None,
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
    } mkString " "

  def withEncodedPieces(allPieces: Iterable[(Pos, Piece, Boolean)]) = copy(
    ps = encodePieces(allPieces)
  )

  def withUser(user: User)(dbRef: User ⇒ DBRef): DbPlayer = copy(
    user = dbRef(user).some,
    elo = user.elo.some)

  def isAi = aiLevel.isDefined

  def isHuman = !isAi

  def userId: Option[String] = user map (_.getId.toString)

  def hasUser = user.isDefined

  def isUser(u: User) = user.fold(_.getId == u.id, false)

  def withUser(u: User, ref: DBRef) = copy(
    elo = u.elo.some,
    user = ref.some)

  def wins = isWinner getOrElse false

  def hasMoveTimes = moveTimes.size > 10

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
    c = color.name,
    ps = ps,
    aiLevel = aiLevel,
    w = isWinner,
    elo = elo,
    eloDiff = eloDiff,
    isOfferingDraw = if (isOfferingDraw) Some(true) else None,
    isOfferingRematch = if (isOfferingRematch) Some(true) else None,
    lastDrawOffer = lastDrawOffer,
    isProposingTakeback = if (isProposingTakeback) Some(true) else None,
    user = user,
    mts = Some(moveTimes) filter ("" !=),
    blurs = Some(blurs) filter (0 !=)
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
    c: String,
    ps: String,
    aiLevel: Option[Int],
    w: Option[Boolean],
    elo: Option[Int],
    eloDiff: Option[Int],
    isOfferingDraw: Option[Boolean],
    isOfferingRematch: Option[Boolean],
    lastDrawOffer: Option[Int],
    isProposingTakeback: Option[Boolean],
    user: Option[DBRef],
    mts: Option[String],
    blurs: Option[Int]) {

  def decode: Option[DbPlayer] = for {
    trueColor ← Color(c)
  } yield DbPlayer(
    id = id,
    color = trueColor,
    ps = ps,
    aiLevel = aiLevel,
    isWinner = w,
    elo = elo,
    eloDiff = eloDiff,
    isOfferingDraw = isOfferingDraw | false,
    isOfferingRematch = isOfferingRematch | false,
    lastDrawOffer = lastDrawOffer,
    isProposingTakeback = isProposingTakeback | false,
    user = user,
    moveTimes = mts | "",
    blurs = blurs | 0
  )
}
