package views.html.challenge

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.challenge.Challenge
import lila.challenge.Challenge.Status

import controllers.routes

object theirs {

  def apply(
      c: Challenge,
      json: play.api.libs.json.JsObject,
      user: Option[lila.user.User],
      color: Option[chess.Color]
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = challengeTitle(c),
      openGraph = challengeOpenGraph(c).some,
      moreJs = bits.js(c, json, owner = false, color),
      moreCss = cssTag("challenge.page")
    ) {
      main(cls := "page-small challenge-page challenge-theirs box box-pad")(
        c.status match {
          case Status.Created | Status.Offline =>
            frag(
              h1(
                if (c.isOpen) "Open Challenge"
                else
                  user.fold[Frag]("Anonymous")(u =>
                    frag(
                      userLink(u),
                      " (",
                      u.perfs(c.perfType).glicko.display,
                      ")"
                    )
                  )
              ),
              bits.details(c),
              c.notableInitialFen.map { fen =>
                div(cls := "board-preview", views.html.board.bits.mini(fen, !c.finalColor)(div))
              },
              if (color.map(Challenge.ColorChoice.apply).has(c.colorChoice))
                badTag(
                  // very rare message, don't translate
                  s"You have the wrong color link for this open challenge. The ${color.??(_.name)} player has already joined."
                )
              else if (!c.mode.rated || ctx.isAuth) {
                frag(
                  (c.mode.rated && c.unlimited) option
                    badTag(trans.bewareTheGameIsRatedButHasNoClock()),
                  postForm(cls := "accept", action := routes.Challenge.accept(c.id, color.map(_.name)))(
                    submitButton(cls := "text button button-fat", dataIcon := "G")(trans.joinTheGame())
                  )
                )
              } else
                frag(
                  hr,
                  badTag(
                    p("This game is rated"),
                    p(
                      "You must ",
                      a(
                        cls := "button",
                        href := s"${routes.Auth.login()}?referrer=${routes.Round.watcher(c.id, "white")}"
                      )(trans.signIn()),
                      " to join it."
                    )
                  )
                )
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
              a(
                id := "challenge-redirect",
                href := routes.Round.watcher(c.id, "white"),
                cls := "button button-fat"
              )(
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
