package lila.common

import org.specs2.mutable.Specification

class MarkdownTest extends Specification {

  val render = new MarkdownRender()("test") _

  // "autolinks" should {
  //   "remove tracking tags" in {
  //     val md = Markdown("http://example.com?utm_campaign=spy&utm_source=evil")
  //     render(
  //       md
  //     ) must_== """<p><a href="http://example.com">Example</a></p>
// """
  //   }
  // }
  "markdown links" should {
    "remove tracking tags" in {
      val md = Markdown("[Example](http://example.com?utm_campaign=spy&utm_source=evil)")
      render(
        md
      ) must_== """<p><a href="http://example.com" rel="nofollow noopener noreferrer">Example</a></p>
"""
    }
  }
}
