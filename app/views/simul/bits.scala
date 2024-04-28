package views.simul

import play.api.i18n.Lang

import lila.app.templating.Environment.{ *, given }

object bits:

  def link(simulId: lila.simul.SimulId): Frag =
    a(href := routes.Simul.show(simulId))("Simultaneous exhibition")

  def jsI18n()(using Translate) = i18nJsObject(baseTranslations)

  def notFound()(using PageContext) =
    views.base.layout(title = trans.site.noSimulFound.txt()):
      main(cls := "page-small box box-pad")(
        h1(cls := "box__top")(trans.site.noSimulFound()),
        p(trans.site.noSimulExplanation()),
        p(a(href := routes.Simul.home)(trans.site.returnToSimulHomepage()))
      )

  def homepageSpotlight(s: lila.simul.Simul)(using Context) =
    a(href := routes.Simul.show(s.id), cls := "tour-spotlight little")(
      img(cls := "img icon", src := assetUrl("images/fire-silhouette.svg")),
      span(cls := "content")(
        span(cls := "name")(s.name, " simul"),
        span(cls := "more")(
          trans.site.nbPlayers.plural(s.applicants.size, s.applicants.size.localize),
          " • ",
          trans.site.join()
        )
      )
    )

  def allCreated(simuls: Seq[lila.simul.Simul], withName: Boolean = true)(using Translate) =
    table(cls := "slist"):
      simuls.map: simul =>
        val url = routes.Simul.show(simul.id)
        tr(
          withName.option(td(cls := "name")(a(href := url)(simul.fullName))),
          td:
            if withName then userIdLink(simul.hostId.some)
            else a(href := url)(userIdSpanMini(simul.hostId, true))
          ,
          td(cls := "text", dataIcon := Icon.Clock)(simul.clock.config.show),
          td(cls := "text", dataIcon := Icon.User)(simul.applicants.size)
        )

  private[simul] def setup(sim: lila.simul.Simul) =
    span(cls := List("setup" -> true, "rich" -> sim.variantRich))(
      sim.clock.config.show,
      " • ",
      sim.variants.map(_.name).mkString(", ")
    )

  private val baseTranslations = Vector(
    trans.site.finished,
    trans.site.withdraw,
    trans.site.join,
    trans.site.cancel,
    trans.site.joinTheGame,
    trans.site.nbPlaying,
    trans.site.nbWins,
    trans.site.nbDraws,
    trans.site.nbLosses,
    trans.site.by,
    trans.site.signIn,
    trans.site.mustBeInTeam
  )
