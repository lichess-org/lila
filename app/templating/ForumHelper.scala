package lila.app
package templating

import lila.forum.Post
import lila.team.Env.{ current ⇒ teamEnv }
import lila.user.Context

import play.api.templates.Html

trait ForumHelper { self: UserHelper with StringHelper ⇒

  private object Granter extends lila.forum.Granter {

    protected def userBelongsToTeam(teamId: String, userId: String): Fu[Boolean] =
      teamEnv.api.belongsTo(teamId, userId)
  }

  def isGrantedRead(categSlug: String)(implicit ctx: Context) = 
    Granter.isGrantedRead(categSlug)

  def isGrantedWrite(categSlug: String)(implicit ctx: Context) =
    Granter.isGrantedWrite(categSlug).await

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
