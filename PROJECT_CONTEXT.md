# Proiect Netex: agenda de contacte

Ultima actualizare: 23 septembrie 2026

## Scopul acestui fișier

Acesta este contextul de lucru pentru proiectul de interviu. Poate fi dat unui alt chat AI împreună cu repository-ul. La fiecare etapă, actualizăm secțiunile **Stare curentă**, **Următorul pas** și **Jurnal de progres**. O decizie planificată nu trebuie prezentată ca funcționalitate deja implementată.

## Context personal și obiectiv

- Candidat pentru un post junior full-time la Netex.
- Experiență practică anterioară cu TypeScript, React și MongoDB; cunoștințe teoretice de Java, OOP, SOLID, KISS și DRY.
- Este permisă folosirea AI, dar candidatul trebuie să înțeleagă și să poată explica aplicația la interviu.
- Țintă: toate cerințele funcționale până la sfârșitul zilei 4; ziua 5 pentru integrare, verificare, documentație și predare. Planul depinde de timpul disponibil zilnic.
- Prioritate: proiect complet, cu structură clară și extensibilitate justificată, fără arhitectură inutilă.

## Cerințele primite

- Aplicație web în Java pentru gestionarea contactelor; fiecare contact are fotografie, nume și adresă.
- Adăugare, editare, ștergere și export CSV.
- Înregistrare utilizatori; la înregistrare se publică un mesaj într-un topic Kafka și există un consumer care îl procesează.
- Adăugarea și ștergerea cer autentificare; listarea contactelor este publică.
- Folosirea AJAX cât mai mult posibil.
- Spring Boot, Java 25 și un framework UI modern la alegere.
- Căutare după nume.
- Contactele se stochează într-o bază de date **SQL**; structura bazei de date trebuie proiectată.
- Întreaga aplicație rulează în Docker și are instrucțiuni detaliate de pornire.
- Codul se urcă pe GitHub și se trimite URL-ul repository-ului.
- Cel puțin un microserviciu; aplicația comunică cu el prin HTTP.

## Decizii de implementare convenite

### Arhitectură

- Frontend: React + TypeScript, cu cereri HTTP asincrone către API. Interfața nu se reîncarcă pentru căutare sau modificări.
- API principal: Spring Boot pe Java 25, construit cu Maven.
- Persistență: PostgreSQL. MongoDB nu satisface cerința explicită de SQL.
- Autentificare: Spring Security cu sesiune și cookie; regulile de acces se verifică pe server. Configurăm corect CSRF pentru cererile de modificare venite din React.
- Al doilea serviciu: `activity-service`, o aplicație Spring Boot mică. Procesează evenimentul de înregistrare din Kafka și primește prin HTTP evenimente despre modificarea contactelor. Rezultatul procesării trebuie să fie demonstrabil, nu doar un apel HTTP fără efect.
- Docker Compose pornește frontendul, cele două servicii Java, PostgreSQL și Kafka. Java, Node, PostgreSQL și Kafka sunt în imagini/containere; pe laptopul evaluatorului sunt necesare Git și Docker Desktop/Engine cu Compose.

### Directoare și rulare separată

Vom avea **un singur repository Git** pentru toate componentele. Structura dorită este:

```text
backend/             aplicația Spring Boot principală (`contacts-api`)
frontend/            React + TypeScript
microservice/        a doua aplicație Spring Boot (`activity-service`)
docs/                explicații despre structura bazei de date
compose.yaml         pornirea întregului sistem în Docker
README.md            instrucțiuni de dezvoltare și predare
PROJECT_CONTEXT.md   decizii, stare și pași următori
```

- În dezvoltare, frontendul și fiecare aplicație Java pornesc prin comenzi proprii, din directoarele lor. PostgreSQL și Kafka pot rula în Docker.
- Frontendul apelează doar `contacts-api`; acesta comunică cu `activity-service`.
- Pentru sesiune și CSRF, folosim același prefix `/api` din browser: serverul de dezvoltare al frontendului trimite cererile API către backend prin proxy. La rularea completă în Docker, serverul web al frontendului face același lucru. Procesele și containerele rămân separate.
- README-ul final va conține atât comenzile de rulare separată pentru dezvoltare, cât și comanda Docker Compose pentru evaluator. Pentru rularea Java/React în afara Docker sunt necesare local Java 25 și Node; pentru rularea integrală în Docker sunt necesare doar Git și Docker cu Compose.
- Recomandare de lucru: IntelliJ IDEA pentru cele două aplicații Java și VS Code pentru frontend. Proiectul se construiește cu Maven și Docker, independent de IDE. Funcțiile Spring avansate din IntelliJ necesită Ultimate; ediția gratuită rămâne utilizabilă pentru Java și Maven. VS Code este o alternativă validă pentru backend cu extensiile Java și Spring Boot.

