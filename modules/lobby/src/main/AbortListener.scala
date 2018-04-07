package lila.lobby

import lila.game.Pov
import lila.user.UserRepo

private[lobby] final class AbortListener(seekApi: SeekApi) {

  def apply(pov: Pov): Funit =
    (pov.game.isCorrespondence ?? recreateSeek(pov)) >>-
      cancelColorIncrement(pov)

  private def cancelColorIncrement(pov: Pov): Unit = pov.game.userIds match {
    case List(u1, u2) =>
      UserRepo.incColor(u1, -1)
      UserRepo.incColor(u2, 1)
    case _ =>
  }

  private def recreateSeek(pov: Pov): Funit = pov.player.userId ?? { aborterId =>
    seekApi.findArchived(pov.gameId) flatMap {
      _ ?? { seek =>
        (seek.user.id != aborterId) ?? {
          worthRecreating(seek) flatMap {
            _ ?? seekApi.insert(Seek renew seek)
          }
        }
      }
    }
  }

  private def worthRecreating(seek: Seek): Fu[Boolean] = UserRepo byId seek.user.id map {
    _ exists { u =>
      u.enabled && !u.lame
    }
  }
}
