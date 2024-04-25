package lila.app
package templating

import lila.ui.*
import lila.web.ui.*

object Environment
    extends ScalatagsTemplate
    with RouterHelper
    with lila.ui.PaginatorHelper
    with lila.setup.SetupUi
    with lila.pref.PrefUi
    with SecurityHelper
    with TeamHelper
    with Helpers
    with AssetFullHelper:

  export lila.core.lilaism.Lilaism.{ *, given }
  export lila.common.extensions.*
  export lila.common.String.html.richText
  export lila.ui.Icon
  export lila.web.Nonce
  export lila.web.ui.{ PageModule, EsmList }
  export lila.api.Context.{ ctxToTranslate as _, *, given }
  export lila.api.PageData

  private var envVar: Option[Env] = None
  def setEnv(e: Env)              = envVar = Some(e)
  def env: Env                    = envVar.get

  def netConfig           = env.net
  def contactEmailInClear = env.net.email.value
  def picfitUrl           = env.memo.picfitUrl

  given lila.core.config.NetDomain              = env.net.domain
  given (using ctx: PageContext): Option[Nonce] = ctx.nonce

  def apiVersion = lila.security.Mobile.Api.currentVersion

  def explorerEndpoint       = env.explorerEndpoint
  def tablebaseEndpoint      = env.tablebaseEndpoint
  def externalEngineEndpoint = env.externalEngineEndpoint

  // helpers dependencies
  lazy val assetBaseUrl       = netConfig.assetBaseUrl
  lazy val netBaseUrl         = netConfig.baseUrl
  protected val ratingApi     = lila.rating.ratingApi
  protected lazy val flairApi = env.user.flairApi
  lazy val isOnline           = env.socket.isOnline
  lazy val lightUserSync      = env.user.lightUserSync
  def manifest                = env.web.manifest
  protected val jsDump        = lila.i18n.JsDump
  protected val translator    = lila.i18n.Translator
  protected val namer         = lila.game.Namer

  def helpers: Helpers             = this
  def assetHelper: AssetFullHelper = this

  def lightUserFallback           = env.user.lightUserSyncFallback
  def isStreaming(userId: UserId) = env.streamer.liveStreamApi.isStreaming(userId)

  def titleOrText(v: String)(using ctx: Context): Modifier = titleOrTextFor(ctx.blind, v)
