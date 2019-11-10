package lila.mod

import lila.user.{ User, UserRepo }
import lila.security.Granter
import lila.hub.actorApi.mod.Impersonate

final class ImpersonateApi(bus: lila.common.Bus) {

  private var modToUser = Map.empty[User.ID, User.ID]
  private var userToMod = Map.empty[User.ID, User.ID]

  def start(mod: User, user: User): Unit = Granter(_.Impersonate)(mod) ?? {
    stop(user)
    modToUser = modToUser + (mod.id -> user.id)
    userToMod = userToMod + (user.id -> mod.id)
    logger.info(s"${mod.username} starts impersonating ${user.username}")
    bus.publish(Impersonate(user.id, mod.id.some), 'impersonate)
  }

  def stop(user: User): Unit = userToMod.get(user.id) ?? { modId =>
    modToUser = modToUser - modId
    userToMod = userToMod - user.id
    logger.info(s"${modId} stops impersonating ${user.username}")
    bus.publish(Impersonate(user.id, none), 'impersonate)
  }

  def impersonating(mod: User): Fu[Option[User]] = modToUser.get(mod.id) ?? UserRepo.byId

  def impersonatedBy(user: User): Option[User.ID] = userToMod get user.id

  def isImpersonated(user: User) = userToMod contains user.id
}
