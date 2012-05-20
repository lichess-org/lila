package lila
package round

import scalaz.effects._
import com.mongodb.casbah.MongoCollection
import akka.actor.Props
import play.api.libs.concurrent._
import play.api.Application

import game.{ GameRepo, DbGame }
import user.{ UserRepo, User, EloUpdater }
import ai.Ai
import core.Settings
import i18n.I18nKeys

final class RoundEnv(
    app: Application,
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    gameRepo: GameRepo,
    userRepo: UserRepo,
    eloUpdater: EloUpdater,
    i18nKeys: I18nKeys,
    ai: () ⇒ Ai) {

  implicit val ctx = app
  import settings._

  lazy val history = () ⇒ new History(timeout = GameMessageLifetime)

  lazy val hubMaster = Akka.system.actorOf(Props(new HubMaster(
    makeHistory = history,
    uidTimeout = GameUidTimeout,
    hubTimeout = GameHubTimeout,
    playerTimeout = GamePlayerTimeout
  )), name = ActorGameHubMaster)

  lazy val socket = new Socket(
    getWatcherPov = gameRepo.pov,
    getPlayerPov = gameRepo.pov,
    hand = hand,
    hubMaster = hubMaster,
    messenger = messenger)

  lazy val hand = new Hand(
    gameRepo = gameRepo,
    messenger = messenger,
    ai = ai,
    finisher = finisher,
    takeback = takeback,
    hubMaster = hubMaster,
    moretimeSeconds = MoretimeSeconds)

  lazy val finisher = new Finisher(
    userRepo = userRepo,
    gameRepo = gameRepo,
    messenger = messenger,
    eloUpdater = eloUpdater,
    eloCalculator = eloCalculator,
    finisherLock = finisherLock)

  lazy val eloCalculator = new chess.EloCalculator

  lazy val finisherLock = new FinisherLock(timeout = FinisherLockTimeout)

  lazy val takeback = new Takeback(
    gameRepo = gameRepo,
    messenger = messenger)

  lazy val messenger = new Messenger(
    roomRepo = roomRepo,
    i18nKeys = i18nKeys)

  lazy val roomRepo = new RoomRepo(mongodb(MongoCollectionRoom))
}
