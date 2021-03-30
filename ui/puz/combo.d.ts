import { Config, TimeMod } from './interfaces';
export declare class Combo {
    readonly config: Config;
    current: number;
    best: number;
    constructor(config: Config);
    inc: () => void;
    reset: () => void;
    level: () => number;
    percent: () => number;
    bonus: () => TimeMod | undefined;
}
