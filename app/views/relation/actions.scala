package views.html.relation

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object actions:

  private val dataHoverText = data("hover-text")

  def apply(
      user: lila.common.LightUser,
      relation: Option[lila.relation.Relation],
      followable: Boolean,
      blocked: Boolean,
      signup: Boolean = false
  )(implicit ctx: Context) =
    div(cls := "relation-actions btn-rack")(
      (!ctx.is(user) && !blocked) option a(
        titleOrText(trans.challenge.challengeToPlay.txt()),
        href     := s"${routes.Lobby.home}?user=${user.name}#friend",
        cls      := "btn-rack__btn",
        dataIcon := ""
      ),
      ctx.userId map { myId =>
        !user.is(myId) ?? frag(
          (!blocked && !user.isBot) option a(
            titleOrText(trans.composeMessage.txt()),
            href     := routes.Msg.convo(user.name),
            cls      := "btn-rack__btn",
            dataIcon := ""
          ),
          relation match {
            case None =>
              frag(
                followable && !blocked option a(
                  cls  := "btn-rack__btn relation-button",
                  href := routes.Relation.follow(user.name),
                  titleOrText(trans.follow.txt()),
                  dataIcon := ""
                ),
                a(
                  cls  := "btn-rack__btn relation-button",
                  href := routes.Relation.block(user.name),
                  titleOrText(trans.block.txt()),
                  dataIcon := ""
                )
              )
            case Some(true) =>
              a(
                dataIcon := "",
                cls      := "btn-rack__btn relation-button text hover-text",
                href     := routes.Relation.unfollow(user.name),
                titleOrText(trans.following.txt()),
                dataHoverText := trans.unfollow.txt()
              )
            case Some(false) =>
              a(
                dataIcon := "",
                cls      := "btn-rack__btn relation-button text hover-text",
                href     := routes.Relation.unblock(user.name),
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
