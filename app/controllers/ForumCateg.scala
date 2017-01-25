package controllers

import lila.app._
import views._

object ForumCateg extends LilaController with ForumController {

  def index = Open { implicit ctx =>
    NotForKids {
      for {
        teamIds <- ctx.userId ?? teamCache.teamIds
        categs <- categApi.list(teamIds, ctx.troll)
      } yield html.forum.categ.index(categs)
    }
  }

  def show(slug: String, page: Int) = Open { implicit ctx =>
    NotForKids {
      Reasonable(page, 50, errorPage = notFound) {
        CategGrantRead(slug) {
          OptionFuOk(categApi.show(slug, page, ctx.troll)) {
            case (categ, topics) =>
              isGrantedWrite(categ.slug) map { canWrite =>
                html.forum.categ.show(categ, topics, canWrite)
              }
          }
        }
      }
    }
  }
}
