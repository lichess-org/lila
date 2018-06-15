package lila.app
package templating

import play.twirl.api.Html

import lila.api.Context
import lila.forum.Post

trait ForumHelper { self: UserHelper with StringHelper =>

  private object Granter extends lila.forum.Granter {

    protected def userBelongsToTeam(teamId: String, userId: String): Fu[Boolean] =
      Env.team.api.belongsTo(teamId, userId)

    protected def userOwnsTeam(teamId: String, userId: String): Fu[Boolean] =
      Env.team.api.owns(teamId, userId)
  }

  def isGrantedRead(categSlug: String)(implicit ctx: Context) =
    Granter isGrantedRead categSlug

  def isGrantedWrite(categSlug: String)(implicit ctx: Context) =
    Granter isGrantedWrite categSlug

  def authorName(post: Post) = post.userId match {
    case Some(userId) => userIdSpanMini(userId, withOnline = true)
    case None => Html(lila.user.User.anonymous)
  }

  def authorLink(
    post: Post,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    modIcon: Boolean = false
  ) =
    if (post.erased) Html(s"""<span class="author">${lila.common.String.erasedHtml}</span>""")
    else post.userId.fold(Html(lila.user.User.anonymous)) { userId =>
      userIdLink(userId.some, cssClass = cssClass, withOnline = withOnline, modIcon = modIcon)
    }
}
