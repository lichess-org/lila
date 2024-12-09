package lila.app

import lila.ui.*
import lila.web.ui.*

object UiEnv
    extends ScalatagsTemplate
    with lila.pref.PrefHelper
    with SecurityHelper
    with TeamHelper
    with Helpers
    with AssetFullHelper:

  export lila.core.lilaism.Lilaism.{ *, given }
  export lila.core.id.ImageId
  export lila.common.extensions.*
  export lila.common.String.html.richText
  export lila.ui.{ Page, Nonce, OpenGraph, PageModule, EsmList, Icon }
  export lila.api.Context.{ ctxToTranslate as _, *, given }

  private var envVar: Option[Env] = None
  def setEnv(e: Env)              = envVar = Some(e)
  def env: Env                    = envVar.get

  def netConfig           = env.net
  def contactEmailInClear = env.net.email.value
  def picfitUrl           = env.memo.picfitUrl
  def socketTest          = env.web.socketTest

  given lila.core.config.NetDomain                           = env.net.domain
  given (using ctx: PageContext): Option[Nonce]              = ctx.nonce
  given Conversion[lila.team.Team, lila.core.team.LightTeam] = _.light

  def apiVersion = lila.security.Mobile.Api.currentVersion

  def explorerEndpoint       = env.explorerEndpoint
  def tablebaseEndpoint      = env.tablebaseEndpoint
  def externalEngineEndpoint = env.externalEngineEndpoint

  // helpers dependencies
  lazy val assetBaseUrl            = netConfig.assetBaseUrl
  lazy val netBaseUrl              = netConfig.baseUrl
  protected val ratingApi          = lila.rating.ratingApi
  protected lazy val flairApi      = env.user.flairApi
  lazy val isOnline                = env.socket.isOnline
  lazy val lightUserSync           = env.user.lightUserSync
  def manifest                     = env.web.manifest
  protected val translator         = lila.i18n.Translator
  val langList                     = lila.i18n.LangList
  protected val namer              = lila.game.Namer
  protected lazy val lightTeamSync = env.team.lightTeamSync
  protected lazy val syncBelongsTo = env.team.api.syncBelongsTo

  def helpers: Helpers                 = this
  def assetHelper: AssetFullHelper     = this
  def prefHelper: lila.pref.PrefHelper = this

  def flagApi = lila.user.Flags

  def lightUserFallback           = env.user.lightUserSyncFallback
  def isStreaming(userId: UserId) = env.streamer.liveStreamApi.isStreaming(userId)
