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
  sasso = inputs.sasso.packages.${pkgs.stdenv.system}.default;
in
{
  env = {
    pnpm_config_pm_on_fail = "error";
    REDIS_URI = "redis://127.0.0.1:${toString config.processes.redis.ports.main.value}";
  };

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

  processes = {
    lila = {
      start.enable = false;
      exec = "./lila.sh run";
    };
    lila-ws = {
      start.enable = false;
      env = {
        JAVA_TOOL_OPTIONS = "-Dredis.uri=${config.env.REDIS_URI}";
      };
    };
    ui = {
      start.enable = false;
      exec = "./ui/build --watch --debug";
    };
  };

  packages = [
    pkgs-unstable.nodejs-slim
    pkgs-master.pnpm_12
    pkgs.svgo
    pkgs-master.oxlint
    pkgs-master.oxfmt
    pkgs-master.tsgolint
    pkgs.lint-staged
    pkgs-unstable.stylelint
    sasso
  ];

  tasks = {
    "lint:code" = {
      exec = "oxlint --type-aware";
    };
    "lint:style" = {
      exec = ''stylelint "ui/**/*.scss" --fix'';
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
