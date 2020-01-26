package lila.msg

import lila.common.LightUser
import lila.relation.Relations

case class MsgConvo(
    contact: LightUser,
    thread: MsgThread,
    msgs: List[Msg],
    relations: Relations
)
