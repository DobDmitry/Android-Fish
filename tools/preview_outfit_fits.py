"""Render the placements exported by OutfitLayoutTest with the actual vector paths."""
from pathlib import Path
from draw_outfits import art
root=Path(__file__).resolve().parents[1]
rows=[line.split('\t') for line in (root/'app/build/outfit-fit-fixtures.tsv').read_text().splitlines()]
def accessory(row):
    _,_,_,id,x,y,size,angle=row
    return f'<g transform="translate({x},{y}) rotate({angle}) scale({float(size)/100}) translate(-50,-50)">'+''.join(f'<path d="{d}" fill="{f}" fill-rule="evenodd" stroke="{s}" stroke-width="{w}" stroke-linecap="round" stroke-linejoin="round"/>' for d,f,s,w in art[id])+'</g>'
svg=['<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="500"><rect width="1280" height="500" fill="#FFF5E2"/>']
for i in range(4):
    group=[r for r in rows if int(r[0])==i];w=float(group[0][1]);h=float(group[0][2])
    scale=min(240/(w+70),330/(h+80));x=i*320+160-w*scale/2;y=90
    svg.append(f'<g transform="translate({x},{y}) scale({scale})">')
    svg.append(accessory(group[0]))
    svg.append(f'<path d="M{w-55},120 L{w-10},170" fill="none" stroke="#514738" stroke-width="15" stroke-linecap="round"/>')
    svg.append(f'<ellipse cx="{w/2}" cy="{h/2}" rx="{(w-40)/2}" ry="{(h-40)/2}" fill="{["#B8CDA2","#E5BE98","#B6CCD4","#DDC1D7"][i]}" stroke="#514738" stroke-width="3"/>')
    eyes=[[(75,65),(125,75)],[(115,55),(165,55)],[(60,70),(100,70)],[]][i]
    for ex,ey in eyes: svg.append(f'<circle cx="{ex}" cy="{ey}" r="5" fill="#514738"/>')
    for r in group[1:]:svg.append(accessory(r))
    svg.append('</g>')
    svg.append(f'<text x="{i*320+160}" y="445" text-anchor="middle" font-size="18" font-family="sans-serif" fill="#514738">{["Наклон глаз","Широкий рисунок","Вытянутый рисунок","Без разметки глаз"][i]}</text>')
svg.append('</svg>')
(root/'docs/outfit-fitting.svg').write_text(''.join(svg),encoding='utf-8')
