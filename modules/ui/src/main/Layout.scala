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
    title: String,
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
  def add(esm: EsmInit) = copy(modules = modules :+ esm.some)

object Layout:
  type Build = Layout => Layout
