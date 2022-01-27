package lila.api

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables._
import play.api.libs.ws.StandaloneWSClient

import lila.hub.actorApi.Announce

final private class PagerDuty(ws: StandaloneWSClient, config: ApiConfig.PagerDuty)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  def lilaRestart(date: DateTime): Funit =
    (config.serviceId.nonEmpty && config.apiKey.value.nonEmpty) ??
      ws.url("https://api.pagerduty.com/maintenance_windows")
        .withHttpHeaders(
          "Authorization" -> s"Token token=${config.apiKey.value}",
          "Content-type"  -> "application/json",
          "Accept"        -> "application/vnd.pagerduty+json;version=2"
        )
        .post(
          Json
            .obj(
              "maintenance_window" -> Json.obj(
                "type"        -> "maintenance_window",
                "start_time"  -> formatDate(date),
                "end_time"    -> formatDate(date.plusMinutes(3)),
                "description" -> "restart announce",
                "services" -> Json.arr(
                  Json.obj(
                    "id"   -> config.serviceId,
                    "type" -> "service_reference"
                  )
                )
              )
            )
        )
        .addEffects(
          err => logger.error("lilaRestart failed", err),
          res =>
            if (res.status != 201) {
              println(res.body)
              logger.warn(s"lilaRestart status=${res.status}")
            }
        )
        .void

  private lazy val logger = lila.log("pagerDuty")

  private def formatDate(date: DateTime) = ISODateTimeFormat.dateTime print date
}
