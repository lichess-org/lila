package lila.api

import play.api.libs.json.*

import lila.db.JSON
import lila.core.config.NetConfig

final class SocketTestResult(resultsDb: lila.db.AsyncCollFailingSilently)(using Executor):
  def put(results: JsObject) = resultsDb: coll =>
    coll.insert.one(JSON.bdoc(results)).void

object SocketTest:

  def isUserInTestBucket(net: NetConfig)(using ctx: Context) =
    net.socketTest && ctx.pref.usingAltSocket.isEmpty && ctx.userId.exists: userId =>
      java.security.MessageDigest.getInstance("MD5").digest(userId.value.getBytes).head % 128 == 0

  def socketEndpoints(net: NetConfig)(using ctx: Context): List[String] =
    ctx.pref.usingAltSocket.match
      case Some(true)  => net.socketAlts
      case Some(false) => net.socketDomains
      case _ if isUserInTestBucket(net) =>
        (net.socketDomains.head :: net.socketAlts.headOption.toList)
      case _ => net.socketDomains
