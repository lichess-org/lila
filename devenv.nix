{
  pkgs,
  lib,
  config,
  inputs,
  ...
}:
let
  pkgs-master = import inputs.nixpkgs-master { system = pkgs.stdenv.system; };
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
      enable = false; # it adds node_modules/.bin to the $PATH!
    };
  };

  # https://devenv.sh/services/
  services = {
    mongodb.enable = true;
    redis.enable = true;
  };

  packages = [
    pkgs.nodejs-slim
    pkgs.pnpm
    pkgs.svgo
    pkgs-master.oxlint
    pkgs-master.oxfmt
    pkgs.lint-staged
    pkgs.stylelint
  ];

  # pnpm installs dynamically linked binaries there,
  # and forcefully adds the directory to the $PATH.
  enterShell = ''
    rm -rf node_modules/.bin
  '';
}
