package lila.common

import scala.jdk.CollectionConverters.*

object LilaJvm:

  def threadGroups(): List[(String, Int)] = Thread
    .getAllStackTraces()
    .keySet()
    .asScala
    .toList
    .map(_.getName)
    .map("""-\d+$""".r.replaceAllIn(_, ""))
    .groupBy(identity)
    .view
    .mapValues(_.size)
    .toList
    .sortBy(-_._2)
