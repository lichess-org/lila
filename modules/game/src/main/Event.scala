package lila.game

import lila.common.PimpedJson._
import play.api.libs.json._

import chess.Pos
import chess.Pos.{ piotr, allPiotrs }
import chess.{ PromotableRole, Pos, Color, Situation, Move => ChessMove, Clock => ChessClock, Status }
import lila.chat.{ Line, UserLine, PlayerLine }

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

  case class Move(
      orig: Pos,
      dest: Pos,
      color: Color,
      san: String,
      fen: String,
      check: Boolean,
      threefold: Boolean,
      promotion: Option[Promotion],
      enpassant: Option[Enpassant],
      castle: Option[Castling],
      state: State,
      clock: Option[Event],
      possibleMoves: Map[Pos, List[Pos]]) extends Event {
    def typ = "move"
    def data = Json.obj(
      // legacy data
      "from" -> orig.key,
      "to" -> dest.key,
      "color" -> color,
      // new data
      "uci" -> s"${orig.key}${dest.key}",
      "san" -> san,
      "fen" -> fen,
      "check" -> check.option(true),
      "threefold" -> threefold.option(true),
      "promotion" -> promotion.map(_.data),
      "enpassant" -> enpassant.map(_.data),
      "castle" -> castle.map(_.data),
      "ply" -> state.turns,
      "status" -> state.status,
      "winner" -> state.winner,
      "wDraw" -> state.whiteOffersDraw.option(true),
      "bDraw" -> state.blackOffersDraw.option(true),
      "clock" -> clock.map(_.data),
      "dests" -> PossibleMoves.json(possibleMoves)
    ).noNull
  }
  object Move {
    def apply(move: ChessMove, situation: Situation, state: State, clock: Option[Event]): Move = Move(
      orig = move.orig,
      dest = move.dest,
      color = move.piece.color,
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
      possibleMoves = situation.destinations)
  }

  case class PossibleMoves(
      color: Color,
      moves: Map[Pos, List[Pos]]) extends Event {
    def typ = "possibleMoves"
    def data = PossibleMoves json moves
    override def only = Some(color)
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
    def data = Line toJson line
    override def owner = true
    override def troll = false
  }

  case class UserMessage(line: UserLine, w: Boolean) extends Event {
    def typ = "message"
    def data = Line toJson line
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
      "white" -> white,
      "black" -> black
    )
  }
  object Clock {
    def apply(clock: ChessClock): Clock = Clock(
      clock remainingTime Color.White,
      clock remainingTime Color.Black)
    def tenths(white: Int, black: Int): Clock = Clock(white.toFloat / 10, black.toFloat / 10)
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
      watchers: List[String]) extends Event {
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
