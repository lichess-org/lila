export type ProcessLine<T> = (line: T) => void;

/*
 * `response` is the result of a `fetch` request.
 * `processLine` will be called with each element of the stream.
 * https://gist.github.com/ornicar/a097406810939cf7be1df8ea30e94f3e
 */
export const readNdJson = async <T>(response: Response, processLine: ProcessLine<T>): Promise<void> => {
  if (!response.ok) throw new Error(`Status ${response.status}`);
  const stream = response.body!.getReader();
  const matcher = /\r?\n/;
  const decoder = new TextDecoder();
  let buf = '';
  let done, value;
  do {
    ({ done, value } = await stream.read());
    buf += decoder.decode(value || new Uint8Array(), { stream: !done });
    const parts = buf.split(matcher);
    if (!done) buf = parts.pop()!;
    for (const part of parts) if (part) processLine(JSON.parse(part));
  } while (!done);
};
