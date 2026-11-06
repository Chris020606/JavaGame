"""Format Java source using a pinned, checksum-verified google-java-format release.

Run with --check for a read-only verification. The downloaded tool lives under build/.
No formatter dependencies are added to the shipped game.
"""

import argparse
import hashlib
from pathlib import Path
import shutil
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
VERSION = "2.0.0"
SHA256 = "c78feb48c549fa7114b7ea11db3b456808f7b371ffe84e6643a44dd2f9588639"
URL = (
    "https://repo.maven.apache.org/maven2/com/google/googlejavaformat/"
    f"google-java-format/{VERSION}/google-java-format-{VERSION}-all-deps.jar"
)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="Report formatting differences without editing",
    )
    args = parser.parse_args()
    java = shutil.which("java")
    if java is None:
        parser.error("Java is required; use the same JDK as the game build")
    jar = ROOT / "build" / "tools" / f"google-java-format-{VERSION}.jar"
    jar.parent.mkdir(parents=True, exist_ok=True)
    if not jar.exists():
        with urllib.request.urlopen(URL, timeout=30) as response:
            content = response.read()
        if hashlib.sha256(content).hexdigest() != SHA256:
            raise RuntimeError("Formatter download checksum did not match")
        jar.write_bytes(content)
    if hashlib.sha256(jar.read_bytes()).hexdigest() != SHA256:
        raise RuntimeError(f"Unexpected formatter contents: {jar}; remove it and retry")
    files = sorted(str(path) for path in (ROOT / "src").rglob("*.java"))
    options = ["--dry-run", "--set-exit-if-changed"] if args.check else ["--replace"]
    subprocess.run([java, "-jar", str(jar), "--aosp", *options, *files], check=True)


if __name__ == "__main__":
    main()
