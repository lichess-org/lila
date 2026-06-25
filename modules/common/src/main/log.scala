package lila

object log:

  def apply(name: String): play.api.LoggerLike = new:
    val logger = org.slf4j.LoggerFactory.getLogger(name)

  def http(status: Int, body: String) =
    s"$status ${body.linesIterator.take(1).toList.headOption.getOrElse("-")}"
