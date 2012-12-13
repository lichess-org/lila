package lila
package forum

import security.{ Permission, Granter }
import http.Context
import scalaz.effects._

trait ForumGranter {

  private val TeamSlugPattern = """^team-([\w-]+)$""".r
  private val StaffSlug = "staff"

  //                                teamId  userId
  protected def userBelongsToTeam: (String, String) ⇒ IO[Boolean]

  def isGrantedRead(categSlug: String)(implicit ctx: Context): Boolean =
    (categSlug == StaffSlug).fold(
      ctx.me exists { u ⇒ Granter(Permission.StaffForum)(u) },
      true
    )

  def isGrantedWrite(categSlug: String)(implicit ctx: Context): Boolean = categSlug match {
    case StaffSlug               ⇒ ctx.me exists { u ⇒ Granter(Permission.StaffForum)(u) }
    case TeamSlugPattern(teamId) ⇒ ctx.me.fold(me ⇒ userBelongsToTeam(teamId, me.id).unsafePerformIO, false)
    case _                       ⇒ true
  }
}
