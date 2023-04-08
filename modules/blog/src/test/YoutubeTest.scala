package lila.blog

import org.specs2.mutable._

final class YoutubeTest extends Specification {

  "fix youtube timestamps" >> {
    "no youtube embed" >> {
      Youtube.fixStartTimes(Fixtures.noYoutube) === Fixtures.noYoutube
    }
    "with youtube embed" >> {
      val fixed = Youtube.fixStartTimes(Fixtures.withYoutube).value
      fixed must not(
        contain(
          """<div data-oembed="https://www.youtube.com/watch?v=uz-dZ2W4Bf0#t=4m14s" data-oembed-type="video" data-oembed-provider="youtube"><iframe width="480" height="270" src="https://www.youtube.com/embed/uz-dZ2W4Bf0?feature=oembed" frameborder="0" allowfullscreen></iframe></div>"""
        )
      )
      fixed must contain(
        """<div data-oembed="https://www.youtube.com/watch?v=uz-dZ2W4Bf0#t=4m14s" data-oembed-type="video" data-oembed-provider="youtube"><iframe width="480" height="270" src="https://www.youtube.com/embed/uz-dZ2W4Bf0?feature=oembed&start=254" frameborder="0" allowfullscreen></iframe></div>"""
      )
    }
  }
}
