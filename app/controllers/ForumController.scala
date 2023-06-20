package controllers

import play.api.mvc.*

import lila.app.{ given, * }
import lila.forum.ForumTopic

private[controllers] trait ForumController { self: LilaController =>

  protected def categApi  = env.forum.categApi
  protected def topicApi  = env.forum.topicApi
  protected def topicRepo = env.forum.topicRepo
  protected def postApi   = env.forum.postApi
  protected def forms     = env.forum.forms
  protected def access    = env.api.forumAccess
  protected def teamCache = env.team.cached

  protected def CategGrantWrite[A <: Result](
      categId: ForumCategId,
      tryingToPostAsMod: Boolean = false
  )(a: => Fu[A])(using WebContext, Me): Fu[Result] =
    access.isGrantedWrite(categId, tryingToPostAsMod) flatMap {
      if _ then a
      else Forbidden("You cannot post to this category")
    }

  protected def CategGrantMod[A <: Result](
      categId: ForumCategId
  )(a: => Fu[A])(using WebContext, Me): Fu[Result] =
    access.isGrantedMod(categId) flatMap { granted =>
      if granted | isGranted(_.ModerateForum) then a
      else Forbidden("You cannot post to this category")
    }

  protected def TopicGrantModBySlug[A <: Result](
      categId: ForumCategId,
      topicSlug: String
  )(a: => Fu[A])(using WebContext, Me): Fu[Result] =
    TopicGrantMod(categId)(topicRepo.byTree(categId, topicSlug))(a)

  protected def TopicGrantModById[A <: Result](categId: ForumCategId, topicId: ForumTopicId)(
      a: => Fu[A]
  )(using WebContext)(using me: Me): Fu[Result] =
    TopicGrantMod(categId)(topicRepo.forUser(me.some).byId(topicId))(a)

  private def TopicGrantMod[A <: Result](categId: ForumCategId)(
      getTopic: => Fu[Option[ForumTopic]]
  )(a: => Fu[A])(using WebContext)(using me: Me): Fu[Result] =
    access.isGrantedMod(categId) flatMap { granted =>
      if granted | isGranted(_.ModerateForum)
      then a
      else
        getTopic flatMap { topic =>
          if topic.exists(_ isUblogAuthor me) then a
          else Forbidden("You cannot moderate this forum")
        }
    }
}
