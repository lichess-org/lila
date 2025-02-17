package lila.msg

import lila.core.LightUser
import lila.core.relation.Relations

case class MsgConvo(
    contact: LightUser,
    msgs: List[Msg],
    relations: Relations,
    postable: Boolean,
    contactDetailsForMods: Option[ContactDetailsForMods]
)

case class ModMsgConvo(
    contact: User,
    msgs: List[Msg],
    relations: Relations,
    truncated: Boolean
)

case class ContactDetailsForMods(kid: Boolean, openInbox: Boolean)
