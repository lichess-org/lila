package lila.relay
package command

sealed trait Command {
  type Result
  def str: String
  def parse(lines: List[String]): Option[Result]
}

case object ListTourney extends Command {
  type Result = List[Tourney]
  val str = "tell relay listtourney"
  def parse(lines: List[String]) = {
    lines.exists(_ contains "The following tournaments are currently in progress:")
  } option {
    lines.collect {
      case Regexp(id, name, status) => parseIntOption(id) map {
        Tourney(_, name.trim, status match {
          case "Round Started" => Relay.Status.Started
          case "Round Over"    => Relay.Status.Finished
          case _               => Relay.Status.Unknown
        })
      }
    }.flatten
  }
  case class Tourney(id: Int, name: String, status: Relay.Status)
  private val Regexp = """^:(\d+)\s+(.+)\s{2,}(.+)$""".r
}
