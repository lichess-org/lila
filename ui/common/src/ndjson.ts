import { sync, Sync } from './sync';

export type ProcessLine<T> = (line: T) => void;

export interface CancellableStream {
  cancel(): void;
  end: Sync<Error | true>;
}

/*
 * Utility function to read an HTTP stream line by line.
 * `processLine` is a function taking a line string. It will be called with each element of the stream.
 * `response` is the result of a `fetch` request.
 * https://gist.github.com/ornicar/a097406810939cf7be1df8ea30e94f3e
 */
export const readNdJson =
  <T>(processLine: ProcessLine<T>) =>
  (response: Response): CancellableStream => {
    const stream = response.body!.getReader();
    const matcher = /\r?\n/;
    const decoder = new TextDecoder();
    let buf = '';

    const loop = (): Promise<Error | true> =>
      stream.read().then(({ done, value }) => {
        buf += decoder.decode(value || new Uint8Array(), { stream: !done });
        const parts = buf.split(matcher);
        if (!done) buf = parts.pop()!;
        for (const part of parts) if (part) processLine(JSON.parse(part));
        return done ? Promise.resolve(true) : loop();
      });

    return {
      cancel: () => stream.cancel(),
      end: sync(loop()),
    };
  };
