package views.html.relation

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object actions {

  def apply(
    userId: lila.user.User.ID,
    relation: Option[lila.relation.Relation],
    followable: Boolean,
    blocked: Boolean,
    signup: Boolean = false
  )(implicit ctx: Context) =
    div(cls := "relation-actions")(
      ctx.userId map { myId =>
        (myId != userId) ?? frag(
          !blocked option frag(
            a(
              title := trans.challengeToPlay.txt(),
              href := s"${routes.Lobby.home()}?user=$userId#friend",
              cls := "button button-empty",
              dataIcon := "U"
            ),
            a(
              title := trans.composeMessage.txt(),
              href := s"${routes.Message.form()}?user=$userId", cls := "button button-empty",
              dataIcon := "c"
            )
          ),
          relation match {
            case None => frag(
              followable && !blocked option a(
                cls := "button button-empty relation-button",
                href := routes.Relation.follow(userId),
                title := trans.follow.txt(),
                dataIcon := "h"
              ),
              a(
                cls := "button button-empty relation-button",
                href := routes.Relation.block(userId),
                title := trans.block.txt(),
                dataIcon := "k"
              )
            )
            case Some(true) =>
              a(cls := "button button-empty relation-button hover-text", href := routes.Relation.unfollow(userId))(
                iconTag("h")(cls := "base text")(trans.following()),
                iconTag("h")(cls := "hover text")(trans.unfollow())
              )
            case Some(false) =>
              a(cls := "button button-empty relation-button hover-text", href := routes.Relation.unblock(userId))(
                iconTag("k")(cls := "base text")(trans.blocked()),
                iconTag("k")(cls := "hover text")(trans.unblock())
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
