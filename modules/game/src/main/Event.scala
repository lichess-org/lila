package lila.game

import play.api.libs.json.*

import chess.variant.Crazyhouse
import chess.{
  Ply,
  Check,
  Centis,
  Clock as ChessClock,
  Color,
  Drop as ChessDrop,
  Move as ChessMove,
  Square,
  PromotableRole,
  Situation,
  Status
}
import chess.format.{ Fen, BoardFen }
import chess.format.pgn.SanStr
import chess.bitboard.Bitboard
import JsonView.{ *, given }
import lila.chat.{ PlayerLine, UserLine }
import lila.common.ApiVersion
import lila.common.Json.given

sealed trait Event:
  def typ: String
  def data: JsValue
  def only: Option[Color]   = None
  def owner: Boolean        = false
  def watcher: Boolean      = false
  def troll: Boolean        = false
  def moveBy: Option[Color] = None

object Event:

  sealed trait Empty extends Event:
    def data = JsNull

  object Start extends Empty:
    def typ = "start"

  object MoveOrDrop:

    def data(
        fen: BoardFen,
        check: Check,
        threefold: Boolean,
        state: State,
        clock: Option[ClockEvent],
        possibleMoves: Map[Square, Bitboard],
        possibleDrops: Option[List[Square]],
        crazyData: Option[Crazyhouse.Data]
    )(extra: JsObject) =
      extra ++ Json
        .obj(
          "fen"   -> fen,
          "ply"   -> state.turns,
          "dests" -> PossibleMoves.oldJson(possibleMoves)
        )
        .add("clock" -> clock.map(_.data))
        .add("status" -> state.status)
        .add("winner" -> state.winner)
        .add("check" -> check)
        .add("threefold" -> threefold)
        .add("wDraw" -> state.whiteOffersDraw)
        .add("bDraw" -> state.blackOffersDraw)
        .add("crazyhouse" -> crazyData)
        .add("drops" -> possibleDrops.map: squares =>
          JsString(squares.map(_.key).mkString))

  case class Move(
      orig: Square,
      dest: Square,
      san: SanStr,
      fen: BoardFen,
      check: Check,
      threefold: Boolean,
      promotion: Option[Promotion],
      enpassant: Option[Enpassant],
      castle: Option[Castling],
      state: State,
      clock: Option[ClockEvent],
      possibleMoves: Map[Square, Bitboard],
      possibleDrops: Option[List[Square]],
      crazyData: Option[Crazyhouse.Data]
  ) extends Event:
    def typ = "move"
    def data = MoveOrDrop.data(fen, check, threefold, state, clock, possibleMoves, possibleDrops, crazyData):
      Json
        .obj(
          "uci" -> s"${orig.key}${dest.key}",
          "san" -> san
        )
        .add("promotion" -> promotion.map(_.data))
        .add("enpassant" -> enpassant.map(_.data))
        .add("castle" -> castle.map(_.data))
    override def moveBy = Some(!state.turns.turn)
  object Move:
    def apply(
        move: ChessMove,
        situation: Situation,
        state: State,
        clock: Option[ClockEvent],
        crazyData: Option[Crazyhouse.Data]
    ): Move = Move(
      orig = move.orig,
      dest = move.dest,
      san = move.san,
      fen = Fen.writeBoard(situation.board),
      check = situation.check,
      threefold = situation.threefoldRepetition,
      promotion = move.promotion.map { Promotion(_, move.dest) },
      enpassant = move.capture.ifTrue(move.enpassant).map(Event.Enpassant(_, !move.color)),
      castle = move.castle.map(_.value).map((king, rook) => Castling(king, rook, move.color)),
      state = state,
      clock = clock,
      possibleMoves = situation.destinations,
      possibleDrops = situation.drops,
      crazyData = crazyData
    )

  case class Drop(
      role: chess.Role,
      pos: Square,
      san: SanStr,
      fen: BoardFen,
      check: Check,
      threefold: Boolean,
      state: State,
      clock: Option[ClockEvent],
      possibleMoves: Map[Square, Bitboard],
      crazyData: Option[Crazyhouse.Data],
      possibleDrops: Option[List[Square]]
  ) extends Event:
    def typ = "drop"
    def data = MoveOrDrop.data(fen, check, threefold, state, clock, possibleMoves, possibleDrops, crazyData):
      Json.obj(
        "role" -> role.name,
        "uci"  -> s"${role.pgn}@${pos.key}",
        "san"  -> san
      )
    override def moveBy = Some(!state.turns.turn)
  object Drop:
    def apply(
        drop: ChessDrop,
        situation: Situation,
        state: State,
        clock: Option[ClockEvent],
        crazyData: Option[Crazyhouse.Data]
    ): Drop = Drop(
      role = drop.piece.role,
      pos = drop.square,
      san = drop.san,
      fen = Fen.writeBoard(situation.board),
      check = situation.check,
      threefold = situation.threefoldRepetition,
      state = state,
      clock = clock,
      possibleMoves = situation.destinations,
      possibleDrops = situation.drops,
      crazyData = crazyData
    )

  object PossibleMoves:
    def json(moves: Map[Square, Bitboard]): JsValue =
      if moves.isEmpty then JsNull
      else
        val sb    = new java.lang.StringBuilder(128)
        var first = true
        moves.foreach: (orig, dests) =>
          if first then first = false
          else sb append " "
          sb append orig.key
          dests.foreach(s => sb.append(s.key))
        JsString(sb.toString)

    def oldJson(moves: Map[Square, Bitboard]): JsValue =
      if moves.isEmpty then JsNull
      else
        moves.foldLeft(JsObject(Nil)):
          case (res, (o, d)) => res + (o.key -> JsString(d.map(_.key).mkString))

  case class Enpassant(pos: Square, color: Color) extends Event:
    def typ = "enpassant"
    def data =
      Json.obj(
        "key"   -> pos.key,
        "color" -> color
      )

  case class Castling(king: (Square, Square), rook: (Square, Square), color: Color) extends Event:
    def typ = "castling"
    def data =
      Json.obj(
        "king"  -> Json.arr(king._1.key, king._2.key),
        "rook"  -> Json.arr(rook._1.key, rook._2.key),
        "color" -> color
      )

  case class RedirectOwner(
      color: Color,
      id: GameFullId,
      cookie: Option[JsObject]
  ) extends Event:
    def typ = "redirect"
    def data =
      Json
        .obj(
          "id"  -> id,
          "url" -> s"/$id"
        )
        .add("cookie" -> cookie)
    override def only = Some(color)

  case class Promotion(role: PromotableRole, pos: Square) extends Event:
    def typ = "promotion"
    def data =
      Json.obj(
        "key"        -> pos.key,
        "pieceClass" -> role.toString.toLowerCase
      )

  case class PlayerMessage(line: PlayerLine) extends Event:
    def typ            = "message"
    def data           = lila.chat.JsonView(line)
    override def owner = true
    override def troll = false

  case class UserMessage(line: UserLine, w: Boolean) extends Event:
    def typ              = "message"
    def data             = lila.chat.JsonView(line)
    override def troll   = line.troll
    override def watcher = w
    override def owner   = !w

  case class EndData(game: Game, ratingDiff: Option[RatingDiffs]) extends Event:
    def typ = "endData"
    def data =
      Json
        .obj(
          "winner" -> game.winnerColor,
          "status" -> game.status
        )
        .add("clock" -> game.clock.map { c =>
          Json.obj(
            "wc" -> c.remainingTime(Color.White).centis,
            "bc" -> c.remainingTime(Color.Black).centis
          )
        })
        .add("ratingDiff" -> ratingDiff.map { rds =>
          Json.obj(
            Color.White.name -> rds.white,
            Color.Black.name -> rds.black
          )
        })
        .add("boosted" -> game.boosted)

  case object Reload extends Empty:
    def typ = "reload"
  case object ReloadOwner extends Empty:
    def typ            = "reload"
    override def owner = true

  private def reloadOr[A: Writes](typ: String, data: A) = Json.obj("t" -> typ, "d" -> data)

  // use t:reload for mobile app BC,
  // but send extra data for the web to avoid reloading
  case class RematchOffer(by: Option[Color]) extends Event:
    def typ            = "reload"
    def data           = reloadOr("rematchOffer", by)
    override def owner = true

  case class RematchTaken(nextId: GameId) extends Event:
    def typ  = "reload"
    def data = reloadOr("rematchTaken", nextId)

  case class DrawOffer(by: Option[Color]) extends Event:
    def typ  = "reload"
    def data = reloadOr("drawOffer", by)

  case class ClockInc(color: Color, time: Centis, newClock: ChessClock) extends Event:
    def typ = "clockInc"
    def data =
      Json.obj(
        "color" -> color,
        "time"  -> time.centis,
        "total" -> newClock.remainingTime(color).centis
      )

  sealed trait ClockEvent extends Event

  case class Clock(white: Centis, black: Centis, nextLagComp: Option[Centis] = None) extends ClockEvent:
    def typ = "clock"
    def data =
      Json
        .obj(
          "white" -> white.toSeconds,
          "black" -> black.toSeconds
        )
        .add("lag" -> nextLagComp.filter(_ > 1))
  object Clock:
    def apply(clock: ChessClock): Clock =
      Clock(
        clock remainingTime Color.White,
        clock remainingTime Color.Black,
        clock lagCompEstimate clock.color
      )

  case class Berserk(color: Color) extends Event:
    def typ  = "berserk"
    def data = Json.toJson(color)

  case class CorrespondenceClock(white: Float, black: Float) extends ClockEvent:
    def typ  = "cclock"
    def data = Json.obj("white" -> white, "black" -> black)
  object CorrespondenceClock:
    def apply(clock: lila.game.CorrespondenceClock): CorrespondenceClock =
      CorrespondenceClock(clock.whiteTime, clock.blackTime)

  case class CheckCount(white: Int, black: Int) extends Event:
    def typ = "checkCount"
    def data =
      Json.obj(
        "white" -> white,
        "black" -> black
      )

  case class State(
      turns: Ply,
      status: Option[Status],
      winner: Option[Color],
      whiteOffersDraw: Boolean,
      blackOffersDraw: Boolean
  ) extends Event:
    def typ = "state"
    def data =
      Json
        .obj(
          "color" -> turns.turn,
          "turns" -> turns
        )
        .add("status" -> status)
        .add("winner" -> winner)
        .add("wDraw" -> whiteOffersDraw)
        .add("bDraw" -> blackOffersDraw)

  case class TakebackOffers(
      white: Boolean,
      black: Boolean
  ) extends Event:
    def typ = "takebackOffers"
    def data =
      Json
        .obj()
        .add("white" -> white)
        .add("black" -> black)
    override def owner = true

  case class Crowd(
      white: Boolean,
      black: Boolean,
      watchers: Option[JsValue]
  ) extends Event:
    def typ = "crowd"
    def data =
      Json
        .obj(
          "white" -> white,
          "black" -> black
        )
        .add("watchers" -> watchers)
