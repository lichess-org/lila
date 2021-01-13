package views.html.challenge

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.challenge.Challenge.Status

import controllers.routes

object mine {

  def apply(c: lila.challenge.Challenge, json: play.api.libs.json.JsObject, error: Option[String])(implicit
      ctx: Context
  ) = {

    val cancelForm =
      postForm(action := routes.Challenge.cancel(c.id), cls := "cancel xhr")(
        submitButton(cls := "button button-red text", dataIcon := "L")(trans.cancel())
      )

    views.html.base.layout(
      title = challengeTitle(c),
      openGraph = challengeOpenGraph(c).some,
      moreJs = bits.js(c, json, owner = true),
      moreCss = cssTag("challenge.page")
    ) {
      val challengeLink = s"$netBaseUrl${routes.Round.watcher(c.id, "white")}"
      main(cls := "page-small challenge-page box box-pad")(
        c.status match {
          case Status.Created | Status.Offline =>
            div(id := "ping-challenge")(
              h1(if (c.isOpen) "Open challenge" else trans.challengeToPlay.txt()),
              bits.details(c),
              c.destUserId.map { destId =>
                div(cls := "waiting")(
                  userIdLink(destId.some, cssClass = "target".some),
                  spinner,
                  p(trans.waitingForOpponent())
                )
              } getOrElse {
                if (c.isOpen)
                  div(cls := "waiting")(
                    spinner,
                    p(trans.waitingForOpponent())
                  )
                else
                  div(cls := "invite")(
                    div(
                      h2(cls := "ninja-title", trans.toInviteSomeoneToPlayGiveThisUrl(), ": "),
                      br,
                      p(cls := "challenge-id-form")(
                        input(
                          id := "challenge-id",
                          cls := "copyable autoselect",
                          spellcheck := "false",
                          readonly,
                          value := challengeLink,
                          size := challengeLink.length
                        ),
                        button(
                          title := "Copy URL",
                          cls := "copy button",
                          dataRel := "challenge-id",
                          dataIcon := "\""
                        )
                      ),
                      p(trans.theFirstPersonToComeOnThisUrlWillPlayWithYou())
                    ),
                    ctx.isAuth option div(
                      h2(cls := "ninja-title", "Or invite a Lichess user:"),
                      br,
                      postForm(
                        cls := "user-invite complete-parent",
                        action := routes.Challenge.toFriend(c.id)
                      )(
                        input(
                          name := "username",
                          cls := "friend-autocomplete",
                          placeholder := trans.search.search.txt()
                        ),
                        error.map { badTag(_) }
                      )
                    )
                  )
              },
              c.notableInitialFen.map { fen =>
                frag(
                  br,
                  div(cls := "board-preview", views.html.board.bits.mini(fen, c.finalColor)(div))
                )
              },
              !c.isOpen option cancelForm
            )
          case Status.Declined =>
            div(cls := "follow-up")(
              h1(trans.challengeDeclined()),
              bits.details(c),
              a(cls := "button button-fat", href := routes.Lobby.home())(trans.newOpponent())
            )
          case Status.Accepted =>
            div(cls := "follow-up")(
              h1(trans.challengeAccepted()),
              bits.details(c),
              a(id := "challenge-redirect", href := routes.Round.watcher(c.id, "white"), cls := "button-fat")(
                trans.joinTheGame()
              )
            )
          case Status.Canceled =>
            div(cls := "follow-up")(
              h1(trans.challengeCanceled()),
              bits.details(c),
              a(cls := "button button-fat", href := routes.Lobby.home())(trans.newOpponent())
            )
        }
      )
    }
  }
}
