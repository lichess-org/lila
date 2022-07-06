package controllers

import scala.concurrent.Future
import lila.app._
import views._
import lila.common.config
import lila.team.{ Fu, Team }

final class ForumCateg(env: Env) extends LilaController(env) with ForumController {

  private def filterHiddenForum(id: Team.ID): Fu[Option[String]] =
    teamCache.forumAccess.get(id) map {
      case a if a != Team.Access.NONE => Option(id)
      case _                          => None
    }

  def index =
    Open { implicit ctx =>
      pageHit
      NotForKids {
        for {
          teamIds           <- ctx.userId ?? teamCache.teamIdsList
          visibleTeamForums <- Future.sequence { teamIds map filterHiddenForum }
          categs            <- postApi.categsForUser(visibleTeamForums flatten, ctx.me)
          _                 <- env.user.lightUserApi preloadMany categs.flatMap(_.lastPostUserId)
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
