package views

import lila.app.UiEnv.{ *, given }

export lila.web.ui.bits

val captcha = lila.web.ui.CaptchaUi(helpers)

val chat = lila.chat.ChatUi(helpers)

val boardEditor = lila.web.ui.BoardEditorUi(helpers)

val userAnalysisI18n = lila.analyse.ui.AnalyseI18n(helpers)
val analysisI18n     = lila.analyse.ui.GameAnalyseI18n(helpers, userAnalysisI18n)

val setup = lila.setup.ui.SetupUi(helpers)

val gathering = lila.gathering.ui.GatheringUi(helpers)(env.web.settings.prizeTournamentMakers.get)

val learn = lila.web.ui.LearnUi(helpers)

val coordinate = lila.coordinate.ui.CoordinateUi(helpers)

val atomUi = lila.ui.AtomUi(netConfig.baseUrl)

val irwin = lila.irwin.IrwinUi(helpers)(menu = mod.ui.menu)

val dgt = lila.web.ui.DgtUi(helpers)

val relation = lila.relation.ui.RelationUi(helpers)

object oAuth:
  val token     = lila.oauth.ui.TokenUi(helpers)(account.ui.AccountPage)
  val authorize = lila.oauth.ui.AuthorizeUi(helpers)(lightUserFallback)

val plan      = lila.plan.ui.PlanUi(helpers)(netConfig.email)
val planPages = lila.plan.ui.PlanPages(helpers)(lila.fishnet.FishnetLimiter.maxPerDay)

val feed =
  lila.feed.ui.FeedUi(helpers, atomUi)(title => _ ?=> site.ui.SitePage(title, "news", ""))(using
    env.executor
  )

val cms = lila.cms.ui.CmsUi(helpers)(mod.ui.menu("cms"))

val event = lila.event.ui.EventUi(helpers)(mod.ui.menu("event"))(using env.executor)

val userTournament = lila.tournament.ui.UserTournament(helpers, tournament.ui)

object account:
  val ui        = lila.pref.ui.AccountUi(helpers)
  val pages     = lila.pref.ui.AccountPages(helpers, ui, flagApi)
  val pref      = lila.pref.ui.AccountPref(helpers, prefHelper, ui)
  val twoFactor = lila.pref.ui.TwoFactorUi(helpers, ui)
  val security  = lila.security.ui.AccountSecurity(helpers)(env.net.email, ui.AccountPage)

val practice = lila.practice.ui.PracticeUi(helpers)(
  csp = analyse.ui.csp,
  translations = userAnalysisI18n.vector(),
  board.explorerAndCevalConfig,
  modMenu = mod.ui.menu("practice")
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

val timeline = lila.timeline.ui.TimelineUi(helpers)(streamer.bits.redirectLink(_))

object opening:
  val bits = lila.opening.ui.OpeningBits(helpers)
  val wiki = lila.opening.ui.WikiUi(helpers, bits)
  val ui   = lila.opening.ui.OpeningUi(helpers, bits, wiki)

val video = lila.video.ui.VideoUi(helpers)

val gameSearch = lila.gameSearch.ui.GameSearchUi(helpers)(game.widgets(_))

val auth = lila.web.ui.AuthUi(helpers)

val storm = lila.storm.ui.StormUi(helpers)

val racer = lila.racer.ui.RacerUi(helpers)

val challenge = lila.challenge.ui.ChallengeUi(helpers)

val dev = lila.web.ui.DevUi(helpers)(mod.ui.menu)

def mobile(p: lila.cms.CmsPage.Render)(using Context) =
  lila.web.ui.mobile(helpers)(cms.render(p))
