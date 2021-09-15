package lila.msg

import lila.common.LightUser
import lila.relation.Relations

case class MsgConvo(
    contact: LightUser,
    msgs: List[Msg],
    relations: Relations,
    postable: Boolean
)

case class ModMsgConvo(
    convo: MsgConvo,
    truncated: Boolean
)
