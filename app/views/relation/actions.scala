package views.html.relation

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object actions {

  private val dataHoverText = data("hover-text")

  def apply(
    userId: lila.user.User.ID,
    relation: Option[lila.relation.Relation],
    followable: Boolean,
    blocked: Boolean,
    signup: Boolean = false
  )(implicit ctx: Context) =
    div(cls := "relation-actions btn-rack")(
      ctx.userId map { myId =>
        (myId != userId) ?? frag(
          !blocked option frag(
            a(
              ariaTitle(trans.challengeToPlay.txt()),
              href := s"${routes.Lobby.home()}?user=$userId#friend",
              cls := "btn-rack__btn",
              dataIcon := "U"
            ),
            a(
              ariaTitle(trans.composeMessage.txt()),
              href := s"${routes.Message.form()}?user=$userId",
              cls := "btn-rack__btn",
              dataIcon := "c"
            )
          ),
          relation match {
            case None => frag(
              followable && !blocked option a(
                cls := "btn-rack__btn relation-button",
                href := routes.Relation.follow(userId),
                ariaTitle(trans.follow.txt()),
                dataIcon := "h"
              ),
              a(
                cls := "btn-rack__btn relation-button",
                href := routes.Relation.block(userId),
                ariaTitle(trans.block.txt()),
                dataIcon := "k"
              )
            )
            case Some(true) => a(
              dataIcon := "h",
              cls := "btn-rack__btn relation-button text hover-text",
              href := routes.Relation.unfollow(userId),
              ariaTitle(trans.following.txt()),
              dataHoverText := trans.unfollow.txt()
            )
            case Some(false) => a(
              dataIcon := "k",
              cls := "btn-rack__btn relation-button text hover-text",
              href := routes.Relation.unblock(userId),
              ariaTitle(trans.blocked.txt()),
              dataHoverText := trans.unblock.txt()
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
