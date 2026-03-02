package lila.mailer

final private class MailerCli(mailer: Mailer):

  lila.common.Cli.handle:
    case "test-email" :: clientName :: address :: Nil =>
      EmailAddress.from(address) match
        case None => fuccess(s"Invalid email address: $address")
        case Some(email) =>
          mailer.getClient(clientName) match
            case None => fuccess(s"Unknown client '$clientName'. Available: primary, secondary")
            case Some(client) =>
              val msg = Mailer.Message(
                to = email,
                subject = "Lichess test email",
                text = "This is a test email from Lichess: https://lichess.org/dev/cli"
              )
              mailer
                .sendTest(msg, client)
                .inject(s"Test email sent to $address via $client client.")
