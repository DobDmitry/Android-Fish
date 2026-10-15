import math
from pathlib import Path
root=Path(__file__).resolve().parents[1]
INK='#514738'; CREAM='#FFF3D6'; RED='#D97B68'; GOLD='#E4B857'; GREEN='#8BAE87'; BLUE='#80AAB3'; PURPLE='#AC92B8'
art={}
def path(d,fill=CREAM,stroke=INK,width=2.4): return (d,fill,stroke,width)
def circle(x,y,r,fill,stroke=INK,width=2.4): return path(f'M{x-r},{y} a{r},{r} 0 1,0 {2*r},0 a{r},{r} 0 1,0 {-2*r},0',fill,stroke,width)
def line(d,stroke=INK,width=2.4): return path(d,'none',stroke,width)
def star(x,y,r,r2,fill=GOLD,n=5):
    pts=[(x+math.sin(i*math.pi/n)* (r if i%2==0 else r2),y-math.cos(i*math.pi/n)*(r if i%2==0 else r2)) for i in range(n*2)]
    return path('M'+' L'.join(f'{a:.2f},{b:.2f}' for a,b in pts)+' Z',fill)
def flower(pink=False):
    ps=[]
    for i in range(5 if pink else 8):
        a=i*2*math.pi/(5 if pink else 8); ps.append(circle(50+21*math.sin(a),50+21*math.cos(a),17 if pink else 13, '#E7A4AD' if pink else CREAM))
    return ps+[circle(50,50,13,GOLD),circle(46,46,3,CREAM,'none')]
art['flower']=flower();art['blossom']=flower(True)
art['bow']=[path('M45,45 Q10,15 8,30 L12,74 Q26,81 46,56 Z',RED),path('M55,45 Q90,15 92,30 L88,74 Q74,81 54,56 Z',RED),line('M20,40 L43,50 M80,40 L57,50'),path('M40,42 Q50,35 60,42 L58,61 Q50,66 42,60 Z',GOLD)]
art['cap']=[path('M15,76 Q12,24 53,24 Q86,28 86,76 Z',BLUE),path('M13,74 Q46,69 88,73 L97,82 Q51,89 10,81 Z',GREEN),line('M53,27 Q42,48 47,72'),circle(53,24,4,GOLD),star(67,52,10,5,CREAM)]
art['sunhat']=[path('M25,72 L33,34 Q51,24 69,34 L77,72 Z',GOLD),path('M27,61 L75,61 L78,72 L25,72 Z',RED),path('M5,77 Q19,63 49,71 Q83,64 96,77 Q78,91 50,86 Q17,92 5,77 Z',GOLD),line('M16,78 Q50,86 85,78',CREAM,3)]
art['tophat']=[path('M25,75 L20,20 Q50,10 80,20 L75,75 Z',PURPLE),path('M24,61 L76,61 L75,75 L25,75 Z',RED),path('M7,77 Q50,68 93,77 L90,84 Q50,92 10,84 Z',PURPLE),line('M30,27 L33,55',CREAM,3)]
art['helmet']=[path('M12,76 Q12,22 50,22 Q88,22 88,76 Z',RED),path('M41,24 L41,65 L59,65 L59,24',GOLD),path('M6,73 L94,73 L93,83 L7,83 Z',GOLD),path('M45,40 L55,40 L55,46 L61,46 L61,56 L55,56 L55,62 L45,62 L45,56 L39,56 L39,46 L45,46 Z',CREAM)]
art['mushroom']=[path('M5,76 Q13,20 50,19 Q88,20 95,76 Q50,89 5,76 Z',RED),circle(30,48,8,CREAM),circle(62,36,7,CREAM),circle(73,62,9,CREAM),path('M9,77 Q50,71 91,77 Q50,91 9,77 Z',CREAM)]
art['grad']=[path('M24,59 L76,59 L73,81 Q50,88 27,81 Z',PURPLE),path('M4,48 L50,24 L96,48 L50,68 Z',BLUE),line('M50,48 L86,61 L86,84',GOLD,3),path('M81,83 L91,83 L94,96 L79,96 Z',GOLD)]
art['crown']=[path('M15,82 L8,33 L32,50 L50,18 L68,50 L92,33 L85,82 Z',GOLD),path('M15,73 L85,73 L85,85 L15,85 Z',CREAM),circle(50,57,7,RED),circle(28,62,4,BLUE),circle(73,62,4,GREEN)]
art['star']=[star(50,50,43,21),star(50,50,27,13,CREAM),circle(50,50,7,RED)]
for id,fill in [('specs','none'),('glasses','#596D70')]:
    art[id]=[line('M3,46 L8,48 M92,48 L97,46'),circle(25,50,19,fill),circle(75,50,19,fill),line('M44,49 Q50,43 56,49'),line('M16,39 L12,45 M66,39 L62,45',CREAM,3)]
