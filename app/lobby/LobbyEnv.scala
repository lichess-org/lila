package lila
package lobby

import akka.actor._
import play.api.libs.concurrent._
import play.api.Application
import play.api.i18n.Lang
import play.api.i18n.MessagesPlugin
import scalaz.effects._
import com.mongodb.casbah.MongoCollection

import timeline.Entry
import user.{ User, UserRepo }
import game.{ DbGame, Featured }
import round.{ Socket ⇒ RoundSocket, Messenger ⇒ RoundMessenger }
import socket.History
import security.Flood
import core.Settings

final class LobbyEnv(
    app: Application,
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    userRepo: UserRepo,
    getGame: String => IO[Option[DbGame]],
    featured: Featured,
    roundSocket: RoundSocket,
    roundMessenger: RoundMessenger,
    flood: Flood) {

  implicit val ctx = app
  import settings._

  lazy val history = new History(timeout = LobbyMessageLifetime)

  lazy val messenger = new Messenger(
    messageRepo = messageRepo,
    userRepo = userRepo)

  lazy val hub: ActorRef = Akka.system.actorOf(Props(new Hub(
    messenger = messenger,
    history = history,
    timeout = SiteUidTimeout
  )), name = ActorLobbyHub)

  lazy val socket = new Socket(
    hub = hub,
    flood = flood)

  lazy val fisherman = new Fisherman(
    hookRepo = hookRepo,
    hookMemo = hookMemo,
    socket = socket)

  lazy val messageRepo = new MessageRepo(
    collection = mongodb(LobbyCollectionMessage),
    max = LobbyMessageMax)

  lazy val hookRepo = new HookRepo(mongodb(LobbyCollectionHook))

  lazy val hookMemo = new HookMemo(timeout = MemoHookTimeout)

  lazy val preloader = new Preload(
    fisherman = fisherman,
    history = history,
    hookRepo = hookRepo,
    getGame = getGame,
    messageRepo = messageRepo,
    featured = featured)
}
