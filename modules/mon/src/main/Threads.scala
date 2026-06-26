package lila.mon

import scala.jdk.CollectionConverters.*

object Threads:

  def blocked: List[(String, List[String])] = Thread
    .getAllStackTraces()
    .asScala
    .filter: (thread, _) =>
      thread.getState == Thread.State.BLOCKED
    .map: (thread, stack) =>
      thread.getName -> stack.map(_.toString).toList
    .toList

  def blockedStr =
    val threads = blocked
    s"Found ${threads.size} blocked threads:\n" +
      threads
        .map: (name, stack) =>
          s"$name\n${stack.mkString("\n")}\n---------"
        .mkString("\n")
