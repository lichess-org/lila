package controllers

import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.forum.Topic
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
      categSlug: String,
      tryingToPostAsMod: Boolean = false
  )(a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    access.isGrantedWrite(categSlug, tryingToPostAsMod) flatMap { granted =>
      if (granted) a
      else fuccess(Forbidden("You cannot post to this category"))
    }

  protected def CategGrantMod[A <: Result](
      categSlug: String
  )(a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    access.isGrantedMod(categSlug) flatMap { granted =>
      if (granted | isGranted(_.ModerateForum)) a
      else fuccess(Forbidden("You cannot post to this category"))
    }

  protected def TopicGrantMod[A <: Result](
      categSlug: String,
      forUser: Option[User],
      topicSlug: Option[String] = None, // pass one
      topicId: Option[String] = None    // or the other
  )(a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    access.isGrantedMod(categSlug) flatMap { granted =>
      if (granted | isGranted(_.ModerateForum)) a
      else {
        val fuTopic =
          if (topicSlug.nonEmpty)
            topicRepo.forUser(forUser).byTree(categSlug, topicSlug.get)
          else
            topicRepo.forUser(forUser).byId(topicId.get)
        fuTopic flatMap { topic =>
          if (topic.nonEmpty && topic.get.canOwnerMod(forUser)) a
          else fuccess(Forbidden("You cannot post to this category"))
        }
      }
    }
}
