package lila.ui

import ScalatagsTemplate.*

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
    siteName: String = "lichess.org"
)

case class Page(
    title: String,
    body: Option[Frag] = None,
    fullTitle: Option[String] = None,
    robots: Boolean = true,
    cssKeys: List[String] = Nil,
    modules: EsmList = Nil,
    jsFrag: Option[WithNonce[Frag]] = None,
    pageModule: Option[PageModule] = None,
    playing: Boolean = false,
    openGraph: Option[OpenGraph] = None,
    zoomable: Boolean = false,
    zenable: Boolean = false,
    csp: Option[Update[ContentSecurityPolicy]] = None,
    fullScreenClass: Boolean = false,
    atomLinkTag: Option[Tag] = None,
    withHrefLangs: Option[LangPath] = None,
    transform: Update[Frag] = identity
):
  def js(esm: EsmInit): Page               = copy(modules = modules :+ esm.some)
  def js(esm: EsmList): Page               = copy(modules = modules ::: esm)
  def js(f: WithNonce[Frag]): Page         = copy(jsFrag = jsFrag.foldLeft(f)(_ |+| _).some)
  def js(f: Option[WithNonce[Frag]]): Page = f.foldLeft(this)(_.js(_))
  def js(pm: PageModule): Page             = copy(pageModule = pm.some)
  @scala.annotation.targetName("jsModuleOption")
  def js(pm: Option[PageModule]): Page                             = copy(pageModule = pm)
  def iife(iifeFrag: Frag): Page                                   = js(_ => iifeFrag)
  def iife(iifeFrag: Option[Frag]): Page                           = iifeFrag.foldLeft(this)(_.iife(_))
  def graph(og: OpenGraph): Page                                   = copy(openGraph = og.some)
  def graph(title: String, description: String, url: String): Page = graph(OpenGraph(title, description, url))
  def robots(b: Boolean): Page                                     = copy(robots = b)
  def css(keys: String*): Page                                     = copy(cssKeys = cssKeys ::: keys.toList)
  def css(key: Option[String]): Page                               = copy(cssKeys = cssKeys ::: key.toList)
  def csp(up: Update[ContentSecurityPolicy]): Page                 = copy(csp = csp.fold(up)(up.compose).some)
  def hrefLangs(path: Option[LangPath]): Page                      = copy(withHrefLangs = path)
  def hrefLangs(path: LangPath): Page                              = copy(withHrefLangs = path.some)
  def fullScreen: Page                                             = copy(fullScreenClass = true)
  def noRobot: Page                                                = robots(false)
  def zoom                                                         = copy(zoomable = true)
  def zen                                                          = copy(zenable = true)

  // body stuff
  def body(b: Frag): Page              = copy(body = b.some)
  def apply(b: Frag): Page             = copy(body = b.some)
  def transform(f: Update[Frag]): Page = copy(transform = transform.compose(f))
  def wrap(f: Update[Frag]): Page      = transform(f)
  def prepend(prelude: Frag): Page     = transform(body => frag(prelude, body))

final class RenderedPage(val html: String)

// when we want to return some random HTML and not a full page,
// usually during an XHR request
final class Snippet(val frag: Frag)
