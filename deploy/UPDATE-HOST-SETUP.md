# FlowVan auto-update host — Ubuntu setup

The app fetches a **static JSON manifest + APK** over HTTPS. No backend code — the
host just serves files at:

```
https://7softwarejo.com/flowvan/updates/<customer>/android.json
https://7softwarejo.com/flowvan/updates/<customer>/flowvan-<versionCode>.apk
```

Customers today: `ferdous`, `dev`, `tal3at` (see `build-logic/.../CustomerFlavors.kt`).
The app requests the manifest cache-busted (`?t=…`) and Cloudflare sits in front of
this host. See `docs/auto-update.md` for the app side.

---

## 0. Find what already serves 7softwarejo.com (don't install a 2nd server)

HTTPS already works on this box, so something holds :443. Identify it first:

```bash
sudo ss -ltnp | grep -E ':(80|443)\b'      # which process owns 80/443
systemctl is-active nginx caddy apache2 2>/dev/null
docker ps --format '{{.Names}}\t{{.Ports}}' 2>/dev/null | grep -E '443|80'   # if it's containerised
```

- `nginx` → §1  ·  `caddy` → §2  ·  `apache2` → §3
- A **docker** container on 443 (e.g. a Caddy/nginx reverse proxy) → edit that
  container's config instead, same directives as below.

Then create the content directory (used by all options):

```bash
sudo mkdir -p /var/www/flowvan/updates/{ferdous,dev,tal3at}
sudo chown -R "$USER":www-data /var/www/flowvan
sudo find /var/www/flowvan -type d -exec chmod 755 {} \;
```

The URL path `/flowvan/updates/…` maps 1:1 to `/var/www/flowvan/updates/…`.

---

## 1. nginx

Add inside the existing `server { … }` block for `7softwarejo.com` (usually
`/etc/nginx/sites-available/…` or `/etc/nginx/conf.d/…`):

```nginx
# --- FlowVan auto-update (static files) ---
# APK: versioned filename, immutable → cache hard.
location ~ ^/flowvan/updates/.+\.apk$ {
    root /var/www;
    default_type application/vnd.android.package-archive;
    add_header Cache-Control "public, max-age=31536000, immutable";
    add_header Access-Control-Allow-Origin "*";
}
# Manifest (and anything else here): must never be stale.
location /flowvan/updates/ {
    root /var/www;
    default_type application/json;
    add_header Cache-Control "no-store";
    add_header Access-Control-Allow-Origin "*";
    autoindex off;
}
```

```bash
sudo nginx -t && sudo systemctl reload nginx
```

## 2. Caddy

In the `7softwarejo.com { … }` site block of `/etc/caddy/Caddyfile`:

```caddy
7softwarejo.com {
    # …existing config…

    @apk    path /flowvan/updates/*.apk
    @manif  path /flowvan/updates/*android.json

    handle_path /flowvan/updates/* {
        root * /var/www/flowvan/updates
        file_server
        header @apk   Content-Type "application/vnd.android.package-archive"
        header @apk   Cache-Control "public, max-age=31536000, immutable"
        header @manif Content-Type "application/json"
        header @manif Cache-Control "no-store"
        header Access-Control-Allow-Origin "*"
    }
}
```

```bash
sudo caddy validate --config /etc/caddy/Caddyfile && sudo systemctl reload caddy
```

## 3. Apache

```apache
Alias /flowvan/updates /var/www/flowvan/updates
<Directory /var/www/flowvan/updates>
    Require all granted
    Options -Indexes
    AddType application/vnd.android.package-archive .apk
    <FilesMatch "\.apk$">
        Header set Cache-Control "public, max-age=31536000, immutable"
    </FilesMatch>
    <FilesMatch "android\.json$">
        Header set Cache-Control "no-store"
    </FilesMatch>
    Header set Access-Control-Allow-Origin "*"
</Directory>
```

```bash
sudo a2enmod headers && sudo apachectl configtest && sudo systemctl reload apache2
```

---

## 4. Cloudflare (it's in front of this host)

- **SSL/TLS mode:** Full (or Full strict) — the origin already serves HTTPS.
- The app cache-busts the manifest with `?t=`, so a stale floor is unlikely, but to
  be safe add a **Cache Rule**: if URI path contains `/flowvan/updates/` and ends
  with `android.json` → **Bypass cache**. Leave `.apk` cached (it's versioned).

---

## 5. First publish + verify

Put a real APK + its manifest in one customer's folder (example: ferdous):

```bash
# copy the built APK over (versioned name = its versionCode)
sudo cp flowvan-<versionCode>.apk /var/www/flowvan/updates/ferdous/
sha256=$(sha256sum /var/www/flowvan/updates/ferdous/flowvan-<versionCode>.apk | cut -d' ' -f1)
size=$(stat -c%s /var/www/flowvan/updates/ferdous/flowvan-<versionCode>.apk)

sudo tee /var/www/flowvan/updates/ferdous/android.json >/dev/null <<JSON
{
  "versionCode": <versionCode>,
  "versionName": "<versionName>",
  "apkUrl": "https://7softwarejo.com/flowvan/updates/ferdous/flowvan-<versionCode>.apk",
  "sha256": "$sha256",
  "sizeBytes": $size,
  "minVersionCode": 0,
  "notes": ""
}
JSON
sudo chown www-data:www-data /var/www/flowvan/updates/ferdous/*
```

Verify from anywhere:

```bash
curl -sS "https://7softwarejo.com/flowvan/updates/ferdous/android.json?t=$(date +%s)"     # → the JSON, no HTML, no redirect
curl -sI "https://7softwarejo.com/flowvan/updates/ferdous/flowvan-<versionCode>.apk" | grep -i -E 'content-type|content-length'
# content-type MUST be application/vnd.android.package-archive
```

Both 200, JSON is plain (no `{success,data}` envelope), APK content-type correct → the host is done.

---

## 6. Per release (afterwards)

1. Bump `flowvan.versionCode` (+ `flowvan.versionName`) in `gradle.properties`, build the customer variant.
2. Upload the two files to that customer's folder (APK + rewritten `android.json`).
3. Handsets take it on next app resume. **`versionCode` must always increase** or nothing updates.

A `scripts/publish-update.sh` can generate `android.json` (sha256 + size) automatically — ask and I'll add it.
