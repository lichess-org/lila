package lila.team

import lila.db.dsl.Coll

private final class Colls(
    val team: Coll,
    val request: Coll,
    val member: Coll
)
