"""SoundCloud account support for the on-device backend (stdlib only).

Official SoundCloud API access needs a registered developer app, so - like the
YouTube Music side of GammaTunes - this talks to the same private ``api-v2``
endpoints that soundcloud.com itself uses. Requests are authenticated with the
``oauth_token`` of a normal web session (captured by the in-app login WebView or
pasted by the user) plus a public ``client_id`` scraped from the site's JS.

Id scheme exposed to the app (all fit the existing Track/Artist/Album models):

    sc_<id>   track      (already used by search / streaming)
    scu_<id>  user       -> "artist"
    scp_<id>  playlist   -> "album" / "playlist"
"""
from __future__ import annotations

import json
import re
import threading
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor

API = "https://api-v2.soundcloud.com"
WEB = "https://soundcloud.com"

TRACK_PREFIX = "sc_"
USER_PREFIX = "scu_"
PLAYLIST_PREFIX = "scp_"

_UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
)
_PAGE_SIZE = 200          # server may cap it; we follow next_href either way
_MAX_PAGES = 100
_HYDRATE_CHUNK = 30


class ScApiError(RuntimeError):
    def __init__(self, message: str, status: int | None = None):
        super().__init__(message)
        self.status = status


class ScAuthError(ScApiError):
    """No / rejected SoundCloud session (maps to HTTP 401 in the backend)."""


_lock = threading.RLock()
_session: dict = {"token": None, "user_id": None, "username": None}
_client_id: str | None = None

# Indirection so tests can run without network.
_urlopen = urllib.request.urlopen


# --------------------------------------------------------------------------
# HTTP plumbing
# --------------------------------------------------------------------------

def _fetch_text(url: str) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": _UA})
    with _urlopen(req, timeout=20) as resp:
        return resp.read().decode("utf-8", errors="replace")


def _get_client_id(force: bool = False) -> str:
    """Public client_id used by soundcloud.com's own web app (scraped, cached)."""
    global _client_id
    with _lock:
        if _client_id and not force:
            return _client_id
        try:
            html = _fetch_text(WEB + "/")
            scripts = re.findall(r'<script[^>]+src="(https://[^"]+\.js)"', html)
            for url in reversed(scripts):
                try:
                    text = _fetch_text(url)
                except Exception:
                    continue
                m = re.search(r'client_id\s*:\s*"([0-9a-zA-Z]{32})"', text)
                if m:
                    _client_id = m.group(1)
                    return _client_id
        except Exception as exc:
            raise ScApiError(f"Не удалось получить client_id SoundCloud: {exc}") from exc
        raise ScApiError("Не удалось получить client_id SoundCloud")


def _error_detail(exc: urllib.error.HTTPError) -> str:
    try:
        text = exc.read().decode("utf-8", errors="replace")
    except Exception:
        return ""
    try:
        data = json.loads(text)
        if isinstance(data, dict):
            errs = data.get("errors")
            if isinstance(errs, list) and errs and isinstance(errs[0], dict):
                return str(errs[0].get("error_message") or errs[0])[:200]
            return str(data.get("error") or data.get("message") or text)[:200]
    except ValueError:
        pass
    return " ".join(text.split())[:200]


def _request(method: str, url: str, params: dict | None = None,
             body: dict | None = None, token: str | None = None):
    """JSON request against api-v2. Attaches the session token when we have one.

    A 401/403 is retried once with a freshly scraped client_id (the cached one
    may simply have rotated); if it still fails the session is reported as
    rejected.
    """
    if url.startswith("/"):
        url = API + url
    tok = token or _session.get("token")

    last_status = None
    last_detail = ""
    for attempt in (0, 1):
        parts = urllib.parse.urlsplit(url)
        query = dict(urllib.parse.parse_qsl(parts.query, keep_blank_values=True))
        for k, v in (params or {}).items():
            query[k] = str(v)
        if attempt == 1 or "client_id" not in query:
            query["client_id"] = _get_client_id(force=(attempt == 1))
        full = urllib.parse.urlunsplit(parts._replace(query=urllib.parse.urlencode(query)))

        headers = {
            "User-Agent": _UA,
            "Accept": "application/json, text/javascript, */*; q=0.01",
            "Origin": WEB,
            "Referer": WEB + "/",
        }
        if tok:
            headers["Authorization"] = f"OAuth {tok}"
        data = None
        if body is not None:
            data = json.dumps(body).encode("utf-8")
            headers["Content-Type"] = "application/json"
        elif method in ("PUT", "POST"):
            data = b""

        req = urllib.request.Request(full, data=data, headers=headers, method=method)
        try:
            with _urlopen(req, timeout=20) as resp:
                raw = resp.read()
        except urllib.error.HTTPError as exc:
            last_status, last_detail = exc.code, _error_detail(exc)
            if exc.code in (401, 403) and attempt == 0:
                continue
            break
        except OSError as exc:  # URLError, timeouts, connection resets
            raise ScApiError(f"Нет связи с SoundCloud: {exc}") from exc

        if not raw:
            return {}
        try:
            return json.loads(raw.decode("utf-8", errors="replace"))
        except ValueError as exc:
            raise ScApiError("Некорректный ответ SoundCloud") from exc

    if last_status in (401, 403) and tok:
        raise ScAuthError("SoundCloud отклонил сессию — войдите заново", last_status)
    raise ScApiError(f"SoundCloud HTTP {last_status}: {last_detail}".strip(), last_status)


