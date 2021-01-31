package lila.common.base

object Levenshtein {
  final def isLevenshteinDistanceLessThan(a: String, b: String, threshold: Int): Boolean = {
    val inf = Int.MaxValue - 1
    def f(a: String, b: String): Boolean = {
      /* dd : destination diagonal */
      val dd = b.length - a.length
      val t  = (threshold - 1) >> 1
      def rowRange(j: Int) = {
        //j - i == dd
        val i = j - dd
        ((i - (threshold - 1)) max 0) until (1 + ((i + t) min a.length))
      }
      @scala.annotation.tailrec
      def loop(j: Int, prev: Array[Int], prevr: Range, next: Array[Int]): Int = {
        if (j > b.length) prev.last
        else {
          val c = b(j - 1)
          val q = rowRange(j)
          for (i <- q) {
            next(i) =
              if (i == 0) j
              else {
                val t0 = if (i < prevr.end) prev(i) else inf
                val t1 = 1 + (if (i > q.start) t0 min next(i - 1) else t0)
                if (prevr.start < i) {
                  val t = prev(i - 1)
                  t1 min (if (c != a(i - 1)) 1 + t else t)
                } else t1
              }
          }
          loop(j + 1, next, q, prev)
        }
      }
      loop(1, Array.range(0, a.length + 1), rowRange(0), Array.ofDim[Int](a.length + 1)) < threshold
    }
    if (a.lengthIs < b.length) f(b, a) else f(a, b)
  }
}
