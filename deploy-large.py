#!/usr/bin/env python3
"""Deploy the large sauna JAR to the existing webpanel-b service only."""
import argparse
import hashlib
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parent


def install_interactively(ssh, helper_path, stage, checksum, port):
    # Keep stdin attached to the user's terminal. sudo reads its password there;
    # it never passes through Python, command arguments, or a file.
    checks = ['sudo -v']
    for action in ('stop', 'start'):
        checks.append(shlex.join(['sudo', '-n', '-l', '--', 'service', 'webpanel-b', action]) + ' >/dev/null')
    checks.append(shlex.join(['python3', helper_path, 'install',
                             stage + '/sauna_large.jar', checksum, str(port)]))
    subprocess.run(ssh[:1] + ['-t'] + ssh[1:] + [' && '.join(checks)], check=True)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    mode = p.add_mutually_exclusive_group(required=True)
    mode.add_argument('--check', action='store_true', help='Только проверить SSH и сервис')
    mode.add_argument('--deploy', action='store_true', help='Установить JAR и перезапустить webpanel-b')
    privileges = p.add_mutually_exclusive_group()
    privileges.add_argument('--no-sudo', action='store_true', help='Останавливать и запускать webpanel-b через service без sudo')
    privileges.add_argument('--ask-sudo', action='store_true', help='Запросить пароль sudo в терминале при установке')
    p.add_argument('--build', action='store_true', help='Перед установкой запустить тесты и собрать JAR')
    p.add_argument('--host', default='212.164.86.237')
    p.add_argument('--user', default='webpanel')
    p.add_argument('--ssh-port', type=int, default=22)
    p.add_argument('--web-port', type=int, default=8083)
    p.add_argument('--key', type=Path, default=Path.home()/'.ssh/deploy_key')
    args = p.parse_args()
    if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]*', args.host) or not re.fullmatch(r'[A-Za-z0-9_][A-Za-z0-9_-]*', args.user):
        p.error('Некорректный SSH-адрес')
    if not 1 <= args.ssh_port <= 65535 or not 1 <= args.web_port <= 65535:
        p.error('Некорректный порт')
    if args.build and not args.deploy:
        p.error('--build используется с --deploy')
    if (args.ask_sudo or args.no_sudo) and (not args.deploy or not sys.stdin.isatty()):
        p.error('--ask-sudo/--no-sudo нужен с --deploy в обычном интерактивном терминале')
    options = ['-o','BatchMode=yes','-o','ConnectTimeout=10','-o','StrictHostKeyChecking=yes','-o','ServerAliveInterval=15','-o','ServerAliveCountMax=3','-i',str(args.key)]
    host = args.user + '@' + args.host
    ssh = ['ssh'] + options + ['-p',str(args.ssh_port),host]
    helper = (ROOT/'tools/deploy_large_remote.py').read_text()
    def remote_helper(*words):
        command = shlex.join(['python3','-'] + list(words))
        subprocess.run(ssh + [command], input=helper, text=True, check=True)
    print('Проверка SSH и webpanel-b на ' + args.host, flush=True)
    remote_helper('check')
    if args.check:
        return
    if args.build:
        env = os.environ.copy()
        java_home = env.get('JAVA_8_HOME', str(Path.home()/'Library/Java/JavaVirtualMachines/corretto-1.8.0_482/Contents/Home'))
        if not (Path(java_home)/'bin/java').is_file():
            raise RuntimeError('Укажи JAVA_8_HOME с путём к Java 8')
        env.update(JAVA_HOME=java_home, PATH=java_home+'/bin:'+env.get('PATH',''))
        for command in [['npm','test'],['npm','run','build'],['mvn','-f','backend/pom.xml','clean','package','-Dskip.frontend.build=true']]:
            subprocess.run(command, cwd=ROOT, env=env, check=True)
    jar = ROOT/'backend/target/sauna_large.jar'
    with zipfile.ZipFile(jar) as z:
        if z.testzip():
            raise RuntimeError('Повреждён локальный JAR')
        z.getinfo('BOOT-INF/classes/static/index.html')
    checksum = hashlib.sha256(jar.read_bytes()).hexdigest()
    print('Большая сауна: %0.1f МиБ, SHA-256 %s' % (jar.stat().st_size/1024/1024, checksum), flush=True)
    stage = subprocess.check_output(ssh + ['mktemp -d /tmp/sauna-webpanel-b.XXXXXXXX'], text=True).strip()
    if not re.fullmatch(r'/tmp/sauna-webpanel-b\.[A-Za-z0-9]+', stage):
        raise RuntimeError('Некорректный временный каталог на сервере')
    try:
        subprocess.run(['scp'] + options + ['-P',str(args.ssh_port),str(jar),host+':'+stage+'/sauna_large.jar'], check=True)
        if args.ask_sudo or args.no_sudo:
            subprocess.run(['scp'] + options + ['-P', str(args.ssh_port),
                           str(ROOT/'tools/deploy_large_remote.py'), host+':'+stage+'/installer.py'], check=True)
            if args.no_sudo:
                command = shlex.join(['python3', stage+'/installer.py', 'install-no-sudo',
                                      stage+'/sauna_large.jar', checksum, str(args.web_port)])
                subprocess.run(ssh[:1] + ['-t'] + ssh[1:] + [command], check=True)
            else:
                print('Введи пароль sudo пользователя ' + args.user + ' на сервере. Символы не отображаются.', flush=True)
                install_interactively(ssh, stage+'/installer.py', stage, checksum, args.web_port)
        else:
            remote_helper('install',stage+'/sauna_large.jar',checksum,str(args.web_port))
    except subprocess.CalledProcessError:
        print('Обновление не подтверждено. При обрыве SSH после установки проверь sudo service webpanel-b status.', file=sys.stderr)
        raise
    finally:
        # Only remove the exact file and empty directory created for this upload.
        cleanup = shlex.join(['rm','-f',stage+'/sauna_large.jar',stage+'/installer.py']) + ' && ' + shlex.join(['rmdir',stage])
        subprocess.run(ssh + [cleanup], check=False)


if __name__ == '__main__':
    try:
        main()
    except (OSError, RuntimeError, subprocess.SubprocessError, zipfile.BadZipFile) as e:
        print('Не выполнено: ' + (str(e) if isinstance(e, RuntimeError) else type(e).__name__), file=sys.stderr)
        sys.exit(1)
