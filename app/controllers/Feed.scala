package controllers

import play.api.mvc.*

import lila.app.{ *, given }

final class Feed(env: Env) extends LilaController(env):

  def api = env.feed.api

  def index(page: Int) = Open: ctx ?=>
    Reasonable(page):
      for
        updates <- env.feed.paginator.recent(isGrantedOpt(_.Feed), page)
        comments <- env.forum.topicApi.feedTopics(updates.currentPageResults)
        _ <- env.user.lightUserApi.preloadMany(comments.values.flatMap(_.lastCommentBy).toSeq)
        renderedPage <- renderPage:
          views.feed.index(
            updates.mapResults(update => lila.feed.Feed.View(update, comments.get(update.id)))
          )
      yield Ok(renderedPage)

  def createForm = Secure(_.Feed) { _ ?=> _ ?=>
    Ok.async(views.feed.create(api.form(none)))
  }

  def create = SecureBody(_.Feed) { _ ?=> _ ?=>
    bindForm(api.form(none))(
      err => BadRequest.async(views.feed.create(err)),
      data =>
        val update = data.toUpdate(none)
        for
          topicId <-
            if data.comments then env.forum.topicApi.makeFeedTopic(update).map(_.id.some)
            else fuccess(none)
          saved = update.copy(topicId = topicId)
          _ <- api.set(saved)
        yield Redirect(routes.Feed.edit(saved.id)).flashSuccess
    )
  }

  def edit(id: String) = Secure(_.Feed) { _ ?=> _ ?=>
    Found(api.get(id)): up =>
      Ok.async(views.feed.edit(api.form(up.some), up))
  }

  def update(id: String) = SecureBody(_.Feed) { _ ?=> _ ?=>
    Found(api.get(id)): from =>
      bindForm(api.form(from.some))(
        err => BadRequest.async(views.feed.edit(err, from)),
        data =>
          val update = data.toUpdate(from.id.some, from.topicId)
          for
            topicId <-
              if data.comments && from.topicId.isEmpty then
                env.forum.topicApi.makeFeedTopic(update).map(_.id.some)
              else fuccess(none)
            saved = update.copy(topicId = topicId.orElse(from.topicId))
            _ <- api.set(saved)
            _ <- env.forum.topicApi.updateFeedTopic(saved, data.comments)
          yield Redirect(routes.Feed.edit(saved.id)).flashSuccess
      )
  }

  def delete(id: String) = Secure(_.Feed) { _ ?=> _ ?=>
    Found(api.get(id)): up =>
      up.topicId.so(_ =>
        env.forum.topicApi.removeTopic(lila.forum.ForumCateg.feedId, lila.core.id.ForumTopicSlug(up.id))
      ) >>
        api.delete(up.id).inject(Redirect(routes.Feed.index(1)).flashSuccess)
  }

  def atom = Anon:
    api.recentPublished.map: ups =>
      Ok.snip(views.feed.atom(ups)).as(XML)
