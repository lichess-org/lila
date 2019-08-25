package views.html
package round

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
    moreJs: Frag = emptyFrag,
    openGraph: Option[lidraughts.app.ui.OpenGraph] = None,
    moreCss: Frag = emptyFrag,
    draughtsground: Boolean = true,
    playing: Boolean = false,
    robots: Boolean = false
  )(body: Frag)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      openGraph = openGraph,
      moreJs = moreJs,
      moreCss = frag(
        cssTag("round"),
        ctx.blind option cssTag("round.nvui"),
        moreCss
      ),
      draughtsground = draughtsground,
      playing = playing,
      robots = robots,
      deferJs = true,
      zoomable = true,
      csp = defaultCsp.withPeer.some
    )(body)

  def crosstable(cross: Option[lidraughts.game.Crosstable.WithMatchup], game: Game)(implicit ctx: Context) =
    cross map { c =>
      views.html.game.crosstable(ctx.userId.fold(c)(c.fromPov), game.id.some)
    }

  def underchat(game: Game)(implicit ctx: Context) = frag(
    div(
      cls := "chat__members none",
      aria.live := "off",
      aria.relevant := "additions removals text"
    )(
        span(cls := "number")(nbsp),
        " ",
        trans.spectators.txt().replace(":", ""),
        " ",
        span(cls := "list")
      ),
    isGranted(_.ViewBlurs) option div(cls := "round__mod")(
      game.players.filter(p => game.playerBlurPercent(p.color) > 30) map { p =>
        div(
          playerLink(p, cssClass = s"is color-icon ${p.color.name}".some, withOnline = false, mod = true),
          s" ${p.blurs.nb}/${game.playerMoves(p.color)} blurs ",
          strong(game.playerBlurPercent(p.color), "%")
        )
      }
    // game.players flatMap { p => p.holdAlert.map(p ->) } map {
    //   case (p, h) => div(
    //     playerLink(p, cssClass = s"is color-icon ${p.color.name}".some, mod = true, withOnline = false),
    //     " hold alert",
    //     br,
    //     s"(ply: ${h.ply}, mean: ${h.mean} ms, SD: ${h.sd})"
    //   )
    // }
    )
  )

  private val wrap = tag("wrap")

  def simulStanding(simul: lidraughts.simul.Simul)(implicit ctx: Context) =
    span(cls := "simul simul-standings")(
      a(cls := "simul-link")(href := routes.Simul.show(simul.id))(trans.simulShortName()),
      span(cls := "win")(simul.wins, " W"), " / ",
      span(cls := "draw")(simul.draws, " D"), " / ",
      span(cls := "loss")(simul.losses, " L"), " / ",
      span(cls := "ongoing")(trans.ongoing(simul.ongoing))
    )

  def simulTarget(simul: lidraughts.simul.Simul)(implicit ctx: Context) =
    simul.targetPct.isDefined option
      span(cls := "simul")(
        trans.toReachTarget(),
        span(cls := s"simul-targets")(
          if (simul.targetReached) span(cls := "win")(trans.succeeded())
          else if (simul.targetFailed) span(cls := "loss")(trans.failed())
          else frag(
            simul.requiredWins.map { w => span(cls := "win")(trans.nbVictories.pluralSame(w)) },
            simul.requiredDraws.map { d => span(cls := "draw")(trans.nbDraws.pluralSame(d)) }
          )
        )
      )

  def cheatFlag(game: Game, flag: Boolean)(implicit ctx: Context) =
    div(cls := "cheatlist-container")(
      button(
        dataUrl := routes.Mod.cheatList(game.id),
        cls := s"button text cheat-list${if (flag) " active" else ""}",
        dataIcon := "n",
        title := "If you are sure this game is cheated, add it to the list, so we have data we can use later on."
      )("Add to cheated games")
    )

  def others(current: Pov, playing: List[Pov], simul: Option[lidraughts.simul.Simul])(implicit ctx: Context) = frag(
    h3(
      simul.map {
        simulStanding(_)
      } getOrElse trans.currentGames(),
      simul.fold(true)(_.isHost(ctx.me)) option "round-toggle-autoswitch" |> { id =>
        span(cls := "move-on switcher", st.title := trans.automaticallyProceedToNextGameAfterMoving.txt())(
          label(`for` := id)(trans.autoSwitch()),
          span(cls := "switch")(
            input(st.id := id, cls := "cmn-toggle", tpe := "checkbox"),
            label(`for` := id)
          )
        )
      }
    ),
    simul.map { sim =>
      h3(
        simulTarget(sim),
        (sim.pairings.length >= 8 && sim.isHost(ctx.me)) option
          "simul-toggle-sequential" |> { id =>
            span(cls := "move-seq switcher", st.title := trans.switchGamesInSameOrder.txt(), style := "visibility:collapse")(
              label(`for` := id)(trans.sequentialSwitch()),
              span(cls := "switch")(
                input(st.id := id, cls := "cmn-toggle", tpe := "checkbox"),
                label(`for` := id)
              )
            )
          }
      )
    },
    simul.??(_.isHost(ctx.me)) && playing.nonEmpty option {
      val toMove = playing.count(_.isMyTurn) + (if (current.isMyTurn) 1 else 0)
      h3(cls := "simul-tomove")(
        div(cls := "tomove-text")(trans.yourTurnInX(span(cls := "simul-tomove-count")(trans.nbGames.pluralSameTxt(toMove)))),
        div(cls := "tomove-count")(toMove)
      )
    },
    simul.fold(true)(_.isHost(ctx.me)) option playing.partition(_.game.isWithinTimeOut) |> {
      case (inTimeOut, noTimeOut) => frag(
        div(cls := "now-playing")(
          noTimeOut.partition(_.isMyTurn) |> {
            case (myTurn, otherTurn) =>
              (myTurn ++ otherTurn.take(6 - myTurn.size)) take 9 map { pov =>
                a(href := routes.Round.player(pov.fullId), cls := pov.isMyTurn.option("my_turn"), id := "others_" + pov.gameId)(
                  gameFen(pov, withLink = false, withTitle = false, withLive = simul.isDefined),
                  span(cls := "meta")(
                    playerText(pov.opponent),
                    span(cls := "indicator")(
                      if (pov.isMyTurn) pov.remainingSeconds.fold(frag(trans.yourTurn()))(secondsFromNow(_, true))
                      else nbsp
                    )
                  )
                )
              }
          }
        ),
        (simul.isDefined && inTimeOut.nonEmpty) option frag(
          h3(cls := "timeouts-title")(trans.gamesInTimeout()),
          div(cls := "now-playing simul-timeouts")(
            inTimeOut take 9 map { pov =>
              a(href := routes.Round.player(pov.fullId), cls := "game-timeout" + ~pov.isMyTurn.option(" my_turn"), id := "others_" + pov.gameId)(
                gameFen(pov, withLink = false, withTitle = false),
                span(cls := "meta")(
                  playerText(pov.opponent),
                  span(cls := "indicator")(
                    "Timeout ",
                    secondsFromNow(pov.game.timeOutRemaining, true)
                  )
                )
              )
            }
          )
        )
      )
    }
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
      div(cls := "round__app__board main-board")(draughtsground(pov)),
      div(cls := "round__app__table"),
      div(cls := "ruser ruser-top user-link")(i(cls := "line"), a(cls := "text")(playerText(pov.opponent))),
      div(cls := "ruser ruser-bottom user-link")(i(cls := "line"), a(cls := "text")(playerText(pov.player))),
      div(cls := "rclock rclock-top preload")(div(cls := "time")(nbsp)),
      div(cls := "rclock rclock-bottom preload")(div(cls := "time")(nbsp)),
      div(cls := "rmoves")(div(cls := "moves")),
      controls option div(cls := "rcontrols")(i(cls := "ddloader"))
    )
}
