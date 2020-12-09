package lila.security

import play.api.i18n.Lang
import cats.implicits._

import lila.common.EmailAddress
import lila.common.config.BaseUrl
import lila.i18n.I18nKeys.{ emails => trans }
import lila.user.{ User, UserRepo }
import lila.hub.actorApi.msg.SystemMsg

final class AutomaticEmail(
    userRepo: UserRepo,
    mailgun: Mailgun,
    baseUrl: BaseUrl
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Mailgun.html._

  val regards = """Regards,

The Lichess team"""

  def welcome(user: User, email: EmailAddress)(implicit lang: Lang): Funit = {
    val profileUrl = s"$baseUrl/@/${user.username}"
    val editUrl    = s"$baseUrl/account/profile"
    mailgun send Mailgun.Message(
      to = email,
      subject = trans.welcome_subject.txt(user.username),
      text = s"""
${trans.welcome_text.txt(profileUrl, editUrl)}

${Mailgun.txt.serviceNote}
""",
      htmlBody = standardEmail(
        trans.welcome_text.txt(profileUrl, editUrl)
      ).some
    )
  }

  def onTitleSet(username: String): Funit =
    for {
      user        <- userRepo named username orFail s"No such user $username"
      emailOption <- userRepo email user.id
      _ <- ~(user.title, emailOption).mapN { case (title, email) =>
        implicit val lang = userLang(user)

        val body = s"""Hello,

Thank you for confirming your $title title on Lichess.
It is now visible on your profile page: $baseUrl/@/${user.username}.

$regards
"""

        lila.common.Bus.publish(SystemMsg(user.id, body), "msgSystemSend")

        mailgun send Mailgun.Message(
          to = email,
          subject = s"$title title confirmed on lichess.org",
          text = s"""
$body

${Mailgun.txt.serviceNote}
""",
          htmlBody = standardEmail(body).some
        )
      }
    } yield ()

  def onBecomeCoach(user: User): Funit =
    userRepo email user.id flatMap {
      _ ?? { email =>
        implicit val lang = userLang(user)
        val body          = s"""Hello,

It is our pleasure to welcome you as a Lichess coach.
Your coach profile awaits you on $baseUrl/coach/edit.

$regards
"""

        lila.common.Bus.publish(SystemMsg(user.id, body), "msgSystemSend")

        mailgun send Mailgun.Message(
          to = email,
          subject = "Coach profile unlocked on lichess.org",
          text = s"""
$body

${Mailgun.txt.serviceNote}
""",
          htmlBody = standardEmail(body).some
        )
      }
    }

  def onFishnetKey(userId: User.ID, key: String): Funit =
    for {
      user        <- userRepo named userId orFail s"No such user $userId"
      emailOption <- userRepo email user.id
      _ <- emailOption.?? { email =>
        implicit val lang = userLang(user)
        val body          = s"""Hello,

This message contains your private fishnet key. Please treat it like a password. You can use the same key on multiple machines (even at the same time), but you should not share it with anyone.

Thank you very much for your help! Thanks to you, chess lovers all around the world will enjoy swift and powerful analysis for their games.

Your key is:

$key

$regards
"""

        lila.common.Bus.publish(SystemMsg(user.id, body), "msgSystemSend")

        mailgun send Mailgun.Message(
          to = email,
          subject = "Your private fishnet key",
          text = s"""
$body

${Mailgun.txt.serviceNote}
""",
          htmlBody = standardEmail(body).some
        )
      }
    } yield ()

  def onAppealReply(user: User): Funit = {
    val body = s"""Hello,

      Your appeal has received a response from the moderation team: ${baseUrl}/appeal

$regards
"""

    lila.common.Bus.publish(SystemMsg(user.id, body), "msgSystemSend")
    funit
  }

  private def userLang(user: User) = user.realLang | lila.i18n.defaultLang
}
