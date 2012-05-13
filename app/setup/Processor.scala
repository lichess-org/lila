package lila
package setup

import http.Context

import scalaz.effects._

final class Processor(
    userConfigRepo: UserConfigRepo) {

  def ai(config: AiConfig)(implicit ctx: Context): IO[Unit] = for {
    _ ← ctx.me.fold(
      user ⇒ userConfigRepo.update(user, ((c: UserConfig) ⇒ c withAi config)),
      io()
    )
    color = config.color.resolve
  } yield ()
}
