package controllers

import lila.app._
import lila.user.Context
import lila.forum

import play.api.mvc._, Results._

trait ForumController extends forum.Granter { self: LilaController ⇒

  protected def categApi = Env.forum.categApi
  protected def topicApi = Env.forum.topicApi
  protected def postApi = Env.forum.postApi
  protected def forms = Env.forum.forms

  protected def teamCache = Env.team.cached

  protected def userBelongsToTeam(teamId: String, userId: String): Fu[Boolean] =
    Env.team.api.belongsTo(teamId, userId)

  protected def CategGrantRead[A <: Result](categSlug: String)(a: ⇒ Fu[A])(implicit ctx: Context): Fu[Result] =
    isGrantedRead(categSlug).fold(a,
      fuccess(Forbidden("You cannot access to this category"))
    )

  protected def CategGrantWrite[A <: Result](categSlug: String)(a: ⇒ Fu[A])(implicit ctx: Context): Fu[Result] =
    isGrantedWrite(categSlug) flatMap {
      _ fold (
        a,
        fuccess(Forbidden("You cannot post to this category"))
      )
    }
}
