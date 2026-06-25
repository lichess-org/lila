package lila

object log:

  type Logger = play.api.LoggerLike

  def apply(name: String): Logger = new:
    val logger = org.slf4j.LoggerFactory.getLogger(name)

  def http(status: Int, body: String) =
    s"$status ${body.linesIterator.take(1).toList.headOption.getOrElse("-")}"
