package lidraughts.blog

object ProtocolFix {

  private val RemoveRegex = """http://(\w{2}\.)?lidraughts\.org""".r
  def remove(html: String) = RemoveRegex.replaceAllIn(html, _ => "//lidraughts.org")

  private val AddRegex = """(https?:)?(//)?(\w{2}\.)?lidraughts\.org""".r
  def add(html: String) = AddRegex.replaceAllIn(html, _ => "https://lidraughts.org")
}
