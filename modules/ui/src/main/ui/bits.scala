package lila.ui

import play.api.i18n.Lang

import java.time.YearMonth

import chess.format.Fen

import lila.core.i18n.Translate
import lila.core.security.HcaptchaForm

import ScalatagsTemplate.{ *, given }

object bits:

  val engineFullName = "Stockfish 17.1"

  def subnav(mods: Modifier*) = st.aside(cls := "subnav"):
    st.nav(cls := "subnav__inner")(mods)

  def pageMenuSubnav(mods: Modifier*) = subnav(cls := "page-menu__menu", mods)

  def mselect(id: String, current: Frag, items: Seq[Tag]) =
    div(cls := "mselect")(
      input(
        tpe := "checkbox",
        cls := "mselect__toggle fullscreen-toggle",
        st.id := s"mselect-$id",
        autocomplete := "off"
      ),
      label(`for` := s"mselect-$id", cls := "mselect__label")(current),
      label(`for` := s"mselect-$id", cls := "fullscreen-mask"),
      st.nav(cls := "mselect__list")(items.map(_(cls := "mselect__item")))
    )

  // url: (year: Int, month: Int)
  def calendarMselect(
      helpers: Helpers,
      id: String,
      allYears: List[Int],
      firstMonth: YearMonth,
      url: (Int, Int) => play.api.mvc.Call
  )(at: YearMonth)(using Lang) =
    import helpers.showMonth
    val prefix = s"calendar-mselect"
    def prefixed(suffix: String) = s"${prefix}$suffix"
    def nonEmptyDate(date: YearMonth) =
      val ok = date.isAfter(firstMonth.minusMonths(1)) && date.isBefore(YearMonth.now.plusMonths(12))
      Option.when(ok)(date)
    val prev = nonEmptyDate(at.minusMonths(1))
    val next = nonEmptyDate(at.plusMonths(1))
    def urlOf(ym: YearMonth) = url(ym.getYear, ym.getMonthValue)
    div(cls := s"$prefix $prefix--$id")(
      a(
        href := prev.map(urlOf),
        dataIcon := Icon.LessThan,
        cls := List("disabled" -> prev.isEmpty)
      ),
      div(cls := prefixed("__selects"))(
        mselect(
          prefixed(s"__year--$id"),
          span(at.getYear),
          allYears.map: y =>
            a(
              cls := (y == at.getYear).option("current"),
              href := url(y, at.getMonthValue)
            )(y)
        ),
        mselect(
          prefixed(s"__month--$id"),
          span(showMonth(at.getMonth)),
          java.time.Month.values.toIndexedSeq.map: m =>
            a(
              cls := (m == at.getMonth).option("current"),
              href := url(at.getYear, m.getValue)
            )(showMonth(m))
        )
      ),
      a(
        href := next.map(urlOf),
        dataIcon := Icon.GreaterThan,
        cls := List("disabled" -> next.isEmpty)
      )
    )

  def fenAnalysisLink(fen: Fen.Full)(using Translate) =
    a(href := routes.UserAnalysis.parseArg(ChessHelper.underscoreFen(fen)))(
      lila.core.i18n.I18nKey.site.analysis()
    )

  private val dataSitekey = attr("data-sitekey")

  def hcaptcha(form: HcaptchaForm[?]) =
    div(cls := "h-captcha form-group", dataSitekey := form.config.key)

  def contactEmailLinkEmpty(email: String) =
    a(cls := "contact-email-obfuscated", attr("data-email") := scalalib.StringOps.base64.encode(email))

  def ariaTabList(prefix: String, selected: String)(tabs: (String, String, Frag)*) = frag(
    div(cls := "tab-list", role := "tablist")(
      tabs.map: (id, name, _) =>
        button(
          st.id := s"$prefix-tab-$id",
          aria("controls") := s"$prefix-panel-$id",
          role := "tab",
          cls := "tab-list__tab",
          aria("selected") := (selected == id).option("true"),
          tabindex := 0
        )(name)
    ),
    div(cls := "panel-list")(
      tabs.map: (id, _, content) =>
        div(
          st.id := s"$prefix-panel-$id",
          aria("labelledby") := s"$prefix-tab-$id",
          role := "tabpanel",
          cls := List("panel-list__panel" -> true, "none" -> (selected != id)),
          tabindex := 0
        )(content)
    )
  )

  def modMenu(active: String)(using ctx: Context): Frag = ctx.me.foldUse(emptyFrag): me ?=>
    pageMenuSubnav(
      Granter(_.SeeReport)
        .option(a(cls := itemCls(active, "report"), href := routes.Report.list)("Reports")),
      Granter(_.PublicChatView)
        .option(a(cls := itemCls(active, "public-chat"), href := routes.Mod.publicChat)("Public Chats")),
      Granter(_.GamifyView)
        .option(a(cls := itemCls(active, "activity"), href := routes.Mod.activity)("Mod activity")),
      Granter(_.GamifyView)
        .option(a(cls := itemCls(active, "queues"), href := routes.Mod.queues("month"))("Queues stats")),
      Granter(_.GamifyView)
        .option(a(cls := itemCls(active, "gamify"), href := routes.Mod.gamify)("Hall of fame")),
      Granter(_.GamifyView)
        .option(a(cls := itemCls(active, "log"), href := routes.Mod.log(me.username.some))("Mod logs")),
      Granter(_.UserSearch)
        .option(a(cls := itemCls(active, "search"), href := routes.Mod.search)("Search users")),
      Granter(_.Admin).option(a(cls := itemCls(active, "notes"), href := routes.Mod.notes())("Mod notes")),
      Granter(_.SetEmail)
        .option(a(cls := itemCls(active, "email"), href := routes.Mod.emailConfirm)("Email confirm")),
      Granter(_.Pages).option(a(cls := itemCls(active, "cms"), href := routes.Cms.index)("Pages")),
      Granter(_.ManageTournament)
        .option(a(cls := itemCls(active, "tour"), href := routes.TournamentCrud.index(1))("Tournaments")),
      Granter(_.ManageEvent)
        .option(a(cls := itemCls(active, "event"), href := routes.Event.manager)("Events")),
      Granter(_.ModerateBlog)
        .option(a(cls := itemCls(active, "carousel"), href := routes.Ublog.modShowCarousel)("Blog carousel")),
      Granter(_.MarkEngine)
        .option(a(cls := itemCls(active, "irwin"), href := routes.Irwin.dashboard)("Irwin dashboard")),
      Granter(_.MarkEngine)
        .option(a(cls := itemCls(active, "kaladin"), href := routes.Irwin.kaladin)("Kaladin dashboard")),
      Granter(_.Admin).option(a(cls := itemCls(active, "mods"), href := routes.Mod.table)("Mods")),
      Granter(_.Presets)
        .option(a(cls := itemCls(active, "presets"), href := routes.Mod.presets("PM"))("Msg presets")),
      Granter(_.Settings)
        .option(a(cls := itemCls(active, "setting"), href := routes.Dev.settings)("Settings")),
      Granter(_.Cli).option(a(cls := itemCls(active, "cli"), href := routes.Dev.cli)("CLI"))
    )

  private def itemCls(active: String, item: String) = if active == item then "active" else ""
