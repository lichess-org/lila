package views.html.challenge

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.challenge.Challenge.Status

import controllers.routes

object mine {

  def apply(c: lila.challenge.Challenge, json: play.api.libs.json.JsObject, error: Option[String])(implicit ctx: Context) = {

    val cancelForm =
      form(method := "post", action := routes.Challenge.cancel(c.id), cls := "cancel xhr")(
        button(tpe := "submit", cls := "button button-red text", dataIcon := "L")(trans.cancel())
      )

    views.html.base.layout(
      title = challengeTitle(c),
      openGraph = challengeOpenGraph(c).some,
      moreJs = bits.js(c, json, true),
      moreCss = cssTag("challenge.page")
    ) {
        val challengeLink = s"$netBaseUrl${routes.Round.watcher(c.id, "white")}"
        main(cls := "page-small challenge-page box box-pad")(
          c.status match {
            case Status.Created | Status.Offline => div(id := "ping-challenge")(
              h1(trans.challengeToPlay()),
              bits.details(c),
              c.destUserId.map { destId =>
                div(cls := "waiting")(
                  userIdLink(destId.some, cssClass = "target".some),
                  spinner,
                  p(trans.waitingForOpponent())
                )
              } getOrElse div(cls := "invite")(
                div(
                  h2(cls := "ninja-title", trans.toInviteSomeoneToPlayGiveThisUrl(), ": "), br,
                  p(cls := "challenge-id-form")(
                    input(
                      id := "challenge-id",
                      cls := "copyable autoselect",
                      spellcheck := "false",
                      readonly,
                      value := challengeLink,
                      size := challengeLink.size
                    ),
                    button(title := "Copy URL", cls := "copy button", dataRel := "challenge-id", dataIcon := "\"")
                  ),
                  p(trans.theFirstPersonToComeOnThisUrlWillPlayWithYou())
                ),
                ctx.isAuth option div(
                  h2(cls := "ninja-title", "Or invite a lichess user:"), br,
                  form(cls := "user-invite", action := routes.Challenge.toFriend(c.id), method := "POST")(
                    input(name := "username", cls := "friend-autocomplete", placeholder := trans.search.txt()),
                    error.map { badTag(_) }
                  )
                )
              ),
              c.notableInitialFen.map { fen =>
                frag(
                  br,
                  div(cls := "board-preview", views.html.game.bits.miniBoard(fen, color = c.finalColor))
                )
              },
              cancelForm
            )
            case Status.Declined => div(cls := "follow-up")(
              h1("Challenge declined"),
              bits.details(c),
              a(cls := "button button-fat", href := routes.Lobby.home())(trans.newOpponent())
            )
            case Status.Accepted => div(cls := "follow-up")(
              h1("Challenge accepted!"),
              bits.details(c),
              a(id := "challenge-redirect", href := routes.Round.watcher(c.id, "white"), cls := "button-fat")(
                trans.joinTheGame()
              )
            )
            case Status.Canceled => div(cls := "follow-up")(
              h1("Challenge canceled."),
              bits.details(c),
              a(cls := "button button-fat", href := routes.Lobby.home())(trans.newOpponent())
            )
          }
        )
      }
  }
}
