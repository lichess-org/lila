package lila
package forum

import user.UserHelper

trait ForumHelper { self: UserHelper ⇒

  def showAuthorName(post: Post) =
    post.userId.fold(userIdToUsername, post.showAuthor)

}
