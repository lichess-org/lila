package lila.chat

import lila.user.User

case class UserModInfo(
    user: User,
    history: List[ChatTimeout.UserEntry]
)
