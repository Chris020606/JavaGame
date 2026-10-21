"""Archive jpackage output while preserving executable permissions on Unix."""
import hashlib, platform, shutil, sys, tarfile
from pathlib import Path
root=Path(__file__).resolve().parents[1]
name='Outpost-'+sys.argv[1]
out=root/'build/release';out.mkdir(parents=True,exist_ok=True)
native=root/'build/native'
if platform.system()=='Windows':
    archive=Path(shutil.make_archive(str(out/name),'zip',native))
else:
    archive=out/(name+'.tar.gz')
    with tarfile.open(archive,'w:gz') as tar:
        for child in native.iterdir(): tar.add(child,arcname=child.name)
(out/(archive.name+'.sha256')).write_text(hashlib.sha256(archive.read_bytes()).hexdigest()+'  '+archive.name+'\n')
print(archive)
