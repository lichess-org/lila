package lila.socket

import scala.collection.mutable.AnyRefMap

import actorApi.{ SocketLeave, StartWatching }
import lila.hub.Trouper
import lila.hub.actorApi.round.MoveEvent

private final class MoveBroadcast(system: akka.actor.ActorSystem) extends Trouper {

  system.lilaBus.subscribe(this, 'moveEvent, 'socketLeave, 'socketMoveBroadcast)

  private type SriString = String
  private type GameId = String

  private case class WatchingMember(member: SocketMember, gameIds: Set[GameId])

  private val members = AnyRefMap.empty[SriString, WatchingMember]
  private val games = AnyRefMap.empty[GameId, Set[SriString]]

  val process: Trouper.Receive = {

    case MoveEvent(gameId, fen, move) =>
      games get gameId foreach { mIds =>
        val msg = Socket.makeMessage("fen", play.api.libs.json.Json.obj(
          "id" -> gameId,
          "fen" -> fen,
          "lm" -> move
        ))
        mIds foreach { mId =>
          members get mId foreach (_.member push msg)
        }
      }

    case StartWatching(sri, member, gameIds) =>
      members += (sri.value -> WatchingMember(member, gameIds ++ members.get(sri.value).??(_.gameIds)))
      gameIds foreach { id =>
        games += (id -> (~games.get(id) + sri.value))
      }

    case SocketLeave(sri, _) => members get sri.value foreach { m =>
      members -= sri.value
      m.gameIds foreach { id =>
        games get id foreach { sris =>
          val newSris = sris - sri.value
          if (newSris.isEmpty) games -= id
          else games += (id -> newSris)
        }
      }
    }
  }
}
