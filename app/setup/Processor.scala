package lila
package setup

import http.Context

import scalaz.effects._

final class Processor(
    configRepo: UserConfigRepo) {

  def ai(config: AiConfig)(implicit ctx: Context): IO[Unit] = for {
    _ ← ctx.me.fold(
      user ⇒ configRepo.update(user, ((c: UserConfig) ⇒ c withAi config)),
      io()
    )
    color = config.color.resolve
  } yield ()
}
