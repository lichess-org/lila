package lila.message

object Event {
  case class NewMessage(t: Thread, p: Post)
}
