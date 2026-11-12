import { HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformServer } from '@angular/common';
import { timeout } from 'rxjs';

/** How long a server render waits for the API before giving up (and answering 503). */
const SERVER_REQUEST_TIMEOUT_MS = 5000;

/** Server renders must not hang on a slow or unreachable API; browsers keep their normal behaviour. */
export function serverTimeoutInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn) {
  return isPlatformServer(inject(PLATFORM_ID)) ? next(req).pipe(timeout(SERVER_REQUEST_TIMEOUT_MS)) : next(req);
}
