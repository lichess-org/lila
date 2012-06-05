package lila
package forum

import user.UserHelper
import templating.StringHelper

import play.api.templates.Html

trait ForumHelper { self: UserHelper with StringHelper ⇒

  def authorName(post: Post) =
    post.userId.fold(userIdToUsername, escape(post.showAuthor))

  def authorLink(
    post: Post, 
    cssClass: Option[String] = None,
    withOnline: Boolean = true) =
    post.userId.fold(
      userId ⇒ userIdLink(userId.some, cssClass = cssClass, withOnline = withOnline),
      Html("""<span class="%s">%s</span>"""
        .format(cssClass | "", authorName(post)))
    )
}
