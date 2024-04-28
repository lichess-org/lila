package lila.ui

import ScalatagsTemplate.{ *, given }

opaque type LangPath = String
object LangPath extends OpaqueString[LangPath]:
  def apply(call: play.api.mvc.Call): LangPath = LangPath(call.url)

opaque type Nonce = String
object Nonce extends OpaqueString[Nonce]:
  extension (a: Nonce) def scriptSrc = s"'nonce-${a.value}'"
  def random: Nonce                  = Nonce(scalalib.SecureRandom.nextString(24))

case class Layout(
    fullTitle: Option[String],
    robots: Boolean,
    moreCss: Frag,
    modules: EsmList,
    moreJs: Frag,
    pageModule: Option[PageModule],
    playing: Boolean,
    openGraph: Option[OpenGraph],
    zoomable: Boolean,
    zenable: Boolean,
    csp: Option[ContentSecurityPolicy],
    wrapClass: String,
    atomLinkTag: Option[Tag],
    withHrefLangs: Option[LangPath]
):
  def apply(esm: EsmInit): Layout   = copy(modules = modules :+ esm.some)
  def apply(og: OpenGraph): Layout  = copy(openGraph = og.some)
  def apply(pm: PageModule): Layout = copy(pageModule = pm.some)
  def robots(b: Boolean): Layout    = copy(robots = b)
  def css(f: Frag): Layout          = copy(moreCss = frag(moreCss, f))
  def js(f: Frag): Layout           = copy(moreJs = frag(moreJs, f))

object Layout:
  type Build = Layout => Layout
  val default: Build = identity

case class Page(title: String, layout: Layout.Build = identity)(val body: Frag):
  def apply(build: Layout.Build): Page     = copy(layout = build.compose(layout))(body)
  def contramap(build: Layout.Build): Page = copy(layout = layout.compose(build))(body)
