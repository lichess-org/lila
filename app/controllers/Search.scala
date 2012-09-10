package controllers

import lila._
import views._
import search.SearchData
import http.Context

import play.api.mvc.Result

object Search extends LilaController with BaseGame {

  private def indexer = env.search.indexer
  private def forms = env.search.forms

  def form(page: Int) = OpenBody { implicit ctx ⇒
    reasonable(page) {
      implicit def req = ctx.body
      forms.search.fill(SearchData()).bindFromRequest.fold(
        failure ⇒ Ok(html.search.form(makeListMenu, failure)),
        data ⇒ {
          Ok(html.search.form(
            makeListMenu,
            forms.search fill data,
            data.query.nonEmpty option env.search.paginator(data.query, page)
          ))
        }
      )
    }
  }
}