art['mask']=[path('M3,28 Q25,21 50,35 Q75,21 97,28 L91,65 Q72,79 50,61 Q28,79 9,65 Z',PURPLE),path('M13,44 Q25,34 37,44 L37,55 Q25,63 13,55 Z M63,44 Q75,34 87,44 L87,55 Q75,63 63,55 Z','none'),star(50,28,10,5,GOLD)]
# Mask eye holes use even-odd cutouts, rather than painted opaque lenses.
art['mask'][0]=path(art['mask'][0][0]+' M13,44 Q25,34 37,44 L37,55 Q25,63 13,55 Z M63,44 Q75,34 87,44 L87,55 Q75,63 63,55 Z',PURPLE)
art['bell']=[path('M12,23 Q50,35 88,23 L86,37 Q50,49 14,37 Z',RED),circle(50,50,7,GOLD),path('M34,73 Q32,48 50,47 Q68,48 66,73 L73,82 L27,82 Z',GOLD),circle(50,84,5,INK),line('M44,57 Q39,61 40,68',CREAM,3)]
art['scarf']=[path('M50,40 L72,40 L81,91 L58,94 Z',RED),path('M25,41 L47,41 L41,87 L20,83 Z',RED),path('M10,21 Q50,34 90,21 L89,44 Q50,56 11,44 Z',GREEN),line('M24,72 L43,76 M59,79 L78,76',CREAM,4),line('M14,33 Q50,46 86,33',CREAM,2)]
art['beads']=[line('M12,25 Q9,81 50,86 Q91,81 88,25',GOLD,3)]+[circle(50+37*math.cos(a),28+53*math.sin(a),6,[RED,BLUE,GOLD][i%3]) for i,a in enumerate([i*math.pi/8 for i in range(9)])]
art['tie']=[path('M15,20 L50,32 L85,20 L76,43 L50,33 L24,43 Z',CREAM),path('M40,32 L60,32 L56,48 L44,48 Z',BLUE),path('M44,48 L56,48 L66,81 L50,95 L34,81 Z',BLUE),line('M42,61 L59,69 M38,75 L57,84',CREAM,3)]
art['ribbon']=[path('M37,29 Q50,9 63,29 Q73,44 58,60 L34,92 L25,78 L49,49 Q61,32 51,29 Q43,29 43,39 L74,80 L62,92 L34,49 Q29,38 37,29 Z',GOLD)]
art['medal']=[path('M18,15 L34,15 L56,53 L43,62 Z',RED),path('M66,15 L82,15 L57,62 L44,53 Z',BLUE),circle(50,72,23,GOLD),circle(50,72,17,CREAM),star(50,72,12,6,GOLD)]
art['balloon']=[line('M50,54 Q39,68 50,79 L50,94',INK,1.8),path('M45,53 L55,53 L58,60 L42,60 Z',RED),path('M50,8 C12,8 17,54 50,56 C83,54 88,8 50,8 Z',RED),line('M35,19 Q28,26 31,35',CREAM,4)]
art['party']=[path('M50,91 L17,42 L76,55 Z',GOLD),line('M29,58 L65,69 M39,74 L57,78',RED,5),line('M43,38 Q36,11 48,7 M58,43 Q88,25 83,15 M29,31 L16,18',BLUE,3),star(67,20,9,4,RED),circle(17,10,3,GREEN)]
art['sunflower']=[line('M50,40 L50,94',GREEN,5),path('M49,73 Q20,54 23,73 Q34,83 49,78 Z',GREEN)]
for i in range(8):
    a=i*math.pi/4;art['sunflower'].append(circle(50+22*math.sin(a),34+22*math.cos(a),10,GOLD))
