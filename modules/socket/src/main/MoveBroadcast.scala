package lidraughts.socket

import akka.actor._
import scala.collection.mutable.AnyRefMap

import actorApi.{ SocketLeave, StartWatching }
import lidraughts.hub.actorApi.round.MoveEvent

private final class MoveBroadcast extends Actor {

  override def preStart(): Unit = {
    context.system.lidraughtsBus.subscribe(self, 'moveEvent, 'socketDoor)
  }

  override def postStop(): Unit = {
    super.postStop()
    context.system.lidraughtsBus.unsubscribe(self)
  }

  type UID = String
  type GameId = String

  case class WatchingMember(member: SocketMember, gameIds: Set[GameId])

  val members = AnyRefMap.empty[UID, WatchingMember]
  val games = AnyRefMap.empty[GameId, Set[UID]]

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
