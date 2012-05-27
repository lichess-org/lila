package lila
package forum

import user.UserHelper

import play.api.templates.Html

trait ForumHelper { self: UserHelper ⇒

  def authorName(post: Post) =
    post.userIdString.fold(userIdToUsername, post.showAuthor)

  def authorLink(post: Post, cssClass: Option[String] = None) =
    post.userIdString.fold(
      userId ⇒ userIdLink(userId.some, cssClass),
      Html("""<span class="%s">%s</span>"""
        .format(cssClass | "", authorName(post)))
    )
}
