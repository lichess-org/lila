package lila.game

import scalalib.SecureRandom

import lila.core.game.{ Game, NewGame }
import lila.core.id.GameId
import lila.common.BatchProvider
import lila.db.dsl.{ *, given }

final class IdGenerator(gameRepo: GameRepo)(using Executor, Scheduler) extends lila.core.game.IdGenerator:

  import lila.core.game.IdGenerator.*

  private val batchProvider = BatchProvider[GameId]("idGenerator", timeout = 3.seconds): () =>
    // must NOT use `games(nb)` for it would cause a deadlock
    // due to `games` calling `game` which calls `batchProvider.one`
    val ids = List.fill(256)(uncheckedGame).distinct
    gameRepo.coll
      .distinctEasy[GameId, List]("_id", $inIds(ids))
      .monValue: collisions =>
        _.game.idGenerator(collisions.size)
      .map:
        case Nil => ids
        case collisions => ids.filterNot(collisions.contains)

  def game: Fu[GameId] = batchProvider.one

  def games(nb: Int): Fu[List[GameId]] =
    if nb < 1 then fuccess(Nil)
    else if nb == 1 then game.dmap(List(_))
    else if nb < 5 then scala.concurrent.Future.sequence(List.fill(nb)(game))
    else
      val ids = Set.fill(nb)(uncheckedGame)
      gameRepo.coll
        .distinctEasy[GameId, Set]("_id", $inIds(ids))
        .monValue: collisions =>
          _.game.idGenerator(collisions.size)
        .flatMap: collisions =>
          games(collisions.size).dmap { _ ++ (ids.diff(collisions)) }
        .map(_.toList)

  def withUniqueId(sloppy: NewGame): Fu[Game] =
    game.map(sloppy.withId)

object IdGenerator:

  private val whiteSuffixChars = (('0' to '4') ++ ('A' to 'Z')).mkString
  private val blackSuffixChars = (('5' to '9') ++ ('a' to 'z')).mkString

  def player(color: Color): GamePlayerId =
    // Trick to avoid collisions between player ids in the same game.
    val suffixChars = color.fold(whiteSuffixChars, blackSuffixChars)
    val suffix = suffixChars(SecureRandom.nextInt(suffixChars.length))
    GamePlayerId(SecureRandom.nextString(GamePlayerId.size - 1) + suffix)