def _collect(path: str, params: dict | None = None, limit: int = _PAGE_SIZE) -> list:
    """Follow ``next_href`` pagination and return up to ``limit`` collection items."""
    limit = max(1, int(limit))
    first_params = dict(params or {})
    first_params.setdefault("limit", min(_PAGE_SIZE, limit))
    first_params["linked_partitioning"] = 1
    out: list = []
    url: str | None = path
    for page in range(_MAX_PAGES):
        data = _request("GET", url, params=first_params if page == 0 else None)
        items = data.get("collection") if isinstance(data, dict) else None
        if not items:
            break
        out.extend(items)
        if len(out) >= limit:
            break
        url = data.get("next_href")
        if not url:
            break
    return out[:limit]


def _safe_collect(path: str, limit: int) -> list:
    try:
        return _collect(path, {}, limit)
    except ScApiError as exc:
        print(f"[sc] {path} failed: {exc}")
        return []


# --------------------------------------------------------------------------
# Session
# --------------------------------------------------------------------------

def extract_token(raw: str) -> str:
    """Pull the OAuth token out of whatever the user / WebView gave us.

    Accepts: a bare token, ``oauth_token=...`` (alone or inside a Cookie header),
    an ``Authorization: OAuth ...`` header, or JSON like {"oauthToken": "..."}.
    """
    raw = (raw or "").strip().strip("\"'")
    if not raw:
        raise ValueError("Пустой токен")
    if raw.startswith("{"):
        try:
            data = json.loads(raw)
        except ValueError:
            data = None
        if isinstance(data, dict):
            for key in ("oauthToken", "oauth_token", "access_token", "token"):
                if data.get(key):
                    return str(data[key]).strip()
    m = re.search(r"(?:^|[;\s])oauth_token=([^;\s]+)", raw)
    if m:
        return urllib.parse.unquote(m.group(1)).strip("\"'")
    m = re.search(r"OAuth\s+([^\s\"']+)", raw, re.IGNORECASE)
    if m:
        return m.group(1)
    if re.fullmatch(r"[A-Za-z0-9._~+/=-]{10,}", raw):
        return raw
    raise ValueError(
        "Не найден oauth_token. Войдите через браузер в приложении или вставьте "
        "значение cookie oauth_token с soundcloud.com."
    )


def login(raw: str) -> dict:
    token = extract_token(raw)
    me = _request("GET", "/me", token=token)
    user_id = me.get("id") if isinstance(me, dict) else None
    if not user_id:
        raise ScAuthError("SoundCloud не принял токен (нет профиля)")
    username = me.get("username") or me.get("full_name") or "SoundCloud"
    with _lock:
        _session.update(token=token, user_id=str(user_id), username=str(username))
    return {
        "ok": True,
        "accountName": str(username),
        "authJson": json.dumps({"oauthToken": token}),
    }


def logout() -> None:
    with _lock:
        _session.update(token=None, user_id=None, username=None)


def is_logged_in() -> bool:
    return bool(_session.get("token") and _session.get("user_id"))


def account_name() -> str | None:
    return _session.get("username")


def _require_user() -> str:
    if not is_logged_in():
        raise ScAuthError("Not authenticated")
    return str(_session["user_id"])


# --------------------------------------------------------------------------
# Mapping api-v2 JSON -> the app's Track / Artist / Album / Playlist shapes
# --------------------------------------------------------------------------

_ART_RE = re.compile(
    r"-(?:mini|tiny|small|badge|t67x67|large|t300x300|crop|t500x500|original)\.\w+$"
)


def _art(url: str | None) -> str | None:
    if not url:
        return None
    return _ART_RE.sub("-t500x500.jpg", url)


def _first_track_art(pl: dict) -> str | None:
    for t in pl.get("tracks") or []:
        if isinstance(t, dict) and t.get("artwork_url"):
            return t["artwork_url"]
    return None


def _strip_id(value: str, prefix: str) -> str:
    v = value[len(prefix):] if value.startswith(prefix) else value
    if not v.isdigit():
        raise ValueError(f"Invalid SoundCloud id: {value}")
    return v


