package controllers

import lila.app._
import views._

object ForumCateg extends LilaController with ForumController {

  def index = Open { implicit ctx =>
    pageHit
    NotForKids {
      for {
        teamIds <- ctx.userId ?? teamCache.teamIdsList
        categs <- categApi.list(teamIds, ctx.troll)
        _ <- Env.user.lightUserApi preloadMany categs.flatMap(_.lastPostUserId)
      } yield html.forum.categ.index(categs)
    }
  }

  def show(slug: String, page: Int) = Open { implicit ctx =>
    NotForKids {
      Reasonable(page, 50, errorPage = notFound) {
        CategGrantRead(slug) {
          OptionFuOk(categApi.show(slug, page, ctx.troll)) {
            case (categ, topics) => for {
              canWrite <- isGrantedWrite(categ.slug)
              stickyPosts <- (page == 1) ?? lila.forum.Env.current.topicApi.getSticky(categ, ctx.troll)
              _ <- Env.user.lightUserApi preloadMany topics.currentPageResults.flatMap(_.lastPostUserId)
            } yield html.forum.categ.show(categ, topics, canWrite, stickyPosts)
          }
        }
      }
    }
  }
}
