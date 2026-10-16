#!/usr/bin/env python3
"""Enable unattended installation in an existing, validated updater configuration."""
import argparse
import fcntl
from pathlib import Path
from updater import load_config, save_json


def enable(path):
    path = Path(path).resolve(strict=True)
    config = load_config(path)
    root = Path(config['root']).resolve(strict=True)
    if path != root / 'config.json':
        raise RuntimeError('Configuration must be inside its updater directory')
    with (root / 'updater.lock').open('a') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        config = load_config(path)
        config['automatic'] = True
        save_json(path, config)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', required=True)
    args = parser.parse_args()
    enable(args.config)
    print('Automatic installation enabled. The existing updater schedule will apply releases.')
