package views.html.relation

import scalatags.Text.all._

import lila.api.Context
import lila.app.templating.Environment._
import lila.i18n.{ I18nKeys => trans }

import controllers.routes

object actions {

  def apply(
    userId: lila.user.User.ID,
    relation: Option[lila.relation.Relation],
    followable: Boolean,
    blocked: Boolean,
    signup: Boolean = false
  )(implicit ctx: Context) =
    div(cls := "relation_actions")(
      ctx.userId map { myId =>
        (myId != userId) ?? frag(
          !blocked option frag(
            a(dataHint := trans.challengeToPlay.txt(), href := s"${routes.Lobby.home()}?user=$userId#friend", cls := "icon button hint--bottom")(
              i(dataIcon := "U")
            ),
            a(dataHint := trans.composeMessage.txt(), href := s"${routes.Message.form()}?user=$userId", cls := "icon button hint--bottom")(
              i(dataIcon := "c")
            )
          ),
          relation match {
            case None if followable && !blocked => frag(
              a(
                cls := "icon button relation hint--bottom",
                href := routes.Relation.follow(userId),
                dataHint := trans.follow.txt()
              )(i(dataIcon := "h")),
              a(
                cls := "icon button relation hint--bottom",
                href := routes.Relation.block(userId),
                dataHint := trans.block.txt()
              )(i(dataIcon := "k"))
            )
            case None => emptyFrag
            case Some(true) =>
              a(cls := "button relation hover_text", href := routes.Relation.unfollow(userId))(
                i(dataIcon := "h", cls := "base text")(trans.following()),
                i(dataIcon := "h", cls := "hover text")(trans.unfollow())
              )
            case Some(false) =>
              a(cls := "button relation hover_text", href := routes.Relation.unblock(userId))(
                i(dataIcon := "k", cls := "base text")(trans.blocked()),
                i(dataIcon := "k", cls := "hover text")(trans.unblock())
              )
          }
        )
      } getOrElse {
        signup option frag(
          trans.youNeedAnAccountToDoThat(),
          a(href := routes.Auth.login, cls := "signup")(trans.signUp())
        )
      }
    )
}
