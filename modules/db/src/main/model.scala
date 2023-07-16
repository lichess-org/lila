package lila.db

final class RunCommand(f: (dsl.Bdoc, dsl.ReadPref) => Fu[dsl.Bdoc])
    extends ((dsl.Bdoc, dsl.ReadPref) => Fu[dsl.Bdoc]):
  def apply(d: dsl.Bdoc, r: dsl.ReadPref) = f(d, r)
