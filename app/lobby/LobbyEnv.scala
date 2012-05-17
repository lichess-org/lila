package lila
package lobby

import com.mongodb.casbah.MongoCollection

import akka.actor._

import play.api.libs.concurrent._
import play.api.Application
import play.api.i18n.Lang
import play.api.i18n.MessagesPlugin

import user.UserRepo
import game.GameRepo
import round.{ Socket ⇒ RoundSocket, Messenger ⇒ RoundMessenger }
import ai.Ai
import core.Settings

final class LobbyEnv(
    app: Application,
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    userRepo: UserRepo,
    roundSocket: RoundSocket,
    roundMessenger: RoundMessenger) {

  implicit val ctx = app
  import settings._

  lazy val history = new History(timeout = LobbyMessageLifetime)

  lazy val messenger = new Messenger(
    messageRepo = messageRepo,
    userRepo = userRepo)

  lazy val hub = Akka.system.actorOf(Props(new Hub(
    messenger = messenger,
    history = history,
    timeout = SiteUidTimeout
  )), name = ActorLobbyHub)

  lazy val socket = new Socket(hub = hub)

  lazy val fisherman = new Fisherman(
    hookRepo = hookRepo,
    hookMemo = hookMemo,
    socket = socket)

  lazy val messageRepo = new MessageRepo(
    collection = mongodb(MongoCollectionMessage),
    max = LobbyMessageMax)

  //lazy val api = new Api(
    //hookRepo = hookRepo,
    //fisherman = fisherman,
    //gameRepo = gameRepo,
    //roundSocket = roundSocket,
    //roundMessenger = roundMessenger,
    //starter = starter)

  lazy val hookRepo = new HookRepo(mongodb(MongoCollectionHook))

  lazy val hookMemo = new HookMemo(timeout = MemoHookTimeout)
}
