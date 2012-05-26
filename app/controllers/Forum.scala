package controllers

import lila._
import views._
import security._
import http.Context

import play.api.mvc._
import play.api.mvc.Results._

trait Forum {

  def CategGrant[A <: Result](categSlug: String)(a: ⇒ A)(implicit ctx: Context): Result =
    isGranted(categSlug)(ctx).fold(
      a,
      Forbidden("You cannot access to this category")
    )

  def isGranted(categSlug: String)(ctx: Context) =
    (categSlug == "staff").fold(
      ctx.me exists { u ⇒ Granter(Permission.StaffForum)(u) },
      true)
}