art['sunflower'] += [circle(50,34,15,'#8A654C'),circle(46,30,3,CREAM,'none')]
art['lollipop']=[line('M50,45 L50,94',CREAM,6),circle(50,33,27,RED),line('M50,33 C35,14 24,42 46,49 C72,56 74,15 47,14 C19,13 15,51 43,57',CREAM,5)]
art['ball']=[circle(50,50,43,CREAM),star(50,50,17,14,INK),path('M21,20 L30,29 L22,43 L8,42 Z M79,20 L70,29 L78,43 L92,42 Z M30,88 L32,74 L49,70 L59,87 Z',INK),line('M32,43 L23,42 M68,43 L78,42 M50,67 L49,70')]
art['umbrella']=[line('M50,34 L50,86 Q50,98 37,91',INK,4),path('M5,44 Q13,9 50,8 Q87,9 95,44 Q80,34 66,44 Q50,34 34,44 Q20,34 5,44 Z',PURPLE),line('M50,9 Q34,21 34,43 M50,9 Q66,21 66,43',CREAM,2)]
art['guitar']=[path('M47,44 Q28,32 22,50 Q25,59 15,65 Q7,83 27,92 Q50,99 59,80 Q64,69 54,62 Q65,44 47,44 Z',GOLD),path('M40,59 L63,13 L72,18 L49,65 Z',RED),path('M61,15 L64,5 L80,12 L75,23 Z',GREEN),circle(38,67,9,INK),line('M34,80 L68,16',CREAM,2),line('M24,79 L43,87',INK,4)]
art['crystal']=[circle(50,43,32,PURPLE),star(43,34,12,5,CREAM),circle(64,51,4,CREAM,'none'),path('M25,71 Q50,83 75,71 L84,91 L16,91 Z',GOLD)]
art['trophy']=[path('M24,26 L9,26 Q4,61 37,62 M76,26 L91,26 Q96,61 63,62','none',GOLD,7),path('M24,17 L76,17 Q77,61 55,65 L55,80 L72,84 L72,94 L28,94 L28,84 L45,80 L45,65 Q23,61 24,17 Z',GOLD),star(50,39,12,6,CREAM)]
art['backpack']=[path('M35,24 Q33,3 50,4 Q67,3 65,24','none',INK,6),path('M16,36 Q10,21 24,18 L76,18 Q90,21 84,36 L87,87 Q50,99 13,87 Z',GREEN),path('M18,25 Q50,12 82,25 L78,47 Q50,57 22,47 Z',BLUE),path('M27,62 L73,62 L72,85 L28,85 Z',GOLD),path('M45,37 L55,37 L55,57 L45,57 Z',RED),line('M34,70 L66,70')]
art['butterfly']=[path('M49,49 C14,-10 -5,14 8,49 Q10,63 33,61 C0,89 21,104 49,65 Z',PURPLE),path('M51,49 C86,-10 105,14 92,49 Q90,63 67,61 C100,89 79,104 51,65 Z',PURPLE),path('M44,46 Q14,13 16,37 Q17,50 36,49 Z M56,46 Q86,13 84,37 Q83,50 64,49 Z',BLUE),path('M40,65 Q19,75 29,84 Z M60,65 Q81,75 71,84 Z',GOLD),path('M46,35 Q50,30 54,35 L55,75 Q50,86 45,75 Z',INK)]
art['rainbow']=[path('M5,88 L5,53 C5,-7 95,-7 95,53 L95,88 L81,88 L81,53 C81,12 19,12 19,53 L19,88 Z',RED),path('M19,88 L19,53 C19,12 81,12 81,53 L81,88 L68,88 L68,53 C68,30 32,30 32,53 L32,88 Z',GOLD),path('M32,88 L32,53 C32,30 68,30 68,53 L68,88 L57,88 L57,53 C57,44 43,44 43,53 L43,88 Z',GREEN)]
# Each path is drawn unchanged by Android and exported SVG previews.
lines=['package com.dobdmitry.drawnpets.ui.common','','internal data class OutfitPath(val d: String, val fill: String, val stroke: String, val width: Float)','','internal val outfitPaths = mapOf(']
for id,ps in art.items():
    lines.append(f'    "{id}" to listOf(')
    lines += [f'        OutfitPath("{d}", "{f}", "{s}", {w}f),' for d,f,s,w in ps]
    lines.append('    ),')
lines.append(')')
(root/'app/src/main/java/com/dobdmitry/drawnpets/ui/common/OutfitPaths.kt').write_text('\n'.join(lines)+'\n',encoding='utf-8')
# Contact sheet uses exactly the runtime geometry, not a separate concept.
svg=['<svg xmlns="http://www.w3.org/2000/svg" width="1040" height="950" viewBox="0 0 1040 950"><rect width="1040" height="950" fill="#FFF5E2"/>']
for i,(id,ps) in enumerate(art.items()):
    x=20+(i%8)*128;y=20+(i//8)*230
    svg.append(f'<g transform="translate({x},{y})"><rect width="116" height="205" rx="12" fill="#FFFCF5"/><g transform="translate(8,35)">')
    for d,f,s,w in ps: svg.append(f'<path d="{d}" fill="{f}" fill-rule="evenodd" stroke="{s}" stroke-width="{w}" stroke-linejoin="round" stroke-linecap="round"/>')
    svg.append(f'</g><text x="58" y="175" text-anchor="middle" font-family="sans-serif" font-size="13" fill="{INK}">{id}</text></g>')
svg.append('</svg>')
(root/'docs/outfit-catalog.svg').write_text(''.join(svg),encoding='utf-8')
print('Generated',len(art),'accessories')
