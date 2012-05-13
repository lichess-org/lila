package lila
package http

import ui.Color
import db.UserRepo

import play.api.mvc.Session
import scalaz.effects._

final class Setting(ctx: Context) {

  def color = Color(get("color"))

  def color(value: String) = set("color", Color(value)) _

  private def get(name: String, default: String = ""): String =
    ctx.req.session.get(name) orElse {
      ctx.me flatMap (_ setting name)
    } getOrElse default

  private def set(name: String, value: Any)(userRepo: UserRepo): IO[Session] =
    ctx.me.fold(
      m ⇒ userRepo.saveSetting(m, name, value.toString),
      io()
    ) map { _ ⇒ ctx.req.session + (name -> value.toString) }
}

object Setting {

  def apply(ctx: Context) = new Setting(ctx)
}
