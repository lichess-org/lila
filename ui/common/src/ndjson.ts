import { sync, Sync } from './sync';

export type ProcessLine = (line: any) => void;

export interface CancellableStream {
  cancel(): void;
  end: Sync<Error | undefined>;
}

/*
 * Utility function to read a ND-JSON HTTP stream.
 * `processLine` is a function taking a JSON object. It will be called with each element of the stream.
 * `response` is the result of a `fetch` request.
 * https://gist.github.com/ornicar/a097406810939cf7be1df8ea30e94f3e
 */
export const readNdJson =
  (processLine: ProcessLine) =>
  (response: Response): CancellableStream => {
    const stream = response.body!.getReader();
    const matcher = /\r?\n/;
    const decoder = new TextDecoder();
    let buf = '';

    const loop = (): Promise<Error | undefined> =>
      stream.read().then(({ done, value }) => {
        if (done) {
          if (buf.length > 0) processLine(JSON.parse(buf));
          return Promise.resolve(undefined);
        } else {
          const chunk = decoder.decode(value, { stream: true });
          buf += chunk;
          const parts = buf.split(matcher);
          buf = parts.pop() || '';
          for (const i of parts.filter(p => p)) processLine(JSON.parse(i));
          return loop();
        }
      });

    return {
      cancel: () => stream.cancel(),
      end: sync(loop()),
    };
  };
