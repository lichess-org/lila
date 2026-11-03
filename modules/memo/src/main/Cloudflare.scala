package lila.memo

import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.{ StandaloneWSClient, StandaloneWSRequest, StandaloneWSResponse }

final class CloudflareApi(ws: StandaloneWSClient, config: CloudflareConfig)(using Executor):

  if config.alertPolicyId.nonEmpty then
    lila.common.Bus.sub[lila.core.socket.Announce]:
      case lila.core.socket.Announce(msg, date, _) if msg.contains("will restart") =>
        request(s"accounts/${config.accountId}/alerting/v3/silences"):
          _.post:
            Json.arr:
              Json.obj(
                "policy_id" -> config.alertPolicyId,
                "start_time" -> date.minusSeconds(30).toString,
                "end_time" -> date.plusMinutes(2).toString // I'm around during planned deploys anyway
              )

  def purge(urls: List[String]): Funit = urls.nonEmpty.so:
    request(s"zones/${config.zoneId}/purge_cache"):
      _.post(Json.obj("files" -> urls))

  def request(path: String)(send: StandaloneWSRequest => Fu[StandaloneWSResponse]): Funit =
    if config.zoneId.isEmpty || config.accountId.isEmpty || config.apiToken.value.isEmpty
    then fuccess(lila.log.system.info(s"Cloudflare API request skipped: $path"))
    else
      lila.log.system.info(s"Sending Cloudflare API request: $path")
      send:
        ws.url(s"https://api.cloudflare.com/client/v4/$path")
          .addHttpHeaders(
            "Authorization" -> s"Bearer ${config.apiToken.value}",
            "Content-Type" -> "application/json"
          )
      .flatMap: rsp =>
        if rsp.status / 100 != 2 then fufail(lila.log.http(rsp))
        else funit
      .recover:
        case err: Exception =>
          lila.log.system.error(s"""Cloudflare API request "$path" failed: ${err.getMessage}""")
          ()
