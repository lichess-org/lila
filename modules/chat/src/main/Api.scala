package lila.chat

private[chat] final class Api {

  object userChat {

    def write(chatId: String, username: String, text: String): Fu[Option[UserLine]] =
      makeLine(chanName, userId, text) flatMap {
        case None ⇒ {
          logger.info(s"$userId @ $chanName : $text")
          fuccess(none)
        }
        case Some(line) if flood.allowMessage(line.userId, line.text) ⇒ (line.chan match {
          case UserChan(u1, u2) ⇒ relationApi.areFriends(u1, u2)
          case _                ⇒ fuccess(true)
        }) flatMap {
          _ ?? (LineRepo insert line inject line.some)
        }
        case Some(line) ⇒ {
          logger.info(s"Flood: $userId @ $chanName : $text")
          fuccess(none)
        }
      }

    def makeLine(chanKey: String, userId: String, t1: String): Fu[Option[Line]] =
      getUser(userId) flatMap { user ⇒
        val chanOption = Chan parse chanKey
        (chanOption match {
          case Some(TeamChan(teamId)) ⇒ getTeamIds(user.id) map (_ contains teamId)
          case _                      ⇒ fuccess(true)
        }) map {
          _ ?? {
            for {
              chan ← chanOption
              t2 ← Some(t1.trim take 200) filter (_.nonEmpty)
              if !user.disabled
            } yield Line.make(chan, user, Writer preprocessUserInput t2)
          }
        }
      }
  }
}
