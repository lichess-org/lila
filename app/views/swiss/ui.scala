package views.swiss

import play.api.data.Form

import lila.app.templating.Environment.{ *, given }
import lila.swiss.{ Swiss, SwissForm }

lazy val bits           = lila.swiss.ui.SwissBitsUi(helpers, env.swiss.getName)
private lazy val home   = lila.swiss.ui.SwissHomeUi(helpers)
private lazy val formUi = lila.swiss.ui.SwissFormUi(helpers)(translatedVariantChoicesWithVariants)

def homepage(featured: lila.swiss.FeaturedSwisses)(using PageContext) =
  views.base.layout(
    title = trans.swiss.swissTournaments.txt(),
    moreCss = cssTag("swiss.home"),
    withHrefLangs = lila.web.LangPath(routes.Swiss.home).some
  )(home.page(featured))

def notFound()(using PageContext) =
  views.base.layout(title = trans.site.tournamentNotFound.txt())(bits.notFound)

object form:

  import formUi.*

  def create(form: Form[SwissForm.SwissData], teamId: TeamId)(using PageContext) =
    views.base.layout(
      title = trans.swiss.newSwiss.txt(),
      moreCss = cssTag("swiss.form"),
      modules = jsModule("bits.tourForm")
    )(formUi.create(form, teamId))

  def edit(swiss: Swiss, form: Form[SwissForm.SwissData])(using PageContext) =
    views.base.layout(
      title = swiss.name,
      moreCss = cssTag("swiss.form"),
      modules = jsModule("bits.tourForm")
    )(formUi.edit(swiss, form))
