#!/usr/bin/env python3
"""Called after tests/build, before publishing the exact tested JAR."""
import hashlib
import json
import os
import subprocess
from pathlib import Path
from github_release import REPOSITORY, validate_manifest


def main():
    target = os.environ['DEPLOY_TARGET']
    run_id = int(os.environ['GITHUB_RUN_ID'])
    attempt = int(os.environ['GITHUB_RUN_ATTEMPT'])
    jar = Path('backend/target/sauna_large.jar')
    data = dict(schema=1, repository=REPOSITORY, target=target, jar='sauna_large.jar',
                description=subprocess.check_output(['git','log','-1','--format=%s'], text=True).strip()[:2000],
                commit=os.environ['GITHUB_SHA'], run_id=run_id, run_attempt=attempt,
                size=jar.stat().st_size, sha256=hashlib.file_digest(jar.open('rb'), 'sha256').hexdigest())
    tag = 'large-%s-%s-%s' % (target, run_id, attempt)
    validate_manifest(data, {'tag_name': tag}, target)
    Path('backend/target/manifest.json').write_text(json.dumps(data, indent=2) + '\n')
    with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
        output.write('tag=' + tag + '\n')


if __name__ == '__main__':
    main()
