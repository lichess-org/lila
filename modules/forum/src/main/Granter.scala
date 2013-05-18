package lila.forum

import lila.security.{ Permission, Granter => Master }
import lila.user.Context

trait Granter {

  private val TeamSlugPattern = """^team-([\w-]+)$""".r
  private val StaffSlug = "staff"

  protected def userBelongsToTeam(teamId: String, userId: String): Fu[Boolean] 

  def isGrantedRead(categSlug: String)(implicit ctx: Context): Boolean =
    (categSlug == StaffSlug).fold(
      ctx.me exists Master(Permission.StaffForum),
      true)

  def isGrantedWrite(categSlug: String)(implicit ctx: Context): Fu[Boolean] =
    categSlug match {
      case StaffSlug ⇒
        fuccess(ctx.me exists Master(Permission.StaffForum))
      case TeamSlugPattern(teamId) ⇒
        ctx.me.??(me ⇒ userBelongsToTeam(teamId, me.id))
      case _ ⇒ fuccess(true)
    }
}
