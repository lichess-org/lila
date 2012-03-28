package lila.system
package model

import lila.chess._
import Pos.{ posAt, piotr }
import Role.forsyth

case class DbGame(
    id: String,
    whitePlayer: DbPlayer,
    blackPlayer: DbPlayer,
    pgn: String,
    status: Status,
    turns: Int,
    clock: Option[Clock],
    lastMove: Option[String],
    check: Option[Pos] = None,
    creatorColor: Color,
    positionHashes: String = "",
    castles: String = "KQkq",
    isRated: Boolean = false,
    variant: Variant = Standard,
    winnerId: Option[String]) {

  val players = List(whitePlayer, blackPlayer)

  val playersByColor: Map[Color, DbPlayer] = Map(
    White -> whitePlayer,
    Black -> blackPlayer
  )

  def player(color: Color): DbPlayer = color match {
    case White ⇒ whitePlayer
    case Black ⇒ blackPlayer
  }

  def player(playerId: String): Option[DbPlayer] =
    players find (_.id == playerId)

  def isPlayerFullId(player: DbPlayer, fullId: String): Boolean =
    (fullId.size == DbGame.fullIdSize) && player.id == (fullId drop 8)

  def opponent(p: DbPlayer): DbPlayer = player(!(p.color))

  def player: DbPlayer = player(if (0 == turns % 2) White else Black)

  def fullIdOf(player: DbPlayer): Option[String] =
    (players contains player) option id + player.id

  def fullIdOf(color: Color): String = id + player(color).id

  def toChess: Game = {

    def posPiece(posCode: Char, roleCode: Char, color: Color): Option[(Pos, Piece)] = for {
      pos ← piotr(posCode)
      role ← forsyth(roleCode)
    } yield (pos, Piece(color, role))

    val (pieces, deads) = {
      for {
        player ← players
        color = player.color
        piece ← player.ps.split(' ')
      } yield (color, piece(0), piece(1))
    }.foldLeft((Map[Pos, Piece](), List[(Pos, Piece)]())) {
      case ((ps, ds), (color, pos, role)) ⇒ {
        if (role.isUpper) posPiece(pos, role.toLower, color) map { p ⇒ (ps, p :: ds) }
        else posPiece(pos, role, color) map { p ⇒ (ps + p, ds) }
      } getOrElse (ps, ds)
      case (acc, _) ⇒ acc
    }

    Game(
      board = Board(pieces, toChessHistory),
      player = if (0 == turns % 2) White else Black,
      pgnMoves = pgn,
      clock = clock,
      deads = deads,
      turns = turns
    )
  }

  def toChessHistory = History(
    lastMove = lastMove flatMap {
      case MoveString(a, b) ⇒ for (o ← posAt(a); d ← posAt(b)) yield (o, d)
      case _                ⇒ None
    },
    whiteCastleKingSide = castles contains 'K',
    whiteCastleQueenSide = castles contains 'Q',
    blackCastleKingSide = castles contains 'k',
    blackCastleQueenSide = castles contains 'q',
    positionHashes = positionHashes grouped History.hashSize toList
  )

  def update(game: Game, move: Move): DbGame = {
    val allPieces = (game.board.pieces map {
      case (pos, piece) ⇒ (pos, piece, false)
    }) ++ (game.deads map {
      case (pos, piece) ⇒ (pos, piece, true)
    })
    val (history, situation) = (game.board.history, game.situation)
    val events = (Event fromMove move) ::: (Event fromSituation game.situation)

    def updatePlayer(player: DbPlayer) = player.copy(
      ps = player encodePieces allPieces,
      evts = player.newEvts(events :+ Event.possibleMoves(game.situation, player.color)))

    val updated = copy(
      pgn = game.pgnMoves,
      whitePlayer = updatePlayer(whitePlayer),
      blackPlayer = updatePlayer(blackPlayer),
      turns = game.turns,
      positionHashes = history.positionHashes mkString,
      castles = history.castleNotation,
      status =
        if (situation.checkMate) Mate
        else if (situation.staleMate) Stalemate
        else if (situation.autoDraw) Draw
        else status,
      clock = game.clock,
      check = if (game.situation.check) game.situation.kingPos else None
    )

    if (abortable != updated.abortable || (Color.all exists { color ⇒
      playerCanOfferDraw(color) != updated.playerCanOfferDraw(color)
    })) updated withEvents List(ReloadTableEvent())
    else updated
  }

  def withEvents(events: List[Event]): DbGame = copy(
    whitePlayer = whitePlayer withEvents events,
    blackPlayer = blackPlayer withEvents events
  )

  def withEvents(color: Color, events: List[Event]): DbGame = color match {
    case White ⇒ withEvents(events, Nil)
    case Black ⇒ withEvents(Nil, events)
  }

  def withEvents(whiteEvents: List[Event], blackEvents: List[Event]): DbGame =
    copy(
      whitePlayer = whitePlayer withEvents whiteEvents,
      blackPlayer = blackPlayer withEvents blackEvents)

  def playable = status < Aborted

  def aiLevel: Option[Int] = players find (_.isAi) flatMap (_.aiLevel)

  def mapPlayers(f: DbPlayer ⇒ DbPlayer) = copy(
    whitePlayer = f(whitePlayer),
    blackPlayer = f(blackPlayer)
  )

  def playerCanOfferDraw(color: Color) =
    status >= Started &&
      status < Aborted &&
      turns >= 2 &&
      !player(color).isOfferingDraw &&
      !(player(!color).isAi) &&
      !(player(!color).isOfferingDraw) &&
      !(playerHasOfferedDraw(color))

  def playerHasOfferedDraw(color: Color) =
    player(color).lastDrawOffer some (_ >= turns - 1) none false

  def abortable = status == Started && turns < 2

  def resignable = playable && !abortable

  def finish(status: Status, winner: Option[Color], msg: Option[String]) = copy(
    status = status,
    winnerId = winner flatMap (player(_).userId),
    whitePlayer = whitePlayer finish (winner == Some(White)),
    blackPlayer = blackPlayer finish (winner == Some(Black)),
    positionHashes = ""
  ) withEvents (EndEvent() :: msg.map(MessageEvent("system", _)).toList)

  def rated = isRated

  def finished = status >= Mate

  def winnerColor: Option[Color] = players find (_.wins) map (_.color)

  def outoftimePlayer: Option[DbPlayer] = for {
    c ← clock
    if this.playable
    if c outoftime player.color
  } yield player

}

object DbGame {

  val gameIdSize = 8
  val playerIdSize = 4
  val fullIdSize = 12
}
