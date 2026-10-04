package lila.feed

import play.api.libs.json.*
import lila.common.Json.given
import lila.core.lilaism.Lilaism.add

object FeedJsonView:

  given OWrites[Feed.Update] = OWrites: u =>
    Json
      .obj(
        "id" -> u.id,
        "at" -> u.at,
        "url" -> s"${routes.Feed.index().url}#${u.id}",
        "contentHtml" -> u.rendered.value
      )
      .add("flair", u.flair)
