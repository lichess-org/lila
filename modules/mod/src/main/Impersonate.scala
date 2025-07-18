package lila.mod

import lila.common.Bus
import lila.core.mod.Impersonate
import lila.user.{ Me, User, UserRepo }

final class ImpersonateApi(userRepo: UserRepo):

  private var modToUser = Map.empty[ModId, UserId]
  private var userToMod = Map.empty[UserId, ModId]

  def start(mod: ModId, user: User): Unit =
    stop(user)
    modToUser = modToUser + (mod     -> user.id)
    userToMod = userToMod + (user.id -> mod)
    logger.info(s"$mod starts impersonating ${user.username}")
    Bus.pub(Impersonate(user.id, mod.some))

  def stop(user: User): Unit =
    userToMod.get(user.id).so { modId =>
      modToUser = modToUser - modId
      userToMod = userToMod - user.id
      logger.info(s"$modId stops impersonating ${user.username}")
      Bus.pub(Impersonate(user.id, none))
    }

  def impersonating(me: Me): Fu[Option[User]] = modToUser.get(me.modId).so(userRepo.byId)

  def isImpersonated(user: User) = userToMod contains user.id
