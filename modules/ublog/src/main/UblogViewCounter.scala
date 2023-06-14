package lila.ublog

import bloomfilter.mutable.BloomFilter

import lila.common.IpAddress
import lila.db.dsl.{ *, given }

final class UblogViewCounter(colls: UblogColls)(using Executor):

  private val bloomFilter = BloomFilter[String](
    numberOfItems = 200_000,
    falsePositiveRate = 0.001
  )

  def apply(post: UblogPost, ip: IpAddress): UblogPost =
    if (post.live) post.copy(views = {
      val key = s"${post.id}:${ip.value}"
      if (bloomFilter mightContain key) post.views
      else {
        bloomFilter.add(key)
        lila.mon.ublog.view(post.created.by.value).increment()
        colls.post.incFieldUnchecked($id(post.id), "views")
        post.views + 1
      }
    })
    else post
