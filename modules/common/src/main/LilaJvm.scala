package lila.common

import scala.jdk.CollectionConverters.*

object LilaJvm:

  case class ThreadGroup(name: String, states: Map[Thread.State, Int]):
    def total             = states.values.sum
    def running           = states.getOrElse(Thread.State.RUNNABLE, 0)
    override def toString = s"$name total: $total runnable: $running"

  def threadGroups(): List[ThreadGroup] = threadList()
    .map { thread => """-\d+$""".r.replaceAllIn(thread.getName, "") -> thread.getState }
    .groupBy(_._1)
    .view
    .map { (name, states) =>
      ThreadGroup(name, states.groupBy(_._2).view.mapValues(_.size).toMap)
    }
    .toList
    .sortBy(-_.total)

  private def threadList(): List[Thread] = Thread
    .getAllStackTraces()
    .keySet()
    .asScala
    .toList
