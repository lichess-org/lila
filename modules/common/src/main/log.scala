package lila

object log:

  type Logger = play.api.LoggerLike

  def apply(name: String): Logger = new:
    val logger = org.slf4j.LoggerFactory.getLogger(name)

  val system = apply("system")

  def http(res: play.api.libs.ws.StandaloneWSResponse): String =
    s"${res.status} ${(res.body: String).linesIterator.find(_.exists(_.isLetter)).getOrElse("-")}"
