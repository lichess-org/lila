{
  pkgs,
  lib,
  config,
  ...
}:
{
  # https://devenv.sh/languages/
  languages = {
    java = {
      enable = true;
      jdk.package = pkgs.openjdk21;
    };
    scala = {
      enable = true;
      sbt.enable = true;
    };
    javascript = {
      enable = true;
      pnpm.enable = true;
    };
  };

  # https://devenv.sh/services/
  services = {
    mongodb.enable = true;
    redis.enable = true;
    elasticsearch.enable = true;
  };
}
