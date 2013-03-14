package lila.app
package setup

import core.Settings
import game.{ DbGame, GameRepo, PgnRepo }
import lobby.{ HookRepo, Fisherman }
import round.Messenger
import ai.Ai
import user.{ User, UserRepo }

import com.mongodb.casbah.MongoCollection
import scalaz.effects._

final class SetupEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    gameRepo: GameRepo,
    pgnRepo: PgnRepo,
    hookRepo: HookRepo,
    fisherman: Fisherman,
    userRepo: UserRepo,
    timelinePush: DbGame ⇒ IO[Unit],
    roundMessenger: Messenger,
    ai: () ⇒ Ai) {

  import settings._

  lazy val userConfigRepo = new UserConfigRepo(mongodb(UserCollectionConfig))

  lazy val anonConfigRepo = new AnonConfigRepo(mongodb(AnonCollectionConfig))

  lazy val formFactory = new FormFactory(
    userConfigRepo = userConfigRepo,
    anonConfigRepo = anonConfigRepo)

  lazy val processor = new Processor(
    userConfigRepo = userConfigRepo,
    anonConfigRepo = anonConfigRepo,
    friendConfigMemo = friendConfigMemo,
    gameRepo = gameRepo,
    pgnRepo = pgnRepo,
    fisherman = fisherman,
    timelinePush = timelinePush,
    ai = ai)

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
    timelinePush = timelinePush)

  lazy val hookJoiner = new HookJoiner(
    hookRepo = hookRepo,
    fisherman = fisherman,
    gameRepo = gameRepo,
    userRepo = userRepo,
    timelinePush = timelinePush,
    messenger = roundMessenger)

  def filter(implicit ctx: http.Context): IO[FilterConfig] = 
    ctx.me.fold(anonConfigRepo filter ctx.req)(userConfigRepo.filter)
}
