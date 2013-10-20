package lila.pref

import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env) {

  def forms = new DataForm(api)

  lazy val api = new PrefApi

  private val CollectionPref = config getString "collection.pref"

  private[pref] lazy val prefColl = db(CollectionPref)
}

object Env {

  private def app = play.api.Play.current

  lazy val current = "[boot] pref" describes new Env(
    config = lila.common.PlayApp loadConfig "pref",
    db = lila.db.Env.current)
}
