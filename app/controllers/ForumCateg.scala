package controllers

import lila.app.{ given, * }
import views.*
import lila.common.config
import lila.team.Team

final class ForumCateg(env: Env) extends LilaController(env) with ForumController:

  def index = Open:
    NotForKids:
      for
        allTeamIds <- ctx.userId so teamCache.teamIdsList
        teamIds <- allTeamIds.filterA:
          teamCache.forumAccess.get(_).map(_ != Team.Access.NONE)
        categs <- postApi.categsForUser(teamIds, ctx.me)
        _      <- env.user.lightUserApi preloadMany categs.flatMap(_.lastPostUserId)
        page   <- renderPage(html.forum.categ.index(categs))
      yield Ok(page)

  def show(slug: ForumCategId, page: Int) = Open:
    if slug == lila.forum.ForumCateg.ublogId
    then Redirect(routes.Ublog.communityAll())
    else
      NotForKids:
        Reasonable(page, config.Max(50), notFound):
          Found(categApi.show(slug, page)): (categ, topics) =>
            for
              canRead     <- access.isGrantedRead(categ.id)
              canWrite    <- access.isGrantedWrite(categ.id)
              stickyPosts <- (page == 1) so env.forum.topicApi.getSticky(categ, ctx.me)
              _ <- env.user.lightUserApi preloadMany topics.currentPageResults.flatMap(_.lastPostUserId)
              res <-
                if canRead then Ok.page(html.forum.categ.show(categ, topics, canWrite, stickyPosts))
                else notFound
            yield res
