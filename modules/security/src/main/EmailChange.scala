package lila.security

import scalatags.Text.all.*
import lila.common.config.*
import lila.common.EmailAddress
import lila.common.Iso
import lila.i18n.I18nKeys.{ emails as trans }
import lila.user.{ User, Me, UserRepo }
import lila.mailer.Mailer

final class EmailChange(
    userRepo: UserRepo,
    mailer: Mailer,
    baseUrl: BaseUrl,
    tokenerSecret: Secret
)(using Executor):

  import Mailer.html.*

  def send(user: User, email: EmailAddress): Funit =
    !email.looksLikeFakeEmail so {
      tokener make TokenPayload(user.id, email).some flatMap { token =>
        lila.mon.email.send.change.increment()
        given play.api.i18n.Lang = user.realLang | lila.i18n.defaultLang
        val url                  = s"$baseUrl/account/email/confirm/$token"
        lila.log("auth").info(s"Change email URL ${user.username} $email $url")
        mailer send Mailer.Message(
          to = email,
          subject = trans.emailChange_subject.txt(user.username),
          text = Mailer.txt.addServiceNote(s"""
${trans.emailChange_intro.txt()}
${trans.emailChange_click.txt()}

$url

${trans.common_orPaste.txt()}
"""),
          htmlBody = emailMessage(
            pDesc(trans.emailChange_intro()),
            p(trans.emailChange_click()),
            potentialAction(metaName("Change email address"), Mailer.html.url(url)),
            serviceNote
          ).some
        )
      }
    }

  // also returns the previous email address
  def confirm(token: String): Fu[Option[(Me, Option[EmailAddress])]] =
    tokener read token dmap (_.flatten) flatMapz { case TokenPayload(userId, email) =>
      userRepo.email(userId) flatMap { previous =>
        (userRepo.setEmail(userId, email).recoverDefault >> userRepo.me(userId))
          .map2(_ -> previous)
      }
    }

  case class TokenPayload(userId: UserId, email: EmailAddress)

  private given Iso.StringIso[Option[TokenPayload]] with
    private val sep = ' '
    val from = str =>
      str.split(sep) match
        case Array(id, email) => EmailAddress from email map { TokenPayload(UserId(id), _) }
        case _                => none
    val to = a =>
      a so { case TokenPayload(userId, email) =>
        s"$userId$sep$email"
      }

  private val tokener = new StringToken[Option[TokenPayload]](
    secret = tokenerSecret,
    getCurrentValue = p =>
      p so { case TokenPayload(userId, _) =>
        userRepo email userId dmap (_.so(_.value))
      }
  )
