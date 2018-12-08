package lila.socket

import akka.actor._
import scala.collection.mutable.AnyRefMap

import actorApi.{ SocketLeave, StartWatching }
import lila.hub.actorApi.round.MoveEvent

private final class MoveBroadcast extends Actor {

  override def preStart(): Unit = {
    context.system.lilaBus.subscribe(self, 'moveEvent, 'socketLeave)
  }

  override def postStop(): Unit = {
    super.postStop()
    context.system.lilaBus.unsubscribe(self)
  }

  type UidString = String
  type GameId = String

  case class WatchingMember(member: SocketMember, gameIds: Set[GameId])

  val members = AnyRefMap.empty[UidString, WatchingMember]
  val games = AnyRefMap.empty[GameId, Set[UidString]]

  def receive = {

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

    case StartWatching(uid, member, gameIds) =>
      members += (uid.value -> WatchingMember(member, gameIds ++ members.get(uid.value).??(_.gameIds)))
      gameIds foreach { id =>
        games += (id -> (~games.get(id) + uid.value))
      }

    case SocketLeave(uid, _) => members get uid.value foreach { m =>
      members -= uid.value
      m.gameIds foreach { id =>
        games get id foreach { uids =>
          val newUids = uids - uid.value
          if (newUids.isEmpty) games -= id
          else games += (id -> newUids)
        }
      }
    }
  }
}
