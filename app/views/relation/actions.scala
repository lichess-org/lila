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
              titleOrText(trans.challenge.challengeToPlay.txt()),
              href := s"${routes.Lobby.home}?user=$userId#friend",
              cls := "btn-rack__btn",
              dataIcon := ""
            ),
            a(
              titleOrText(trans.composeMessage.txt()),
              href := routes.Msg.convo(userId),
              cls := "btn-rack__btn",
              dataIcon := ""
            )
          ),
          relation match {
            case None =>
              frag(
                followable && !blocked option a(
                  cls := "btn-rack__btn relation-button",
                  href := routes.Relation.follow(userId),
                  titleOrText(trans.follow.txt()),
                  dataIcon := ""
                ),
                a(
                  cls := "btn-rack__btn relation-button",
                  href := routes.Relation.block(userId),
                  titleOrText(trans.block.txt()),
                  dataIcon := ""
                )
              )
            case Some(true) =>
              a(
                dataIcon := "",
                cls := "btn-rack__btn relation-button text hover-text",
                href := routes.Relation.unfollow(userId),
                titleOrText(trans.following.txt()),
                dataHoverText := trans.unfollow.txt()
              )
            case Some(false) =>
              a(
                dataIcon := "",
                cls := "btn-rack__btn relation-button text hover-text",
                href := routes.Relation.unblock(userId),
                titleOrText(trans.blocked.txt()),
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
