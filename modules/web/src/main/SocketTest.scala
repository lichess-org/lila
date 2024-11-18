package lila.web

import play.api.libs.json.*

import lila.db.JSON
import lila.core.config.NetConfig
import lila.ui.Context

final class SocketTest(
    resultsDb: lila.db.AsyncCollFailingSilently,
    settingStore: lila.memo.SettingStore.Builder
)(using Executor):

  val distributionSetting = settingStore[Int](
    "socketTestDistribution",
    default = 0,
    text = "Participates to socket test if userId.hashCode % distribution == 0".some
  )

  def put(results: JsObject) = resultsDb: coll =>
    coll.insert.one(JSON.bdoc(results)).void

  def isTestRunning() = distributionSetting.get() > 0

  def isUserInTestBucket()(using ctx: Context) =
    isTestRunning() &&
      ctx.pref.usingAltSocket.isEmpty &&
      ctx.userId.exists(_.value.hashCode % distributionSetting.get() == 0)

  def socketEndpoints(net: NetConfig)(using ctx: Context): List[String] =
    ctx.pref.usingAltSocket.match
      case Some(true)                => net.socketAlts
      case Some(false)               => net.socketDomains
      case _ if isUserInTestBucket() => net.socketDomains.head :: net.socketAlts.headOption.toList
      case _                         => net.socketDomains
