package lila

object log {

  def apply(name: String): Logger = new Logger(name)

  val boot = apply("boot")

  final class Logger(name: String) extends play.api.LoggerLike {

    val logger = org.slf4j.LoggerFactory getLogger name

    def branch(childName: String) = new Logger(name = s"$name.$childName")
  }
}
