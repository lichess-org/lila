package lila.msg

import lila.common.LightUser
import lila.hub.relation.Relations
import lila.user.User

case class MsgConvo(
    contact: LightUser,
    msgs: List[Msg],
    relations: Relations,
    postable: Boolean
)

case class ModMsgConvo(
    contact: User,
    msgs: List[Msg],
    relations: Relations,
    truncated: Boolean
)
