import { Config } from './interfaces';
export declare class Clock {
    readonly config: Config;
    startAt: number | undefined;
    initialMillis: number;
    constructor(config: Config, startedMillisAgo?: number);
    start: () => void;
    started: () => boolean;
    millis: () => number;
    addSeconds: (seconds: number) => void;
    flag: () => boolean;
}
