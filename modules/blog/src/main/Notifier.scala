package lila.blog

import io.prismic.Document

import lila.hub.actorApi.timeline.BlogPost
import lila.timeline.EntryApi

final private[blog] class Notifier(
    blogApi: BlogApi,
    timelineApi: EntryApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(id: String): Funit =
    blogApi.prismicApi flatMap { prismicApi =>
      blogApi.one(prismicApi, none, id) orFail
        s"No such document: $id" flatMap doSend
    }

  private def doSend(post: Document): Funit =
    post.getText("blog.title") ?? { title =>
      timelineApi.broadcast.insert {
        BlogPost(id = post.id, slug = post.slug, title = title)
      }
    }
}
