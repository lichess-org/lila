package lila.web

import play.api.Mode
import play.api.mvc.RequestHeader

import lila.memo.ViewerCount

final class PlayerCounter(using scheduler: Scheduler, mode: Mode)(using Executor):

  val lichobile = AnonAuthCounters(capacity(1_000_000))
  val mobile = AnonAuthCounters(capacity(1_000_000))
  val website = AnonAuthCounters(capacity(2_000_000))
  val takex3 = AnonAuthCounters(capacity(50_000))

  private def capacity(max: Int) =
    if mode.isProd then max else math.cbrt(max.toDouble).toInt.atLeast(1)

  private val allCounters =
    List("lichobile" -> lichobile, "mobile" -> mobile, "website" -> website, "takex3" -> takex3)

  scheduler.scheduleWithFixedDelay(1.minutes, 1.minutes): () =>
    for
      (client, counter) <- allCounters
      auth <- List(true, false)
    do lila.mon.uniquePlayers.count(client, auth).update(counter.get(auth))

  scheduler.scheduleWithFixedDelay(1.day, 1.day): () =>
    allCounters._2F.foreach(_.reset())

private final class AnonAuthCounters(val max: Int):
  private val anon = ViewerCount(0, max / 2)
  private val auth = ViewerCount(0, max / 2)

  def hit()(using req: RequestHeader, me: Option[Me]): Unit =
    val viewer = ViewerCount.makeViewer(req, me.map(_.userId))
    if me.isDefined then auth.hit(viewer) else anon.hit(viewer)

  def get(isAuth: Boolean): Int = if isAuth then auth.get else anon.get

  def reset(): Unit =
    anon.reset()
    auth.reset()
