package controllers

import play.api.mvc._, Results._

import lila.api.Context
import lila.app._
import lila.forum

private[controllers] trait ForumController extends forum.Granter { self: LilaController =>

  protected def categApi = Env.forum.categApi
  protected def topicApi = Env.forum.topicApi
  protected def postApi = Env.forum.postApi
  protected def forms = Env.forum.forms

  protected def teamCache = Env.team.cached

  protected def userBelongsToTeam(teamId: String, userId: String): Boolean =
    Env.team.api.belongsTo(teamId, userId)

  protected def userOwnsTeam(teamId: String, userId: String): Fu[Boolean] =
    Env.team.api.owns(teamId, userId)

  protected def CategGrantRead[A <: SimpleResult](categSlug: String)(a: => Fu[A])(implicit ctx: Context): Fu[SimpleResult] =
    isGrantedRead(categSlug).fold(a,
      fuccess(Forbidden("You cannot access to this category"))
    )

  protected def CategGrantWrite[A <: SimpleResult](categSlug: String)(a: => Fu[A])(implicit ctx: Context): Fu[SimpleResult] =
    if (isGrantedWrite(categSlug)) a
    else fuccess(Forbidden("You cannot post to this category"))

  protected def CategGrantMod[A <: SimpleResult](categSlug: String)(a: => Fu[A])(implicit ctx: Context): Fu[SimpleResult] =
    isGrantedMod(categSlug) flatMap { granted =>
      (granted | isGranted(_.ModerateForum)) fold (
        a,
        fuccess(Forbidden("You cannot post to this category"))
      )
    }
}
