package lila
package forum

import user.UserHelper

trait ForumHelper { self: UserHelper ⇒

  def authorName(post: Post) =
    post.userId.fold(userIdToUsername, post.showAuthor)

  def authorLink(post: Post, cssClass: Option[String] = None) =
    post.userId.fold(
      userId ⇒ userIdLink(userId.some, cssClass),
      authorName(post)
    )
}
