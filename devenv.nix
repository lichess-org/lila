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

  lilaHost = "lila.localhost";
  port = name: toString config.processes.${name}.ports.http.value;
  caddyfile = pkgs.writeText "Caddyfile" ''
    {
      persist_config off
      servers {
        trusted_proxies static private_ranges
      }
    }

    http://:${port "caddy"} {
      @insecure not header X-Forwarded-Proto https
      redir @insecure https://${lilaHost}{uri}

      @websocket header Upgrade websocket
      reverse_proxy @websocket 127.0.0.1:${port "lila-ws"}
      reverse_proxy 127.0.0.1:${port "lila"}

      handle_errors 502 {
        header Content-Type "text/plain; charset=utf-8"
        respond @websocket "lila-ws is not running. Start it with: devenv processes start lila-ws" 502
        respond <<TEXT
             |\_    _ _      _
             /o \  | (_) ___| |__   ___  ___ ___   ___  _ __ __ _
           (_. ||  | | |/ __| '_ \ / _ \/ __/ __| / _ \| '__/ _` |
             /__\  | | | (__| | | |  __/\__ \__ \| (_) | | | (_| |
            )___(  |_|_|\___|_| |_|\___||___/___(_)___/|_|  \__, |
                                                             |___/

          lila is not running, or is still starting up.

          Start it with:

              devenv processes start lila

          then reload this page once it is listening on port ${port "lila"}.
          TEXT 502
      }
    }
  '';
  javaOpts = opts: lib.concatStringsSep " " (lib.mapAttrsToList (k: v: "-D${k}=${toString v}") opts);
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

  process.proxy.enable = true;

  processes = {
    caddy = {
      exec = "exec ${pkgs.caddy}/bin/caddy run --adapter caddyfile --config ${caddyfile}";
      ports.http.allocate = 9660;
      proxy = {
        hostname = lilaHost;
        https.enable = true;
      };
    };
    lila = {
      start.enable = false;
      exec = "./lila.sh run";
      ports.http.allocate = 9663;
      env = {
        JAVA_TOOL_OPTIONS = javaOpts {
          "http.port" = config.processes.lila.ports.http.value;
          "net.domain" = lilaHost;
          "net.base_url" = "https://${lilaHost}";
          "net.socket.domains.0" = lilaHost;
        };
      };
    };
    lila-ws = {
      start.enable = false;
      ports.http.allocate = 9664;
      env = {
        JAVA_TOOL_OPTIONS = javaOpts {
          "redis.uri" = config.env.REDIS_URI;
          "http.port" = config.processes.lila-ws.ports.http.value;
          "csrf.origin" = "https://${lilaHost}";
        };
      };
    };
    lila-fishnet = {
      start.enable = false;
      env = {
        REDIS_PORT = toString config.processes.redis.ports.main.value;
      };
    };
    fishnet-play = {
      start.enable = false;
    };
    fishnet-analyze = {
      start.enable = false;
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
