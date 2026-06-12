# comunio-news — server deployment

Runs as a standalone stack behind the shared **edge-caddy** (TLS + the
`news.zingler46.unividuell.org` route **and its basicauth** live in the edge, not here).
This stack is just `app` + `redis`; it publishes no host ports. `app` joins the external
`edge` network as `comunio-news-app`; `redis` stays on an internal net.

Server dir: **`/opt/unividuell/comunio-news/`**. Images: `ghcr.io/unividuell/comunio-news:latest`
(public package — pulls without auth).

The app stores its **H2 file DB** under `./data/` (bind-mounted to `/data`); the host dir must be
owned by the buildpack run user (uid `1002`, gid `1000`) or H2 can't create the file:

```bash
sudo mkdir -p /opt/unividuell/comunio-news/data
sudo chown -R 1002:1000 /opt/unividuell/comunio-news/data
```

## Bootstrap / update
```bash
mkdir -p /opt/unividuell/comunio-news && cd /opt/unividuell/comunio-news
curl -fsSL https://raw.githubusercontent.com/unividuell/comunio-news/main/deploy/update.sh -o update.sh && chmod +x update.sh
./update.sh          # fetches compose + .env template, then stops
# edit .env: GOOGLE_GENAI_API_KEY, OPENAI_API_KEY, STATS_COMUNIO_USER, STATS_COMUNIO_PW
./update.sh          # ensures edge net, pulls, starts app + redis
```

The shared edge-caddy stack must be up (it owns 80/443 + TLS + the basicauth for this site).
redis data is treated as a disposable cache (fresh `comunio-news_redis-data` volume is fine).
