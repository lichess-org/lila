package lila.report

import akka.actor._
import akka.pattern.pipe
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val CollectionReport = config getString "collection.report"

  lazy val api = new ReportApi

  private[report] lazy val reportColl = db(CollectionReport)
}

object Env {

  private def app = play.api.Play.current

  lazy val current = "[boot] report" describes new Env(
    config = lila.common.PlayApp loadConfig "report",
    db = lila.db.Env.current)
}
