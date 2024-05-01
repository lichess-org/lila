package views

import lila.app.UiEnv.{ *, given }

export lila.web.ui.bits

lazy val captcha = lila.web.ui.CaptchaUi(helpers)

lazy val chat = lila.chat.ChatUi(helpers)

lazy val gathering = lila.gathering.ui.GatheringUi(helpers)(env.web.settings.prizeTournamentMakers.get)

lazy val learn = lila.web.ui.LearnUi(helpers)

lazy val coordinate = lila.coordinate.ui.CoordinateUi(helpers)

lazy val atomUi = lila.ui.AtomUi(netConfig.baseUrl)

val irwin = lila.irwin.IrwinUi(helpers)(menu = views.mod.ui.menu)

object oAuth:
  val token     = lila.oauth.ui.TokenUi(helpers)(views.account.ui.AccountPage)
  val authorize = lila.oauth.ui.AuthorizeUi(helpers)(lightUserFallback)

lazy val plan      = lila.plan.ui.PlanUi(helpers)(netConfig.email)
lazy val planPages = lila.plan.ui.PlanPages(helpers)(lila.fishnet.FishnetLimiter.maxPerDay)

lazy val feed =
  lila.feed.ui.FeedUi(helpers, atomUi)(title => _ ?=> site.page.SitePage(title, "news"))(using env.executor)

lazy val cms = lila.cms.ui.CmsUi(helpers)(views.mod.ui.menu("cms"))

lazy val userTournament = lila.tournament.ui.UserTournament(helpers, views.tournament.ui)

object account:
  val ui        = lila.pref.ui.AccountUi(helpers)
  val pages     = lila.pref.ui.AccountPages(helpers, ui, flagApi)
  val pref      = lila.pref.ui.AccountPref(helpers, prefHelper, ui)
  val twoFactor = lila.pref.ui.TwoFactorUi(helpers, ui)
  val security  = lila.security.ui.AccountSecurity(helpers)(env.net.email, ui.AccountPage)

lazy val practice = lila.practice.ui.PracticeUi(helpers)(
  csp = analysisCsp,
  translations = userAnalysisI18n.vector(),
  views.board.bits.explorerAndCevalConfig,
  modMenu = views.mod.ui.menu("practice")
)

object forum:
  import lila.forum.ui.*
  val bits  = ForumBits(helpers)
  val post  = PostUi(helpers, bits)
  val categ = CategUi(helpers, bits)
  val topic = TopicUi(helpers, bits, post)(
    captcha.apply,
    lila.msg.MsgPreset.forumDeletion.presets
  )

lazy val timeline = lila.timeline.ui.TimelineUi(helpers)(views.streamer.bits.redirectLink(_))

lazy val userAnalysisI18n = lila.analyse.ui.AnalyseI18n(helpers)
lazy val analysisI18n     = lila.analyse.ui.GameAnalyseI18n(helpers, userAnalysisI18n)

object opening:
  val bits = lila.opening.ui.OpeningBits(helpers)
  val wiki = lila.opening.ui.WikiUi(helpers, bits)
  val ui   = lila.opening.ui.OpeningUi(helpers, bits, wiki)

lazy val video = lila.video.ui.VideoUi(helpers)

lazy val gameSearch = lila.gameSearch.ui.GameSearchUi(helpers)(views.game.widgets(_))

lazy val auth = lila.web.ui.AuthUi(helpers)

def mobile(p: lila.cms.CmsPage.Render)(using Context) =
  lila.web.ui.mobile(helpers)(cms.render(p))
