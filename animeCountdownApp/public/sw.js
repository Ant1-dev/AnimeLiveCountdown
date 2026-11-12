/*
 * Anime Live Countdown service worker.
 *
 * Handles Web Push (episode reminders) and makes the site installable. It deliberately does not
 * cache pages or assets, so a new deploy is always picked up on the next load.
 */
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (event) => event.waitUntil(self.clients.claim()));

self.addEventListener('push', (event) => {
  let data = {};
  try {
    data = event.data ? event.data.json() : {};
  } catch (e) {
    data = { title: 'Anime Live Countdown', body: event.data ? event.data.text() : '' };
  }

  const title = data.title || 'Anime Live Countdown';
  const options = {
    body: data.body || '',
    icon: data.icon || '/icons/android-chrome-192x192.png',
    badge: '/icons/badge-72x72.png',
    tag: data.tag || undefined,
    renotify: !!data.tag,
    data: { url: data.url || '/' },
  };

  event.waitUntil(
    self.registration.showNotification(title, options).then(() =>
      // Let open tabs refresh the notification bell
      self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clients) => {
        clients.forEach((client) => client.postMessage({ type: 'notification-received' }));
      })
    )
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const target = new URL((event.notification.data && event.notification.data.url) || '/', self.location.origin);
  // Only ever open pages on this site
  const url = target.origin === self.location.origin ? target.href : self.location.origin + '/';

  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clients) => {
      for (const client of clients) {
        if (client.url === url && 'focus' in client) return client.focus();
      }
      const sameSite = clients.find((client) => new URL(client.url).origin === self.location.origin);
      if (sameSite && 'navigate' in sameSite) {
        return sameSite.focus().then((focused) => (focused || sameSite).navigate(url));
      }
      return self.clients.openWindow(url);
    })
  );
});
