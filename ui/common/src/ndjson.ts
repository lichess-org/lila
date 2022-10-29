export class NdJsonSink<T> implements UnderlyingSink<Uint8Array> {
  private matcher = /\r?\n/;
  private decoder = new TextDecoder();
  private buf = '';

  constructor(readonly processLine: (line: T) => void) {}

  write(chunk: Uint8Array) {
    this.handleChunk(false, chunk);
  }

  close() {
    this.handleChunk(true, new Uint8Array());
  }

  private handleChunk(done: boolean, chunk: Uint8Array) {
    this.buf += this.decoder.decode(chunk, { stream: !done });
    const parts = this.buf.split(this.matcher);
    if (!done) this.buf = parts.pop()!;
    for (const part of parts) if (part) this.processLine(JSON.parse(part));
  }
}
