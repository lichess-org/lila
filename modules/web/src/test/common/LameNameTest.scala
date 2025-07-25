package lila.common

class LameNameTest extends munit.FunSuite:

  def isLame(str: String) = LameName.username(UserName(str))
  def explain(str: String) = LameName.explain(UserName(str))

  test("disallow separated titles"):
    assert(isLame("fm-foo"))
    assert(isLame("fm_foo"))
    assert(isLame("wgm-foo"))
    assert(isLame("wcm_foo"))
    assert(isLame("gmFoobar"))
    assert(isLame("gm007"))
    assert(isLame("GmFoo"))
    assert(isLame("nm_brianmatthews"))
    assert(isLame("the_nm_brianmatthews"))

  test("disallow uppercase titles"):
    assert(isLame("GMfoo"))
    assert(isLame("IMfoo"))
    assert(isLame("WFMfoo"))
    assert(isLame("WIMfoo"))
    assert(isLame("WIM-foo"))
    assert(isLame("WIM_foo"))
    assert(isLame("1Mfoo"))
    assert(isLame("BriaNMatthews"))
    assert(isLame("NMBrianMatthews"))
    assert(isLame("BrianMatthews_NM"))
    assert(isLame("BrianMatthewsNM"))
    assert(isLame("TheGMBrianMatthews"))

  test("disallow gross"):
    assert(isLame("douchebag"))
    assert(isLame("d0uchebag"))
    assert(isLame("urcunt-blah"))
    assert(isLame("urcuntblah"))
    assert(isLame("fuckster"))
    assert(isLame("fuuckster"))

  test("allow good stuff"):
    assert(!isLame("joey"))
    assert(!isLame("gmfoo"))
    assert(!isLame("g-foo"))
    assert(!isLame("g_foo"))
    assert(!isLame("g-foo"))
    assert(!isLame("agm-foo"))
    assert(!isLame("atf90"))
    assert(!isLame("a_b"))
    assert(!isLame("BRIANMATTHEWS"))
    assert(!isLame("BrianMatthews"))
    assert(!isLame("BrianMatthewsnm"))
    assert(!isLame("TheGMBRianMatthews"))

  test("explain"):
    assertEquals(explain("cammy"), None)
    assertEquals(explain("FM-cammy"), Some("Contains a title"))
    assertEquals(explain("cammyGM"), Some("Contains a title"))
    assertEquals(explain("moderator"), Some("""Lame username: "moderator""""))
    assertEquals(explain("iammoderator_yay"), Some("""Lame username: "mmoderator""""))
    assertEquals(explain("d0uchebag"), Some("""Lame username: "d0uche""""))
    assertEquals(explain("FOOurcunt-blah"), Some("""Lame username: "cunt""""))
