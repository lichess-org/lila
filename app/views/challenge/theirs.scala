package views.html.challenge

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.challenge.Challenge
import lila.challenge.Challenge.Status

import controllers.routes

object theirs:

  def apply(
      c: Challenge,
      json: play.api.libs.json.JsObject,
      user: Option[lila.user.User.WithPerf],
      color: Option[chess.Color]
  )(using ctx: PageContext) =
    views.html.base.layout(
      title = challengeTitle(c),
      openGraph = challengeOpenGraph(c).some,
      moreJs = bits.js(c, json, owner = false, color),
      moreCss = cssTag("challenge.page")
    ):
      main(cls := "page-small challenge-page challenge-theirs box box-pad"):
        c.status match
          case Status.Created | Status.Offline =>
            frag(
              boxTop:
                h1:
                  if c.isOpen then c.name | "Open challenge"
                  else
                    user.fold[Frag]("Anonymous"): u =>
                      frag(
                        userLink(u.user),
                        ctx.pref.showRatings option frag(" (", u.perf.glicko.display, ")")
                      )
              ,
              bits.details(c, color),
              c.notableInitialFen.map: fen =>
                div(cls := "board-preview", views.html.board.bits.mini(fen.board, !c.finalColor)(div)),
              if c.open.exists(!_.canJoin) then
                div(
                  "Waiting for ",
                  fragList((~c.open.flatMap(_.userIdList)).map(uid => userIdLink(uid.some)), " and "),
                  " to start the game."
                )
              else if color.map(Challenge.ColorChoice.apply).has(c.colorChoice) then
                badTag(
                  // very rare message, don't translate
                  s"You have the wrong color link for this open challenge. The ${color.so(_.name)} player has already joined."
                )
              else if !c.mode.rated || ctx.isAuth then
                frag(
                  (c.mode.rated && c.unlimited) option
                    badTag(trans.bewareTheGameIsRatedButHasNoClock()),
                  postForm(cls := "accept", action := routes.Challenge.accept(c.id, color.map(_.name)))(
                    submitButton(cls := "text button button-fat", dataIcon := licon.PlayTriangle)(
                      trans.joinTheGame()
                    )
                  )
                )
              else
                frag(
                  hr,
                  badTag(
                    p(trans.thisGameIsRated()),
                    a(
                      cls  := "button",
                      href := s"${routes.Auth.login}?referrer=${routes.Round.watcher(c.id, "white")}"
                    )(trans.signIn())
                  )
                )
            )
          case Status.Declined =>
            div(cls := "follow-up")(
              h1(cls := "box__top")(trans.challenge.challengeDeclined()),
              bits.details(c, color),
              a(cls := "button button-fat", href := routes.Lobby.home)(trans.newOpponent())
            )
          case Status.Accepted =>
            div(cls := "follow-up")(
              h1(cls := "box__top")(trans.challenge.challengeAccepted()),
              bits.details(c, color),
              a(
                id   := "challenge-redirect",
                href := routes.Round.watcher(c.id, "white"),
                cls  := "button button-fat"
              )(
                trans.joinTheGame()
              )
            )
          case Status.Canceled =>
            div(cls := "follow-up")(
              h1(cls := "box__top")(trans.challenge.challengeCanceled()),
              bits.details(c, color),
              a(cls := "button button-fat", href := routes.Lobby.home)(trans.newOpponent())
            )
