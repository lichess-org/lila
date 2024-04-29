package lila.ui

import ScalatagsTemplate.{ *, given }

opaque type LangPath = String
object LangPath extends OpaqueString[LangPath]:
  def apply(call: play.api.mvc.Call): LangPath = LangPath(call.url)

case class OpenGraph(
    title: String,
    description: String,
    url: String,
    `type`: String = "website",
    image: Option[String] = None,
    twitterImage: Option[String] = None,
    siteName: String = "lichess.org",
    more: List[(String, String)] = Nil
)

case class Page(
    title: String,
    body: Option[Frag] = None,
    fullTitle: Option[String] = None,
    robots: Option[Boolean] = None,
    cssFrag: Option[Frag] = None,
    modules: EsmList = Nil,
    jsFrag: Option[WithNonce[Frag]] = None,
    pageModule: Option[PageModule] = None,
    playing: Boolean = false,
    openGraph: Option[OpenGraph] = None,
    zoomable: Boolean = false,
    zenable: Boolean = false,
    csp: Option[Update[ContentSecurityPolicy]] = None,
    wrapClass: String = "",
    atomLinkTag: Option[Tag] = None,
    withHrefLangs: Option[LangPath] = None,
    transform: Update[Frag] = identity
):
  def js(esm: EsmInit): Page       = copy(modules = modules :+ esm.some)
  def js(esm: EsmList): Page       = copy(modules = modules ::: esm)
  def js(f: WithNonce[Frag]): Page = copy(jsFrag = jsFrag.foldLeft(f)(_ |+| _).some)
  def js(pm: PageModule): Page     = copy(pageModule = pm.some)
  def graph(og: OpenGraph): Page   = copy(openGraph = og.some)
  def graph(title: String, description: String, url: String): Page = graph(OpenGraph(title, description, url))
  def robots(b: Boolean): Page                                     = copy(robots = b.some)
  def css(f: Frag): Page                           = copy(cssFrag = cssFrag.foldLeft(f)(_ |+| _).some)
  def csp(up: Update[ContentSecurityPolicy]): Page = copy(csp = csp.fold(up)(up.compose).some)
  def body(b: Frag): Page                          = copy(body = b.some)
  def apply(b: Frag): Page                         = copy(body = b.some)
  def wrap(f: Update[Frag]): Page                  = copy(transform = transform.compose(f))
