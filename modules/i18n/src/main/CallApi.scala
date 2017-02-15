package lila.i18n

import play.api.mvc.RequestHeader
import scala.util.Random.shuffle

import lila.user.User

private final class CallApi(
    minGames: Int,
    hideCallsCookieName: String,
    transInfos: TransInfos
) {

  private var submitted = Set.empty[String]

  def apply(userOption: Option[User], req: RequestHeader): Option[TransInfo] =
    userOption.flatMap { user =>
      if (req.cookies.get(hideCallsCookieName).isDefined) None
      else if (user.count.game < minGames) None
      else shuffle {
        (req.acceptLanguages map transInfos.get).flatten filterNot { i =>
          i.complete || submitted.contains(i.code)
        }
      }.headOption
    }

  private[i18n] def submit(code: String) = submitted += code
}
