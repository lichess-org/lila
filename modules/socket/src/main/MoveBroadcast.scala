package lila.socket

import akka.actor._
import scala.concurrent.duration._

import actorApi.{ SocketLeave, StartWatching }
import lila.hub.actorApi.round.MoveEvent

private final class MoveBroadcast extends Actor {

  context.system.lilaBus.subscribe(self, 'moveEvent, 'socketDoor)

  type UID = String
  type GameId = String

  case class WatchingMember(member: SocketMember, gameIds: Set[GameId])

  var members = Map.empty[UID, WatchingMember]
  var games = Map.empty[GameId, Set[UID]]

  def status = s"members: $members\ngames: $games"

  def receive = {

    case move: MoveEvent =>
      games get move.gameId foreach { mIds =>
        val msg = Socket.makeMessage("fen", play.api.libs.json.Json.obj(
          "id" -> move.gameId,
          "fen" -> move.fen,
          "lm" -> move.move
        ))
        mIds flatMap members.get foreach (_.member push msg)
      }

    case StartWatching(uid, member, gameIds) =>
      members = members + (uid -> WatchingMember(member, gameIds ++ members.get(uid).??(_.gameIds)))
      gameIds foreach { id =>
        games = games + (id -> (~games.get(id) + uid))
      }

    case SocketLeave(uid) => members get uid foreach { m =>
      members = members - uid
      m.gameIds foreach { id =>
        games get id foreach { uids =>
          val newUids = uids - uid
          if (newUids.isEmpty) games = games - id
          else games = games + (id -> newUids)
        }
      }
    }
  }
}
