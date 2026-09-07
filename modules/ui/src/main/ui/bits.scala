package lila.ui

import play.api.i18n.Lang
import java.time.YearMonth
import chess.format.Fen
import scalalib.StringOps.addQueryParams

import lila.core.i18n.Translate
import lila.core.config.ImageGetOrigin
import lila.ui.ScalatagsTemplate.{ *, given }

object bits:

  val engineFullName = "Stockfish 18"

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
      label(`for` := s"mselect-$id", cls := "mselect__label", role := "menu")(current),
      label(`for` := s"mselect-$id", cls := "fullscreen-mask"),
      st.nav(cls := "mselect__list")(items.map(_(cls := "mselect__item", role := "menuitem")))
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

  def markdownEditor(realm: MarkdownRealm)(textareaTag: Tag)(using
      imageGetOrigin: ImageGetOrigin
  )(using ctx: Context) =
    val editorClass = if realm.toastUi then "markdown-toastui" else "markdown-textarea"
    val canUploadImages = ctx.me.soUse(lila.core.security.canUploadImages(realm.key))
    val uploadUrl = canUploadImages.option(routes.Main.uploadImage(realm))
    val imageUploadButton = (!realm.toastUi && canUploadImages).option:
      button(cls := "upload-image", tpe := "button", title := "Upload image")
    val previewStyle = realm match
      case MarkdownRealm.blog => "ublog-post__markup"
      case MarkdownRealm.cms => "cms-preview"
      case _ => ""
    div(
      cls := s"markdown-editor $editorClass",
      attr("data-markdown-realm") := realm.key,
      attr("data-image-download-origin") := imageGetOrigin,
      attr("data-image-count-max") := realm.maxImageCount,
      attr("data-image-design-width") := realm.imageDesignWidth,
      uploadUrl.map(url => attr("data-image-upload-url") := url)
    )(
      div(cls := "header")(
        button(cls := "header-tab write-tab active", tpe := "button")("Write"),
        button(cls := "header-tab preview-tab", tpe := "button")("Preview"),
        imageUploadButton
      ),
      div(cls := "content")(
        textareaTag(cls := "markdown-content-textarea"),
        if realm.toastUi then div(cls := "toastui-container") else emptyFrag,
        div(cls := s"preview none $previewStyle")
      )
    )

  def markdownAlternate(url: String, params: Map[String, String] = Map.empty) =
    link(
      rel := "alternate",
      tpe := "text/markdown",
      href := addQueryParams(url, params + ("output_format" -> "md"))
    )
