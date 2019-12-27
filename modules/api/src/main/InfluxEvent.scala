package lila.api

import akka.actor._
import play.api.libs.ws.WSClient

import lila.common.Bus

final private class InfluxEvent(
    ws: WSClient,
    endpoint: String,
    env: String
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val seed = ornicar.scalalib.Random.nextString(6)

  def start() = apply("lila_start", s"Lila starts: $seed")

  private def apply(key: String, text: String) =
    ws.url(endpoint)
      .post(s"""event,program=lila,env=$env,title=$key text="$text"""")
      .effectFold(
        err => lila.log("influxEvent").error(endpoint, err),
        res => if (res.status != 204) lila.log("influxEvent").error(s"$endpoint ${res.status}")
      )
}
