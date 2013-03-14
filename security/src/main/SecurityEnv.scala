package lila.security

import lila.db.ReactiveColl
import lila.user.{ User, UserRepo }
// import site.Captcha

import com.typesafe.config.Config

final class SecurityEnv(
    config: Config,
    // captcha: Captcha,
    db: String ⇒ ReactiveColl,
    userRepo: UserRepo) {

  val settings = new Settings(config)
  import settings._

  // lazy val store = new security.Store(
  //   collection = mongodb(SecurityCollectionSecurity))

  // lazy val firewall = new security.Firewall(
  //   collection = mongodb(FirewallCollectionFirewall),
  //   cookieName = FirewallCookieName.some filter (_ ⇒ FirewallCookieEnabled),
  //   enabled = FirewallEnabled)

  // lazy val flood = new security.Flood

  // lazy val wiretap = new security.Wiretap(SecurityWiretapIps.toSet)

  // lazy val forms = new DataForm(
  //   userRepo = userRepo,
  //   captcher = captcha)
}
