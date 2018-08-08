package lidraughts.chat

import lidraughts.user.User

case class UserModInfo(
    user: User,
    history: List[ChatTimeout.UserEntry]
)
