# Einkaufsliste

Einfache Einkaufslisten-App für Android – ohne Konto, ohne Werbung, alle Daten bleiben auf dem Handy.

## Funktionen

- **Mehrere Listen** – z. B. pro Laden, für die Drogerie oder als Packliste, jeweils mit eigenem Symbol.
- **Vorschläge beim Eintippen** – alles, was du einmal eingetragen hast, wird gespeichert und beim nächsten
  Mal vorgeschlagen (sortiert danach, wie oft du es kaufst und auf welcher Liste). Ein Tipp auf den Vorschlag fügt
  den Artikel hinzu. Im leeren Eingabefeld erscheinen deine häufigsten Artikel.
- **Erledigt markieren per Wischgeste** – Artikel **nach rechts wischen** = erledigt (bzw. zurückholen),
  **nach links wischen** = löschen. Ein normales Antippen öffnet nur den Bearbeiten-Dialog (Name, Menge/Notiz),
  dadurch wird nichts versehentlich abgehakt. Jede Aktion lässt sich über „Rückgängig“ zurücknehmen.
- **Listen teilen** – über das Menü ⋮ → „Liste teilen“ (WhatsApp, Signal, E-Mail …). Die Nachricht enthält die
  lesbare Liste und einen Code. Wer die App hat, kann die Nachricht in WhatsApp lange drücken → Teilen →
  *Einkaufsliste* wählen, oder sie kopieren und in der App „Liste importieren“ nutzen. Die Liste kann als neue Liste
  angelegt oder in eine gleichnamige Liste übernommen werden.
- **Vorschläge verwalten** – falsch geschriebene oder nicht mehr gebrauchte Vorschläge lassen sich löschen.
- Heller und dunkler Modus (folgt der Systemeinstellung).

## Installation

Bei jedem Push baut GitHub Actions automatisch die APK und veröffentlicht sie unter
**Releases** (`Einkaufsliste.apk`).

1. Auf dem Handy die neueste Release-Seite öffnen und `Einkaufsliste.apk` herunterladen.
2. Datei öffnen. Beim ersten Mal fragt Android, ob der Browser/Dateimanager Apps installieren darf → erlauben.
3. Für Updates einfach die neue APK genauso installieren – die Listen bleiben erhalten.

## Aufbau

| Ordner | Inhalt |
|---|---|
| `web/` | Die eigentliche App (HTML/CSS/JavaScript). Läuft auch im Browser und lässt sich dort als Web-App installieren. |
| `android/` | Schlanke Android-Hülle (WebView), die `web/` einbettet und Teilen, Zwischenablage und Vibration bereitstellt. |
| `.github/workflows/android.yml` | Baut die APK und erstellt das Release. |

Lokal testen: `cd web && python3 -m http.server` und `http://localhost:8000` öffnen.
Lokal bauen (Android-SDK nötig): `cd android && ./gradlew assembleRelease`.

Der Signaturschlüssel `android/app/app.keystore` liegt bewusst im Repo, damit jede gebaute APK dieselbe Signatur
hat und sich über die vorherige Version installieren lässt.
