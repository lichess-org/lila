package lila
package modlog

import user.User

import scalaz.effects._

final class ModlogApi(repo: ModlogRepo) {

  def engine(mod: User, user: User, v: Boolean) = add {
    Modlog(mod.id, user.id.some, v.fold(Modlog.engine, Modlog.unengine)).pp
  }

  private def add(modLog: Modlog): IO[Unit] = repo saveIO modLog
}
