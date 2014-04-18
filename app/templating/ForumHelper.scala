package lila.app
package templating

import play.api.templates.Html

import lila.forum.Post
import lila.api.Context

trait ForumHelper { self: UserHelper with StringHelper =>

  private object Granter extends lila.forum.Granter {

    protected def userBelongsToTeam(teamId: String, userId: String): Boolean =
      Env.team.api.belongsTo(teamId, userId)

    protected def userOwnsTeam(teamId: String, userId: String): Fu[Boolean] =
      Env.team.api.owns(teamId, userId)
  }

  def isGrantedRead(categSlug: String)(implicit ctx: Context) =
    Granter isGrantedRead categSlug

  def isGrantedWrite(categSlug: String)(implicit ctx: Context) =
    Granter isGrantedWrite categSlug

  def isGrantedMod(categSlug: String)(implicit ctx: Context) =
    Granter.isGrantedMod(categSlug).await

  def authorName(post: Post) =
    post.userId.flatMap(lightUser).fold(escape(post.showAuthor))(_.titleName)

  def authorLink(
    post: Post,
    cssClass: Option[String] = None,
    withOnline: Boolean = true) = post.userId.fold(
    Html("""<span class="%s">%s</span>""".format(~cssClass, authorName(post)))
  ) { userId =>
      userIdLink(userId.some, cssClass = cssClass, withOnline = withOnline)
    }
}
