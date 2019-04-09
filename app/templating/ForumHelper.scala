package lidraughts.app
package templating

import lidraughts.api.Context
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.forum.Post

trait ForumHelper { self: UserHelper with StringHelper =>

  private object Granter extends lidraughts.forum.Granter {

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
    case None => frag(lidraughts.user.User.anonymous)
  }

  def authorLink(
    post: Post,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    modIcon: Boolean = false
  ): Frag =
    if (post.erased) span(cls := "author")(lidraughts.common.String.erasedHtml)
    else post.userId.fold(frag(lidraughts.user.User.anonymous)) { userId =>
      userIdLink(userId.some, cssClass = cssClass, withOnline = withOnline, modIcon = modIcon)
    }
}
