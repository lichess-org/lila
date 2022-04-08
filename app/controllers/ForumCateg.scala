package controllers

import lila.app._
import lila.common.config.MaxPerPage
import lila.common.paginator.Paginator
import lila.forum.TopicView
import views._

import scala.concurrent.Future

final class ForumCateg(env: Env) extends LilaController(env) with ForumController {

  def index =
    Open { implicit ctx =>
      pageHit
      NotForKids {
        for {
          teamIds <- ctx.userId ?? teamCache.teamIdsList
          categs  <- postApi.categsForUser(teamIds, ctx.me)
          _       <- env.user.lightUserApi preloadMany categs.flatMap(_.lastPostUserId)
        } yield html.forum.categ.index(categs)
      }
    }

  def show(slug: String, page: Int) =
    OpenBody { implicit ctx =>
      NotForKids {
        OptionFuResult(categApi.show(slug, page, ctx.me)) { case (categ, topics) =>
          for {
            canRead     <- access.isGrantedRead(slug)
            canWrite    <- access.isGrantedWrite(slug)
            stickyPosts <- (page == 1) ?? env.forum.topicApi.getSticky(categ, ctx.me)
            _           <- env.user.lightUserApi preloadMany topics.currentPageResults.flatMap(_.lastPostUserId)
            res <-
              if (canRead) Ok(html.forum.categ.show(categ, topics, canWrite, stickyPosts)).fuccess
              else notFound
          } yield res
        }
      }
    }
}
