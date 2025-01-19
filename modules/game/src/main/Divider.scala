package lila.game

import scala.concurrent.duration._

import com.github.blemale.scaffeine.Cache

import shogi.Division
import shogi.format.forsyth.Sfen
import shogi.variant.Variant

final class Divider {

  private val cache: Cache[Game.ID, Division] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(5 minutes)
    .build[Game.ID, Division]()

  def apply(game: Game): Division =
    apply(game.id, game.usis, game.variant, game.initialSfen)

  def apply(id: Game.ID, usis: => Usis, variant: Variant, initialSfen: Option[Sfen]) =
    if (!Variant.divisionSensibleVariants(variant)) Division.empty
    else
      cache.get(
        id,
        _ =>
          shogi.Replay
            .situations(
              usis = usis,
              initialSfen = initialSfen,
              variant = variant,
            )
            .toOption
            .map(_.toList)
            .fold(Division.empty)(shogi.Divider.apply),
      )
}