### Contacte, fotografii și permisiuni

- Utilizatorul încarcă o fotografie reală din formularul de creare sau editare. Nu folosim un URL introdus manual și nu hardcodăm fotografiile.
- API-ul primește fișierul prin `multipart/form-data`, validează tipul și dimensiunea și îl salvează sub un nume generat de server într-un director persistent prin volum Docker. În SQL păstrăm calea/numele fișierului. Fotografiile au un endpoint public de citire.
- Oricine poate lista și căuta contacte. Orice utilizator autentificat poate crea contacte. Doar autorul poate edita sau șterge propriile contacte; serverul verifică `created_by_user_id` și răspunde cu 403 când regula nu este îndeplinită.
- **Interpretare suplimentară:** cerința Netex nu cere explicit proprietatea contactelor sau autentificare pentru editare. Aceste reguli au fost alese pentru un comportament coerent și vor fi explicate în README.
- Exportul CSV va conține datele contactului și o referință/URL la fotografie, deoarece un fișier imagine binar nu se exportă direct într-o celulă CSV.

### Structură și calitatea codului

- Organizăm codul Java pe funcționalități: `contact`, `auth`, `image`, `activity`, plus configurarea comună necesară.
- Controllerul gestionează HTTP, service-ul regulile aplicației, repository-ul accesul la SQL. Entitățile de persistare nu devin automat contractul public al API-ului; folosim DTO-uri unde sunt utile.
- Injecție prin constructor, validare a intrărilor, răspunsuri HTTP clare și migrații SQL reproductibile.
- Folosim interfețe la granițe unde există o variație plauzibilă: de exemplu `ImageStorage` pentru stocarea fotografiilor și un client pentru comunicarea HTTP cu `activity-service`. Nu creăm câte o interfață pentru fiecare clasă.
- Aplicăm SOLID, KISS și DRY practic: fiecare abstracție trebuie să separe o responsabilitate reală sau să simplifice o schimbare probabilă.
- Teste țintite pentru regulile importante: acces public, acces protejat, verificarea autorului, operații cu contacte, CSV, upload și procesarea evenimentului de înregistrare.

## Structura de date propusă

În baza aplicației principale:

- `users`: `id`, `email` unic, `password_hash`, `created_at`.
- `contacts`: `id`, `name`, `address`, `picture_path`, `created_by_user_id` (cheie externă către `users.id`), `created_at`, `updated_at`.

Adresa rămâne un singur câmp text, suficient pentru această cerință. Structura exactă a istoricului din `activity-service` se stabilește la implementarea serviciului; nu presupunem că este deja făcută.

Definiția executabilă a tabelelor va fi în `backend/src/main/resources/db/migration/`, prin migrații SQL Flyway precum `V1__create_users_and_contacts.sql`. Fișierele vor arăta explicit `CREATE TABLE`, cheile primare, cheile externe, constrângerile și indicii. Pentru schimbări ulterioare adăugăm o migrație nouă, de exemplu `V2__add_contact_field.sql`, în loc să rescriem o migrație deja aplicată. Vom adăuga și `docs/database.md`, cu o explicație scurtă și o diagramă a relației dintre tabele. Fișierele se creează la implementare; momentan schema este doar propusă.

## Planul pe 5 zile

| Ziua | Rezultat verificabil |
| --- | --- |
| 1 | Scheletul proiectului, PostgreSQL în Docker, schema SQL și API-ul de bază pentru contacte. |
| 2 | React conectat la API; înregistrare, login, logout și reguli de acces pe server. |
| 3 | Upload real de fotografii, căutare după nume, export CSV și publicarea evenimentului de înregistrare în Kafka. |
| 4 | Consumerul Kafka, apelul HTTP dintre servicii și pornirea completă prin Docker Compose; demonstrație cap-coadă. |
| 5 | Remedierea problemelor, teste, README detaliat, verificare dintr-un clone curat și push pe GitHub. |

