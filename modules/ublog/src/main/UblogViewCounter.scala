package lila.ublog

import bloomfilter.mutable.BloomFilter
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.IpAddress
import lila.db.dsl._

final class UblogViewCounter(colls: UblogColls)(implicit ec: ExecutionContext) {

  private val bloomFilter = BloomFilter[String](
    numberOfItems = 200_000,
    falsePositiveRate = 0.001
  )

  def apply(post: UblogPost, ip: IpAddress): Unit = {
    val key = s"${post.id.value}:${ip.value}"
    if (!bloomFilter.mightContain(key)) {
      bloomFilter.add(key)
      lila.mon.ublog.view(post.created.by).increment()
      colls.post.incFieldUnchecked($id(post.id.value), "views")
    }
  }
}
