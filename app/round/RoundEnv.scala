package lila
package round

import com.mongodb.casbah.MongoCollection

import akka.actor._

import play.api.libs.concurrent._
import play.api.Application

import game.{ GameRepo }
import user.{ UserRepo, EloUpdater }
import ai.Ai
import core.Settings

final class RoundEnv(
    app: Application,
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    gameRepo: GameRepo,
    userRepo: UserRepo,
    eloUpdater: EloUpdater,
    ai: () => Ai) {

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
    getGame = gameRepo.game,
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

  lazy val messenger = new Messenger(roomRepo = roomRepo)

  lazy val roomRepo = new RoomRepo(mongodb(MongoCollectionRoom))
}
