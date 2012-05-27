package lila
package forum

import security.{ Permission, Granter }
import http.Context

import play.api.mvc._
import play.api.mvc.Results._

trait Controller {

  def CategGrant[A <: Result](categSlug: String)(a: ⇒ A)(implicit ctx: Context): Result =
    isGranted(categSlug)(ctx).fold(
      a,
      Forbidden("You cannot access to this category")
    )

  private def isGranted(categSlug: String)(ctx: Context) =
    (categSlug == "staff").fold(
      ctx.me exists { u ⇒ Granter(Permission.StaffForum)(u) },
      true)
}
