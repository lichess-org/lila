package lila.forum

import lila.security.{ Permission, Granter => Master }
import lila.user.{ User, UserContext }

trait Granter {

  private val TeamSlugPattern = """team-([\w-]++)""".r

  protected def userBelongsToTeam(teamId: String, userId: String): Fu[Boolean]
  protected def userOwnsTeam(teamId: String, userId: String): Fu[Boolean]

  def isGrantedWrite(categSlug: String)(implicit ctx: UserContext): Fu[Boolean] =
    ctx.me.filter(isOldEnoughToForum) ?? { me =>
      categSlug match {
        case TeamSlugPattern(teamId) => userBelongsToTeam(teamId, me.id)
        case _                       => fuTrue
      }
    }

  private def isOldEnoughToForum(u: User) = {
    u.count.game >= 3 //&& u.createdSinceDays(2)
  } || u.hasTitle || u.isVerified

  def isGrantedMod(categSlug: String)(implicit ctx: UserContext): Fu[Boolean] =
    categSlug match {
      case _ if ctx.me ?? Master(Permission.ModerateForum) => fuTrue
      case TeamSlugPattern(teamId) =>
        ctx.me ?? { me =>
          userOwnsTeam(teamId, me.id)
        }
      case _ => fuFalse
    }
}
