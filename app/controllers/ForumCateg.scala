package controllers

import lila.app._
import views._
import lila.common.config
import lila.team.Team

final class ForumCateg(env: Env) extends LilaController(env) with ForumController {

  def index =
    Open { implicit ctx =>
      pageHit
      NotForKids {
        for {
          allTeamIds <- ctx.userId ?? teamCache.teamIdsList
          teamIds <- lila.common.Future.filter(allTeamIds) {
            teamCache.forumAccess.get(_).map(_ != Team.Access.NONE)
          }
          categs <- postApi.categsForUser(teamIds, ctx.me)
          _      <- env.user.lightUserApi preloadMany categs.flatMap(_.lastPostUserId)
        } yield html.forum.categ.index(categs)
      }
    }

  def show(slug: String, page: Int) =
    Open { implicit ctx =>
      NotForKids {
        Reasonable(page, config.Max(50), notFound) {
          OptionFuResult(categApi.show(slug, ctx.me, page)) { case (categ, topics) =>
            for {
              canRead     <- access.isGrantedRead(categ.slug)
              canWrite    <- access.isGrantedWrite(categ.slug)
              stickyPosts <- (page == 1) ?? env.forum.topicApi.getSticky(categ, ctx.me)
              _ <- env.user.lightUserApi preloadMany topics.currentPageResults.flatMap(_.lastPostUserId)
              res <-
                if (canRead) Ok(html.forum.categ.show(categ, topics, canWrite, stickyPosts)).fuccess
                else notFound
            } yield res
          }
        }
      }
    }
}
