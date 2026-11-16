package lila.core

import lila.core.net.ipAddress

class IpAddressTest extends munit.FunSuite:

  test("iso ipv4"):
    assertEquals(ipAddress.unchecked("0.0.0.0").toString, "0.0.0.0")
    assertEquals(ipAddress.unchecked("1.2.3.4").toString, "1.2.3.4")
  test("iso ipv6"):
    assertEquals(ipAddress.unchecked("::1").toString, "::1")
    assertEquals(ipAddress.unchecked("0:0:0:0:0:0:0:1").toString, "::1")
    assertEquals(ipAddress.unchecked("2a09:bac0:23::815:b5f").toString, "2a09:bac0:23::815:b5f")
    assertEquals(
      ipAddress.unchecked("2A09:BAC0:0023:0000:0000:0000:0815:0B5F").toString,
      "2a09:bac0:23::815:b5f"
    )

  test("equality ipv4"):
    assertEquals(ipAddress.unchecked("0.0.0.0"), ipAddress.unchecked("0.0.0.0"))
    assertNotEquals(ipAddress.unchecked("0.0.0.0"), ipAddress.unchecked("0.1.0.0"))
  test("equality ipv6"):
    assertEquals(ipAddress.unchecked("2a09:bac0:23::815:b5f"), ipAddress.unchecked("2a09:bac0:23:0::815:b5f"))
    assertNotEquals(
      ipAddress.unchecked("2a09:bac0:123::815:b5f"),
      ipAddress.unchecked("2a09:bac0:23::815:b5f")
    )
  test("equality mixed"):
    assertNotEquals(ipAddress.unchecked("0.0.0.0"), ipAddress.unchecked("::"))
