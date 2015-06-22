package lila.lobby

import org.joda.time.DateTime

import lila.game.Pov

private[lobby] final class AbortListener(seekApi: SeekApi) {

  def recreateSeek(pov: Pov): Funit = pov.player.userId ?? { aborterId =>
    seekApi.findArchived(pov.game.id) flatMap {
      _ ?? { seek =>
        (seek.user.id != aborterId) ?? seekApi.insert(Seek renew seek)
      }
    }
  }
}
