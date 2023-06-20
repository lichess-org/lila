package lila.blog

class YoutubeTest extends munit.FunSuite {

  test("no youtube embed") {
    assertEquals(Youtube.fixStartTimes(Fixtures.noYoutube), Fixtures.noYoutube)
  }
  test("with youtube embed") {
    val fixed = Youtube.fixStartTimes(Fixtures.withYoutube).value
    assert(
      !fixed.contains(
        """<div data-oembed="https://www.youtube.com/watch?v=uz-dZ2W4Bf0#t=4m14s" data-oembed-type="video" data-oembed-provider="youtube"><iframe width="480" height="270" src="https://www.youtube.com/embed/uz-dZ2W4Bf0?feature=oembed" frameborder="0" allowfullscreen></iframe></div>"""
      )
    )
    assert(
      fixed.contains(
        """<div data-oembed="https://www.youtube.com/watch?v=uz-dZ2W4Bf0#t=4m14s" data-oembed-type="video" data-oembed-provider="youtube"><iframe width="480" height="270" src="https://www.youtube.com/embed/uz-dZ2W4Bf0?feature=oembed&start=254" frameborder="0" allowfullscreen></iframe></div>"""
      )
    )
  }
}
