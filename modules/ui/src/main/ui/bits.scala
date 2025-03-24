package lila.ui

import play.api.i18n.Lang

import java.time.{ Month, Year, YearMonth }

import chess.format.Fen

import lila.core.i18n.Translate
import lila.core.security.HcaptchaForm

import ScalatagsTemplate.{ *, given }

object bits:

  val engineFullName = "Stockfish 17"

  def subnav(mods: Modifier*) = st.aside(cls := "subnav"):
    st.nav(cls := "subnav__inner")(mods)

  def pageMenuSubnav(mods: Modifier*) = subnav(cls := "page-menu__menu", mods)

  def mselect(id: String, current: Frag, items: Seq[Tag]) =
    div(cls := "mselect")(
      input(
        tpe          := "checkbox",
        cls          := "mselect__toggle fullscreen-toggle",
        st.id        := s"mselect-$id",
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
      url: (Int, Int) => play.api.mvc.Call
  )(at: YearMonth)(using
      Lang
  ) =
    import helpers.showMonth
    val prefix                   = s"calendar-mselect"
    def prefixed(suffix: String) = s"${prefix}$suffix"
    div(cls := s"$prefix $prefix--$id")(
      a(
        href     := url(at.minusMonths(1).getYear, at.minusMonths(1).getMonthValue),
        dataIcon := Icon.LessThan
      ),
      div(cls := prefixed("__selects"))(
        mselect(
          prefixed(s"__year--$id"),
          span(at.getYear),
          allYears.map: y =>
            a(
              cls  := (y == at.getYear).option("current"),
              href := url(y, at.getMonthValue)
            )(y)
        ),
        mselect(
          prefixed(s"__month--$id"),
          span(showMonth(at.getMonth)),
          java.time.Month.values.toIndexedSeq.map: m =>
            a(
              cls  := (m == at.getMonth).option("current"),
              href := url(at.getYear, m.getValue)
            )(showMonth(m))
        )
      ),
      a(
        href     := url(at.plusMonths(1).getYear, at.plusMonths(1).getMonthValue),
        dataIcon := Icon.GreaterThan
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
          st.id            := s"$prefix-tab-$id",
          aria("controls") := s"$prefix-panel-$id",
          role             := "tab",
          cls              := "tab-list__tab",
          aria("selected") := (selected == id).option("true"),
          tabindex         := 0
        )(name)
    ),
    div(cls := "panel-list")(
      tabs.map: (id, _, content) =>
        div(
          st.id              := s"$prefix-panel-$id",
          aria("labelledby") := s"$prefix-tab-$id",
          role               := "tabpanel",
          cls                := List("panel-list__panel" -> true, "none" -> (selected != id)),
          tabindex           := 0
        )(content)
    )
  )
