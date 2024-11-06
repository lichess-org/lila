package lila

object log:

  def apply(name: String): Logger = Logger(name)

  final class Logger(name: String) extends play.api.LoggerLike:

    val logger = org.slf4j.LoggerFactory.getLogger(name)

    def branch(childName: String) = new Logger(name = s"$name.$childName")

  def http(status: Int, body: String) =
    s"$status ${body.linesIterator.take(1).toList.headOption.getOrElse("-")}"

  object asyncActorMonitor:
    lazy val full = scalalib.actor.AsyncActorBounded.Monitor(
      overflow = name =>
        lila.mon.asyncActor.overflow(name).increment()
        lila.log("asyncActor").warn(s"[$name] queue is full")
      ,
      queueSize = (name, size) => lila.mon.asyncActor.queueSize(name).record(size),
      unhandled = (name, msg) => lila.log("asyncActor").warn(s"[$name] unhandled msg: $msg")
    )

    lazy val highCardinality = full.copy(
      queueSize = (_, _) => ()
    )

    lazy val unhandled = full.copy(
      overflow = _ => (),
      queueSize = (_, _) => ()
    )
