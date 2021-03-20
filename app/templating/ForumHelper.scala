package lila.app
package templating

import play.api.i18n.Lang

import lila.api.Context
import lila.app.ui.ScalatagsTemplate._
import lila.forum.Post

trait ForumHelper { self: UserHelper with StringHelper with HasEnv =>

  private object Granter extends lila.forum.Granter {

    protected def userBelongsToTeam(teamId: String, userId: String): Fu[Boolean] =
      env.team.api.belongsTo(teamId, userId)

    protected def userOwnsTeam(teamId: String, userId: String): Fu[Boolean] =
      env.team.api.leads(teamId, userId)
  }

  def isGrantedWrite(categSlug: String)(implicit ctx: Context) =
    Granter isGrantedWrite categSlug

  def authorName(post: Post)(implicit lang: Lang) =
    post.userId match {
      case Some(userId) => userIdSpanMini(userId, withOnline = true)
      case None         => frag(lila.user.User.anonymous)
    }

  def authorLink(
      post: Post,
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      modIcon: Boolean = false
  )(implicit lang: Lang): Frag =
    if (post.erased) span(cls := "author")("<erased>")
    else userIdLink(post.userId, cssClass = cssClass, withOnline = withOnline, modIcon = modIcon)
}
