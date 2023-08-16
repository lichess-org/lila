package lila.blog

import lila.timeline.EntryApi

final private[blog] class Notifier(
    blogApi: BlogApi,
    timelineApi: EntryApi
)(using Executor):

  def apply(id: String): Funit =
    blogApi.prismicApi flatMap { prismicApi =>
      blogApi.one(prismicApi, none, id) orFail
        s"No such document: $id" flatMap doSend
    }

  private def doSend(post: BlogPost): Funit =
    post.getText("blog.title") so { title =>
      timelineApi.broadcast.insert:
        lila.hub.actorApi.timeline.BlogPost(id = post.id, slug = post.slug, title = title)
    }
