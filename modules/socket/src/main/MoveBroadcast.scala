package lila.socket

import akka.actor._
import scala.concurrent.duration._

import actorApi.{ SocketLeave, StartWatching }
import lila.hub.actorApi.round.MoveEvent

private final class MoveBroadcast extends Actor {

  override def preStart() {
    context.system.lilaBus.subscribe(self, 'moveEvent, 'socketDoor)
  }

  override def postStop() {
    super.postStop()
    context.system.lilaBus.unsubscribe(self)
  }

  type UID = String
  type GameId = String

  case class WatchingMember(member: SocketMember, gameIds: Set[GameId])

  val members = scala.collection.mutable.Map.empty[UID, WatchingMember]
  val games = scala.collection.mutable.Map.empty[GameId, Set[UID]]

  def receive = {

    case move: MoveEvent =>
      games get move.gameId foreach { mIds =>
        val msg = Socket.makeMessage("fen", play.api.libs.json.Json.obj(
          "id" -> move.gameId,
          "fen" -> move.fen,
          "lm" -> move.move
        ))
        mIds foreach { mId =>
          members get mId foreach (_.member push msg)
        }
      }

    case StartWatching(uid, member, gameIds) =>
      members += (uid -> WatchingMember(member, gameIds ++ members.get(uid).??(_.gameIds)))
      gameIds foreach { id =>
        games += (id -> (~games.get(id) + uid))
      }

    case SocketLeave(uid, _) => members get uid foreach { m =>
      members -= uid
      m.gameIds foreach { id =>
        games get id foreach { uids =>
          val newUids = uids - uid
          if (newUids.isEmpty) games -= id
          else games += (id -> newUids)
        }
      }
    }
  }
}
