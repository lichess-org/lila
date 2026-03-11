{
  pkgs,
  lib,
  config,
  inputs,
  ...
}:
let
  pkgs-oxfmt-pr = import inputs.oxfmt-pr { system = pkgs.stdenv.system; };
in
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
  };

  packages = [
    pkgs.svgo
    pkgs.oxlint
    pkgs-oxfmt-pr.oxfmt
  ];
}
