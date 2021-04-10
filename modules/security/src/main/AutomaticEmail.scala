package lila.security

import play.api.i18n.Lang
import scala.util.chaining._

import lila.common.config.BaseUrl
import lila.common.EmailAddress
import lila.hub.actorApi.msg.SystemMsg
import lila.i18n.I18nKeys.{ emails => trans }
import lila.user.{ User, UserRepo }
import lila.base.LilaException

final class AutomaticEmail(
    userRepo: UserRepo,
    mailer: Mailer,
    baseUrl: BaseUrl
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Mailer.html._

  val regards = """Regards,

The Lichess team"""

  def welcome(user: User, email: EmailAddress)(implicit lang: Lang): Funit = {
    lila.mon.email.send.welcome.increment()
    val profileUrl = s"$baseUrl/@/${user.username}"
    val editUrl    = s"$baseUrl/account/profile"
    mailer send Mailer.Message(
      to = email,
      subject = trans.welcome_subject.txt(user.username),
      text = s"""
${trans.welcome_text.txt(profileUrl, editUrl)}

${Mailer.txt.serviceNote}
""",
      htmlBody = standardEmail(
        trans.welcome_text.txt(profileUrl, editUrl)
      ).some
    )
  }

  def onTitleSet(username: String): Funit = {
    for {
      user        <- userRepo named username orFail s"No such user $username"
      emailOption <- userRepo email user.id
      title       <- fuccess(user.title) orFail "User doesn't have a title!"
      body = alsoSendAsPrivateMessage(user) { implicit lang =>
        s"""Hello,

Thank you for confirming your $title title on Lichess.
It is now visible on your profile page: $baseUrl/@/${user.username}.

$regards
"""
      }
      _ <- emailOption ?? { email =>
        implicit val lang = userLang(user)
        mailer send Mailer.Message(
          to = email,
          subject = s"$title title confirmed on lichess.org",
          text = s"""
$body

${Mailer.txt.serviceNote}
""",
          htmlBody = standardEmail(body).some
        )
      }
    } yield ()
  } recover { case e: LilaException =>
    logger.info(e.message)
  }

  def onBecomeCoach(user: User): Funit = {
    val body = alsoSendAsPrivateMessage(user) { implicit lang =>
      s"""Hello,

It is our pleasure to welcome you as a Lichess coach.
Your coach profile awaits you on $baseUrl/coach/edit.

$regards
"""
    }
    userRepo email user.id flatMap {
      _ ?? { email =>
        implicit val lang = userLang(user)
        mailer send Mailer.Message(
          to = email,
          subject = "Coach profile unlocked on lichess.org",
          text = s"""
$body

${Mailer.txt.serviceNote}
""",
          htmlBody = standardEmail(body).some
        )
      }
    }
  }

  def onFishnetKey(userId: User.ID, key: String): Funit =
    for {
      user        <- userRepo named userId orFail s"No such user $userId"
      emailOption <- userRepo email user.id
      body = alsoSendAsPrivateMessage(user) { implicit lang =>
        s"""Hello,

This message contains your private fishnet key. Please treat it like a password. You can use the same key on multiple machines (even at the same time), but you should not share it with anyone.

Thank you very much for your help! Thanks to you, chess lovers all around the world will enjoy swift and powerful analysis for their games.

Your key is:

$key

$regards
"""
      }
      _ <- emailOption.?? { email =>
        implicit val lang = userLang(user)
        mailer send Mailer.Message(
          to = email,
          subject = "Your private fishnet key",
          text = s"""
$body

${Mailer.txt.serviceNote}
""",
          htmlBody = standardEmail(body).some
        )
      }
    } yield ()

  def onAppealReply(user: User): Funit = {
    alsoSendAsPrivateMessage(user) { implicit lang =>
      s"""Hello,

      Your appeal has received a response from the moderation team: ${baseUrl}/appeal

$regards
"""
    }
    funit
  }

  private def alsoSendAsPrivateMessage(user: User)(body: Lang => String): String = {
    implicit val lang = userLang(user)
    body(userLang(user)) tap { txt =>
      lila.common.Bus.publish(SystemMsg(user.id, txt), "msgSystemSend")
    }
  }

  private def userLang(user: User): Lang = user.realLang | lila.i18n.defaultLang
}
