package controllers

import lila._
import views._
import http.Context

import play.api.mvc.Result

object Search extends LilaController with BaseGame {

  val forms = env.search.forms

  val form = OpenBody { implicit ctx ⇒
    val f = forms.search
    implicit def req = ctx.body
    f.bindFromRequest.fold(
      failure ⇒ throw new Exception(failure.toString),
      data ⇒ {
        Ok(html.search.form(makeListMenu, f fill data))
      }
    )
  }
}
