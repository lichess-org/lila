package lila.forum

import lila.security.{ Permission, Granter }
import lila.user.Context

trait Granter {

  private val TeamSlugPattern = """^team-([\w-]+)$""".r
  private val StaffSlug = "staff"

  //                                teamId  userId
  protected def userBelongsToTeam: (String, String) ⇒ Boolean

  def isGrantedRead(categSlug: String)(implicit ctx: Context): Boolean =
    (categSlug == StaffSlug).fold(
      ctx.me exists Granter(Permission.StaffForum),
      true)

  def isGrantedWrite(categSlug: String)(implicit ctx: Context): Boolean =
    categSlug match {
      case StaffSlug               ⇒ ctx.me exists Granter(Permission.StaffForum)
      case TeamSlugPattern(teamId) ⇒ ctx.me.zmap(me ⇒ userBelongsToTeam(teamId, me.id))
      case _                       ⇒ true
    }
}
