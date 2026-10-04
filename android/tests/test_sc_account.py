"""Offline tests for the SoundCloud account layer (no network, no Android).

Run from the repo root:  python3 -m unittest discover -s android/tests -v

SoundCloud is replaced by a small fake that answers in the api-v2 shape the
code expects, so these check OUR logic (parsing, pagination, routing, error
mapping). They cannot prove SoundCloud's live API still behaves this way.
"""
import io
import json
import os
import sys
import threading
import types
import unittest
import urllib.error
import urllib.parse
import urllib.request
from http.server import ThreadingHTTPServer

PY_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "python")
sys.path.insert(0, os.path.abspath(PY_DIR))

import sc_account as sc  # noqa: E402

CLIENT_ID = "abcdefghijklmnopqrstuvwxyz012345"
TOKEN = "2-293854-42-AbCdEfGhIj"


def track(i, title=None, uid=7, user="Artist"):
    t = {"id": i, "kind": "track"}
    if title is not None:
        t.update(title=title, duration=185000, full_duration=185000,
                 artwork_url=f"https://i1.sndcdn.com/artworks-{i}-large.jpg",
                 user={"id": uid, "username": user, "avatar_url": "https://i1.sndcdn.com/avatars-x-large.jpg"})
    return t


class Resp(io.BytesIO):
    def __enter__(self):
        return self

    def __exit__(self, *a):
        return False


class FakeSoundCloud:
    """Routes by (method, path); records every request."""

    def __init__(self):
        self.calls = []
        self.reject_client_id = None      # client_id value that yields 401
        self.reject_token = False
        self.put_fail_first_shape = False
        self.cid_served = [CLIENT_ID]

    def __call__(self, req, timeout=None):
        url = req.full_url
        method = req.get_method()
        parts = urllib.parse.urlsplit(url)
        q = dict(urllib.parse.parse_qsl(parts.query))
        body = json.loads(req.data) if req.data else None
        self.calls.append((method, parts.path, q, body, req.headers.get("Authorization")))

        if parts.netloc == "soundcloud.com":
            return Resp(b'<html><script crossorigin src="https://a-v2.sndcdn.com/assets/1-x.js"></script></html>')
        if parts.netloc == "a-v2.sndcdn.com":
            return Resp(f'foo({{client_id:"{self.cid_served[0]}"}})'.encode())

        if self.reject_client_id and q.get("client_id") == self.reject_client_id:
            raise urllib.error.HTTPError(url, 401, "no", {}, io.BytesIO(b'{"error":"bad client"}'))
        if self.reject_token or (req.headers.get("Authorization") not in (None, f"OAuth {TOKEN}")):
            raise urllib.error.HTTPError(url, 401, "no", {}, io.BytesIO(b"{}"))

        p = parts.path
        ok = lambda obj: Resp(json.dumps(obj).encode())  # noqa: E731

        if p == "/me":
            return ok({"id": 42, "username": "tester"})
        if p == "/users/42/track_likes":
            if "offset" not in q:
                nxt = f"{sc.API}/users/42/track_likes?offset=2&limit=200&linked_partitioning=1"
                return ok({"collection": [{"track": track(1, "One")}, {"track": track(2, "Two")}], "next_href": nxt})
            return ok({"collection": [{"track": track(3, "Three")}], "next_href": None})
        if p == "/users/42/playlists/liked_and_owned":
            return ok({"collection": [
                {"type": "playlist", "playlist": {"id": 100, "title": "Mine", "track_count": 3, "user": {"id": 42}, "tracks": [track(1, "One")]}},
                {"type": "playlist-like", "playlist": {"id": 200, "title": "Theirs", "track_count": 9, "user": {"id": 7, "avatar_url": "https://i1.sndcdn.com/avatars-9-large.jpg"}, "tracks": []}},
                {"type": "system-playlist-like", "system_playlist": {"urn": "soundcloud:system-playlists:x"}},
            ], "next_href": None})
        if p == "/playlists/100" and method == "GET":
            return ok({"id": 100, "title": "Mine", "user": {"id": 42},
                       "tracks": [track(1, "One"), track(2), track(3)]})
        if p == "/playlists/200" and method == "GET":
            return ok({"id": 200, "title": "Theirs", "user": {"id": 7}, "tracks": []})
        if p == "/playlists/300" and method == "GET":
            return ok({"id": 300, "title": "Album X", "user": {"id": 7}, "release_date": "2021-05-01T00:00:00Z",
                       "artwork_url": "https://i1.sndcdn.com/artworks-300-large.jpg", "tracks": [track(5, "Five")]})
        if p == "/tracks":
            ids = [int(x) for x in q["ids"].split(",")]
            return ok([track(i, f"T{i}") for i in ids])
        if p.startswith("/users/42/track_likes/") and method in ("PUT", "DELETE"):
            return ok({})
        if p == "/playlists/100" and method == "PUT":
            if self.put_fail_first_shape and "playlist" in (body or {}):
                raise urllib.error.HTTPError(url, 422, "bad", {}, io.BytesIO(b'{"error":"unprocessable"}'))
            return ok({"id": 100})
        if p == "/search/users":
            return ok({"collection": [{"id": 7, "username": "Artist", "avatar_url": "https://i1.sndcdn.com/avatars-7-large.jpg"}]})
        if p == "/users/7":
            return ok({"id": 7, "username": "Artist", "avatar_url": "https://i1.sndcdn.com/avatars-7-large.jpg"})
        if p == "/users/7/toptracks":
            return ok({"collection": [track(11, "Hit", 7)], "next_href": None})
        if p == "/users/7/tracks":
            return ok({"collection": [track(12, "New", 7), track(11, "Hit", 7)], "next_href": None})
        if p == "/users/7/albums":
            return ok({"collection": [{"id": 300, "title": "Album X", "release_date": "2021-05-01T00:00:00Z", "user": {"id": 7}}], "next_href": None})
        if p == "/users/7/playlists_without_albums":
            return ok({"collection": [{"id": 400, "title": "Mixes", "user": {"id": 7}, "tracks": [track(12, "New", 7)]}], "next_href": None})
        raise urllib.error.HTTPError(url, 404, "nf", {}, io.BytesIO(b'{"error":"not found"}'))


