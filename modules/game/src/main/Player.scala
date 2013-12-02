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
    elo: Option[Int] = None,
    eloDiff: Option[Int] = None,
    moveTimes: String = "",
    blurs: Int = 0,
    name: Option[String] = None) {

  def encodePieces(allPieces: Iterable[(Pos, Piece, Boolean)]): String =
    allPieces collect {
      case (pos, piece, dead) if piece.color == color ⇒ pos.piotrStr + {
        if (dead) piece.role.forsyth.toUpper
        else piece.role.forsyth
      }
    } mkString ""

  def withUser(user: User): Player = copy(
    userId = user.id.some,
    elo = user.elo.some)

  def isAi = aiLevel.isDefined

  def isHuman = !isAi

  def hasUser = userId.isDefined

  def isUser(u: User) = userId.fold(false)(_ == u.id)

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

  def withName(name: String) = copy(name = name.some)

  def encode: RawPlayer = RawPlayer(
    id = id,
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
    bs = Some(blurs) filter (0 !=),
    na = name
  )
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

  import reactivemongo.bson._
  import lila.db.BSON

  implicit val playerBSONHandler = new BSON[Color ⇒ Player] {

    val id = "_id"
    val aiLevel = "ai"
    val isWinner = "w"
    val isOfferingDraw = "isOfferingDraw"
    val isOfferingRematch = "isOfferingRematch"
    val lastDrawOffer = "lastDrawOffer"
    val isProposingTakeback = "isProposingTakeback"
    val userId = "uid"
    val elo = "elo"
    val eloDiff = "ed"
    val moveTimes = "mts"
    val blurs = "bs"
    val name = "na"

    def reads(r: BSON.Reader) = color ⇒ Player(
      id = r str id,
      color = color,
      aiLevel = r intO aiLevel,
      isWinner = r boolO isWinner,
      isOfferingDraw = r boolD isOfferingDraw,
      isOfferingRematch = r boolD isOfferingRematch,
      lastDrawOffer = r intO lastDrawOffer,
      isProposingTakeback = r boolD isProposingTakeback,
      userId = r strO userId,
      elo = r intO elo,
      eloDiff = r intO eloDiff,
      moveTimes = r strD moveTimes,
      blurs = r intD blurs,
      name = r strO name)

    def writes(w: BSON.Writer, o: Color ⇒ Player) = o(chess.White) |> { p ⇒
      BSONDocument(
        id -> p.id,
        aiLevel -> p.aiLevel,
        isWinner -> p.isWinner,
        isOfferingDraw -> w.boolO(p.isOfferingDraw),
        isOfferingRematch -> w.boolO(p.isOfferingRematch),
        lastDrawOffer -> p.lastDrawOffer,
        isProposingTakeback -> w.boolO(p.isProposingTakeback),
        userId -> p.userId,
        elo -> p.elo,
        eloDiff -> p.eloDiff,
        moveTimes -> p.moveTimes,
        blurs -> blurs,
        name -> p.name)
    }
  }
}

private[game] case class RawPlayer(
    id: String,
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
    bs: Option[Int],
    na: Option[String]) {

  def decode(color: Color): Player = Player(
    id = id,
    color = color,
    aiLevel = ai,
    isWinner = w,
    elo = elo,
    eloDiff = ed,
    isOfferingDraw = ~isOfferingDraw,
    isOfferingRematch = ~isOfferingRematch,
    lastDrawOffer = lastDrawOffer,
    isProposingTakeback = ~isProposingTakeback,
    userId = uid,
    moveTimes = ~mts,
    blurs = ~bs,
    name = na)
}

private[game] object RawPlayer {

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private def defaults = Json.obj(
    "w" -> none[Boolean],
    "isOfferingDraw" -> false,
    "isOfferingRematch" -> false,
    "lastDrawOffer" -> none[Int],
    "isProposingTakeback" -> false,
    "uid" -> none[String],
    "elo" -> none[Int],
    "ed" -> none[Int],
    "mts" -> "",
    "bs" -> 0,
    "na" -> none[String])

  private[game] lazy val tube = Tube(
    (__.json update merge(defaults)) andThen Json.reads[RawPlayer],
    Json.writes[RawPlayer])
}
