package lila.irwin

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lila.db.Env
) {

  private val reportColl = db(config getString "collection.report")

  val api = new IrwinApi(
    reportColl = reportColl
  )
}

object Env {

  lazy val current: Env = "irwin" boot new Env(
    db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "irwin"
  )
}
