package views.html.relation

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
  )(using ctx: Context) =
    div(cls := "relation-actions btn-rack")(
      (!ctx.is(user) && !blocked) option a(
        titleOrText(trans.challenge.challengeToPlay.txt()),
        href     := s"${routes.Lobby.home}?user=${user.name}#friend",
        cls      := "btn-rack__btn",
        dataIcon := licon.Swords
      ),
      ctx.userId
        .map: myId =>
          !user.is(myId) so frag(
            (!blocked && !user.isBot) option a(
              titleOrText(trans.composeMessage.txt()),
              href     := routes.Msg.convo(user.name),
              cls      := "btn-rack__btn",
              dataIcon := licon.BubbleSpeech
            ),
            (!blocked && !user.isPatron) option a(
              titleOrText(trans.patron.giftPatronWingsShort.txt()),
              href     := s"${routes.Plan.list}?dest=gift&giftUsername=${user.name}",
              cls      := "btn-rack__btn",
              dataIcon := licon.Wings
            ),
            relation match
              case None =>
                frag(
                  followable && !blocked option a(
                    cls  := "btn-rack__btn relation-button",
                    href := routes.Relation.follow(user.name),
                    titleOrText(trans.follow.txt()),
                    dataIcon := licon.ThumbsUp
                  ),
                  a(
                    cls  := "btn-rack__btn relation-button",
                    href := routes.Relation.block(user.name),
                    titleOrText(trans.block.txt()),
                    dataIcon := licon.NotAllowed
                  )
                )
              case Some(true) =>
                a(
                  dataIcon := licon.ThumbsUp,
                  cls      := "btn-rack__btn relation-button text hover-text",
                  href     := routes.Relation.unfollow(user.name),
                  titleOrText(trans.following.txt()),
                  dataHoverText := trans.unfollow.txt()
                )
              case Some(false) =>
                a(
                  dataIcon := licon.NotAllowed,
                  cls      := "btn-rack__btn relation-button text hover-text",
                  href     := routes.Relation.unblock(user.name),
                  titleOrText(trans.blocked.txt()),
                  dataHoverText := trans.unblock.txt()
                )
          )
        .getOrElse:
          signup option frag(
            trans.youNeedAnAccountToDoThat(),
            a(href := routes.Auth.login, cls := "signup")(trans.signUp())
          )
    )
