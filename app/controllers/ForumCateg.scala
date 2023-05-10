package controllers

import lila.app.{ given, * }
import views.*
import lila.common.config
import lila.team.Team

final class ForumCateg(env: Env) extends LilaController(env) with ForumController:

  def index = Open:
    NotForKids:
      pageHit
      for
        allTeamIds <- ctx.userId ?? teamCache.teamIdsList
        teamIds <- lila.common.LilaFuture.filter(allTeamIds) {
          teamCache.forumAccess.get(_).map(_ != Team.Access.NONE)
        }
        categs <- postApi.categsForUser(teamIds, ctx.me)
        _      <- env.user.lightUserApi preloadMany categs.flatMap(_.lastPostUserId)
      yield html.forum.categ.index(categs)

  def show(slug: ForumCategId, page: Int) = Open:
    if slug == lila.forum.ForumCateg.ublogId
    then Redirect(routes.Ublog.communityAll()).toFuccess
    else
      NotForKids:
        Reasonable(page, config.Max(50), notFound):
          OptionFuResult(categApi.show(slug, ctx.me, page)): (categ, topics) =>
            for
              canRead     <- access.isGrantedRead(categ.id)
              canWrite    <- access.isGrantedWrite(categ.id)
              stickyPosts <- (page == 1) ?? env.forum.topicApi.getSticky(categ, ctx.me)
              _ <- env.user.lightUserApi preloadMany topics.currentPageResults.flatMap(_.lastPostUserId)
              res <-
                if (canRead) Ok(html.forum.categ.show(categ, topics, canWrite, stickyPosts)).toFuccess
                else notFound
            yield res
