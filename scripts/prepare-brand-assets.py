from PIL import Image
from pathlib import Path
import hashlib
res=Path('app/src/main/res/drawable-nodpi')
gold=(0xdb,0xbf,0x8b)
# Preserve the original transparency and contours; normalize only the explicitly requested brand color.
im=Image.open('el le.png').convert('RGBA'); a=im.getchannel('A'); bbox=a.getbbox(); logo=Image.new('RGBA',im.size,gold+(0,));logo.putalpha(a);logo=logo.crop(bbox);logo.save(res/'elledue_logo_gold.png')
im=Image.open('Elledue nero verticale.png').convert('RGBA');a=im.getchannel('A')
# The original vertical asset contains separated symbol and wordmark bands.
rows=[y for y in range(im.height) if a.crop((0,y,im.width,y+1)).getbbox()]
bands=[]
for y in rows:
 if not bands or y>bands[-1][-1]+1:bands.append([y])
 else:bands[-1].append(y)
print('Original visible bands:',[(b[0],b[-1]) for b in bands])
symbol=a.crop((0,bands[0][0],im.width,bands[0][-1]+1));symbol=symbol.crop(symbol.getbbox())
fg=Image.new('RGBA',symbol.size,gold+(0,));fg.putalpha(symbol);fg.thumbnail((192,192),Image.Resampling.LANCZOS)
canvas=Image.new('RGBA',(432,432),(0,0,0,0));canvas.alpha_composite(fg,((432-fg.width)//2,(432-fg.height)//2));canvas.save(res/'elledue_symbol_gold.png')
print('Header size:',logo.size)
Path('docs/brand/ORIGINALS.md').write_text('# Marchio ufficiale\n\nOriginali caricati dall’utente nel commit 9c6ceb1, conservati senza modifiche nella radice del repository.\n\nIl logo orizzontale trasparente deriva da `el le.png`: ritaglio dei soli margini trasparenti, stessa alpha e proporzioni; colore uniformato al dorato ufficiale #DBBF8B richiesto dall’utente.\n\nL’icona usa il simbolo separato dalla scritta nel PNG verticale trasparente `Elledue nero verticale.png`, senza ridisegno; stessa alpha, proporzioni preservate, dorato ufficiale e margini per la zona sicura dell’icona adattiva. Sfondo #111B32. Lo splash usa il medesimo simbolo. Il PDF conserva nel proprio snapshot il PNG del logo utilizzato.\n\n'+''.join(f'- {p.name}: SHA-256 `{hashlib.sha256(p.read_bytes()).hexdigest()}`\n' for p in sorted(Path('.').glob('*.png'))))

import shutil
asset_dir=Path("app/src/main/assets/brand");asset_dir.mkdir(parents=True,exist_ok=True);shutil.copy2(res/"elledue_logo_gold.png",asset_dir/"elledue_logo_gold.png")
