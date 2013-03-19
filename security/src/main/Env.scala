package lila.security

import lila.db.ReactiveColl
import lila.user.{ User, UserRepo }
// import site.Captcha

import com.typesafe.config.Config

final class Env(
    config: Config,
    // captcha: Captcha,
    db: lila.db.Env,
    userRepo: UserRepo) {

  val settings = new Settings(config)
  import settings._

  lazy val api = new Api(
    store = store,
    userRepo = userRepo)

  lazy val store = new Store(
    coll = db(CollectionSecurity))

  lazy val firewall = new Firewall(
    coll = db(FirewallCollectionFirewall),
    cookieName = FirewallCookieName.some filter (_ ⇒ FirewallCookieEnabled),
    enabled = FirewallEnabled)

  lazy val flood = new Flood

  lazy val wiretap = new Wiretap(WiretapIps)

  // lazy val forms = new DataForm(
  //   userRepo = userRepo,
  //   captcher = captcha)
}

object Env {

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "security",
    db = lila.db.Env.current,
    userRepo = lila.user.Env.current.userRepo)
}
