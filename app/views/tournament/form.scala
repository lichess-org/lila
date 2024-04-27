package views.tournament

import play.api.data.{ Field, Form }
import scalalib.paginator.Paginator

import lila.app.templating.Environment.{ *, given }
import lila.core.team.LightTeam
import lila.tournament.Tournament
import lila.tournament.ui.*

object form:
  lazy val ui = TournamentForm(helpers, show.ui)(translatedVariantChoicesWithVariantsById)

  def create(form: Form[?], leaderTeams: List[LightTeam])(using PageContext) =
    views.base.layout(
      title = trans.site.newTournament.txt(),
      moreCss = cssTag("tournament.form"),
      modules = jsModule("bits.tourForm")
    )(ui.create(form, leaderTeams))

  def edit(tour: Tournament, form: Form[?], myTeams: List[LightTeam])(using PageContext) =
    views.base.layout(
      title = tour.name(),
      moreCss = cssTag("tournament.form"),
      modules = jsModule("bits.tourForm")
    )(ui.edit(tour, form, myTeams))

object crud:
  given prefix: FormPrefix = FormPrefix.make("setup")

  private def layout(
      title: String,
      modules: EsmList = Nil,
      evenMoreJs: Frag = emptyFrag,
      css: String = "mod.misc"
  )(body: Frag)(using PageContext) =
    views.base.layout(
      title = title,
      moreCss = cssTag(css),
      modules = jsModule("bits.flatpick") ++ modules,
      moreJs = evenMoreJs
    ):
      main(cls := "page-menu")(views.mod.ui.menu("tour"), body)

  def create(form: Form[?])(using PageContext) =
    layout(
      title = "New tournament",
      css = "mod.form"
    ):
      div(cls := "crud page-menu__content box box-pad")(
        h1(cls := "box__top")("New tournament"),
        postForm(cls := "form3", action := routes.TournamentCrud.create)(
          views.tournament.form.ui.spotlightAndTeamBattle(form, none),
          errMsg(form("setup")),
          views.tournament.form.ui.setupCreate(form, Nil),
          form3.action(form3.submit(trans.site.apply()))
        )
      )

  def edit(tour: Tournament, f: Form[?])(using PageContext) =
    layout(title = tour.name(), css = "mod.form")(form.ui.crudEdit(tour, f))

  def index(tours: Paginator[Tournament])(using PageContext) =
    layout(title = "Tournament manager", modules = infiniteScrollTag)(form.ui.crudIndex(tours))
