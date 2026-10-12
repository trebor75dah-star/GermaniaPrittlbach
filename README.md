# Germania Prittlbach – Android-App V3 + automatischer Feed

## Was automatisch läuft
- GitHub Actions ruft alle 30 Minuten die sieben öffentlichen Ergebnisquellen ab und prüft deren Erreichbarkeit.
- Nachrichtenlinks werden aus Google News RSS (Suche nach Germania Prittlbach bei Merkur/SZ) und aus den Links der Vereinswebsite gesammelt, dedupliziert und als `docs/news.json` veröffentlicht.
- Die Android-App liest beim Start `news.json` und zeigt neue Nachrichten an, wenn der Feed eingerichtet und erreichbar ist.
- Instagram wird NICHT automatisiert ausgelesen. Dafür wären ein bestätigter Account, Rechte und ggf. eine Meta-API-Anbindung nötig.
- Tabellenstände, Paarungen und Schießergebnisse werden NICHT automatisch extrahiert; die sieben Ergebnis-Webseiten sind weiterhin direkt in der App abrufbar. Ein korrektes strukturiertes Parsing ist erst nach Quellenprüfung und gegebenenfalls Genehmigung umsetzbar.

## Inbetriebnahme (einmalig durch Vereins-Admin)
1. Dieses Projekt in ein öffentliches GitHub-Repository namens `GermaniaPrittlbach` hochladen (Inhalt des Ordners als Repository-Wurzel).
2. Unter Settings → Actions → General Schreibrechte für GitHub Actions erlauben.
3. Unter Settings → Pages die Veröffentlichung aus dem Branch `main`, Ordner `/docs` aktivieren.
4. GitHub Actions → `Update public club feeds` → Run workflow einmal ausführen. Kontrollieren, ob `docs/news.json` und `docs/teams.json` erzeugt wurden.
5. In `app/src/main/java/de/germania/prittlbach/MainActivity.kt` `YOUR_GITHUB_USERNAME` durch den tatsächlichen GitHub-Benutzernamen ersetzen (oder `FEED_URL` durch die echte öffentliche URL).
6. Android Studio öffnen, Gradle synchronisieren und App auf Gerät testen. APK/AAB erst danach signieren und verteilen.

## Grenzen und Sicherheit
- Ein GitHub-Actions-Zeitplan ist best effort; GitHub kann Läufe verzögern oder bei Inaktivität deaktivieren.
- Google-News-Suche ist kein vollständiges oder garantiert aktuelles Verzeichnis. Nachrichten werden als Link mit Titel angezeigt; keine Volltexte kopiert.
- Quellen können Zugriffe blockieren, ihre HTML-Struktur ändern oder ihre Nutzungsbedingungen anpassen. `source_reachable` sagt nichts über das Vorliegen neuer Ergebnisse aus.
- Die Android-App benötigt Internetzugriff. Der Feed wird beim App-Start geladen; es gibt noch keine Hintergrund-Push-Benachrichtigungen.
- Dies ist ein Quellcode-Prototyp und wurde nicht auf einem Android-Gerät kompiliert oder getestet.
