package views.tournament

import play.api.libs.json.Json
import scalalib.paginator.Paginator

import lila.app.templating.Environment.{ *, given }

import lila.tournament.Tournament
import lila.tournament.Schedule.Freq

private lazy val listUi = lila.tournament.ui.TournamentList(helpers, ui)

export listUi.homepageSpotlight

def home(
    scheduled: List[Tournament],
    finished: List[Tournament],
    winners: lila.tournament.AllWinners,
    json: play.api.libs.json.JsObject
)(using ctx: PageContext) =
  views.base.layout(
    title = trans.site.tournaments.txt(),
    moreCss = cssTag("tournament.home"),
    wrapClass = "full-screen-force",
    modules = infiniteScrollEsmInit,
    pageModule = PageModule(
      "tournament.schedule",
      Json.obj("data" -> json, "i18n" -> ui.scheduleJsI18n)
    ).some,
    openGraph = OpenGraph(
      url = s"$netBaseUrl${routes.Tournament.home.url}",
      title = trans.site.tournamentHomeTitle.txt(),
      description = trans.site.tournamentHomeDescription.txt()
    ).some,
    withHrefLangs = lila.ui.LangPath(routes.Tournament.home).some
  )(listUi.home(scheduled, finished, winners))

def history(freq: Freq, pager: Paginator[Tournament])(using PageContext) =
  views.base.layout(
    title = "Tournament history",
    modules = infiniteScrollEsmInit,
    moreCss = cssTag("tournament.history")
  )(listUi.history(freq, pager))

def calendar(json: play.api.libs.json.JsObject)(using PageContext) =
  views.base.layout(
    title = "Tournament calendar",
    pageModule = PageModule("tournament.calendar", Json.obj("data" -> json)).some,
    moreCss = cssTag("tournament.calendar")
  ):
    main(cls := "box")(
      h1(cls := "box__top")(trans.site.tournamentCalendar()),
      div(id := "tournament-calendar")
    )

def leaderboard(winners: lila.tournament.AllWinners)(using PageContext) =
  views.base.layout(
    title = "Tournament leaderboard",
    moreCss = cssTag("tournament.leaderboard"),
    wrapClass = "full-screen-force"
  )(listUi.leaderboard(winners, views.user.bits.communityMenu("tournament")))

object shields:
  import lila.tournament.TournamentShield
  private def menu(using Translate) = views.user.bits.communityMenu("shield")

  def apply(history: TournamentShield.History)(using PageContext) =
    views.base.layout(
      title = "Tournament shields",
      moreCss = cssTag("tournament.leaderboard"),
      wrapClass = "full-screen-force"
    )(listUi.shields(history, menu))

  def byCateg(categ: TournamentShield.Category, awards: List[TournamentShield.Award])(using PageContext) =
    views.base.layout(
      title = "Tournament shields",
      moreCss = frag(cssTag("tournament.leaderboard"), cssTag("slist"))
    )(listUi.shields.byCateg(categ, awards, menu))
