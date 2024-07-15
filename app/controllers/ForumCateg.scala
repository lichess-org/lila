package controllers

import lila.app.{ *, given }
import lila.core.id.ForumCategId
import lila.forum.ForumCateg.{ diagnosticId, ublogId }

final class ForumCateg(env: Env) extends LilaController(env) with ForumController:

  def index = Open:
    NotForKids:
      for
        allTeamIds <- ctx.userId.so(env.team.cached.teamIdsList)
        teamIds <- allTeamIds.filterA:
          env.team.api.forumAccessOf(_).map(_ != lila.core.team.Access.None)
        categs <- postApi.categsForUser(teamIds, ctx.me)
        _      <- env.user.lightUserApi.preloadMany(categs.flatMap(_.lastPostUserId))
        page   <- renderPage(views.forum.categ.index(categs))
      yield Ok(page)

  def show(slug: ForumCategId, page: Int) = Open:
    if slug == ublogId && !isGrantedOpt(_.ModerateForum) then Redirect(routes.Ublog.communityAll())
    else if slug == diagnosticId && !isGrantedOpt(_.ModerateForum) then notFound
    else
      NotForKids:
        Reasonable(page, Max(50), notFound):
          Found(categApi.show(slug, page)): (categ, topics) =>
            for
              canRead     <- access.isGrantedRead(categ.id)
              canWrite    <- access.isGrantedWrite(categ.id)
              stickyPosts <- (page == 1).so(env.forum.topicApi.getSticky(categ, ctx.me))
              _ <- env.user.lightUserApi.preloadMany(topics.currentPageResults.flatMap(_.lastPostUserId))
              res <-
                if canRead then Ok.page(views.forum.categ.show(categ, topics, canWrite, stickyPosts))
                else notFound
            yield res

  def modFeed(slug: ForumCategId, page: Int) = Secure(_.ModerateForum) { ctx ?=> _ ?=>
    Found(env.forum.categRepo.byId(slug)): categ =>
      for
        posts     <- env.forum.paginator.recent(categ, page)
        postViews <- posts.mapFutureList(env.forum.postApi.views)
        page      <- renderPage(views.forum.categ.modFeed(categ, postViews))
      yield Ok(page)
  }
