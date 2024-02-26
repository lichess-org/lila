package lila.game

import play.api.libs.json._

import shogi.{ Centis, Clock => ShogiClock, Color, Situation, Status }
import shogi.format.forsyth.Sfen
import shogi.format.usi.Usi
import JsonView._
import lila.chat.{ PlayerLine, UserLine }
import lila.common.Json._

sealed trait Event {
  def typ: String
  def data: JsValue
  def only: Option[Color]   = None
  def owner: Boolean        = false
  def watcher: Boolean      = false
  def troll: Boolean        = false
  def moveBy: Option[Color] = None
}

object Event {

  sealed trait Empty extends Event {
    def data = JsNull
  }

  object Start extends Empty {
    def typ = "start"
  }

  case class UsiEvent(
      usi: Usi,
      sfen: Sfen,
      check: Boolean,
      state: State,
      clock: Option[ClockEvent]
  ) extends Event {
    def typ = "usi"
    def data = Json
      .obj(
        "usi"  -> usi.usi,
        "sfen" -> sfen,
        "ply"  -> state.plies
      )
      .add("clock" -> clock.map(_.data))
      .add("status" -> state.status)
      .add("winner" -> state.winner)
      .add("check" -> check)

    override def moveBy = Some(!state.color)
  }

  object UsiEvent {
    def apply(
        usi: Usi,
        situation: Situation,
        state: State,
        clock: Option[ClockEvent]
    ): UsiEvent =
      UsiEvent(
        usi = usi,
        sfen = situation.toSfen,
        check = situation.check,
        state = state,
        clock = clock
      )
  }

  case class RedirectOwner(
      color: Color,
      id: String,
      cookie: Option[JsObject]
  ) extends Event {
    def typ = "redirect"
    def data =
      Json
        .obj(
          "id"  -> id,
          "url" -> s"/$id"
        )
        .add("cookie" -> cookie)
    override def only = Some(color)
  }

  case class PlayerMessage(line: PlayerLine) extends Event {
    def typ            = "message"
    def data           = lila.chat.JsonView(line)
    override def owner = true
    override def troll = false
  }

  case class UserMessage(line: UserLine, w: Boolean) extends Event {
    def typ              = "message"
    def data             = lila.chat.JsonView(line)
    override def troll   = line.troll
    override def watcher = w
    override def owner   = !w
  }

  case class EndData(game: Game, ratingDiff: Option[RatingDiffs]) extends Event {
    def typ = "endData"
    def data =
      Json
        .obj(
          "winner" -> game.winnerColor,
          "status" -> game.status
        )
        .add("clock" -> game.clock.map { c =>
          val senteClock = c currentClockFor Color.Sente
          val goteClock  = c currentClockFor Color.Gote
          Json.obj(
            "sc" -> senteClock.time.centis,
            "gc" -> goteClock.time.centis,
            "sp" -> senteClock.periods,
            "gp" -> goteClock.periods
          )
        })
        .add("ratingDiff" -> ratingDiff.map { rds =>
          Json.obj(
            Color.Sente.name -> rds.sente,
            Color.Gote.name  -> rds.gote
          )
        })
        .add("boosted" -> game.boosted)
  }

  case object Reload extends Empty {
    def typ = "reload"
  }
  case object ReloadOwner extends Empty {
    def typ            = "reload"
    override def owner = true
  }

  private def reloadOr[A: Writes](typ: String, data: A) = Json.obj("t" -> typ, "d" -> data)

  // use t:reload for mobile app BC,
  // but send extra data for the web to avoid reloading
  case class RematchOffer(by: Option[Color]) extends Event {
    def typ            = "reload"
    def data           = reloadOr("rematchOffer", by)
    override def owner = true
  }

  case class RematchTaken(nextId: Game.ID) extends Event {
    def typ  = "reload"
    def data = reloadOr("rematchTaken", nextId)
  }

  case class DrawOffer(by: Option[Color]) extends Event {
    def typ            = "reload"
    def data           = reloadOr("drawOffer", by)
    override def owner = true
  }

  case class ClockInc(color: Color, time: Centis) extends Event {
    def typ = "clockInc"
    def data =
      Json.obj(
        "color" -> color,
        "time"  -> time.centis
      )
  }

  sealed trait ClockEvent extends Event

  case class Clock(
      sente: Centis,
      gote: Centis,
      sPer: Int = 0,
      gPer: Int = 0,
      nextLagComp: Option[Centis] = None
  ) extends ClockEvent {
    def typ = "clock"
    def data =
      Json
        .obj(
          "sente" -> sente.toSeconds,
          "gote"  -> gote.toSeconds,
          "sPer"  -> sPer,
          "gPer"  -> gPer
        )
        .add("lag" -> nextLagComp.collect { case Centis(c) if c > 1 => c })
  }
  object Clock {
    def apply(clock: ShogiClock): Clock = {
      val senteClock = clock currentClockFor Color.Sente
      val goteClock  = clock currentClockFor Color.Gote
      Clock(
        sente = senteClock.time,
        gote = goteClock.time,
        sPer = senteClock.periods,
        gPer = goteClock.periods,
        nextLagComp = clock lagCompEstimate clock.color
      )
    }
  }

  case class Berserk(color: Color) extends Event {
    def typ  = "berserk"
    def data = Json.toJson(color)
  }

  case class CorrespondenceClock(sente: Float, gote: Float) extends ClockEvent {
    def typ  = "cclock"
    def data = Json.obj("sente" -> sente, "gote" -> gote)
  }
  object CorrespondenceClock {
    def apply(clock: lila.game.CorrespondenceClock): CorrespondenceClock =
      CorrespondenceClock(clock.senteTime, clock.goteTime)
  }

  case class State(
      color: Color,
      plies: Int,
      status: Option[Status],
      winner: Option[Color]
  ) extends Event {
    def typ = "state"
    def data =
      Json
        .obj(
          "color" -> color,
          "plies" -> plies
        )
        .add("status" -> status)
        .add("winner" -> winner)
  }

  case class TakebackOffers(
      sente: Boolean,
      gote: Boolean
  ) extends Event {
    def typ = "takebackOffers"
    def data =
      Json
        .obj()
        .add("sente" -> sente)
        .add("gote" -> gote)
    override def owner = true
  }

  case class PauseOffer(by: Option[Color]) extends Event {
    def typ            = "reload"
    def data           = reloadOr("pauseOffer", by)
    override def owner = true
  }

  case class ResumeOffer(by: Option[Color]) extends Event {
    def typ            = "reload"
    def data           = reloadOr("resumeOffer", by)
    override def owner = true
  }

  case class Crowd(
      sente: Boolean,
      gote: Boolean,
      watchers: Option[JsValue]
  ) extends Event {
    def typ = "crowd"
    def data =
      Json
        .obj(
          "sente" -> sente,
          "gote"  -> gote
        )
        .add("watchers" -> watchers)
  }
}
