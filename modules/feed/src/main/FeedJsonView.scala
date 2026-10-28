package lila.feed

import play.api.libs.json.*

import lila.common.Json.given
import lila.core.config.RouteUrl
import lila.core.data.Html
import lila.core.lilaism.Lilaism.add

private final class FeedJsonView(routeUrl: RouteUrl):

  val baseUrl = routeUrl(routes.Feed.index())

  given OWrites[(Feed.Update, Html)] = OWrites: (u, rendered) =>
    Json
      .obj(
        "id" -> u.id,
        "at" -> u.at,
        "url" -> s"$baseUrl#${u.id}",
        "contentHtml" -> rendered
      )
      .add("flair", u.flair)
