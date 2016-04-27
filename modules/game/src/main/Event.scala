package lila.game

import lila.common.PimpedJson._
import play.api.libs.json._

import chess.Pos
import chess.Pos.{ piotr, allPiotrs }
import chess.variant.Crazyhouse
import chess.{ PromotableRole, Pos, Color, Situation, Move => ChessMove, Drop => ChessDrop, Clock => ChessClock, Status }
import JsonView._
import lila.chat.{ Line, UserLine, PlayerLine }
import lila.common.Maths.truncateAt

sealed trait Event {
  def typ: String
  def data: JsValue
  def only: Option[Color] = None
  def owner: Boolean = false
  def watcher: Boolean = false
  def troll: Boolean = false
}

object Event {

  sealed trait Empty extends Event {
    def data = JsNull
  }

  object Start extends Empty {
    def typ = "start"
  }

  private def withCrazyData(
    data: Option[Crazyhouse.Data],
    drops: Option[List[Pos]])(js: JsObject): JsObject =
    data.fold(js) { d =>
      val js1 = js + ("crazyhouse" -> crazyhouseDataWriter.writes(d))
      drops.fold(js1) { squares =>
        js1 + ("drops" -> JsString(squares.map(_.key).mkString))
      }
    }

  object MoveOrDrop {

    def data(
      fen: String,
      check: Boolean,
      threefold: Boolean,
      state: State,
      clock: Option[Event],
      possibleMoves: Map[Pos, List[Pos]],
      possibleDrops: Option[List[Pos]],
      crazyData: Option[Crazyhouse.Data])(extra: JsObject) = {
      extra ++ Json.obj(
        "fen" -> fen,
        "check" -> check.option(true),
        "threefold" -> threefold.option(true),
        "ply" -> state.turns,
        "status" -> state.status,
        "winner" -> state.winner,
        "wDraw" -> state.whiteOffersDraw.option(true),
        "bDraw" -> state.blackOffersDraw.option(true),
        "clock" -> clock.map(_.data),
        "dests" -> PossibleMoves.json(possibleMoves))
    }.noNull |> withCrazyData(crazyData, possibleDrops)
  }

  case class Move(
      orig: Pos,
      dest: Pos,
      san: String,
      fen: String,
      check: Boolean,
      threefold: Boolean,
      promotion: Option[Promotion],
      enpassant: Option[Enpassant],
      castle: Option[Castling],
      state: State,
      clock: Option[Event],
      possibleMoves: Map[Pos, List[Pos]],
      possibleDrops: Option[List[Pos]],
      crazyData: Option[Crazyhouse.Data]) extends Event {
    def typ = "move"
    def data = MoveOrDrop.data(fen, check, threefold, state, clock, possibleMoves, possibleDrops, crazyData) {
      Json.obj(
        "uci" -> s"${orig.key}${dest.key}",
        "san" -> san,
        "promotion" -> promotion.map(_.data),
        "enpassant" -> enpassant.map(_.data),
        "castle" -> castle.map(_.data))
    }
  }
  object Move {
    def apply(move: ChessMove, situation: Situation, state: State, clock: Option[Event], crazyData: Option[Crazyhouse.Data]): Move = Move(
      orig = move.orig,
      dest = move.dest,
      san = chess.format.pgn.Dumper(move),
      fen = chess.format.Forsyth.exportBoard(situation.board),
      check = situation.check,
      threefold = situation.threefoldRepetition,
      promotion = move.promotion.map { Promotion(_, move.dest) },
      enpassant = (move.capture ifTrue move.enpassant).map {
        Event.Enpassant(_, !move.color)
      },
      castle = move.castle.map {
        case (king, rook) => Castling(king, rook, move.color)
      },
      state = state,
      clock = clock,
      possibleMoves = situation.destinations,
      possibleDrops = situation.drops,
      crazyData = crazyData)
  }

  case class Drop(
      role: chess.Role,
      pos: Pos,
      san: String,
      fen: String,
      check: Boolean,
      threefold: Boolean,
      state: State,
      clock: Option[Event],
      possibleMoves: Map[Pos, List[Pos]],
      crazyData: Option[Crazyhouse.Data],
      possibleDrops: Option[List[Pos]]) extends Event {
    def typ = "drop"
    def data = MoveOrDrop.data(fen, check, threefold, state, clock, possibleMoves, possibleDrops, crazyData) {
      Json.obj(
        "role" -> role.name,
        "uci" -> s"${role.pgn}@${pos.key}",
        "san" -> san)
    }
  }
  object Drop {
    def apply(drop: ChessDrop, situation: Situation, state: State, clock: Option[Event], crazyData: Option[Crazyhouse.Data]): Drop = Drop(
      role = drop.piece.role,
      pos = drop.pos,
      san = chess.format.pgn.Dumper(drop),
      fen = chess.format.Forsyth.exportBoard(situation.board),
      check = situation.check,
      threefold = situation.threefoldRepetition,
      state = state,
      clock = clock,
      possibleMoves = situation.destinations,
      possibleDrops = situation.drops,
      crazyData = crazyData)
  }