class Base(unittest.TestCase):
    def setUp(self):
        self.fake = FakeSoundCloud()
        sc._urlopen = self.fake
        sc._client_id = None
        sc.logout()

    def tearDown(self):
        sc._urlopen = urllib.request.urlopen

    def login(self):
        return sc.login(TOKEN)


class TokenExtraction(unittest.TestCase):
    def test_variants(self):
        for raw in (TOKEN, f"oauth_token={TOKEN}", f"Cookie: a=b; oauth_token={TOKEN}; c=d",
                    f"Authorization: OAuth {TOKEN}", json.dumps({"oauthToken": TOKEN})):
            self.assertEqual(sc.extract_token(raw), TOKEN, raw)

    def test_rejects_garbage(self):
        for raw in ("", "   ", "not a token at all"):
            with self.assertRaises(ValueError):
                sc.extract_token(raw)


class Session(Base):
    def test_login_logout(self):
        res = self.login()
        self.assertTrue(res["ok"])
        self.assertEqual(res["accountName"], "tester")
        self.assertEqual(json.loads(res["authJson"]), {"oauthToken": TOKEN})
        self.assertTrue(sc.is_logged_in())
        # The stored authJson must be accepted again on app restart.
        sc.logout()
        self.assertFalse(sc.is_logged_in())
        sc.login(res["authJson"])
        self.assertTrue(sc.is_logged_in())

    def test_sends_oauth_header_and_client_id(self):
        self.login()
        method, path, q, _, auth = self.fake.calls[-1]
        self.assertEqual((method, path), ("GET", "/me"))
        self.assertEqual(auth, f"OAuth {TOKEN}")
        self.assertEqual(q["client_id"], CLIENT_ID)

    def test_rejected_token(self):
        self.fake.reject_token = True
        with self.assertRaises(sc.ScAuthError):
            self.login()
        self.assertFalse(sc.is_logged_in())

    def test_stale_client_id_is_refreshed_once(self):
        self.fake.reject_client_id = CLIENT_ID
        self.fake.cid_served = ["ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"]
        sc._client_id = CLIENT_ID  # cached, now stale
        self.login()
        self.assertEqual(sc._client_id, "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ")

    def test_requires_login(self):
        for fn in (lambda: sc.liked(), lambda: sc.playlists(),
                   lambda: sc.rate("sc_1", "LIKE"), lambda: sc.add_to_playlist("scp_100", "sc_1")):
            with self.assertRaises(sc.ScAuthError):
                fn()


