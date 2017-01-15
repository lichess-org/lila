package lila.message

private[message] final class MessageSecurity(
    follows: (String, String) => Fu[Boolean],
    blocks: (String, String) => Fu[Boolean],
    getPref: String => Fu[lila.pref.Pref]) {

  import lila.pref.Pref.Message._

  def canMessage(from: String, to: String): Fu[Boolean] =
    blocks(to, from) flatMap {
      case true => fuccess(false)
      case false => getPref(to).map(_.message) flatMap {
        case NEVER  => fuccess(false)
        case FRIEND => follows(to, from)
        case ALWAYS => fuccess(true)
      }
    }
}
