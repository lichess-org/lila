package views.html
package round

import play.twirl.api.Html

import draughts.variant.Variant
import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.game.{ Game, Pov, Player }

import controllers.routes

object bits {

  def layout(
    variant: Variant,
    title: String,
    moreJs: Html = emptyHtml,
    openGraph: Option[lidraughts.app.ui.OpenGraph] = None,
    moreCss: Html = emptyFrag,
    draughtsground: Boolean = true,
    playing: Boolean = false,
    robots: Boolean = false
  )(body: Html)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      openGraph = openGraph,
      moreJs = moreJs,
      moreCss = frag(
        responsiveCssTag("round"),
        moreCss
      ),
      responsive = true,
      draughtsground = draughtsground,
      playing = playing,
      robots = robots,
      asyncJs = true,
      zoomable = true
    )(body)

  def crosstable(cross: Option[lidraughts.game.Crosstable.WithMatchup], game: Game)(implicit ctx: Context) =
    cross map { c =>
      views.html.game.crosstable(ctx.userId.fold(c)(c.fromPov), game.id.some)
    }

  def underchat(game: Game)(implicit ctx: Context) = frag(
    views.html.game.bits.watchers,
    isGranted(_.ViewBlurs) option div(cls := "round__mod")(
      game.players.filter(p => game.playerBlurPercent(p.color) > 30) map { p =>
        div(
          playerLink(p, cssClass = s"is color-icon ${p.color.name}".some, withOnline = false, mod = true),
          s"${p.blurs.nb}/${game.playerMoves(p.color)} blurs",
          strong(game.playerBlurPercent(p.color), "%")
        )
      },
      game.players flatMap { p => p.holdAlert.map(p ->) } map {
        case (p, h) => div(
          playerLink(p, cssClass = s"is color-icon ${p.color.name}".some, mod = true, withOnline = false),
          "hold alert",
          br,
          s"(ply: ${h.ply}, mean: ${h.mean} ms, SD: ${h.sd})"
        )
      }
    )
  )

  private val wrap = tag("wrap")

  def simulStanding(simul: lidraughts.simul.Simul)(implicit ctx: Context) =
    span(cls := "simul")(
      a(href := routes.Simul.show(simul.id))("SIMUL"),
      span(cls := "win")(wrap(id := s"simul_w_${simul.id}")(simul.wins, " W")), " / ",
      span(cls := "draw")(wrap(id := s"simul_d_${simul.id}")(simul.draws, " D")), " / ",
      span(cls := "loss")(wrap(id := s"simul_l_${simul.id}")(simul.losses, " L")), " / ",
      trans.ongoing(Html(s"""<wrap id="simul_g_${simul.id}">${simul.ongoing}</wrap>""")),
      simul.targetPct.map { pct =>
        frag(
          br,
          span(cls := "simul-targets")(
            trans.winningPercentage.frag(),
            span(cls := s"pct simul_pct_${simul.id}")(simul.winningPercentageStr),
            trans.withTarget.frag(),
            span(cls := "pct")(s"$pct%")
          ),
          span(cls := "simul-targets")(
            trans.toReachTarget.frag(),
            wrap(id := s"simul_req_${simul.id}")(
              if (simul.targetReached) span(cls := "win")(trans.succeeded())
              else if (simul.targetFailed) span(cls := "loss")(trans.failed())
              else frag(
                simul.requiredWins.map { w => span(cls := "win req2")(trans.nbVictories.pluralSame(w)) },
                simul.requiredDraws.map { d => span(cls := "draw req2")(trans.nbDraws.pluralSame(d)) }
              )
            )
          )
        )
      }
    )

  def others(current: Pov, playing: List[Pov], simul: Option[lidraughts.simul.Simul])(implicit ctx: Context) = frag(
    h3(
      simul.map {
        simulStanding(_)
      } getOrElse trans.currentGames.frag(),
      "round-toggle-autoswitch" |> { id =>
        span(cls := "move-on switcher", st.title := trans.automaticallyProceedToNextGameAfterMoving.txt())(
          label(`for` := id)(trans.autoSwitch.frag()),
          span(cls := "switch")(
            input(st.id := id, cls := "cmn-toggle", tpe := "checkbox"),
            label(`for` := id)
          )
        )
      },
      simul ?? { _.pairings.length >= 10 } option
        "simul-toggle-sequential" |> { id =>
          span(cls := "move_seq switcher", st.title := trans.switchGamesInSameOrder.txt())(
            label(`for` := id)(trans.sequentialSwitch.frag()),
            span(cls := "switch")(
              input(st.id := id, cls := "cmn-toggle", tpe := "checkbox"),
              label(`for` := id)
            )
          )
        }
    ),
    div(cls := "now-playing")(
      playing.partition(_.game.isWithinTimeOut) |> {
        case (inTimeOut, noTimeOut) => {
          val main = noTimeOut.partition(_.isMyTurn) |> {
            case (myTurn, otherTurn) =>
              val povs = (myTurn ++ otherTurn.take(6 - myTurn.size)) take 9 map { pov =>
                a(href := routes.Round.player(pov.fullId), cls := pov.isMyTurn.option("my_turn"))(
                  gameFen(pov, withLink = false, withTitle = false, withLive = simul.isDefined),
                  span(cls := "meta")(
                    playerText(pov.opponent, withRating = false),
                    span(cls := "indicator")(
                      if (pov.isMyTurn) pov.remainingSeconds.fold(trans.yourTurn())(secondsFromNow(_, true))
                      else nbsp
                    )
                  )
                )
              }
              if (simul.isDefined && povs.nonEmpty) {
                val toMove = playing.count(_.isMyTurn) + (if (current.isMyTurn) 1 else 0)
                h3(cls := "simul_tomove")(
                  trans.yourTurnInX(Html(s"""<span>${trans.nbGames.pluralSameTxt(toMove)}</span>""")),
                  div(cls := "tomove_count")(toMove)
                ) :: povs
              } else povs
          }
          main ::: (simul.isDefined && inTimeOut.nonEmpty).?? {
            inTimeOut take 9 map { pov =>
              a(href := routes.Round.player(pov.fullId), cls := "game_timeout " + ~pov.isMyTurn.option(" my_turn"))(
                gameFen(pov, withLink = false, withTitle = false),
                span(cls := "meta")(
                  playerText(pov.opponent, withRating = false),
                  span(cls := "indicator")(
                    s"Timeout ${secondsFromNow(pov.game.timeOutRemaining, true)}"
                  )
                )
              )
            }
          }
        }
      }
    )
  )

  private[round] def side(
    pov: Pov,
    data: play.api.libs.json.JsObject,
    tour: Option[lidraughts.tournament.TourMiniView],
    simul: Option[lidraughts.simul.Simul],
    userTv: Option[lidraughts.user.User] = None,
    bookmarked: Boolean
  )(implicit ctx: Context) = views.html.game.side(
    pov,
    (data \ "game" \ "initialFen").asOpt[String].map(draughts.format.FEN),
    tour.map(_.tour),
    simul = simul,
    userTv = userTv,
    bookmarked = bookmarked
  )

  def roundAppPreload(pov: Pov, controls: Boolean)(implicit ctx: Context) =
    div(cls := "round__app")(
      div(cls := "round__app__board main-board")(board.bits.domPreload(pov.some)),
      div(cls := "round__app__table"),
      div(cls := "ruser ruser-top user_link")(i(cls := "line"), a(cls := "text")(playerText(pov.opponent))),
      div(cls := "ruser ruser-bottom user_link")(i(cls := "line"), a(cls := "text")(playerText(pov.player))),
      div(cls := "rclock rclock-top preload")(div(cls := "time")(nbsp)),
      div(cls := "rclock rclock-bottom preload")(div(cls := "time")(nbsp)),
      div(cls := "rmoves")(div(cls := "moves")),
      controls option div(cls := "rcontrols")(i(cls := "ddloader"))
    )
}
