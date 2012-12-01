package lila
package mod

import user.User

import scala.concurrent.Future

final class ModlogApi(repo: ModlogRepo) {

  def engine(mod: User, user: User, v: Boolean) = add {
    Modlog(mod.id, user.id.some, v.fold(Modlog.engine, Modlog.unengine))
  }

  def mute(mod: User, user: User, v: Boolean) = add {
    Modlog(mod.id, user.id.some, v.fold(Modlog.mute, Modlog.unmute))
  }

  def ban(mod: User, user: User) = add {
    Modlog(mod.id, user.id.some, Modlog.ipban)
  }

  def ipban(mod: User, ip: String) = add {
    Modlog(mod.id, none, Modlog.ipban, ip.some)
  }

  def deletePost(mod: User, userId: Option[String], author: Option[String], ip: Option[String], text: String) = add {
    Modlog(mod.id, userId, Modlog.deletePost, details = Some(
      author.fold("")(_ + " ") + ip.fold("")(_ + " ") + text.take(140)
    ))
  }

  def recent = repo recent 100

  private def add(modLog: Modlog): Future[Unit] = repo insert modLog
}
