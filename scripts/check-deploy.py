#!/usr/bin/env python3
"""Exercise release-file handling in a temporary filesystem; never deploy a server."""
import io
import os
from pathlib import Path
import re
import subprocess
import tarfile
import tempfile
import unittest

SCRIPT = (Path(__file__).resolve().parent.parent / 'deploy/deploy-server.sh').read_text()


class ReleaseChecks(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix='maian-release-check-')
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        for directory in ['var/www/maian', 'opt/maian', 'etc/systemd/system', 'tmp', 'bin']:
            (self.root / directory).mkdir(parents=True)
        self.config = self.root / 'opt/maian/app.env'
        self.config.write_text('JWT_SECRET=test-only-preserved-value\nBAIDU_OCR_ENABLED=true\n')
        (self.root / 'opt/maian/db.env').write_text('MYSQL_URL=test-only\n')
        (self.root / 'var/www/maian/index.html').write_text('previous release')
        (self.root / 'tmp/maian-server-1.0.0.jar').write_text('test artifact')
        (self.root / 'tmp/maian-server.service').write_text('test service')
        self.archive = self.root / 'tmp/maian-h5.tar.gz'
        self.make_archive('index.html')
        # Only ownership and service operations are stubbed. Extraction, installation,
        # file copying, permissions and removal run against the temporary tree.
        for command in ['systemctl', 'nginx', 'chown']:
            self.stub(command, 'printf "%s\\n" "' + command + ' $*" >> "$DEPLOY_CHECK_COMMANDS"\n')
        self.stub('install', '''args=()
while (($#)); do
  if [[ "$1" == -o || "$1" == -g ]]; then shift 2
  else args+=("$1"); shift; fi
done
exec /usr/bin/install "${args[@]}"
''')
        script = re.sub(r'/(?:var/www|var/lib|opt/maian|etc/systemd/system|tmp)(?=/)',
                        lambda match: str(self.root) + match.group(), SCRIPT)
        self.script = self.root / 'release.sh'
        self.script.write_text(script)
        self.log = self.root / 'commands.log'

    def stub(self, command, body):
        file = self.root / 'bin' / command
        file.write_text('#!/usr/bin/env bash\nset -euo pipefail\n' + body)
        file.chmod(0o700)

    def make_archive(self, name):
        with tarfile.open(self.archive, 'w:gz') as archive:
            entry = tarfile.TarInfo(name)
            content = b'next release'
            entry.size = len(content)
            archive.addfile(entry, io.BytesIO(content))

    def run_release(self):
        env = dict(os.environ, PATH=str(self.root / 'bin') + ':' + os.environ['PATH'],
                   DEPLOY_CHECK_COMMANDS=str(self.log))
        return subprocess.run(['bash', str(self.script)], env=env, capture_output=True, text=True)

    def test_preserves_existing_config_and_replaces_artifacts(self):
        previous_config = self.config.read_bytes()
        result = self.run_release()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.config.read_bytes(), previous_config)
        self.assertEqual((self.root / 'var/www/maian/index.html').read_text(), 'next release')
        self.assertIn('systemctl start maian-server', self.log.read_text())

    def test_invalid_web_archive_keeps_previous_release_running(self):
        self.make_archive('missing-index.txt')
        self.assertNotEqual(self.run_release().returncode, 0)
        self.assertEqual((self.root / 'var/www/maian/index.html').read_text(), 'previous release')
        self.assertFalse(self.log.exists())

    def test_missing_config_does_not_generate_or_publish_defaults(self):
        self.config.unlink()
        self.assertNotEqual(self.run_release().returncode, 0)
        self.assertFalse(self.config.exists())
        self.assertFalse(self.log.exists())
        self.assertEqual((self.root / 'var/www/maian/index.html').read_text(), 'previous release')


if __name__ == '__main__':
    unittest.main()
