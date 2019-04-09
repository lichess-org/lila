package views.html.simul

import play.api.libs.json.Json

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue

import controllers.routes

object bits {

  def link(simulId: lidraughts.simul.Simul.ID): Frag =
    a(href := routes.Simul.show(simulId))("Simultaneous exhibition")

  def jsI18n()(implicit ctx: Context) = safeJsonValue(i18nJsObject(baseTranslations))

  def notFound()(implicit ctx: Context) =
    views.html.base.layout(
      responsive = true,
      title = trans.noSimulFound.txt()
    ) {
        main(cls := "page-small box box-pad")(
          h1(trans.noSimulFound.frag()),
          p(trans.noSimulExplanation.frag()),
          p(a(href := routes.Simul.home())(trans.returnToSimulHomepage.frag()))
        )
      }

  private def imgUrl(spotlight: Option[lidraughts.simul.Spotlight]) =
    spotlight.flatMap(_.iconImg).getOrElse("images/fire-silhouette.svg")

  private def imgClass(spotlight: Option[lidraughts.simul.Spotlight]) =
    if (spotlight.flatMap(_.iconImg).isDefined) "img"
    else "img icon"

  def homepageSpotlight(s: lidraughts.simul.Simul)(implicit ctx: Context) =
    a(href := routes.Simul.show(s.id), cls := "tour-spotlight little id_@s.id")(
      img(cls := imgClass(s.spotlight), src := staticUrl(imgUrl(s.spotlight))),
      span(cls := "content")(
        span(cls := "name")(s.fullName),
        s.spotlight.map(spot =>
          frag(
            span(cls := "headline")(spot.headline),
            span(cls := "more")(
              if (s.isRunning) trans.eventInProgress.frag()
              else if (spot.isNow) trans.startingSoon.frag()
              else momentFromNow(spot.startsAt)
            )
          )).getOrElse(
          span(cls := "more")(
            trans.nbPlayers.plural(s.applicants.size, s.applicants.size.localize),
            " • ",
            trans.join()
          )
        )
      )
    )

  def allCreated(simuls: List[lidraughts.simul.Simul]) =
    table(
      simuls map { simul =>
        tr(
          td(cls := "name")(
            a(cls := "text", href := routes.Simul.show(simul.id))(
              simul.perfTypes map { pt =>
                span(dataIcon := pt.iconChar)
              },
              simul.fullName
            )
          ),
          td(userIdLink(simul.hostId.some)),
          td(cls := "text", dataIcon := "p")(simul.clock.config.show),
          td(cls := "text", dataIcon := "r")(simul.applicants.size),
          td(a(href := routes.Simul.show(simul.id), cls := "button", dataIcon := "G"))
        )
      }
    )

  private[simul] def setup(sim: lidraughts.simul.Simul)(implicit ctx: Context) =
    span(cls := List("setup" -> true, "rich" -> sim.variantRich))(
      sim.clock.config.show,
      " • ",
      sim.variants.map(_.name).mkString(", ")
    )

  private val baseTranslations = Vector(
    trans.finished,
    trans.exportSimulGames,
    trans.withdraw,
    trans.join,
    trans.cancel,
    trans.joinTheGame,
    trans.followSimulHostTv,
    trans.nbPlaying,
    trans.nbWins,
    trans.nbDraws,
    trans.nbLosses,
    trans.nbVictories,
    trans.by,
    trans.arbiter,
    trans.signIn,
    trans.shareSimulUrl,
    trans.deleteThisSimul,
    trans.accept,
    trans.candidatePlayers,
    trans.allowedPlayers,
    trans.acceptedPlayers,
    trans.acceptSomePlayers,
    trans.acceptRandomCandidate,
    trans.youHaveBeenSelected,
    trans.simulParticipationLimited,
    trans.winningPercentage,
    trans.targetPercentage,
    trans.toReachTarget,
    trans.relativeScoreRequired,
    trans.succeeded,
    trans.failed,
    trans.backToSimul
  )
}
