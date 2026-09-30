"""Read-only GitHub release protocol. No credentials in logs or redirected requests."""
import hashlib
import json
import re
import time
import urllib.error
import urllib.parse
import urllib.request

REPOSITORY = 'linkelsnole/saunaProjectLarge'
API = 'https://api.github.com/repos/' + REPOSITORY
WORKFLOW = '.github/workflows/ci.yml'
LAB_BRANCH = 'codex/sauna-updater'
MAX_JAR = 180 * 1024 * 1024
TAG = re.compile(r'large-(lab|production)-([1-9][0-9]*)-([1-9][0-9]*)\Z')


class SafeRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        url = urllib.parse.urlsplit(newurl)
        allowed = {'api.github.com', 'release-assets.githubusercontent.com', 'objects.githubusercontent.com'}
        if url.scheme != 'https' or url.hostname not in allowed or url.username or url.password:
            raise RuntimeError('GitHub returned an unexpected download redirect')
        result = super().redirect_request(req, fp, code, msg, headers, newurl)
        if result is not None:
            result.remove_header('Authorization')
        return result


class GitHub:
    def __init__(self, token):
        self.token = token
        self.opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), SafeRedirect())

    def open(self, endpoint, binary=False):
        if not endpoint.startswith('/') or endpoint.startswith('//'):
            raise ValueError('Only repository-relative endpoints are allowed')
        request = urllib.request.Request(API + endpoint, headers={
            'Authorization': 'Bearer ' + self.token,
            'User-Agent': 'sauna-updater/1',
            'X-GitHub-Api-Version': '2022-11-28',
            'Accept': 'application/octet-stream' if binary else 'application/vnd.github+json',
        })
        return self.opener.open(request, timeout=30)

    def json(self, endpoint, binary=False):
        with self.open(endpoint, binary) as response:
            raw = response.read(1024 * 1024 + 1)
        if len(raw) > 1024 * 1024:
            raise RuntimeError('GitHub response is too large')
        return json.loads(raw)

    def latest(self, target):
        # Releases are never edited by the publisher; creation order is not run order.
        found = []
        for release in self.json('/releases?per_page=100'):
            match = TAG.fullmatch(release.get('tag_name', ''))
            if match and match[1] == target and not release['draft']:
                found.append(((int(match[2]), int(match[3])), release))
        return max(found, key=lambda pair: pair[0])[1] if found else None

    def manifest(self, release, target):
        asset = find_asset(release, 'manifest.json', 32 * 1024)
        manifest = self.json('/releases/assets/' + str(asset['id']), binary=True)
        validate_manifest(manifest, release, target)
        run = self.json('/actions/runs/' + str(manifest['run_id']))
        if run.get('status') != 'completed':
            return None  # publication can precede completion by a few seconds
        if not (run.get('conclusion') == 'success' and run.get('event') in ('workflow_dispatch', 'push')
                and (run.get('head_branch') == 'main' or (target == 'lab' and run.get('head_branch') == LAB_BRANCH))
                and run.get('head_sha') == manifest['commit']
                and run.get('run_attempt') == manifest['run_attempt'] and run.get('path') == WORKFLOW
                and run.get('repository', {}).get('full_name') == REPOSITORY):
            raise RuntimeError('Release is not backed by a successful allowed-branch deployment workflow')
        return manifest

    def download(self, asset, destination, sha256, size):
        if size != asset['size'] or not 0 < size <= MAX_JAR:
            raise RuntimeError('Invalid JAR size')
        checksum = hashlib.sha256()
        received = 0
        deadline = time.monotonic() + 600
        try:
            with self.open('/releases/assets/' + str(asset['id']), binary=True) as response, destination.open('xb') as stream:
                while True:
                    block = response.read(1024 * 1024)
                    if not block:
                        break
                    received += len(block)
                    if received > size or time.monotonic() > deadline:
                        raise RuntimeError('JAR download exceeded its size or time limit')
                    checksum.update(block)
                    stream.write(block)
            if received != size or checksum.hexdigest() != sha256:
                raise RuntimeError('Incomplete download or SHA-256 mismatch')
        except BaseException:
            destination.unlink(missing_ok=True)
            raise


def find_asset(release, name, limit=MAX_JAR):
    found = [a for a in release['assets'] if a['name'] == name and a.get('state') == 'uploaded']
    if len(found) != 1 or type(found[0]['id']) is not int or not 0 < found[0]['size'] <= limit:
        raise RuntimeError('Missing, duplicate or oversized release asset: ' + name)
    return found[0]


def validate_manifest(m, release, target):
    match = TAG.fullmatch(release['tag_name'])
    if not match or match[1] != target or m.get('schema') != 1 or m.get('target') != target:
        raise RuntimeError('Wrong release target or manifest version')
    if (m.get('repository') != REPOSITORY or m.get('jar') != 'sauna_large.jar'
            or m.get('run_id') != int(match[2]) or m.get('run_attempt') != int(match[3])
            or not re.fullmatch(r'[0-9a-f]{40}', m.get('commit', ''))
            or not re.fullmatch(r'[0-9a-f]{64}', m.get('sha256', ''))
            or type(m.get('size')) is not int or not 0 < m['size'] <= MAX_JAR):
        raise RuntimeError('Invalid release manifest')
