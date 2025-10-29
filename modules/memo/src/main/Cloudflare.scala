package lila.memo

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.json.*

private final class CloudflareApi(ws: StandaloneWSClient, config: CloudflareConfig)(using Executor):

  def purge(urls: List[String]): Funit =
    if urls.isEmpty || config.zoneId.isEmpty || config.apiToken.value.isEmpty then funit
    else
      ws.url(s"https://api.cloudflare.com/client/v4/zones/${config.zoneId}/purge_cache")
        .addHttpHeaders(
          "Authorization" -> s"Bearer ${config.apiToken.value}",
          "Content-Type" -> "application/json"
        )
        .post(Json.obj("files" -> urls))
        .flatMap:
          case r if r.status / 100 == 2 => funit
          case r =>
            logger
              .branch("cloudflare")
              .warn(s"purge ${urls.mkString(", ")} failed with: ${r.status} ${r.body[String].take(200)}")
            funit
