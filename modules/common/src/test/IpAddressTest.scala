package lila.common

import org.specs2.mutable.Specification

class IpAddressTest extends Specification {

  "iso" should {
    "ipv4" in {
      IpAddress.unchecked("0.0.0.0").toString must_== "0.0.0.0"
      IpAddress.unchecked("1.2.3.4").toString must_== "1.2.3.4"
    }
    "ipv6" in {
      IpAddress.unchecked("::1").toString must_== "::1"
      IpAddress.unchecked("2a09:bac0:23::815:b5f").toString must_== "2a09:bac0:23::815:b5f"
      IpAddress.unchecked("2A09:BAC0:0023:0000:0000:0000:0815:0B5F").toString must_== "2a09:bac0:23::815:b5f"
    }
  }

  "equality" should {
    "ipv4" in {
      IpAddress.unchecked("0.0.0.0") must_== IpAddress.unchecked("0.0.0.0")
      IpAddress.unchecked("0.0.0.0") must_!= IpAddress.unchecked("0.1.0.0")
    }
    "ipv6" in {
      IpAddress.unchecked("2a09:bac0:23::815:b5f") must_== IpAddress.unchecked("2a09:bac0:23:0::815:b5f")
      IpAddress.unchecked("2a09:bac0:123::815:b5f") must_!= IpAddress.unchecked("2a09:bac0:23::815:b5f")
    }
    "mixed" in {
      IpAddress.unchecked("0.0.0.0") must_!= IpAddress.unchecked("::")
    }
  }

}
