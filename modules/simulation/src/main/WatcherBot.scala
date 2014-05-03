package lila.simulation

import scala.concurrent.duration._
import scala.util.{ Random, Try }

import akka.actor._
import akka.pattern.{ ask, pipe }
import chess.Color
import play.api.libs.json._

import lila.common.PimpedJson._

private[simulation] final class WatcherBot(
    val name: String,
    featured: lila.tv.Featured,
    roundEnv: lila.round.Env) extends Bot with FSM[WatcherBot.State, WatcherBot.Data] {

  import Bot._
  import WatcherBot._

  log("spawned")

  startWith(Offline, NoData)

  when(Offline) {

    case Event(Simulator.Start, _) => {
      featured.one pipeTo self
      stay
    }

    case Event(Some(game: lila.game.Game), _) => {
      roundEnv.socketHandler.watcher(game.id, "white", 0, uid, user, ip) pipeTo self
      goto(TvConnect) using Id(game.id)
    }

    case Event(None, _) => {
      delay(1 second)(self ! Simulator.Start)
      stay
    }
  }

  when(TvConnect) {

    case Event((iteratee: JsIteratee, enumerator: JsEnumerator), Id(id)) => {
      receiveFrom(enumerator)
      delay(1 second)(self ! Ping)
      goto(Tv) using Watcher(id, sendTo(iteratee))
    }
  }

  when(Tv) {

    case Event(Ping, watcher: Watcher) => {
      watcher.channel push Json.obj("t" -> "p", "v" -> watcher.v)
      stay
    }

    case Event(SetVersion(v), watcher: Watcher) =>
      stay using watcher.copy(v = v)

    case Event(Message("featured_id", obj), watcher: Watcher) => obj str "d" map { id =>
      watcher.channel.eofAndEnd()
      roundEnv.socketHandler.watcher(id, "white", 0, uid, user, ip) pipeTo self
      goto(TvConnect) using Id(id)
    } getOrElse stay

    // pong
    case Event(Message("n", obj), _) => {
      delay(1 second)(self ! Ping)
      stay
    }

    // batch
    case Event(Message("b", obj), _) => {
      val events = ~(obj.arrAs("d")(_.asOpt[JsObject]))
      events.map(parseMessage).flatten foreach self.!
      stay
    }

    // any other versioned event
    case Event(Message(_, obj), _) => {
      setVersion(obj)
      stay
    }
  }

  whenUnhandled {

    case e => {
      // log(e)
      stay
    }
  }
}

private[simulation] object WatcherBot {

  type Move = (chess.Pos, chess.Pos)

  sealed trait State
  case object Offline extends State
  case object TvConnect extends State
  case object Tv extends State

  sealed trait Data
  case object NoData extends Data
  case class Id(id: String) extends Data
  case class Watcher(
    id: String,
    channel: JsChannel,
    v: Int = 1) extends Data
}
