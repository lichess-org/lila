package controllers

import play.api.mvc.*

import lila.api.Context
import lila.app.{ given, * }
import lila.forum.ForumTopic
import lila.user.User

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
  )(a: => Fu[A])(using Context): Fu[Result] =
    access.isGrantedWrite(categId, tryingToPostAsMod) flatMap { granted =>
      if (granted) a
      else fuccess(Forbidden("You cannot post to this category"))
    }

  protected def CategGrantMod[A <: Result](
      categId: ForumCategId
  )(a: => Fu[A])(using Context): Fu[Result] =
    access.isGrantedMod(categId) flatMap { granted =>
      if (granted | isGranted(_.ModerateForum)) a
      else fuccess(Forbidden("You cannot post to this category"))
    }

  protected def TopicGrantModBySlug[A <: Result](
      categId: ForumCategId,
      me: User,
      topicSlug: String
  )(a: => Fu[A])(using Context): Fu[Result] =
    TopicGrantMod(categId, me)(topicRepo.byTree(categId, topicSlug))(a)

  protected def TopicGrantModById[A <: Result](categId: ForumCategId, me: User, topicId: ForumTopicId)(
      a: => Fu[A]
  )(using Context): Fu[Result] =
    TopicGrantMod(categId, me)(topicRepo.forUser(me.some).byId(topicId))(a)

  private def TopicGrantMod[A <: Result](categId: ForumCategId, me: User)(
      getTopic: => Fu[Option[ForumTopic]]
  )(a: => Fu[A])(using Context): Fu[Result] =
    access.isGrantedMod(categId) flatMap { granted =>
      if (granted | isGranted(_.ModerateForum)) a
      else
        getTopic flatMap { topic =>
          if (topic.exists(_ isUblogAuthor me)) a
          else fuccess(Forbidden("You cannot moderate this forum"))
        }
    }
}
