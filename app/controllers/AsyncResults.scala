package controllers

import lila._

import play.api.mvc._
import play.api.http._
import play.api.libs.concurrent.Execution.Implicits._

trait AsyncResults { self: Controller ⇒

  protected def AsyncOk[A](op: Fu[A])(implicit writer: Writeable[A], ctype: ContentTypeOf[A]) = Async {
    op map { r ⇒ Ok(r) }
  }

  protected def AsyncRedirect(op: Fu[Call]) = Async {
    op map { Redirect(_) }
  }

  protected def AsyncRedirectUrl(op: Fu[String]) = Async {
    op map { Redirect(_) }
  }
}
