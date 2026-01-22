{
  description = "lila development environment for Nix & flakes";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable"; # unstable Nixpkgs

  outputs = { self, ... }@inputs:

    let
      javaVersion = 25;
      supportedSystems =
        [ "x86_64-linux" "aarch64-linux" "x86_64-darwin" "aarch64-darwin" ];
      forEachSupportedSystem = f:
        inputs.nixpkgs.lib.genAttrs supportedSystems (system:
          f {
            pkgs = import inputs.nixpkgs {
              inherit system;
              overlays = [ inputs.self.overlays.default ];
            };
          });
    in {
      overlays.default = final: prev:
        let jdk = prev."jdk${toString javaVersion}";
        in rec {

          sbt = prev.sbt.override { jre = jdk; };
          scala = prev.scala_3.override { jre = jdk; };

          nodejs_24 = prev.nodejs_24;
          pnpm = (prev.pnpm.override { nodejs = prev.nodejs_24; });

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

      devShells = forEachSupportedSystem ({ pkgs }: {
        default = pkgs.mkShellNoCC {
          packages = with pkgs; [
            nodejs_24
            nodePackages.pnpm
            esbuild

            scala
            sbt
            coursier
          ];
        };
      });
    };
}
