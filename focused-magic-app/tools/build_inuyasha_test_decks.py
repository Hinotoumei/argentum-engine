#!/usr/bin/env python3
import json, re, sqlite3, unicodedata
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
DB=ROOT/'inuyasha-reference'/'SECOND_DRAW_INUYASHA_DATABASE_CURRENT.sqlite'
OUT_JSON=ROOT/'data'/'inuyasha-test-decks.json'
OUT_JS=ROOT/'js'/'inuyasha'/'decks.js'
DECK_IDS=(5,6)

def norm(s):
    s=unicodedata.normalize('NFKD', s or '').encode('ascii','ignore').decode('ascii').lower()
    s=s.replace('&',' and ')
    return re.sub(r'[^a-z0-9]+','',s)

def asset_id(url):
    m=re.search(r'/assets/([A-Za-z0-9_-]{8,80})', url or '')
    return m.group(1) if m else None

con=sqlite3.connect(DB); con.row_factory=sqlite3.Row
sets={r['set_id']:r['set_name'] for r in con.execute('select set_id,set_name from sets')}
cards=list(con.execute('select * from cards'))
card_by_id={r['card_id']:r for r in cards}
card_by_norm={}
for r in cards: card_by_norm.setdefault(norm(r['canonical_name']),[]).append(r)
imgs=list(con.execute("select * from images where url is not null and trim(url)<>'' order by image_id"))
img_by_card={}
for r in imgs:
    if r['card_id'] is not None: img_by_card.setdefault(r['card_id'],[]).append(r)
ext=list(con.execute("select * from external_catalog_entries where external_image_url is not null and trim(external_image_url)<>'' order by catalog_entry_id"))
ext_by_card={}
ext_by_norm={}
for r in ext:
    if r['linked_card_id'] is not None: ext_by_card.setdefault(r['linked_card_id'],[]).append(r)
    ext_by_norm.setdefault(norm(r['external_card_name']),[]).append(r)

def set_score(want, got):
    if not want or not got: return 0
    a,b=norm(want),norm(got)
    if a==b:return 10
    if a and b and (a in b or b in a):return 7
    # Feudal Warfare vs Feudal Warfare Decks
    aa=a.replace('decks','').replace('deck','')
    bb=b.replace('decks','').replace('deck','')
    if aa and aa==bb:return 9
    return 0

def resolve_card(row):
    if row['card_id'] is not None and row['card_id'] in card_by_id:
        return card_by_id[row['card_id']], 'deck_card_id'
    cand=card_by_norm.get(norm(row['raw_card_name']),[])
    if not cand:return None,None
    cand=sorted(cand,key=lambda c:(set_score(row['set_name'],sets.get(c['set_id'])), int(str(c['collector_no'] or '')==str(row['collector_no'] or '')), -(c['card_id'] or 0)),reverse=True)
    return cand[0], 'normalized_name'

def resolve_image(row, card):
    # App/personal image is first authority when linked to the resolved card.
    if card:
        own=img_by_card.get(card['card_id'],[])
        if own:
            r=own[0]; return r['url'],'app_image',None
        linked=ext_by_card.get(card['card_id'],[])
        if linked:
            linked=sorted(linked,key=lambda r:(set_score(row['set_name'],r['external_set_name']), r['link_status'].startswith('LINKED_EXACT'), -r['catalog_entry_id']),reverse=True)
            r=linked[0]; return r['external_image_url'],'catalog_linked',r['catalog_entry_id']
    # Safe exact normalized-name catalog fallback. This is image/reference use only,
    # never a rules/rarity/ownership assertion.
    cand=ext_by_norm.get(norm(row['raw_card_name']),[])
    if cand:
        cand=sorted(cand,key=lambda r:(set_score(row['set_name'],r['external_set_name']), r['link_status'].startswith('LINKED'), -r['catalog_entry_id']),reverse=True)
        r=cand[0]; return r['external_image_url'],'catalog_exact_name',r['catalog_entry_id']
    return None,None,None

payload={'schema':'focused-magic-inuyasha-test-decks-v2','database':DB.name,'source':'canonical SQLite deck_cards + image/catalog references','decks':[]}
for did in DECK_IDS:
    d=con.execute('select * from decks where deck_id=?',(did,)).fetchone()
    rows=list(con.execute("select * from deck_cards where deck_id=? and upper(zone)='MAIN' order by deck_card_id",(did,)))
    out=[]; copies=0; imaged=0
    for row in rows:
        qty=int(row['quantity'] or 0); copies+=qty
        card,method=resolve_card(row)
        url,img_method,catalog_id=resolve_image(row,card)
        if url: imaged+=qty
        out.append({
            'name':row['raw_card_name'],'quantity':qty,
            'cardId':card['card_id'] if card else row['card_id'],
            'cardType':card['card_type'] if card else None,
            'rarity':card['rarity'] if card else None,
            'setName':row['set_name'],'collectorNo':row['collector_no'],
            'evidenceStatus':row['evidence_status'],'reconstructed':bool(row['reconstructed']),
            'identityMethod':method,'imageUrl':url,'imageAssetId':asset_id(url),
            'imageSource':img_method,'catalogEntryId':catalog_id,
        })
    payload['decks'].append({
        'deckId':did,'name':d['deck_name'],'deckKind':d['deck_kind'],
        'cardCount':copies,'declaredCardCount':d['card_count'],'evidenceStatus':d['evidence_status'],
        'notes':d['notes'],'imageBackedCopies':imaged,'cards':out,
    })

OUT_JSON.parent.mkdir(parents=True,exist_ok=True)
text=json.dumps(payload,ensure_ascii=False,separators=(',',':'))
OUT_JSON.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
OUT_JS.write_text('window.InuYashaDeckData='+text+';\n',encoding='utf-8')
for d in payload['decks']:
    print(f"deck {d['deckId']}: {d['cardCount']} cards; {d['imageBackedCopies']} image-backed")
    if d['declaredCardCount'] and d['cardCount']!=d['declaredCardCount']:
        raise SystemExit(f"deck {d['deckId']} total {d['cardCount']} != declared {d['declaredCardCount']}")
    missing=[x['name'] for x in d['cards'] if not x['imageUrl']]
    if missing: print(' missing images:',missing)
