package lila.feed

import play.api.libs.json.*
import lila.common.Json.given
import lila.core.lilaism.Lilaism.add
import lila.core.config.RouteUrl

private final class FeedJsonView(routeUrl: RouteUrl):

  val baseUrl = routeUrl(routes.Feed.index())

  given OWrites[Feed.Update] = OWrites: u =>
    Json
      .obj(
        "id" -> u.id,
        "at" -> u.at,
        "url" -> s"$baseUrl#${u.id}",
        "contentHtml" -> u.rendered.value
      )
      .add("flair", u.flair)
