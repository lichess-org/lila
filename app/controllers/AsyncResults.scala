package controllers

import lila._

import play.api.mvc._
import play.api.http._
import play.api.libs.concurrent.Execution.Implicits._

trait AsyncResults { self: Controller ⇒

  protected def AsyncOk[A, B](fu: Fu[A])(op: A ⇒ B)(implicit writer: Writeable[B], ctype: ContentTypeOf[B]) = Async {
    fu map op map { x ⇒ Ok(x) }
  }

  protected def AsyncUnit[A](fu: Fu[A]) = Async {
    fu map { _ ⇒ Ok("ok") }
  }

  protected def AsyncRedirect(op: Fu[Call]) = Async {
    op map { Redirect(_) }
  }

  protected def AsyncRedirect(op: Funit)(call: Call) = Async {
    op map { _ ⇒ Redirect(call) }
  }

  protected def AsyncRedirectUrl(op: Fu[String]) = Async {
    op map { Redirect(_) }
  }
}
