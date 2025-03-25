package lila.core

import lila.core.net.IpAddress

class IpAddressTest extends munit.FunSuite:

  test("iso ipv4"):
    assertEquals(IpAddress.unchecked("0.0.0.0").toString, "0.0.0.0")
    assertEquals(IpAddress.unchecked("1.2.3.4").toString, "1.2.3.4")
  test("iso ipv6"):
    assertEquals(IpAddress.unchecked("::1").toString, "::1")
    assertEquals(IpAddress.unchecked("0:0:0:0:0:0:0:1").toString, "::1")
    assertEquals(IpAddress.unchecked("2a09:bac0:23::815:b5f").toString, "2a09:bac0:23::815:b5f")
    assertEquals(
      IpAddress.unchecked("2A09:BAC0:0023:0000:0000:0000:0815:0B5F").toString,
      "2a09:bac0:23::815:b5f"
    )

  test("equality ipv4"):
    assertEquals(IpAddress.unchecked("0.0.0.0"), IpAddress.unchecked("0.0.0.0"))
    assertNotEquals(IpAddress.unchecked("0.0.0.0"), IpAddress.unchecked("0.1.0.0"))
  test("equality ipv6"):
    assertEquals(IpAddress.unchecked("2a09:bac0:23::815:b5f"), IpAddress.unchecked("2a09:bac0:23:0::815:b5f"))
    assertNotEquals(
      IpAddress.unchecked("2a09:bac0:123::815:b5f"),
      IpAddress.unchecked("2a09:bac0:23::815:b5f")
    )
  test("equality mixed"):
    assertNotEquals(IpAddress.unchecked("0.0.0.0"), IpAddress.unchecked("::"))
