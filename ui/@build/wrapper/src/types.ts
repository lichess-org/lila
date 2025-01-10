import type { WatchEventType } from 'node:fs';

export type PackageName = string;

export interface PackageInfo {
  name: PackageName;
  deps: PackageName[];
  revDeps: PackageName[];
  path: string;
  lishogi?: Record<string, any>;
}

export interface PackagesWrap {
  root: PackageInfo;
  packages: PackageInfo[];
}

export type WatchPaths = { path: string; options: Record<string, any> }[];

export interface Context {
  name: string;
  init(pkgs: PackagesWrap, flags: string[]): Promise<void>;
  packageWatch: WatchPaths;
  run(pkg: PackageInfo, event: WatchEventType, filepath: string | null): Promise<void>;
  runAll(): Promise<void>;
  globalWatch?: WatchPaths;
  global?(event: WatchEventType, filepath: string | null): Promise<void>;
  stop(): Promise<void>;
}
