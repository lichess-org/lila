package lila.web

import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.StandaloneWSClient
import scalalib.ThreadLocalRandom

final class InfluxEvent(
    ws: StandaloneWSClient,
    endpoint: String,
    env: String
)(using Executor):

  private val seed = ThreadLocalRandom.nextString(6)

  def start() = apply("lila_start", s"Lila starts: $seed")

  private def apply(key: String, text: String) =
    ws.url(endpoint)
      .post(s"""event,program=lila,env=$env,title=$key text="$text"""")
      .addEffects(
        err => logger.error(endpoint, err),
        res => if res.status != 204 then logger.error(s"$endpoint ${res.status}")
      )

  private def logger = lila.log("influxEvent")
