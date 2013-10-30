package lila.simulation

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.util.Try

import akka.actor._
import akka.pattern.{ ask, pipe }
import chess.Color
import play.api.libs.json._

import lila.common.PimpedJson._

private[simulation] final class PlayerBot(
    val name: String,
    lobbyEnv: lila.lobby.Env,
    roundEnv: lila.round.Env) extends Bot with FSM[PlayerBot.State, PlayerBot.Data] {

  import Bot._
  import PlayerBot._

  log("spawned")

  startWith(Offline, NoData)

  when(Offline) {

    case Event(Simulator.Start, _) ⇒ goto(LobbyConnect)
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
      delay(1.5 second)(self ! Move)
      goto(RoundPlay) using Player(id, sendTo(iteratee))
    }
  }

  val roundFallback: StateFunction = {

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

    // any other versioned event
    case Event(Message(_, obj), _) ⇒ {
      log(obj.toString)
      setVersion(obj)
      stay
    }
  }

  def roundHandler(handler: StateFunction) = handler orElse roundFallback

  when(RoundPlay)(roundHandler({

    case Event(Message("end", _), player: Player) ⇒ {
      goto(RoundEnd)
    }

    case Event(Message("possible_moves", obj), player: Player) ⇒ {
      // opponent move
      if ((obj obj "d").isEmpty) {
        val (_, nextPlayer) = player.nextMove
        goto(RoundPlay) using nextPlayer
      }
      else {
        delayRandomMillis(2000)(self ! Move)
        goto(RoundPlay) using player
      }
    }

    case Event(Move, player: Player) ⇒ {
      val (move, nextPlayer) = player.nextMove
      move foreach {
        case (from, to) ⇒ player.channel push Json.obj(
          "t" -> "move",
          "d" -> Json.obj(
            "from" -> from.key,
            "to" -> to.key
          ))
      }
      stay using nextPlayer
    }

    case Event(Ping, player: Player) ⇒ {
      player.channel push Json.obj("t" -> "p", "v" -> player.v)
      stay
    }

    case Event(SetVersion(v), player: Player) ⇒
      stay using player.copy(v = v)
  }))

  onTransition {
    case _ -> RoundEnd ⇒ maybe(3d/4) {
      delayRandomMillis(5000)(self ! Rematch)
    }
  }

  when(RoundEnd, stateTimeout = 5.seconds)(roundHandler({

    case Event(Rematch, player: Player) ⇒ {
      player.channel push Json.obj("t" -> "rematch-yes")
      stay
    }

    case Event(Message("redirect", obj), player: Player) ⇒ obj str "d" map { url ⇒
      player.channel.eofAndEnd()
      val id = url drop 1
      roundEnv.socketHandler.player(id, 0, uid, "token", user, ip) pipeTo self
      goto(RoundConnect) using FullId(id)
    } getOrElse stay

    case Event(StateTimeout, player: Player) ⇒ {
      player.channel.eofAndEnd()
      goto(Lobby) using NoData
    }
  }))

  whenUnhandled {

    case _ ⇒ stay
  }
}

private[simulation] object PlayerBot {

  type Move = (chess.Pos, chess.Pos)

  sealed trait State
  case object Offline extends State
  case object LobbyConnect extends State
  case object Lobby extends State
  case object RoundConnect extends State
  case object RoundPlay extends State
  case object RoundEnd extends State

  sealed trait Data
  case object NoData extends Data
  case class Lobbyist(channel: JsChannel) extends Data
  case class FullId(id: String) extends Data
  case class Player(
      id: String,
      channel: JsChannel,
      v: Int = 1,
      moves: Queue[Move] = allMoves) extends Data {
    def nextMove: (Option[Move], Player) = Try(moves.dequeue).toOption match {
      case Some((move, queue)) ⇒ move.some -> copy(moves = queue)
      case None                ⇒ none -> this
    }
  }

  case object Move
  case object Rematch

  import chess.Pos._
  val allMoves: Queue[Move] = Queue(E2 -> E4, D7 -> D5, E4 -> D5, D8 -> D5, B1 -> C3, D5 -> A5, D2 -> D4, C7 -> C6, G1 -> F3, C8 -> G4, C1 -> F4, E7 -> E6, H2 -> H3, G4 -> F3, D1 -> F3, F8 -> B4, F1 -> E2, B8 -> D7, A2 -> A3, E8 -> C8, A3 -> B4, A5 -> A1, E1 -> D2, A1 -> H1, F3 -> C6, B7 -> C6, E2 -> A6)
}
