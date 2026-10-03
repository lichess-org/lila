package lila.event

import play.api.libs.json.*

import lila.core.config.RouteUrl
import lila.common.Json.given

final class JsonView(routeUrl: RouteUrl):

  def calendar(e: Event): JsObject =
    Json
      .obj(
        "title" -> e.title,
        "enabled" -> e.enabled,
        "start" -> e.startsAt,
        "end" -> e.finishesAt,
        "homepageHours" -> e.homepageHours,
        "url" -> e.url,
        "language" -> e.lang,
        "createdBy" -> e.createdBy,
        "manage" -> routeUrl(routes.Event.edit(e.id))
      )
      .add("hostedBy" -> e.hostedBy)
