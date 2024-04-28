package lila.ui

import ScalatagsTemplate.{ *, given }

opaque type LangPath = String
object LangPath extends OpaqueString[LangPath]:
  def apply(call: play.api.mvc.Call): LangPath = LangPath(call.url)

case class Layout(
    fullTitle: Option[String],
    robots: Boolean,
    cssFrag: Frag,
    modules: EsmList,
    jsFrag: WithNonce[Frag],
    pageModule: Option[PageModule],
    playing: Boolean,
    openGraph: Option[OpenGraph],
    zoomable: Boolean,
    zenable: Boolean,
    csp: Option[Update[ContentSecurityPolicy]],
    wrapClass: String,
    atomLinkTag: Option[Tag],
    withHrefLangs: Option[LangPath]
):
  def apply(esm: EsmInit): Layout                    = copy(modules = modules :+ esm.some)
  def apply(og: OpenGraph): Layout                   = copy(openGraph = og.some)
  def apply(pm: PageModule): Layout                  = copy(pageModule = pm.some)
  def robots(b: Boolean): Layout                     = copy(robots = b)
  def css(f: Frag): Layout                           = copy(cssFrag = cssFrag |+| f)
  def js(f: WithNonce[Frag]): Layout                 = copy(jsFrag = jsFrag |+| f)
  def csp(up: Update[ContentSecurityPolicy]): Layout = copy(csp = csp.fold(up)(up.compose).some)

object Layout:
  type Build = Update[Layout]
  val default: Build = identity

case class Page(title: String, layout: Layout.Build = identity)(val body: Frag):
  def apply(build: Layout.Build): Page     = copy(layout = build.compose(layout))(body)
  def contramap(build: Layout.Build): Page = copy(layout = layout.compose(build))(body)
