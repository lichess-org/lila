package lila.db

import reactivemongo.api.ReadPreference

final class RunCommand(f: (dsl.Bdoc, ReadPreference) => Fu[dsl.Bdoc])
    extends ((dsl.Bdoc, ReadPreference) => Fu[dsl.Bdoc]) {
  def apply(d: dsl.Bdoc, r: ReadPreference) = f(d, r)
}
