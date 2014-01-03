package lila.chat

private[chat] object actorApi {

case class Command(chanOption: Option[Chan], member: ChatMember, text: String)

case class Flash(member: ChatMember, text: String)

case class SetOpen(member: ChatMember, value: Boolean)

case class Query(member: ChatMember, username: String)

case class Join(member: ChatMember, chan: Chan)
case class Part(member: ChatMember, chan: Chan)

case class Say(chan: Chan, member: ChatMember, text: String)

case class Activate(member: ChatMember, chan: Chan)
case class DeActivate(member: ChatMember, chan: Chan)

case class WithChanNicks(key: String, f: List[String] => Unit)
}
