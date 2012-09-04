package controllers

import lila._
import views._
import http.Context

import play.api.mvc.Result

object Search extends LilaController with BaseGame {

  val forms = env.search.forms

  val form = Open { implicit ctx ⇒
    Ok(html.search.form(makeListMenu, forms.search))
  }
}
