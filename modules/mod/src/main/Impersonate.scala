package lila.mod

import lila.common.Bus
import lila.hub.actorApi.mod.Impersonate
import lila.user.{ User, UserRepo }

final class ImpersonateApi(userRepo: UserRepo):

  private var modToUser = Map.empty[UserId, UserId]
  private var userToMod = Map.empty[UserId, UserId]

  def start(mod: User, user: User): Unit =
    stop(user)
    modToUser = modToUser + (mod.id  -> user.id)
    userToMod = userToMod + (user.id -> mod.id)
    logger.info(s"${mod.username} starts impersonating ${user.username}")
    Bus.publish(Impersonate(user.id, mod.id.some), "impersonate")

  def stop(user: User): Unit =
    userToMod.get(user.id) ?? { modId =>
      modToUser = modToUser - modId
      userToMod = userToMod - user.id
      logger.info(s"$modId stops impersonating ${user.username}")
      Bus.publish(Impersonate(user.id, none), "impersonate")
    }

  def impersonating(mod: User): Fu[Option[User]] = modToUser.get(mod.id) ?? userRepo.byId

  def impersonatedBy(user: User): Option[UserId] = userToMod get user.id

  def isImpersonated(user: User) = userToMod contains user.id
