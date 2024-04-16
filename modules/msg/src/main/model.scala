package lila.msg

import lila.core.LightUser
import lila.core.relation.Relations

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
