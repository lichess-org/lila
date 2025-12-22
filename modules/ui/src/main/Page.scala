package lila.ui

import lila.core.i18n.I18nModule
import ScalatagsTemplate.*

opaque type LangPath = String
object LangPath extends OpaqueString[LangPath]:
  def apply(call: play.api.mvc.Call): LangPath = LangPath(call.url)

case class OpenGraph(
    title: String,
    description: String,
    url: Url,
    `type`: String = "website",
    image: Option[Url] = None,
    siteName: String = "lichess.org"
)

enum PageFlags:
  case noRobots, playing, zoom, zen, fullScreen, noHeader

case class Page(
    title: String,
    body: Option[Frag] = None,
    fullTitle: Option[String] = None,
    cssKeys: List[String] = Nil,
    i18nModules: List[I18nModule.Selector] = List(_.site, _.timeago, _.preferences),
    modules: EsmList = Nil,
    pageModule: Option[PageModule] = None,
    openGraph: Option[OpenGraph] = None,
    csp: Option[Update[ContentSecurityPolicy]] = None,
    atomLinkTag: Option[Tag] = None,
    withHrefLangs: Option[LangPath] = None,
    flags: Set[PageFlags] = Set.empty,
    transform: Update[Frag] = identity,
    transformHead: Update[Frag] = identity
):
  def js(esm: Esm): Page = copy(modules = modules :+ esm.some)
  def js(esm: EsmList): Page = copy(modules = modules ::: esm)
  def js(pm: PageModule): Page = copy(pageModule = pm.some)
  @scala.annotation.targetName("jsModuleOption")
  def js(pm: Option[PageModule]): Page = copy(pageModule = pm)
  def i18n(mods: I18nModule.Selector*): Page = copy(i18nModules = i18nModules ::: mods.toList)
  def i18nOpt(cond: Boolean, mods: => I18nModule.Selector*): Page =
    if cond then copy(i18nModules = i18nModules.appendedAll(mods)) else this
  def graph(og: OpenGraph): Page = copy(openGraph = og.some)
  def graph(title: String, description: String, url: Url): Page = graph(OpenGraph(title, description, url))
  def flag(f: PageFlags.type => PageFlags, v: Boolean = true): Page =
    copy(flags = if v then flags + f(PageFlags) else flags - f(PageFlags))
  def css(keys: String*): Page = copy(cssKeys = cssKeys ::: keys.toList)
  def css(key: Option[String]): Page = copy(cssKeys = cssKeys ::: key.toList)
  def csp(up: Update[ContentSecurityPolicy]): Page = copy(csp = csp.fold(up)(up.compose).some)
  def hrefLangs(path: Option[LangPath]): Page = copy(withHrefLangs = path)
  def hrefLangs(path: LangPath): Page = copy(withHrefLangs = path.some)
  def transformHead(f: Update[Frag]): Page = copy(transformHead = transformHead.compose(f))
  def preloadImage(url: Url)(helper: AssetHelper): Page =
    transformHead(head => frag(head, helper.imagePreload(url)))
  def preloadImage(url: Option[Url])(helper: AssetHelper): Page =
    url.fold(this)(preloadImage(_)(helper))

  // body stuff
  def body(b: Frag): Page = copy(body = b.some)
  def apply(b: Frag): Page = copy(body = b.some)
  def transform(f: Update[Frag]): Page = copy(transform = transform.compose(f))
  def wrap(f: Update[Frag]): Page = transform(f)
  def prepend(prelude: Frag): Page = transform(body => frag(prelude, body))
  def append(postlude: Frag): Page = transform(body => frag(body, postlude))

  def markdownTextarea = css("bits.markdownTextarea").js(Esm("bits.markdownTextarea"))

final class RenderedPage(val html: String)

// when we want to return some random HTML and not a full page,
// usually during an XHR request
final class Snippet(val frag: Frag)
