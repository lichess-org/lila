package controllers

import play.api.mvc._

import lidraughts.api.Context
import lidraughts.app._
import lidraughts.forum

private[controllers] trait ForumController extends forum.Granter { self: LidraughtsController =>

  protected def categApi = Env.forum.categApi
  protected def topicApi = Env.forum.topicApi
  protected def postApi = Env.forum.postApi
  protected def forms = Env.forum.forms

  protected def teamCache = Env.team.cached

  protected def userBelongsToTeam(teamId: String, userId: String): Fu[Boolean] =
    Env.team.api.belongsTo(teamId, userId)

  protected def userOwnsTeam(teamId: String, userId: String): Fu[Boolean] =
    Env.team.api.owns(teamId, userId)

  protected def CategGrantRead[A <: Result](categSlug: String)(a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    isGrantedRead(categSlug).fold(
      a,
      fuccess(Forbidden("You cannot access to this category"))
    )

  protected def CategGrantWrite[A <: Result](categSlug: String)(a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    isGrantedWrite(categSlug) flatMap { granted =>
      if (granted) a
      else fuccess(Forbidden("You cannot post to this category"))
    }

  protected def CategGrantMod[A <: Result](categSlug: String)(a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    isGrantedMod(categSlug) flatMap { granted =>
      (granted | isGranted(_.ModerateForum)) fold (
        a,
        fuccess(Forbidden("You cannot post to this category"))
      )
    }
}
