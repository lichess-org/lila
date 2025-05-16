package lila.fide

final class FederationApi(repo: FideRepo):

  export repo.federation.fetch

  def find(str: String): Fu[Option[Federation]] =
    Federation.find(str).so(fetch)
