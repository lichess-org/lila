package views.html.challenge

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.challenge.Challenge.Status

import controllers.routes

object mine {

  def apply(c: lila.challenge.Challenge, json: play.api.libs.json.JsObject, error: Option[String])(implicit ctx: Context) = {

    val cancelForm =
      form(method := "post", action := routes.Challenge.cancel(c.id), cls := "xhr")(
        button(tpe := "submit", cls := "button text", dataIcon := "L")(trans.cancel.frag())
      )

    views.html.base.layout(
      title = challengeTitle(c),
      openGraph = challengeOpenGraph(c).some,
      responsive = true,
      moreJs = bits.js(c, json, true),
      moreCss = responsiveCssTag("challenge.page")
    ) {
        main(cls := "page-small challenge-page box box-pad")(
          c.status match {
            case Status.Created | Status.Offline => div(id := "ping-challenge")(
              h1(trans.challengeToPlay.frag()),
              c.destUserId.map { destId =>
                frag(
                  userIdLink(destId.some, cssClass = "target".some),
                  spinner,
                  trans.waitingForOpponent.frag(),
                  br,
                  cancelForm
                )
              } getOrElse frag(
                trans.toInviteSomeoneToPlayGiveThisUrl(),
                ": ",
                input(
                  id := "challenge-id",
                  cls := "copyable autoselect",
                  spellcheck := "false",
                  readonly := "true",
                  value := s"$netBaseUrl${routes.Round.watcher(c.id, "white")}"
                ),
                button(title := "Copy URL", cls := "copy button", dataRel := "challenge-id", dataIcon := "\""),
                br,
                trans.theFirstPersonToComeOnThisUrlWillPlayWithYou.frag(),
                br,
                ctx.isAuth option frag(
                  br,
                  "Or invite a lichess user:",
                  br,
                  form(cls := "user-invite", action := routes.Challenge.toFriend(c.id), method := "POST")(
                    input(name := "username", cls := "friend-autocomplete", placeholder := trans.search.txt()),
                    error.map { badTag(_) }
                  ),
                  br
                ),
                cancelForm
              ),
              c.initialFen.map { fen =>
                frag(
                  br,
                  views.html.game.bits.miniBoard(fen, color = c.finalColor)
                )
              }
            )
            case Status.Declined => div(cls := "declined")(
              h2("Challenge declined"),
              a(cls := "button", href := routes.Lobby.home())(trans.newOpponent.frag())
            )
            case Status.Accepted => div(cls := "accepted")(
              h2("Challenge accepted!"),
              a(id := "challenge_redirect", href := routes.Round.watcher(c.id, "white"), cls := "button")(
                trans.joinTheGame.frag()
              )
            )
            case Status.Canceled => div(cls := "canceled")(
              h2("Challenge canceled."),
              a(cls := "button", href := routes.Lobby.home())(trans.newOpponent.frag())
            )
          },
          bits.explanation(c)
        )
      }
  }
}
