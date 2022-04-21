package controllers

import lila.app._
import views._

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
    Open { implicit ctx =>
      NotForKids {
        Reasonable(page, 50, errorPage = notFound) {
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
