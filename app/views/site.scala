package views.site

import lila.app.UiEnv.{ *, given }
import lila.cms.CmsPage

val message = lila.web.ui.SiteMessage(helpers)
val ui      = lila.web.ui.SitePages(helpers)

object page:

  val faq = lila.web.ui.FaqUi(helpers, ui)(
    standardRankableDeviation = lila.rating.Glicko.standardRankableDeviation,
    variantRankableDeviation = lila.rating.Glicko.variantRankableDeviation
  )

  def withMenu(active: String, p: CmsPage.Render)(using Context) =
    ui.SitePage(
      title = p.title,
      active = active,
      contentCls = "page box box-pad force-ltr"
    ).css("bits.page")(views.cms.pageContent(p))

  def contact(using Context) =
    ui.SitePage(
      title = trans.contact.contact.txt(),
      active = "contact",
      contentCls = "page box box-pad"
    ).css("bits.contact")
      .js(esmInitBit("contact"))(lila.web.ui.contact(netConfig.email))

  def webmasters(using Context) =
    ui.webmasters(
      li(strong("theme"), ": ", lila.pref.Theme.all.map(_.name).mkString(", ")),
      li(strong("pieceSet"), ": ", lila.pref.PieceSet.all.map(_.name).mkString(", "))
    )

object variant:

  def show(
      p: lila.cms.CmsPage.Render,
      variant: chess.variant.Variant,
      perfType: lila.rating.PerfType
  )(using Context) =
    page(
      title = s"${variant.name} • ${variant.title}",
      klass = "box-pad page variant",
      active = perfType.key.some
    ):
      frag(
        boxTop(h1(cls := "text", dataIcon := perfType.icon)(variant.name)),
        h2(cls := "headline")(variant.title),
        div(cls := "body expand-text")(views.cms.render(p))
      )

  def home(using Context) =
    page(title = "Lichess variants", klass = "variants"):
      frag(
        h1(cls := "box__top")("Lichess variants"),
        div(cls := "body box__pad")(
          "Chess variants introduce variations of or new mechanics in regular Chess that gives it a unique, compelling, or sophisticated gameplay. Are you ready to think outside the box?"
        ),
        div(cls := "variants")(
          lila.rating.PerfType.variants.map: pk =>
            val variant = lila.rating.PerfType.variantOf(pk)
            val pt      = lila.rating.PerfType(pk)
            a(
              cls      := "variant text box__pad",
              href     := routes.Cms.variant(variant.key),
              dataIcon := pt.icon
            ):
              span(
                h2(variant.name),
                h3(cls := "headline")(variant.title)
              )
        )
      )

  private def page(title: String, klass: String, active: Option[PerfKey] = None)(using Context) =
    Page(title)
      .css("bits.variant")
      .js(Esm("bits.expandText"))
      .wrap: body =>
        main(cls := "page-menu")(
          lila.ui.bits.pageMenuSubnav(
            lila.rating.PerfType.variants.map: pk =>
              val variant = lila.rating.PerfType.variantOf(pk)
              a(
                cls      := List("text" -> true, "active" -> active.contains(pk)),
                href     := routes.Cms.variant(variant.key),
                dataIcon := pk.perfIcon
              )(pk.perfTrans)
          ),
          div(cls := s"page-menu__content box $klass")(body)
        )
