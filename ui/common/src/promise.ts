export interface Sync<T> {
  promise: Promise<T>;
  sync: T | undefined;
}

export function sync<T>(promise: Promise<T>): Sync<T> {
  const sync: Sync<T> = {
    sync: undefined,
    promise: promise.then(v => {
      sync.sync = v;
      return v;
    }),
  };
  return sync;
}

// Call an async function with a maximum time limit (in milliseconds) for the timeout
export async function promiseTimeout<A>(asyncPromise: Promise<A>, timeLimit: number): Promise<A> {
  let timeoutHandle: Timeout | undefined = undefined;

  const timeoutPromise = new Promise<A>((_, reject) => {
    timeoutHandle = setTimeout(() => reject(new Error('Async call timeout limit reached')), timeLimit);
  });

  const result = await Promise.race([asyncPromise, timeoutPromise]);
  if (timeoutHandle) clearTimeout(timeoutHandle);
  return result;
}
