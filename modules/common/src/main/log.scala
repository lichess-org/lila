package lila

object log {

  def apply(name: String): Logger = new Logger(name, identity)

  final class Logger(
      name: String,
      transformer: String => String) extends play.api.LoggerLike {

    val logger = org.slf4j.LoggerFactory getLogger name

    def map(trans: String => String) = new Logger(
      name = name,
      transformer = msg => trans(transformer(msg)))

    def prefix(p: String) = map(msg => s"$p: $msg")

    def branch(childName: String) = new Logger(
      name = s"$name.$childName",
      transformer = transformer)

    override def debug(message: => String) = super.debug(transformer(message))
    override def debug(message: => String, error: => Throwable) = super.debug(transformer(message), error)
    override def info(message: => String) = super.info(transformer(message))
    override def info(message: => String, error: => Throwable) = super.info(transformer(message), error)
    override def warn(message: => String) = super.warn(transformer(message))
    override def warn(message: => String, error: => Throwable) = super.warn(transformer(message), error)
    override def error(message: => String) = super.error(transformer(message))
    override def error(message: => String, error: => Throwable) = super.error(transformer(message), error)
  }
}
