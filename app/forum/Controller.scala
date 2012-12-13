package lila
package forum

import core.CoreEnv
import http.Context

import play.api.mvc._
import play.api.mvc.Results._

trait Controller extends ForumGranter { self: controllers.LilaController ⇒

  protected def env: CoreEnv

  protected def userBelongsToTeam = env.team.api.belongsTo _

  protected def CategGrantRead[A <: Result](categSlug: String)(a: ⇒ A)(implicit ctx: Context): Result =
    isGrantedRead(categSlug).fold(a,
      Forbidden("You cannot access to this category")
    )

  protected def CategGrantWrite[A <: Result](categSlug: String)(a: ⇒ A)(implicit ctx: Context): Result =
    isGrantedWrite(categSlug).fold(a,
      Forbidden("You cannot post to this category")
    )
}
