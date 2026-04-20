{
  description = "lila development environment for Nix & flakes";

  inputs.nixpkgs.url = "https://flakehub.com/f/NixOS/nixpkgs/0.1"; # unstable Nixpkgs

  outputs =
    { self, ... }@inputs:

    let
      javaVersion = 21;
      # Source of truth for Node version is .node-version
      nodeVersionFile = builtins.readFile ./.node-version;
      nodeMajorVersion = builtins.elemAt (inputs.nixpkgs.lib.strings.split "\\." (builtins.replaceStrings [ "v" ] [ "" ] nodeVersionFile)) 0;
      supportedSystems = [
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ];
      forEachSupportedSystem =
        f:
        inputs.nixpkgs.lib.genAttrs supportedSystems (
          system:
          f {
            pkgs = import inputs.nixpkgs {
              inherit system;
              overlays = [ inputs.self.overlays.default ];
            };
          }
        );
    in
    {
      overlays.default =
        final: prev:
        let
          jdk = prev."jdk${toString javaVersion}";
        in
        rec {
          java = jdk;
          sbt = prev.sbt.override { jre = jdk; };
          scala = prev.scala_3.override { jre = jdk; };

          nodejs = prev."nodejs_${nodeMajorVersion}";
          pnpm = (prev.pnpm.override { inherit nodejs; });

          esbuild = prev.esbuild.overrideAttrs (previousAttrs: rec {
            version = "0.25.11";
            src = prev.fetchFromGitHub {
              owner = "evanw";
              repo = "esbuild";
              rev = "v${version}";
              hash = "sha256-GLvIAVfd7xyCFrYhQWs2TK63voz7gFm3yUXXFq6VZ74=";
            };
          });
        };

      devShells = forEachSupportedSystem (
        { pkgs }:
        {
          default = pkgs.mkShellNoCC {
            packages = with pkgs; [
              nodejs
              pnpm
              esbuild
              dart-sass
              oxlint
              oxfmt
              stylelint

              java
              scala
              sbt
              coursier

              mongosh
              redis
            ];
            # Required for NixOS to run prebuilt binaries from npm packages
            shellHook = ''
              export LD_LIBRARY_PATH=${pkgs.lib.makeLibraryPath [ pkgs.stdenv.cc.cc ]}:$LD_LIBRARY_PATH
              # Use dart-sass instead of npm's sass-embedded
              export SASS_PATH=${pkgs.dart-sass}/bin/sass
            '';
          };
        }
      );
    };
}
