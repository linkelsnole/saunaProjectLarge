#!/usr/bin/env python3
"""Fail CI if generated/private files are tracked or the JAR lacks the application."""
from pathlib import Path
import re
import subprocess
import zipfile


def main():
    files = subprocess.check_output(['git', 'ls-files', '-z']).decode().split('\0')
    forbidden = re.compile(r'(^|/)(node_modules|target|dist|\.config|__pycache__)/|\.(db|sqlite|sqlite3|token|pem|key|ovpn|p12|pfx)(-|$)|(^|/)\.env($|\.)')
    bad = [p for p in files if forbidden.search(p) and not p.endswith('.example')]
    if bad:
        raise SystemExit('Generated/private files tracked: ' + ', '.join(bad))
    jar = Path('backend/target/sauna_large.jar')
    if not 0 < jar.stat().st_size <= 180 * 1024 * 1024:
        raise SystemExit('Unexpected JAR size')
    with zipfile.ZipFile(jar) as archive:
        if archive.testzip():
            raise SystemExit('Damaged JAR')
        for name in ('BOOT-INF/classes/static/index.html',
                     'BOOT-INF/classes/com/example/modbus_api/ModbusApiApplication.class'):
            archive.getinfo(name)
        bad = [n for n in archive.namelist() if re.search(r'\.(db|sqlite|token|pem|key|ovpn|p12|pfx)$', n)]
        if bad:
            raise SystemExit('Private/runtime files inside JAR: ' + ', '.join(bad))
    print('JAR includes frontend/backend; no database or token files included.')


if __name__ == '__main__':
    main()