Ordinea se poate ajusta dacă apare un blocaj, dar nu eliminăm cerințe obligatorii doar ca să respectăm tabelul. La finalul fiecărei etape, candidatul trebuie să poată explica fluxul implementat.

## Pașii de implementare, în ordine

1. **Pregătirea proiectului (ziua 1) — finalizat.** Git, cele trei aplicații independente, contractul minim în `docs/API.md` și comenzile de dezvoltare în `README.md`. Verificat: fiecare aplicație pornește separat, iar React comunică cu API-ul prin proxy.
2. **PostgreSQL și schema SQL (ziua 1).** Pornim PostgreSQL prin Docker Compose, definim tabelele `users` și `contacts` prin migrații și conectăm API-ul Java. Verificare: pornirea pe o bază nouă creează schema fără pași manuali.
3. **API-ul contactelor (zilele 1–2).** Implementăm listare publică, creare, editare, ștergere, validare și căutare după nume. Păstrăm regulile în service, nu în controller. Verificare: cererile HTTP întorc date și coduri de răspuns corecte.
4. **Prima interfață React (ziua 2).** Afișăm contactele și legăm formularele și căutarea de API prin cereri asincrone. Verificare: modificările apar în pagină fără refresh complet.
5. **Conturi și autorizare (ziua 2).** Implementăm înregistrare, login, logout și sesiunea. Protejăm modificările pe server și verificăm autorul la editare/ștergere; conectăm React la aceste fluxuri. Verificare: vizitatorul poate citi, dar nu poate modifica, iar un utilizator nu poate modifica datele altuia.
6. **Fotografii încărcate de utilizator (ziua 3).** Implementăm upload `multipart/form-data`, validare de fișier, stocare într-un volum Docker și afișare publică. Verificare: imaginea rămâne disponibilă după repornire și poate fi înlocuită la editare.
7. **Export CSV (ziua 3).** Generăm CSV corect, inclusiv pentru texte cu virgule, ghilimele sau linii noi; includem referința fotografiei. Verificare: fișierul descărcat se deschide corect.
8. **Evenimentul de înregistrare în Kafka (zilele 3–4).** La crearea contului, API-ul publică un mesaj; `activity-service` îl consumă și produce un rezultat observabil. Verificare: o înregistrare nouă poate fi urmărită de la API până la consumer.
9. **Comunicarea HTTP cu microserviciul (ziua 4).** API-ul principal trimite către `activity-service` evenimente despre creare, editare și ștergere. Verificare: vedem cererea și efectul ei în serviciul destinatar.
10. **Pornirea completă și predarea (zilele 4–5).** Finalizăm Dockerfile-urile și Compose, verificările de pornire, testele țintite și README-ul. Clonăm proiectul într-un director nou și urmăm exact instrucțiunile; apoi urcăm codul pe GitHub și trimitem URL-ul.

La fiecare pas: implementăm, rulăm, verificăm, explicăm fluxul și actualizăm **Stare curentă** și **Următorul pas**. Un pas este încheiat doar după verificare.

## Criterii de finalizare

- Un evaluator poate clona repository-ul și porni aplicația urmând exclusiv README-ul și comenzile Docker Compose.
- Vizitatorul poate vedea și căuta contacte fără login.
- Utilizatorul își poate crea cont, se poate autentifica, poate adăuga un contact cu fotografie încărcată, își poate edita/șterge contactul și poate exporta CSV.
- Serverul refuză modificările neautorizate și modificarea contactelor altui autor.
- Înregistrarea publică un mesaj Kafka; consumerul îl procesează într-un mod observabil.
- API-ul principal comunică efectiv prin HTTP cu `activity-service`.
- Baza de date și fotografiile persistă după repornirea containerelor.
- Codul este pe GitHub, fără parole sau alte secrete reale comise în repository.

## Stare curentă

**Pasul 1 este implementat și verificat.** Repository-ul conține:

