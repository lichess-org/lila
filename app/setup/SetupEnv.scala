package lila
package setup

import core.Settings
import game.{ DbGame, GameRepo }
import lobby.{ HookRepo, Fisherman }
import round.Messenger
import ai.Ai
import user.{ User, UserRepo }

import com.mongodb.casbah.MongoCollection
import scalaz.effects._
import com.mongodb.DBRef

final class SetupEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    gameRepo: GameRepo,
    hookRepo: HookRepo,
    fisherman: Fisherman,
    userRepo: UserRepo,
    timelinePush: DbGame ⇒ IO[Unit],
    roundMessenger: Messenger,
    ai: () ⇒ Ai,
    userDbRef: User ⇒ DBRef) {

  import settings._

  lazy val configRepo = new UserConfigRepo(mongodb(MongoCollectionConfig))

  lazy val formFactory = new FormFactory(
    configRepo = configRepo)

  lazy val processor = new Processor(
    configRepo = configRepo,
    friendConfigMemo = friendConfigMemo,
    gameRepo = gameRepo,
    fisherman = fisherman,
    timelinePush = timelinePush,
    ai = ai,
    userDbRef = userDbRef)

  lazy val friendConfigMemo = new FriendConfigMemo(
    ttl = SetupFriendConfigMemoTtl)

  lazy val rematcher = new Rematcher(
    gameRepo = gameRepo,
    userRepo = userRepo,
    messenger = roundMessenger,
    timelinePush = timelinePush)

  lazy val friendJoiner = new FriendJoiner(
    gameRepo = gameRepo,
    messenger = roundMessenger,
    timelinePush = timelinePush,
    userDbRef = userDbRef)

  lazy val hookJoiner = new HookJoiner(
    hookRepo = hookRepo,
    fisherman = fisherman,
    gameRepo = gameRepo,
    userRepo = userRepo,
    userDbRef = userDbRef,
    timelinePush = timelinePush,
    messenger = roundMessenger)
}