class Account(Base):
    def setUp(self):
        super().setUp()
        self.login()

    def test_liked_follows_next_href(self):
        tracks = sc.liked()
        self.assertEqual([t["videoId"] for t in tracks], ["sc_1", "sc_2", "sc_3"])
        t = tracks[0]
        self.assertEqual((t["title"], t["artist"], t["durationSeconds"]), ("One", "Artist", 185))
        self.assertEqual(t["artistId"], "scu_7")
        self.assertTrue(t["thumbnail"].endswith("-t500x500.jpg"))

    def test_liked_respects_limit(self):
        self.assertEqual(len(sc.liked(2)), 2)

    def test_playlists_flags_and_filters(self):
        pls = {p["playlistId"]: p for p in sc.playlists()}
        self.assertEqual(set(pls), {"scp_100", "scp_200"})  # system playlist dropped
        self.assertFalse(pls["scp_100"]["readOnly"])
        self.assertTrue(pls["scp_200"]["readOnly"])
        self.assertEqual(pls["scp_100"]["count"], 3)
        self.assertTrue(pls["scp_200"]["thumbnail"].endswith("-t500x500.jpg"))  # falls back to owner avatar

    def test_playlist_tracks_hydrates_in_order(self):
        res = sc.playlist_tracks("scp_100")
        self.assertEqual([t["videoId"] for t in res["tracks"]], ["sc_1", "sc_2", "sc_3"])
        self.assertEqual([t["title"] for t in res["tracks"]], ["One", "T2", "T3"])
        hydrate_calls = [c for c in self.fake.calls if c[1] == "/tracks"]
        self.assertEqual(len(hydrate_calls), 1)
        self.assertEqual(hydrate_calls[0][2]["ids"], "2,3")

    def test_album_detail(self):
        a = sc.album_detail("scp_300")
        self.assertEqual(a["albumId"], "scp_300")
        self.assertEqual(a["tracks"][0]["album"], "Album X")
        self.assertEqual(a["tracks"][0]["albumId"], "scp_300")
        self.assertTrue(a["thumbnail"].endswith("-t500x500.jpg"))

    def test_like_unlike(self):
        sc.rate("sc_5", "LIKE")
        sc.rate("sc_5", "INDIFFERENT")
        writes = [(m, p) for m, p, *_ in self.fake.calls if m in ("PUT", "DELETE")]
        self.assertEqual(writes, [("PUT", "/users/42/track_likes/5"), ("DELETE", "/users/42/track_likes/5")])
        with self.assertRaises(ValueError):
            sc.rate("sc_5", "DISLIKE")
        with self.assertRaises(ValueError):
            sc.rate("sc_notanumber", "LIKE")

    def test_add_to_playlist_appends_and_keeps_order(self):
        sc.add_to_playlist("scp_100", "sc_9")
        put = [c for c in self.fake.calls if c[0] == "PUT"][-1]
        self.assertEqual(put[1], "/playlists/100")
        self.assertEqual(put[3], {"playlist": {"tracks": [{"id": 1}, {"id": 2}, {"id": 3}, {"id": 9}]}})

    def test_add_duplicate_is_noop(self):
        sc.add_to_playlist("scp_100", "sc_2")
        self.assertFalse([c for c in self.fake.calls if c[0] == "PUT"])

    def test_add_to_foreign_playlist_refused(self):
        with self.assertRaises(ValueError):
            sc.add_to_playlist("scp_200", "sc_9")
        self.assertFalse([c for c in self.fake.calls if c[0] == "PUT"])

    def test_add_falls_back_to_alternate_body(self):
        self.fake.put_fail_first_shape = True
        sc.add_to_playlist("scp_100", "sc_9")
        puts = [c for c in self.fake.calls if c[0] == "PUT"]
        self.assertEqual(len(puts), 2)
        self.assertEqual(puts[1][3], {"tracks": [1, 2, 3, 9]})


class Catalogue(Base):
    def test_search_artists_works_logged_out(self):
        res = sc.search_artists("artist")
        self.assertEqual(res[0]["artistId"], "scu_7")
        self.assertEqual(res[0]["name"], "Artist")
        self.assertTrue(all(c[4] is None for c in self.fake.calls))  # no token sent when logged out

    def test_artist_detail_shape(self):
        a = sc.artist_detail("scu_7")
        self.assertEqual((a["artistId"], a["name"]), ("scu_7", "Artist"))
        self.assertEqual([s["videoId"] for s in a["songs"]], ["sc_11", "sc_12"])  # popular first, deduped
        self.assertEqual([x["albumId"] for x in a["albums"]], ["scp_300"])
        self.assertEqual(a["albums"][0]["year"], "2021")
        self.assertEqual([x["albumId"] for x in a["singles"]], ["scp_400"])

    def test_artist_detail_survives_optional_section_failures(self):
        orig = self.fake.__call__

        def failing(req, timeout=None):
            if urllib.parse.urlsplit(req.full_url).path in ("/users/7/albums", "/users/7/toptracks"):
                raise urllib.error.HTTPError(req.full_url, 500, "boom", {}, io.BytesIO(b"{}"))
            return orig(req, timeout)

        sc._urlopen = failing
        a = sc.artist_detail("scu_7")
        self.assertEqual(a["albums"], [])
        self.assertEqual([s["videoId"] for s in a["songs"]], ["sc_12", "sc_11"])

    def test_unknown_user_is_error(self):
        with self.assertRaises(sc.ScApiError):
            sc.artist_detail("scu_999")


