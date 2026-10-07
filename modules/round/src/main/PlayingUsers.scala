package lila.round

import com.github.blemale.scaffeine.LoadingCache

import lila.core.round.CurrentlyPlaying
import lila.common.Bus
import lila.memo.CacheApi
import lila.game.GameRepo

final class PlayingUsers(gameRepo: GameRepo, proxyRepo: GameProxyRepo)(using Executor):

  private val playingUserIds = scalalib.cache.ExpireSetMemo[UserId](3.hours)

  def apply(userId: UserId): Boolean = playingUserIds.get(userId)

  def nbPlaying(userId: UserId): Fu[Int] = apply(userId).so(gameRepo.countNowPlaying(userId))

  val currentlyPlaying = CurrentlyPlaying: userId =>
    lastPlayedPlayingId(userId).flatMapz(proxyRepo.pov(_, userId))

  def lastPlayedPlayingId(userId: UserId): Fu[Option[GameId]] = apply(userId).so:
    lastPlayedPlayingIdCache.get(userId)

  private val lastPlayedPlayingIdCache: LoadingCache[UserId, Fu[Option[GameId]]] =
    CacheApi.scaffeineNoScheduler
      .expireAfterWrite(11.seconds)
      .build(gameRepo.lastPlayedPlayingId)

  Bus.sub[lila.core.game.StartGame]:
    case lila.core.game.StartGame(game, _) if game.hasClock =>
      game.userIds.nonEmptyOption.foreach(playingUserIds.putAll)
      game.userIds.foreach(lastPlayedPlayingIdCache.invalidate)

  Bus.sub[lila.core.game.FinishGame]:
    case lila.core.game.FinishGame(game, _) if game.hasClock =>
      game.userIds.nonEmptyOption.foreach(playingUserIds.removeAll)