  object PossibleMoves {
    def json(moves: Map[Pos, List[Pos]]) =
      if (moves.isEmpty) JsNull
      else moves.foldLeft(JsObject(Nil)) {
        case (res, (o, d)) => res + (o.key, JsString(d map (_.key) mkString))
      }
  }

  case class Enpassant(pos: Pos, color: Color) extends Event {
    def typ = "enpassant"
    def data = Json.obj(
      "key" -> pos.key,
      "color" -> color)
  }

  case class Castling(king: (Pos, Pos), rook: (Pos, Pos), color: Color) extends Event {
    def typ = "castling"
    def data = Json.obj(
      "king" -> Json.arr(king._1.key, king._2.key),
      "rook" -> Json.arr(rook._1.key, rook._2.key),
      "color" -> color
    )
  }

  case class RedirectOwner(
      color: Color,
      id: String,
      cookie: Option[JsObject]) extends Event {
    def typ = "redirect"
    def data = Json.obj(
      "id" -> id,
      "url" -> s"/$id",
      "cookie" -> cookie
    ).noNull
    override def only = Some(color)
    override def owner = true
  }

  case class Promotion(role: PromotableRole, pos: Pos) extends Event {
    def typ = "promotion"
    def data = Json.obj(
      "key" -> pos.key,
      "pieceClass" -> role.toString.toLowerCase
    )
  }

  case class PlayerMessage(line: PlayerLine) extends Event {
    def typ = "message"
    def data = lila.chat.JsonView(line)
    override def owner = true
    override def troll = false
  }

  case class UserMessage(line: UserLine, w: Boolean) extends Event {
    def typ = "message"
    def data = lila.chat.JsonView(line)
    override def troll = line.troll
    override def watcher = w
    override def owner = !w
  }

  case class End(winner: Option[Color]) extends Event {
    def typ = "end"
    def data = Json.toJson(winner)
  }

  case object Reload extends Empty {
    def typ = "reload"
  }
  case object ReloadOwner extends Empty {
    def typ = "reload"
    override def owner = true
  }

  case class Premove(color: Color) extends Empty {
    def typ = "premove"
    override def only = Some(color)
    override def owner = true
  }

  case class Clock(white: Float, black: Float) extends Event {
    def typ = "clock"
    def data = Json.obj(
      "white" -> truncateAt(white, 2),
      "black" -> truncateAt(black, 2))
  }
  object Clock {
    def apply(clock: ChessClock): Clock = Clock(
      clock remainingTime Color.White,
      clock remainingTime Color.Black)
    def tenths(white: Int, black: Int): Clock = Clock(white.toFloat / 10, black.toFloat / 10)
  }

  case class Berserk(color: Color) extends Event {
    def typ = "berserk"
    def data = Json.toJson(color)
  }

  case class CorrespondenceClock(white: Float, black: Float) extends Event {
    def typ = "cclock"
    def data = Json.obj("white" -> white, "black" -> black)
  }
  object CorrespondenceClock {
    def apply(clock: lila.game.CorrespondenceClock): CorrespondenceClock =
      CorrespondenceClock(clock.whiteTime, clock.blackTime)
  }

  case class CheckCount(white: Int, black: Int) extends Event {
    def typ = "checkCount"
    def data = Json.obj(
      "white" -> white,
      "black" -> black
    )
  }

  case class State(
      color: Color,
      turns: Int,
      status: Option[Status],
      winner: Option[Color],
      whiteOffersDraw: Boolean,
      blackOffersDraw: Boolean) extends Event {
    def typ = "state"
    def data = Json.obj(
      "color" -> color,
      "turns" -> turns,
      "status" -> status,
      "winner" -> winner,
      "wDraw" -> whiteOffersDraw.option(true),
      "bDraw" -> blackOffersDraw.option(true)
    ).noNull
  }

  case class TakebackOffers(
      white: Boolean,
      black: Boolean) extends Event {
    def typ = "takebackOffers"
    def data = Json.obj(
      "white" -> white.option(true),
      "black" -> black.option(true)
    ).noNull
    override def owner = true
  }

  case class Crowd(
      white: Boolean,
      black: Boolean,
      watchers: JsValue) extends Event {
    def typ = "crowd"
    def data = Json.obj(
      "white" -> white,
      "black" -> black,
      "watchers" -> watchers)
  }

  private implicit val colorWriter: Writes[Color] = Writes { c =>
    JsString(c.name)
  }
  private implicit val statusWriter: OWrites[Status] = OWrites { s =>
    Json.obj("id" -> s.id, "name" -> s.name)
  }
}