def api_track_to_track(t) -> dict | None:
    if not isinstance(t, dict) or not t.get("id") or not t.get("title"):
        return None
    user = t.get("user") or {}
    dur_ms = t.get("full_duration") or t.get("duration")
    uid = user.get("id")
    return {
        "videoId": f"{TRACK_PREFIX}{t['id']}",
        "title": t.get("title") or "Unknown",
        "artist": user.get("username") or "Unknown",
        "album": None,
        "albumId": None,
        "thumbnail": _art(t.get("artwork_url") or user.get("avatar_url")),
        "durationSeconds": int(dur_ms // 1000) if isinstance(dur_ms, (int, float)) and dur_ms else None,
        "artistId": f"{USER_PREFIX}{uid}" if uid else None,
        "isVideo": False,
    }


def _hydrate(items: list) -> list:
    """api-v2 playlists only inline the first few tracks; fetch the rest by id."""
    missing = [
        it["id"] for it in items
        if isinstance(it, dict) and it.get("id") and not it.get("title")
    ]
    full: dict = {}
    chunks = [missing[i:i + _HYDRATE_CHUNK] for i in range(0, len(missing), _HYDRATE_CHUNK)]

    def fetch(chunk):
        data = _request("GET", "/tracks", params={"ids": ",".join(str(i) for i in chunk)})
        return data if isinstance(data, list) else []

    if chunks:
        with ThreadPoolExecutor(max_workers=4) as pool:
            for result in pool.map(fetch, chunks):
                for t in result:
                    if isinstance(t, dict) and t.get("id"):
                        full[t["id"]] = t
    out = []
    for it in items:
        if not isinstance(it, dict):
            continue
        if it.get("title"):
            out.append(it)
        elif it.get("id") in full:
            out.append(full[it["id"]])
    return out


def _to_tracks(raw_tracks: list) -> list:
    seen: set = set()
    tracks = []
    for t in _hydrate(raw_tracks):
        tr = api_track_to_track(t)
        if tr and tr["videoId"] not in seen:
            seen.add(tr["videoId"])
            tracks.append(tr)
    return tracks


def _unwrap_playlist(item) -> dict | None:
    if not isinstance(item, dict):
        return None
    pl = item.get("playlist") if isinstance(item.get("playlist"), dict) else item
    pid = pl.get("id")
    if not pid or not str(pid).isdigit() or not pl.get("title"):
        return None  # system playlists / mixes have no numeric id
    return pl


def _playlist_summary(pl: dict, my_id: str | None) -> dict:
    owner = (pl.get("user") or {}).get("id")
    return {
        "playlistId": f"{PLAYLIST_PREFIX}{pl['id']}",
        "title": pl.get("title") or "Unknown",
        "thumbnail": _art(
            pl.get("artwork_url") or _first_track_art(pl) or (pl.get("user") or {}).get("avatar_url")
        ),
        "count": pl.get("track_count"),
        "readOnly": bool(my_id and owner is not None and str(owner) != str(my_id)),
    }


def _playlist_to_album(pl: dict) -> dict:
    stamp = pl.get("release_date") or pl.get("display_date") or pl.get("created_at") or ""
    return {
        "albumId": f"{PLAYLIST_PREFIX}{pl['id']}",
        "title": pl.get("title") or "Unknown",
        "thumbnail": _art(
            pl.get("artwork_url") or _first_track_art(pl) or (pl.get("user") or {}).get("avatar_url")
        ),
        "year": stamp[:4] if stamp[:4].isdigit() else None,
    }


# --------------------------------------------------------------------------
# Public operations used by the HTTP handler
# --------------------------------------------------------------------------

def liked(limit: int = 5000) -> list:
    uid = _require_user()
    items = _collect(f"/users/{uid}/track_likes", {}, limit)
    return _to_tracks([it.get("track") for it in items if isinstance(it, dict) and it.get("track")])


def playlists(limit: int = 100) -> list:
    uid = _require_user()
    try:
        items = _collect(f"/users/{uid}/playlists/liked_and_owned", {}, limit)
    except ScAuthError:
        raise
    except ScApiError as exc:
        # Endpoint missing/changed: assemble the same view from two simpler ones.
        print(f"[sc] liked_and_owned failed ({exc}); falling back")
        items = _collect(f"/users/{uid}/playlists_without_albums", {}, limit)
        items += _safe_collect(f"/users/{uid}/playlist_likes", limit)
    out, seen = [], set()
    for it in items:
        pl = _unwrap_playlist(it)
        if pl and pl["id"] not in seen:
            seen.add(pl["id"])
            out.append(_playlist_summary(pl, uid))
    return out


def _load_playlist(playlist_id: str) -> tuple:
    pid = _strip_id(playlist_id, PLAYLIST_PREFIX)
    pl = _request("GET", f"/playlists/{pid}")
    if not isinstance(pl, dict) or not pl.get("id"):
        raise ScApiError("SoundCloud playlist not found", 404)
    return pid, pl, _to_tracks(pl.get("tracks") or [])


def playlist_tracks(playlist_id: str) -> dict:
    pid, pl, tracks = _load_playlist(playlist_id)
    return {"playlistId": f"{PLAYLIST_PREFIX}{pid}", "title": pl.get("title") or pid, "tracks": tracks}


def album_detail(album_id: str) -> dict:
    pid, pl, tracks = _load_playlist(album_id)
    title = pl.get("title") or "Unknown"
    thumb = _art(pl.get("artwork_url")) or next((t["thumbnail"] for t in tracks if t["thumbnail"]), None)
    for t in tracks:
        t["album"] = title
        t["albumId"] = f"{PLAYLIST_PREFIX}{pid}"
        t["thumbnail"] = t["thumbnail"] or thumb
    return {"albumId": f"{PLAYLIST_PREFIX}{pid}", "title": title, "thumbnail": thumb, "tracks": tracks}


def rate(video_id: str, rating: str) -> None:
    uid = _require_user()
    tid = _strip_id(video_id, TRACK_PREFIX)
    rating = rating.upper()
    if rating == "LIKE":
        _request("PUT", f"/users/{uid}/track_likes/{tid}")
    elif rating == "INDIFFERENT":
        _request("DELETE", f"/users/{uid}/track_likes/{tid}")
    else:
        raise ValueError("SoundCloud supports only LIKE and INDIFFERENT")


def add_to_playlist(playlist_id: str, video_id: str) -> None:
    uid = _require_user()
    pid = _strip_id(playlist_id, PLAYLIST_PREFIX)
    tid = int(_strip_id(video_id, TRACK_PREFIX))
    pl = _request("GET", f"/playlists/{pid}")
    owner = (pl.get("user") or {}).get("id") if isinstance(pl, dict) else None
    if owner is not None and str(owner) != uid:
        raise ValueError("В SoundCloud можно добавлять треки только в свои плейлисты")
    ids = [t["id"] for t in (pl.get("tracks") or []) if isinstance(t, dict) and t.get("id")]
    if tid in ids:
        return
    ids.append(tid)
    try:
        _request("PUT", f"/playlists/{pid}", body={"playlist": {"tracks": [{"id": i} for i in ids]}})
    except ScAuthError:
        raise
    except ScApiError as exc:
        if exc.status not in (400, 422):
            raise
        # Same "replace the track list" semantics, web-app flavoured body.
        _request("PUT", f"/playlists/{pid}", body={"tracks": ids})


def search_artists(query: str, limit: int = 20) -> list:
    data = _request("GET", "/search/users", params={"q": query, "limit": max(1, min(50, limit))})
    out = []
    for u in (data.get("collection") if isinstance(data, dict) else None) or []:
        if not isinstance(u, dict) or not u.get("id"):
            continue
        out.append({
            "artistId": f"{USER_PREFIX}{u['id']}",
            "name": u.get("username") or u.get("full_name") or "Unknown",
            "thumbnail": _art(u.get("avatar_url")),
            "albums": [],
        })
    return out


def artist_detail(artist_id: str) -> dict:
    uid = _strip_id(artist_id, USER_PREFIX)
    with ThreadPoolExecutor(max_workers=5) as pool:
        f_user = pool.submit(_request, "GET", f"/users/{uid}")
        f_top = pool.submit(_safe_collect, f"/users/{uid}/toptracks", 10)
        f_tracks = pool.submit(_safe_collect, f"/users/{uid}/tracks", 300)
        f_albums = pool.submit(_safe_collect, f"/users/{uid}/albums", 100)
        f_pls = pool.submit(_safe_collect, f"/users/{uid}/playlists_without_albums", 100)
        user = f_user.result()  # mandatory: let errors surface
        top, tracks, albums, pls = (f.result() for f in (f_top, f_tracks, f_albums, f_pls))
    if not isinstance(user, dict) or not user.get("id"):
        raise ScApiError("SoundCloud user not found", 404)

    # "Popular" first (top tracks), then the rest of the catalogue newest-first.
    songs, seen = [], set()
    for t in _to_tracks([x for x in top + tracks if isinstance(x, dict)]):
        if t["videoId"] not in seen:
            seen.add(t["videoId"])
            songs.append(t)
    return {
        "artistId": f"{USER_PREFIX}{uid}",
        "name": user.get("username") or user.get("full_name") or "Unknown",
        "thumbnail": _art(user.get("avatar_url")),
        "albums": [_playlist_to_album(p) for p in map(_unwrap_playlist, albums) if p],
        # The app's second shelf ("singles") carries the artist's playlists here.
        "singles": [_playlist_to_album(p) for p in map(_unwrap_playlist, pls) if p],
        "songs": songs,
    }
