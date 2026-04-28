{
  pkgs,
  lib,
  config,
  inputs,
  ...
}:
let
  pkgs-master = import inputs.nixpkgs-master { system = pkgs.stdenv.system; };
  pkgs-unstable = import inputs.nixpkgs-unstable { system = pkgs.stdenv.system; };
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
    pkgs-unstable.nodejs-slim
    pkgs.pnpm
    pkgs.svgo
    pkgs-master.oxlint
    pkgs-master.oxfmt
    pkgs-master.tsgolint
    pkgs.lint-staged
    pkgs.stylelint
    pkgs.dart-sass
  ];

  tasks = {
    "lint:code" = {
      exec = "oxlint --type-aware --tsconfig=ui/tsconfig.base.json";
    };
    "lint:style" = {
      exec = ''stylelint "ui/**/*.scss"'';
    };
    "format:ui" = {
      exec = "oxfmt";
    };
    "clean:bin" = {
      # pnpm installs dynamically linked binaries there,
      # and forcefully adds the directory to the $PATH.
      exec = "rm -rf node_modules/.bin";
      before = [ "devenv:enterShell" ];
    };
  };
}
