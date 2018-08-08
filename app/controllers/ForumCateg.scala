package controllers

import lidraughts.app._
import views._

object ForumCateg extends LidraughtsController with ForumController {

  def index = Open { implicit ctx =>
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
              stickyPosts <- (page == 1) ?? lidraughts.forum.Env.current.topicApi.getSticky(categ, ctx.troll)
              _ <- Env.user.lightUserApi preloadMany topics.currentPageResults.flatMap(_.lastPostUserId)
            } yield html.forum.categ.show(categ, topics, canWrite, stickyPosts)
          }
        }
      }
    }
  }
}