class HttpLayer(Base):
    """The real backend_server handler with yt_dlp / ytmusicapi stubbed out."""

    @classmethod
    def setUpClass(cls):
        for name in ("yt_dlp", "ytmusicapi"):
            sys.modules.setdefault(name, types.ModuleType(name))
        sys.modules["ytmusicapi"].YTMusic = object
        sys.modules["ytmusicapi"].setup = lambda **kw: "{}"
        import backend_server
        cls.bs = backend_server
        cls.server = ThreadingHTTPServer(("127.0.0.1", 0), backend_server._Handler)
        cls.port = cls.server.server_address[1]
        threading.Thread(target=cls.server.serve_forever, daemon=True).start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()

    def call(self, method, path, body=None):
        data = json.dumps(body).encode() if body is not None else None
        req = urllib.request.Request(f"http://127.0.0.1:{self.port}{path}", data=data, method=method,
                                     headers={"Content-Type": "application/json"} if data else {})
        # Talk to the local server with the REAL urlopen, not the SoundCloud fake.
        try:
            with urllib.request.build_opener().open(req, timeout=10) as r:
                return r.status, json.loads(r.read() or b"{}")
        except urllib.error.HTTPError as e:
            return e.code, json.loads(e.read() or b"{}")

    def test_full_flow(self):
        st, js = self.call("GET", "/auth/status?source=soundcloud")
        self.assertEqual((st, js["loggedIn"]), (200, False))
        st, js = self.call("GET", "/liked?source=soundcloud")
        self.assertEqual(st, 401)

        st, js = self.call("POST", "/auth/login", {"source": "soundcloud", "headersRaw": f"oauth_token={TOKEN}"})
        self.assertEqual((st, js["ok"], js["accountName"]), (200, True, "tester"))
        st, js = self.call("GET", "/auth/status?source=soundcloud")
        self.assertEqual((js["loggedIn"], js["accountName"]), (True, "tester"))

        st, js = self.call("GET", "/liked?source=soundcloud&limit=5000")
        self.assertEqual([t["videoId"] for t in js["results"]], ["sc_1", "sc_2", "sc_3"])
        st, js = self.call("GET", "/playlists?source=soundcloud&limit=100")
        self.assertEqual({p["playlistId"] for p in js["playlists"]}, {"scp_100", "scp_200"})
        st, js = self.call("GET", "/playlists/scp_100")
        self.assertEqual(len(js["tracks"]), 3)
        st, js = self.call("GET", "/albums/scp_300")
        self.assertEqual(js["title"], "Album X")
        st, js = self.call("GET", "/artists/scu_7")
        self.assertEqual(js["name"], "Artist")
        st, js = self.call("GET", "/search/artists?q=artist&source=soundcloud")
        self.assertEqual(js["artists"][0]["artistId"], "scu_7")

        st, js = self.call("POST", "/rate", {"videoId": "sc_5", "rating": "LIKE"})
        self.assertEqual((st, js["ok"]), (200, True))
        st, js = self.call("POST", "/playlists/add", {"playlistId": "scp_100", "videoId": "sc_9"})
        self.assertEqual((st, js["ok"]), (200, True))
        st, js = self.call("POST", "/playlists/add", {"playlistId": "scp_100", "videoId": "YTVIDEOID11"})
        self.assertEqual(st, 400)
        st, js = self.call("POST", "/playlists/add", {"playlistId": "scp_200", "videoId": "sc_9"})
        self.assertEqual(st, 400)  # someone else's playlist

        st, js = self.call("POST", "/auth/logout?source=soundcloud", {})
        self.assertEqual((st, js["ok"]), (200, True))
        st, js = self.call("POST", "/rate", {"videoId": "sc_5", "rating": "LIKE"})
        self.assertEqual(st, 401)

    def test_bad_login_is_reported(self):
        st, js = self.call("POST", "/auth/login", {"source": "soundcloud", "headersRaw": "not a token at all"})
        self.assertEqual(st, 400)
        self.assertIn("oauth_token", js["detail"])
        self.fake.reject_token = True
        st, js = self.call("POST", "/auth/login", {"source": "soundcloud", "headersRaw": TOKEN})
        self.assertEqual(st, 401)

    def test_ytm_status_untouched(self):
        st, js = self.call("GET", "/auth/status")
        self.assertEqual(st, 200)
        self.assertFalse(js["loggedIn"])  # YTM session is independent of SoundCloud's


if __name__ == "__main__":
    unittest.main()
