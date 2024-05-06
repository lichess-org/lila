package lila.common

class LameNameTest extends munit.FunSuite:

  def isLame(str: String) = LameName.username(UserName(str))

  test("disallow separated titles") {
    assertEquals(isLame("fm-foo"), true)
    assertEquals(isLame("fm_foo"), true)
    assertEquals(isLame("wgm-foo"), true)
    assertEquals(isLame("wcm_foo"), true)
    assertEquals(isLame("gmFoobar"), true)
    assertEquals(isLame("gm007"), true)
    assertEquals(isLame("GmFoo"), true)
    assertEquals(isLame("nm_brianmatthews"), true)
    assertEquals(isLame("the_nm_brianmatthews"), true)
  }
  test("disallow uppercase titles") {
    assertEquals(isLame("GMfoo"), true)
    assertEquals(isLame("IMfoo"), true)
    assertEquals(isLame("WFMfoo"), true)
    assertEquals(isLame("WIMfoo"), true)
    assertEquals(isLame("1Mfoo"), true)
    assertEquals(isLame("BriaNMatthews"), true)
    assertEquals(isLame("NMBrianMatthews"), true)
    assertEquals(isLame("BrianMatthews_NM"), true)
    assertEquals(isLame("BrianMatthewsNM"), true)
    assertEquals(isLame("TheGMBrianMatthews"), true)
  }
  test("disallow gross") {
    assertEquals(isLame("douchebag"), true)
    assertEquals(isLame("d0uchebag"), true)
    assertEquals(isLame("urcunt-blah"), true)
    assertEquals(isLame("urcuntblah"), true)
    assertEquals(isLame("fuckster"), true)
    assertEquals(isLame("fuuckster"), true)
  }
  test("allow good stuff") {
    assertEquals(isLame("joey"), false)
    assertEquals(isLame("gmfoo"), false)
    assertEquals(isLame("g-foo"), false)
    assertEquals(isLame("g_foo"), false)
    assertEquals(isLame("g-foo"), false)
    assertEquals(isLame("agm-foo"), false)
    assertEquals(isLame("atf90"), false)
    assertEquals(isLame("a_b"), false)
    assertEquals(isLame("BRIANMATTHEWS"), false)
    assertEquals(isLame("BrianMatthews"), false)
    assertEquals(isLame("BrianMatthewsnm"), false)
    assertEquals(isLame("TheGMBRianMatthews"), false)
  }
