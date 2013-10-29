package lila.simulation

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }
import chess.Color
import ornicar.scalalib.Random
import play.api.libs.iteratee._
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json._

import actorApi._
import lila.common.PimpedJson._
import lila.user.User

private[simulation] final class Bot(
    name: String,
    lobbyEnv: lila.lobby.Env,
    roundEnv: lila.round.Env) extends Actor with FSM[Bot.State, Bot.Data] {

  import Bot._

  val uid = Random nextString 8
  val user = none[User]
  val sid = none[String]
  val ip = "127.0.0.1"

  log("spawned")

  startWith(Offline, NoData)

  when(Offline) {

    case Event(Start, _) ⇒ goto(LobbyConnect)
  }

  onTransition {
    case _ -> LobbyConnect ⇒ lobbyEnv.socketHandler(uid, user) pipeTo self
  }

  when(LobbyConnect) {

    case Event((iteratee: JsIteratee, enumerator: JsEnumerator), _) ⇒ {
      receiveFrom(enumerator)
      val hook = lila.setup.HookConfig.default.hook(uid, user, sid)
      lobbyEnv.lobby ! lila.lobby.actorApi.AddHook(hook)
      goto(Lobby) using Lobbyist(sendTo(iteratee))
    }
  }

  when(Lobby) {

    case Event(Message("redirect", obj), Lobbyist(channel)) ⇒ obj str "d" map { url ⇒
      channel.eofAndEnd()
      val id = url drop 1
      roundEnv.socketHandler.player(id, 0, uid, "token", user, ip) pipeTo self
      goto(RoundConnect) using FullId(id)
    } getOrElse stay
  }

  when(RoundConnect) {

    case Event((iteratee: JsIteratee, enumerator: JsEnumerator), FullId(id)) ⇒ {
      log(s"joins ${id}")
      receiveFrom(enumerator)
      delay(0.5 second)(self ! Ping)
      delay(1 second)(self ! Move)
      goto(Round) using Player(id, sendTo(iteratee))
    }
  }

  when(Round) {

    // pong
    case Event(Message("n", obj), _) ⇒ {
      delay(1 second)(self ! Ping)
      stay
    }

    // batch
    case Event(Message("b", obj), _) ⇒ {
      val events = ~(obj.arrAs("d")(_.asOpt[JsObject]))
      events.map(parseMessage).flatten foreach self.!
      stay
    }

    case Event(Message("end", _), player: Player) ⇒ {
      player.channel.eofAndEnd()
      goto(LobbyConnect) using NoData
    }

    // any other versioned event
    case Event(Message(_, obj), _) ⇒ {
      log(obj.toString)
      setVersion(obj)
      stay
    }

    case Event(Move, player: Player) ⇒ {
      val (from, to) = moves(player.move % moves.size)
      val move = Json.obj(
        "t" -> "move",
        "d" -> Json.obj(
          "from" -> from.key,
          "to" -> to.key
        )
      )
      player.channel push move
      delay(1 second)(self ! Move)
      stay using player.copy(move = player.move + 1)
    }

    case Event(Ping, player: Player) ⇒ {
      player.channel push Json.obj("t" -> "p", "v" -> player.v)
      stay
    }

    case Event(SetVersion(v), player: Player) ⇒
      stay using player.copy(v = v)
  }

  whenUnhandled {

    case _ ⇒ stay
  }

  def setVersion(obj: JsObject) {
    obj int "v" map SetVersion.apply foreach self.!
  }

  def receiveFrom(enumerator: Enumerator[JsValue]) {
    enumerator &> parsingMessage |>> Iteratee.foreach(self.!)
  }

  def sendTo(iteratee: Iteratee[JsValue, _]): JsChannel = {
    val (enumerator, channel) = Concurrent.broadcast[JsValue]
    enumerator |>> iteratee
    channel
  }

  def log(msg: String) {
    println(s"${name} ${msg}")
  }

  def delay(duration: FiniteDuration)(action: ⇒ Unit) {
    context.system.scheduler.scheduleOnce(duration)(action)
  }
}

private[simulation] object Bot {

  sealed trait State
  case object Offline extends State
  case object LobbyConnect extends State
  case object Lobby extends State
  case object RoundConnect extends State
  case object Round extends State

  sealed trait Data
  case object NoData extends Data
  case class Lobbyist(channel: JsChannel) extends Data
  case class FullId(id: String) extends Data
  case class Player(
    id: String,
    channel: JsChannel,
    v: Int = 1,
    move: Int = 0) extends Data

  case object Ping
  case object Move
  case class SetVersion(v: Int)

  // type, full object
  case class Message(t: String, obj: JsObject)

  val parsingMessage: Enumeratee[JsValue, Message] =
    Enumeratee.mapInput[JsValue] {
      case Input.El(js) ⇒ parseMessage(js).fold[Input[Message]](Input.Empty)(Input.El.apply)
      case _            ⇒ Input.Empty
    }

  def parseMessage(js: JsValue): Option[Message] =
    js.asOpt[JsObject] flatMap { obj ⇒
      obj str "t" map { Message(_, obj) }
    }

  import chess.Pos._
  val moves = IndexedSeq(E2 -> E4, D7 -> D5, E4 -> D5, D8 -> D5, B1 -> C3, D5 -> A5, D2 -> D4, C7 -> C6, G1 -> F3, C8 -> G4, C1 -> F4, E7 -> E6, H2 -> H3, G4 -> F3, D1 -> F3, F8 -> B4, F1 -> E2, B8 -> D7, A2 -> A3, E8 -> C8, A3 -> B4, A5 -> A1, E1 -> D2, A1 -> H1, F3 -> C6, B7 -> C6, E2 -> A6)
}