- `backend/`: `contacts-api`, Spring Boot 3.5.16 pe Java 25, port 8080, cu `GET /api/health`.
- `microservice/`: `activity-service`, aceeași configurație Java, port 8081, cu `GET /internal/health`.
- Maven Wrapper 3.9.16 pentru fiecare serviciu; nu este necesară instalarea separată a Maven.
- `frontend/`: React + TypeScript + Vite, pentru Node.js 24, port 5173. O pagină temporară verifică asincron starea API-ului și permite reîncercarea. Proxy-ul Vite trimite `/api/*` către backend.
- `README.md`: cerințe locale, comenzi separate pentru Windows/macOS/Linux, porturi, configurare opțională și verificări.
- `docs/API.md`: endpointurile de health implementate și contractul propus pentru etapele următoare.
- `.gitignore` pentru dependințe, rezultate de build, IDE, fișiere de mediu și uploaduri; `.gitattributes` pentru terminatoare de linie compatibile între platforme. Fișierele `.gitkeep` au fost înlocuite de fișiere reale.

Verificări efectuate pe Windows cu JDK 25.0.1 și Node.js 24.14.1: `mvnw.cmd verify` reușit pentru ambele servicii (4 teste în total), `npm run lint` fără avertismente și `npm run build` reușit, inclusiv verificarea TypeScript. Cele două endpointuri de health și proxy-ul au răspuns cu HTTP 200. În browser au fost verificate conectarea, eroarea când backendul este oprit și reconectarea prin buton după repornirea backendului. Procesele de verificare au fost oprite la final; aplicațiile se pornesc cu instrucțiunile din README.

**Nu sunt implementate încă** contactele, SQL, autentificarea, fotografiile, CSV, Kafka, comunicarea HTTP de business sau Docker Compose. Health-ul microserviciului nu îndeplinește singur cerința de interacțiune HTTP dintre servicii. Nu marca aceste cerințe ca finalizate înainte de implementare și verificare.

## Următorul pas

**Pasul 2: PostgreSQL și schema SQL.** Pornim motorul Docker Desktop, adăugăm PostgreSQL în Compose cu volum persistent, configurăm conexiunea backendului și Flyway, apoi scriem migrarea pentru `users` și `contacts`. Documentăm tabelele în `docs/database.md`. Verificare: o bază nouă primește schema automat la pornirea backendului. API-ul contactelor urmează în pasul 3.

## Întrebări încă deschise

- Numărul de ore disponibile zilnic și data exactă a predării nu au fost precizate.
- CV-ul a fost menționat, dar nu a fost trimis încă în această conversație.
- Trebuie decis la implementare cum persistă `activity-service` istoricul evenimentelor; comportamentul trebuie să poată fi demonstrat ușor.
- Configurarea variabilelor de mediu pentru SQL, Kafka și Compose se va documenta odată cu introducerea lor. Porturile locale și opțiunea `API_PROXY_TARGET` sunt deja documentate în README.

## Jurnal de progres

- 2026-09-23: cerințele și deciziile discutate au fost centralizate în acest fișier; planul a fost detaliat în pași de implementare. S-a decis folosirea unui singur repository Git cu `backend/`, `frontend/` și `microservice/`, precum și documentarea structurii SQL prin migrații și diagramă.
- 2026-09-23: repository-ul Git a fost conectat la GitHub. Au fost create cele trei directoare majore și `.gitignore`. Nu a început implementarea aplicației.
- 2026-09-23: pasul 1 a fost implementat: două aplicații Spring Boot pe Java 25, React + TypeScript + Vite, Maven Wrapper, health prin Actuator, proxy și pagină de verificare a conexiunii. Au fost adăugate README și contractul HTTP; buildurile, testele Java, lintul și pornirea separată au fost verificate.

## Instrucțiune pentru un alt chat AI

Înainte de modificări, citește acest fișier și inspectează starea reală a repository-ului. Tratează secțiunea **Stare curentă** ca istoric care poate deveni depășit: verifică fișierele și testele. Continuă cu **Următorul pas**, lucrează în pași mici, explică deciziile candidatului și actualizează progresul doar după verificare. Păstrează proiectul suficient de simplu pentru a fi înțeles la un interviu junior, dar implementează complet cerințele Netex.
