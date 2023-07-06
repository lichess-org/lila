package lila.game

import chess.Color
import ornicar.scalalib.{ ThreadLocalRandom, SecureRandom }

import lila.db.dsl.{ *, given }

final class IdGenerator(gameRepo: GameRepo)(using Executor):

  import IdGenerator.*

  def game: Fu[GameId] =
    val id = uncheckedGame
    gameRepo.exists(id).flatMap {
      if _ then game
      else fuccess(id)
    }

  def games(nb: Int): Fu[Set[GameId]] =
    if nb < 1 then fuccess(Set.empty)
    else if nb == 1 then game.dmap(Set(_))
    else if nb < 5 then Set.fill(nb)(game).parallel
    else
      val ids = Set.fill(nb)(uncheckedGame)
      gameRepo.coll.distinctEasy[GameId, Set]("_id", $inIds(ids)) flatMap { collisions =>
        games(collisions.size) dmap { _ ++ (ids diff collisions) }
      }

object IdGenerator:

  private[this] val whiteSuffixChars = ('0' to '4') ++ ('A' to 'Z') mkString
  private[this] val blackSuffixChars = ('5' to '9') ++ ('a' to 'z') mkString

  def uncheckedGame = GameId(ThreadLocalRandom nextString GameId.size)

  def player(color: Color): GamePlayerId =
    // Trick to avoid collisions between player ids in the same game.
    val suffixChars = color.fold(whiteSuffixChars, blackSuffixChars)
    val suffix      = suffixChars(SecureRandom nextInt suffixChars.length)
    GamePlayerId(SecureRandom.nextString(GamePlayerId.size - 1) + suffix)
