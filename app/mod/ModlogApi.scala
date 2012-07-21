package lila
package mod

import user.User

import scalaz.effects._

final class ModlogApi(repo: ModlogRepo) {

  def engine(mod: User, user: User, v: Boolean) = add {
    Modlog(mod.id, user.id.some, v.fold(Modlog.engine, Modlog.unengine))
  }

  def mute(mod: User, user: User, v: Boolean) = add {
    Modlog(mod.id, user.id.some, v.fold(Modlog.mute, Modlog.unmute))
  }

  def ipban(mod: User, user: User) = add {
    Modlog(mod.id, user.id.some, Modlog.ipban)
  }

  private def add(modLog: Modlog): IO[Unit] = repo saveIO modLog
}
