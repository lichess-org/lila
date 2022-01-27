package lila.game

import chess.Color

import lila.common.{ SecureRandom, ThreadLocalRandom }
import lila.db.dsl._

final class IdGenerator(gameRepo: GameRepo)(implicit ec: scala.concurrent.ExecutionContext) {

  import IdGenerator._

  def game: Fu[Game.ID] = {
    val id = uncheckedGame
    gameRepo.exists(id).flatMap {
      case true  => game
      case false => fuccess(id)
    }
  }

  def games(nb: Int): Fu[Set[Game.ID]] =
    if (nb < 1) fuccess(Set.empty)
    else if (nb == 1) game.dmap(Set(_))
    else if (nb < 5) Set.fill(nb)(game).sequenceFu
    else {
      val ids = Set.fill(nb)(uncheckedGame)
      gameRepo.coll.distinctEasy[Game.ID, Set]("_id", $inIds(ids)) flatMap { collisions =>
        games(collisions.size) dmap { _ ++ (ids diff collisions) }
      }
    }
}

object IdGenerator {

  private[this] val whiteSuffixChars = ('0' to '4') ++ ('A' to 'Z') mkString
  private[this] val blackSuffixChars = ('5' to '9') ++ ('a' to 'z') mkString

  def uncheckedGame: Game.ID = ThreadLocalRandom nextString Game.gameIdSize

  def player(color: Color): Player.ID = {
    // Trick to avoid collisions between player ids in the same game.
    val suffixChars = color.fold(whiteSuffixChars, blackSuffixChars)
    val suffix      = suffixChars(SecureRandom nextInt suffixChars.length)
    SecureRandom.nextString(Game.playerIdSize - 1) + suffix
  }
}
