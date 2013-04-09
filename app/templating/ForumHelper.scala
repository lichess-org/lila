package lila.app
package templating

import lila.forum.{ Granter, Post }
import lila.team.Env.{ current ⇒ teamEnv }

import play.api.templates.Html

trait ForumHelper extends Granter { self: UserHelper with StringHelper ⇒

  protected def userBelongsToTeam(teamId: String, userId: String): Fu[Boolean] =
    teamEnv.api.belongsTo(teamId, userId)

  def authorName(post: Post) =
    post.userId.fold(escape(post.showAuthor))(userIdToUsername)

  def authorLink(
    post: Post,
    cssClass: Option[String] = None,
    withOnline: Boolean = true) = post.userId.fold(
    Html("""<span class="%s">%s</span>""".format(~cssClass, authorName(post)))
  ) { userId ⇒
      userIdLink(userId.some, cssClass = cssClass, withOnline = withOnline)
    }
}
