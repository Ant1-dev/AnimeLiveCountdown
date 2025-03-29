import {
  AngularNodeAppEngine,
  createNodeRequestHandler,
  isMainModule,
  writeResponseToNodeResponse,
} from '@angular/ssr/node';
import express from 'express';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import compression from 'compression'

const serverDistFolder = dirname(fileURLToPath(import.meta.url));
const browserDistFolder = resolve(serverDistFolder, '../browser');
const app = express();
const angularApp = new AngularNodeAppEngine();

// Add compression middleware - this is the key addition
app.use(compression({
  level: 6, // Default compression level is 6, can increase up to 9 for max compression
  threshold: 0, // Compress all responses
  filter: (req, res) => {
    // Compress everything except images and videos
    if (req.headers['accept']?.includes('image/') || 
        req.headers['accept']?.includes('video/')) {
      return false;
    }
    return compression.filter(req, res);
  }
}));

/**
 * Serve static files from /browser with improved cache settings
 */
app.use(
  express.static(browserDistFolder, {
    maxAge: '1y',
    index: false,
    redirect: false,
    etag: true, // Enable ETags
    lastModified: true, // Enable Last-Modified
  }),
);

/**
 * Handle all other requests by rendering the Angular application.
 */
app.use('/**', (req, res, next) => {
  angularApp
    .handle(req)
    .then((response) =>
      response ? writeResponseToNodeResponse(response, res) : next(),
    )
    .catch(next);
});

/**
 * Start the server if this module is the main entry point.
 */
if (isMainModule(import.meta.url)) {
  const port = process.env['PORT'] || 4000;
  app.listen(port, () => {
    console.log(`Node Express server listening on http://localhost:${port}`);
  });
}

/**
 * Request handler used by the Angular CLI or Firebase Cloud Functions.
 */
export const reqHandler = createNodeRequestHandler(app);