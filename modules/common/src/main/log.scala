package lila

object log {

  def apply(name: String): Logger = new Logger(name)

  final class Logger(name: String) extends play.api.LoggerLike {

    val logger = org.slf4j.LoggerFactory getLogger name

    def branch(childName: String) = new Logger(name = s"$name.$childName")
  }

  def http(status: Int, body: String) =
    s"$status ${body.linesIterator.take(1).toList.headOption getOrElse "-"}"
}
