# Einkaufsliste

Native Android-App (Kotlin, Jetpack Compose) für Einkaufs- und Packlisten – ohne Konto, ohne Werbung,
alle Daten bleiben auf dem Handy.

## Funktionen

- **Mehrere Listen** – z. B. pro Laden, für die Drogerie oder als Packliste, jeweils mit eigenem Symbol.
- **Mehrere Listen zusammen ansehen** – Umschalter „Zusammen“ oben auf dem Startbildschirm. Dort tippst du in der
  Leiste oben die Listen an, die du gemeinsam sehen willst (z. B. Rewe und dm); sie stehen dann untereinander.
  Nicht ausgewählte Listen (z. B. die Packliste) bleiben ausgeblendet. Mit dem ➕ neben jeder Liste trägst du direkt
  dort Artikel ein. Die Reihenfolge änderst du über ⋮ → „Reihenfolge der Listen“.
- **Vorschläge beim Eintippen** – alles, was du einmal eingetragen hast, wird gemerkt. Ab dem zweiten Buchstaben
  schlägt die App passende Artikel aus allen Listen **mit demselben Symbol** vor (z. B. alle 🛒-Listen; Artikel
  dieser Liste zuerst, Artikel anderer Listen mit deren Symbol). Ein Tipp auf den Vorschlag fügt den Artikel hinzu. Über die Kopfzeile lassen sich die Vorschläge ein- und
  ausklappen; die Einstellung bleibt gespeichert.
- **Richtige Liste** – trägst du einen Artikel ein, der bisher immer auf einer anderen Liste stand (z. B. Zahnpasta
  bei dm, während du Rewe offen hast), fragt die App, ob er lieber dort hin soll.
- **Erledigt markieren per Wischgeste** – Artikel **zur Seite wischen** (egal ob links oder rechts) = erledigt bzw.
  zurückholen. Ein normales Antippen öffnet nur den Bearbeiten-Dialog (Name, Menge/Notiz); nur dort kann ein Artikel
  gelöscht werden – mit Sicherheitsabfrage. So wird nichts versehentlich abgehakt oder gelöscht. Jede Aktion lässt sich
  über „Rückgängig“ zurücknehmen.
- **Zuletzt erledigt** – falls du versehentlich etwas abgehakt hast: In der Ansicht „Zusammen“ stehen unter jeder
  Liste die letzten 3 erledigten Artikel (über ⋮ ein-/ausblendbar), in der einzelnen Liste ebenfalls die letzten 3 mit
  „Alle anzeigen“. Zur Seite wischen holt einen Artikel zurück.
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

| Pfad | Inhalt |
|---|---|
| `android/app/src/main/java/de/einkaufsliste/app/data/` | Datenmodell, Speicherung (JSON-Datei im App-Speicher), Vorschläge, Teilen-Code |
| `android/app/src/main/java/de/einkaufsliste/app/ui/` | Oberfläche (Jetpack Compose) |
| `.github/workflows/android.yml` | Baut die APK und erstellt das Release |

Lokal bauen (Android-SDK nötig): `cd android && ./gradlew assembleRelease`.

Der Signaturschlüssel `android/app/app.keystore` liegt bewusst im Repo, damit jede gebaute APK dieselbe Signatur
hat und sich über die vorherige Version installieren lässt.
