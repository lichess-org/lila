package lila.simulation

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.util.{ Random, Try }

import akka.actor._
import akka.pattern.{ ask, pipe }
import chess.Color
import play.api.libs.json._

import lila.common.PimpedJson._

private[simulation] final class PlayerBot(
    val name: String,
    config: PlayerConfig,
    lobbyEnv: lila.lobby.Env,
    roundEnv: lila.round.Env) extends Bot with FSM[PlayerBot.State, PlayerBot.Data] {

  import Bot._
  import PlayerBot._

  log("spawned")

  startWith(Offline, NoData)

  when(Offline) {

    case Event(Simulator.Start, _) => goto(LobbyConnect)
  }

  onTransition {
    case _ -> LobbyConnect => lobbyEnv.socketHandler(uid, user) pipeTo self
  }

  onTransition {
    case a -> b => log(s"$a -> $b")
  }

  when(LobbyConnect) {

    case Event((iteratee: JsIteratee, enumerator: JsEnumerator), _) => {
      delayRandomMillis(4000) {
        self ! config.hookConfig.hook(uid, user, sid)
      }
      receiveFrom(enumerator)
      goto(Lobby) using Lobbyist(sendTo(iteratee))
    }
  }

  when(Lobby, stateTimeout = 20 seconds) {

    // pong
    case Event(Message("n", obj), _) => {
      delay(1 second)(self ! Ping)
      stay
    }

    case Event(hook: lila.lobby.Hook, _) => {
      lobbyEnv.lobby ! lila.lobby.actorApi.AddHook(hook)
      stay
    }

    case Event(Message("redirect", obj), Lobbyist(channel)) => obj str "d" map { url =>
      channel.eofAndEnd()
      val id = url drop 1
      roundEnv.socketHandler.player(id, 0, uid, "token", user, ip) pipeTo self
      goto(RoundConnect) using FullId(id)
    } getOrElse stay

    case Event(StateTimeout, Lobbyist(channel)) => {
      channel.eofAndEnd()
      goto(LobbyConnect) using NoData
    }
  }

  when(RoundConnect) {

    case Event((iteratee: JsIteratee, enumerator: JsEnumerator), FullId(id)) => {
      receiveFrom(enumerator)
      lila.game.GameRepo.pov(id).flatten(id).map(_.color) pipeTo self
      stay using Player(id, sendTo(iteratee))
    }

    case Event(color: chess.Color, player: Player) => {
      log(s"start ${player.id} as $color")
      delay(1 second)(self ! Ping)
      delay(10 second)(self ! QuitIfStalled(player.v))
      color match {
        case Color.White => {
          delay(2 seconds)(self ! Move)
          goto(RoundPlay) using player
        }
        case Color.Black => goto(RoundPlay) using player.nextMove._2
      }
    }
  }

  val roundFallback: StateFunction = {

    // pong
    case Event(Message("n", _), _) => {
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

    case Event(Ping, player: Player) => {
      player.channel push Json.obj("t" -> "p", "v" -> player.v)
      stay
    }

    case Event(SetVersion(v), player: Player) =>
      stay using player.copy(v = v)
  }

  def roundHandler(handler: StateFunction) = handler orElse roundFallback

  when(RoundPlay)(roundHandler({

    case Event(Message("end", _), player: Player) => {
      goto(RoundEnd)
    }

    case Event(Message("possibleMoves", obj), player: Player) =>
      if (~getVersion(obj) <= player.v) {
        log(s"skip possibleMoves v ${getVersion(obj)} because at v ${player.v}")
        stay
      }
      else {
        setVersion(obj)
        maybe(1d / 200)(self ! Resign)
        log("possibleMoves: " + (obj obj "d"))
        // opponent move
        if ((obj obj "d").isEmpty) {
          val (_, nextPlayer) = player.nextMove
          goto(RoundPlay) using nextPlayer
        }
        else {
          delay(50 millis) {
            delayRandomMillis(config.thinkDelay)(self ! Move)
          }
          goto(RoundPlay) using player
        }
      }

    case Event(QuitIfStalled(v), player: Player) => {
      if (player.v == v) {
        player.channel.eofAndEnd()
        goto(LobbyConnect) using NoData
      } else {
        delay(10 second)(self ! QuitIfStalled(player.v))
      }
      stay
    }

    case Event(Resign, player: Player) => {
      player.channel push Json.obj("t" -> "resign")
      stay
    }

    case Event(Move, player: Player) => {
      val (move, nextPlayer) = player.nextMove
      log(move)
      move foreach {
        case (from, to) => player.channel push Json.obj(
          "t" -> "move",
          "d" -> Json.obj(
            "from" -> from.key,
            "to" -> to.key
          ))
      }
      stay using nextPlayer
    }
  }))

  onTransition {
    case _ -> RoundEnd => maybe(config.rematchProbability) {
      delayRandomMillis(5000)(self ! Rematch)
    }
  }

  when(RoundEnd, stateTimeout = 5.seconds)(roundHandler({

    // don't ping, so the timeout can be reached
    case Event(Message("n", _), _) => stay

    case Event(Rematch, player: Player) => {
      player.channel push Json.obj("t" -> "rematch-yes")
      stay
    }

    case Event(Message("redirect", obj), player: Player) => obj str "d" map { url =>
      player.channel.eofAndEnd()
      val id = url drop 1
      delay(200 millis) {
        roundEnv.socketHandler.player(id, 0, uid, "token", user, ip) pipeTo self
      }
      goto(RoundConnect) using FullId(id)
    } getOrElse stay

    case Event(StateTimeout, player: Player) => {
      player.channel.eofAndEnd()
      goto(LobbyConnect) using NoData
    }
  }))

  whenUnhandled {

    case e => {
      log(e.toString.take(60))
      stay
    }
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
      case Some((move, queue)) => move.some -> copy(moves = queue)
      case None                => none -> this
    }
  }

  case object Move
  case object Rematch
  case object Resign
  case class QuitIfStalled(v: Int)

  import chess.Pos._
  val allMoves: Queue[Move] = Queue(E2 -> E4, D7 -> D5, E4 -> D5, D8 -> D5, B1 -> C3, D5 -> A5, D2 -> D4, C7 -> C6, G1 -> F3, C8 -> G4, C1 -> F4, E7 -> E6, H2 -> H3, G4 -> F3, D1 -> F3, F8 -> B4, F1 -> E2, B8 -> D7, A2 -> A3, E8 -> C8, A3 -> B4, A5 -> A1, E1 -> D2, A1 -> H1, F3 -> C6, B7 -> C6, E2 -> A6)
}
