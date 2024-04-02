package lila.relation

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

export lila.core.relation.{ Relation, Block, Follow }

private type OnlineStudyingCache = com.github.blemale.scaffeine.Cache[UserId, String]
