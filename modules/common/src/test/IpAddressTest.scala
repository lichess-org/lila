package lila.common

import org.specs2.mutable.*

class IpAddressTest extends Specification {

  "iso" >> {
    "ipv4" >> {
      IpAddress.unchecked("0.0.0.0").toString === "0.0.0.0"
      IpAddress.unchecked("1.2.3.4").toString === "1.2.3.4"
    }
    "ipv6" >> {
      IpAddress.unchecked("::1").toString === "::1"
      IpAddress.unchecked("0:0:0:0:0:0:0:1").toString === "::1"
      IpAddress.unchecked("2a09:bac0:23::815:b5f").toString === "2a09:bac0:23::815:b5f"
      IpAddress.unchecked("2A09:BAC0:0023:0000:0000:0000:0815:0B5F").toString === "2a09:bac0:23::815:b5f"
    }
  }

  "equality" >> {
    "ipv4" >> {
      IpAddress.unchecked("0.0.0.0") === IpAddress.unchecked("0.0.0.0")
      IpAddress.unchecked("0.0.0.0") !== IpAddress.unchecked("0.1.0.0")
    }
    "ipv6" >> {
      IpAddress.unchecked("2a09:bac0:23::815:b5f") === IpAddress.unchecked("2a09:bac0:23:0::815:b5f")
      IpAddress.unchecked("2a09:bac0:123::815:b5f") !== IpAddress.unchecked("2a09:bac0:23::815:b5f")
    }
    "mixed" >> {
      IpAddress.unchecked("0.0.0.0") !== IpAddress.unchecked("::")
    }
  }

}
