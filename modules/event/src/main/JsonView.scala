package lila.event

import play.api.libs.json.*

import lila.core.config.RouteUrl

final class JsonView(routeUrl: RouteUrl):

  def calendar(e: Event): JsObject =
    Json
      .obj(
        "title" -> e.title,
        "enabled" -> e.enabled,
        "start" -> isoDateTimeFormatter.print(e.startsAt),
        "end" -> isoDateTimeFormatter.print(e.finishesAt),
        "homepageHours" -> e.homepageHours,
        "url" -> e.url,
        "language" -> e.lang.value,
        "createdBy" -> e.createdBy.value,
        "manage" -> routeUrl(routes.Event.edit(e.id)).value
      )
      .add("hostedBy" -> e.hostedBy.map(_.value))
