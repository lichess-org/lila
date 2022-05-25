package lila.common

import org.specs2.mutable.Specification

class MarkdownTest extends Specification {

  val render = new MarkdownRender()("test") _

  "autolinks" should {
    "add rel" in {
      val md = Markdown("https://example.com")
      render(
        md
      ) must_== """<p><a href="https://example.com" rel="nofollow noopener noreferrer">https://example.com</a></p>
"""
    }
  }
  "markdown links" should {
    "remove tracking tags" in {
      val md = Markdown("[Example](https://example.com?utm_campaign=spy&utm_source=evil)")
      render(
        md
      ) must_== """<p><a href="https://example.com" rel="nofollow noopener noreferrer">Example</a></p>
"""
    }
  }
}
