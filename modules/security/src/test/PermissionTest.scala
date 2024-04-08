package lila.security

import lila.core.perm.*

class PermissionTest extends munit.FunSuite:

  test("Valid permission tree"):
    Permission.all.foreach: p =>
      p.alsoGrants.foreach: child =>
        assert(p.grants(child))
