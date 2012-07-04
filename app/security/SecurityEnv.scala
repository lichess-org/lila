package lila
package security

import user.{ User, UserRepo }
import core.Settings
import site.Captcha

import com.mongodb.casbah.MongoCollection

final class SecurityEnv(
    settings: Settings,
    captcha: Captcha,
    mongodb: String ⇒ MongoCollection,
    userRepo: UserRepo) {

  import settings._

  lazy val store = new security.Store(
    collection = mongodb(SecurityCollectionSecurity))

  lazy val firewall = new security.Firewall(
    collection = mongodb(FirewallCollectionFirewall),
    blockCookieName = FirewallBlockCookie,
    enabled = FirewallEnabled)

  lazy val flood = new security.Flood
  
  lazy val forms = new DataForm(
    userRepo = userRepo,
    captcher = captcha)
}
