package lila.web

import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.StandaloneWSClient
import play.api.http.HeaderNames.*

final private class PagerDuty(ws: StandaloneWSClient, config: WebConfig.PagerDuty)(using Executor):

  lila.common.Bus.sub[lila.core.socket.Announce]:
    case lila.core.socket.Announce(msg, date, _) if msg.contains("will restart") =>
      lilaRestart(date)

  def lilaRestart(date: Instant): Funit =
    (config.serviceId.nonEmpty && config.apiKey.value.nonEmpty).so:
      ws.url("https://api.pagerduty.com/maintenance_windows")
        .withHttpHeaders(
          AUTHORIZATION -> s"Token token=${config.apiKey.value}",
          CONTENT_TYPE -> "application/json",
          ACCEPT -> "application/vnd.pagerduty+json;version=2"
        )
        .post:
          Json.obj(
            "maintenance_window" -> Json.obj(
              "type" -> "maintenance_window",
              "start_time" -> isoDateTimeFormatter.print(date),
              "end_time" -> isoDateTimeFormatter.print(date.plusMinutes(3)),
              "description" -> "restart announce",
              "services" -> Json.arr(
                Json.obj(
                  "id" -> config.serviceId,
                  "type" -> "service_reference"
                )
              )
            )
          )
        .addEffects(
          err => logger.error("lilaRestart failed", err),
          res =>
            if res.status != 201 then
              println(res.body)
              logger.warn(s"lilaRestart status=${res.status}")
        )
        .void

  private def logger = lila.log("pagerDuty")
