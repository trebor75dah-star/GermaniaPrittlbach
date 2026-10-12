"""Generate public JSON feeds; best-effort metadata, no fabricated results."""
import json, datetime, urllib.request, urllib.parse, xml.etree.ElementTree as ET, re
from pathlib import Path
from html.parser import HTMLParser

ROOT=Path(__file__).resolve().parents[1]/'docs'; ROOT.mkdir(exist_ok=True)
HEAD={'User-Agent':'GermaniaPrittlbachClubApp/1.0 (public news aggregation)'}
TEAMS=[
('Luftgewehr 1','https://bundesliga.dsb.de/luftgewehr/1-bl-sued'),
('Luftgewehr 2','https://www.rwk-onlinemelder.de/online/listen/000bssb?sel_group_id=7&sel_discipline_id=1&sel_class_id=4&showCombined=0'),
('Luftgewehr 3','https://www.rwk-onlinemelder.de/online/listen/407?sel_group_id=72&sel_discipline_id=1&sel_class_id=1&sel_list_type=competitionSchedule&sel_turn_date=&showCombined=0'),
('Luftgewehr 4','https://www.rwk-onlinemelder.de/online/listen/407?sel_group_id=72&sel_discipline_id=1&sel_class_id=14&sel_list_type=competitionSchedule&sel_turn_date=&showCombined=0'),
('Luftgewehr 5','https://www.rwk-onlinemelder.de/online/listen/407?sel_group_id=72&sel_discipline_id=1&sel_class_id=32&sel_list_type=competitionSchedule&sel_turn_date=&showCombined=0'),
('Luftpistole 1','https://www.rwk-onlinemelder.de/online/listen/407?sel_group_id=74&sel_discipline_id=2&sel_class_id=6&showCombined=0'),
('Luftpistole 2','https://www.rwk-onlinemelder.de/online/listen/407?sel_group_id=74&sel_discipline_id=2&sel_class_id=7&showCombined=0')]

def get(url):
    with urllib.request.urlopen(urllib.request.Request(url,headers=HEAD),timeout=18) as r:
        return r.read()

class Links(HTMLParser):
    def __init__(self): super().__init__();self.href=None;self.txt='';self.links=[]
    def handle_starttag(self,tag,attrs):
        if tag=='a': self.href=dict(attrs).get('href');self.txt=''
    def handle_data(self,data):
        if self.href is not None:self.txt+=data
    def handle_endtag(self,tag):
        if tag=='a' and self.href:
            self.links.append((self.txt.strip(),self.href));self.href=None

now=datetime.datetime.now(datetime.timezone.utc).isoformat()
news=[]
for publisher,domain in [('Merkur','merkur.de'),('Süddeutsche Zeitung','sueddeutsche.de')]:
    q=urllib.parse.quote('"Germania Prittlbach" site:'+domain)
    rss='https://news.google.com/rss/search?q='+q+'&hl=de&gl=DE&ceid=DE:de'
    try:
        root=ET.fromstring(get(rss))
        for item in root.findall('./channel/item')[:12]:
            title=item.findtext('title','').strip();link=item.findtext('link','').strip()
            if title and link and 'prittlbach' in title.lower():
                news.append(dict(title=title,source=publisher,date=item.findtext('pubDate',''),summary='',url=link))
    except Exception as e:print('News source unavailable:',publisher,str(e)[:100])
try:
    base='https://www.germania-prittlbach.de/'
    parser=Links();parser.feed(get(base).decode('utf-8','replace'))
    for title,href in parser.links:
        url=urllib.parse.urljoin(base,href)
        if 'germania-prittlbach.de' in urllib.parse.urlparse(url).netloc and ('aktuelles/' in url or 'news' in url.lower()) and title and len(title)>8:
            news.append(dict(title=title[:180],source='Vereinswebsite',date='',summary='',url=url))
except Exception as e:print('Club site unavailable:',str(e)[:100])
seen=set();unique=[]
for item in news:
    if item['url'] not in seen:unique.append(item);seen.add(item['url'])
(ROOT/'news.json').write_text(json.dumps(dict(updated_at=now,items=unique[:40]),ensure_ascii=False,indent=2),encoding='utf-8')
status=[]
for name,url in TEAMS:
    available=False
    try:available=len(get(url))>100
    except Exception as e:print('Result page unavailable:',name,str(e)[:100])
    status.append(dict(name=name,url=url,source_reachable=available,results=None,standing=None,updated_at=now))
(ROOT/'teams.json').write_text(json.dumps(dict(updated_at=now,teams=status,note='No structured match scores until a licensed/validated provider adapter is available.'),ensure_ascii=False,indent=2),encoding='utf-8')
print('Published',len(unique),'news links and',len(status),'team source statuses')
