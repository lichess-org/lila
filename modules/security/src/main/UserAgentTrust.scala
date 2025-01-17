package lila.security

import play.api.mvc.RequestHeader
import lila.common.HTTPRequest
import lila.core.net.UserAgent

object UserAgentTrust:

  def isSuspicious(req: RequestHeader): Boolean = HTTPRequest.userAgent(req).forall(isSuspicious)

  def isSuspicious(ua: UserAgent): Boolean =
    ua.value.lengthIs < 30 || !looksNormal(ua)

  private def looksNormal(ua: UserAgent) =
    val sections = ua.value.toLowerCase.split(' ')
    sections.exists: s =>
      isRecentChrome(s) || isRecentFirefox(s) || isRecentSafari(s)

  // based on https://caniuse.com/usage-table
  private val isRecentChrome  = isRecentBrowser("chrome", 109) // also covers Edge and Opera
  private val isRecentFirefox = isRecentBrowser("firefox", 128)
  private val isRecentSafari  = isRecentBrowser("safari", 605) // most safaris also have a chrome/ section

  private def isRecentBrowser(name: String, minVersion: Int): String => Boolean =
    val slashed      = name + "/"
    val prefixLength = slashed.length
    (s: String) =>
      s.startsWith(slashed) &&
        s.drop(prefixLength).takeWhile(_ != '.').toIntOption.exists(_ >= minVersion)
