package lila.blog

import lila.hub.actorApi.timeline.BlogPost
import lila.timeline.EntryApi

final private[blog] class Notifier(
    blogApi: BlogApi,
    timelineApi: EntryApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(id: String): Funit =
    timelineApi.broadcast.insert(BlogPost(id))

}
