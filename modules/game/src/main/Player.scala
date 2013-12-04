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

  object BSONFields {

    val id = "id"
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
  }

  import reactivemongo.bson._
  import lila.db.BSON

  implicit val playerBSONHandler = new BSON[Color ⇒ Player] {

    import BSONFields._

    def reads(r: BSON.Reader) = color ⇒ Player(
      id = (r strO id) | "0000",
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
        id -> (p.isHuman option p.id),
        aiLevel -> p.aiLevel,
        isWinner -> p.isWinner,
        isOfferingDraw -> w.boolO(p.isOfferingDraw),
        isOfferingRematch -> w.boolO(p.isOfferingRematch),
        lastDrawOffer -> p.lastDrawOffer,
        isProposingTakeback -> w.boolO(p.isProposingTakeback),
        userId -> p.userId,
        elo -> p.elo,
        eloDiff -> p.eloDiff,
        moveTimes -> w.strO(p.moveTimes),
        blurs -> w.intO(p.blurs),
        name -> p.name)
    }
  }
}
