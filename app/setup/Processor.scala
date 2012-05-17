package lila
package setup

import http.Context
import game.{ GameRepo, Pov }
import chess.{ Game, Board }

import scalaz.effects._

final class Processor(
    configRepo: UserConfigRepo,
    gameRepo: GameRepo) {

  def ai(config: AiConfig)(implicit ctx: Context): IO[Pov] = for {
    _ ← ctx.me.fold(
      user ⇒ configRepo.update(user)(_ withAi config),
      io()
    )
    pov = config.pov
    _ ← gameRepo insert pov.game
  } yield pov
}
