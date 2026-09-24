# Proiect Netex: agenda de contacte

Ultima actualizare: 24 septembrie 2026

## Scopul acestui fișier

Acesta este contextul de lucru pentru proiectul de interviu. Poate fi dat unui alt chat AI împreună cu repository-ul. La fiecare etapă, actualizăm secțiunile **Stare curentă**, **Următorul pas** și **Jurnal de progres**. O decizie planificată nu trebuie prezentată ca funcționalitate deja implementată.

## Context personal și obiectiv

- Candidat pentru un post junior full-time la Netex.
- Experiență practică anterioară cu TypeScript, React și MongoDB; cunoștințe teoretice de Java, OOP, SOLID, KISS și DRY.
- Este permisă folosirea AI, dar candidatul trebuie să înțeleagă și să poată explica aplicația la interviu.
- Țintă: toate cerințele funcționale până la sfârșitul zilei 4; ziua 5 pentru integrare, verificare, documentație și predare. Planul depinde de timpul disponibil zilnic.
- Prioritate: proiect complet, cu structură clară și extensibilitate justificată, fără arhitectură inutilă.
- Mod de lucru pentru învățare: explică înainte de fiecare schimbare ce este componenta, de ce există și cum se verifică. Candidatul dorește pași mici și este începător în Spring, Docker și SQL. Folosește DBeaver pentru inspecția vizuală a bazei; nu cere candidatului să inspecteze tabelele prin psql. Așteaptă verificarea vizuală a fiecărui punct de învățare înainte de următoarea modificare dependentă.

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

## Structura de date implementată

În baza aplicației principale:

- `users`: `id`, `email` unic, `password_hash`, `created_at`.
- `contacts`: `id`, `name`, `address`, `picture_path`, `created_by_user_id` (cheie externă către `users.id`), `created_at`, `updated_at`.

Adresa este un singur câmp text, limitat prin CHECK la 1000 de caractere. Emailul are index unic pe `lower(email)` și trebuie să fie fără spații la extremități. ID-urile sunt `BIGINT GENERATED ALWAYS AS IDENTITY`. FK-ul autorului folosește `ON DELETE RESTRICT` și are index. `picture_path` permite temporar NULL până la upload (pasul 6). Momentele de creare/modificare au DEFAULT CURRENT_TIMESTAMP; viitorul service Java trebuie să actualizeze `updated_at` la editare. Structura exactă a istoricului din `activity-service` se stabilește la implementarea serviciului; nu presupunem că este deja făcută.

Definiția executabilă este `backend/src/main/resources/db/migration/V1__create_users_and_contacts.sql`, aplicată prin Flyway. Pentru schimbări ulterioare adăugăm o migrație nouă, de exemplu `V2__add_contact_field.sql`, în loc să rescriem V1 deja aplicată. `docs/database.md` este documentul central pentru toate tabelele proiectului: inventar, coloane, reguli, exemple și istoricul Flyway. Diagrama este și în `docs/database.drawio`, `.svg` și `.png`. Actualizează documentația și diagramele la fiecare schimbare de schemă, inclusiv pentru viitorul microserviciu.

## Planul pe 5 zile

| Ziua | Rezultat verificabil |
| --- | --- |
| 1 | Scheletul proiectului, PostgreSQL în Docker, schema SQL și API-ul de bază pentru contacte. |
| 2 | React conectat la API; înregistrare, login, logout, creare/editare/ștergere contacte și reguli de acces pe server. |
| 3 | Upload real de fotografii, export CSV și publicarea evenimentului de înregistrare în Kafka. |
| 4 | Consumerul Kafka, apelul HTTP dintre servicii și pornirea completă prin Docker Compose; demonstrație cap-coadă. |
| 5 | Remedierea problemelor, teste, README detaliat, verificare dintr-un clone curat și push pe GitHub. |

Ordinea se poate ajusta dacă apare un blocaj, dar nu eliminăm cerințe obligatorii doar ca să respectăm tabelul. La finalul fiecărei etape, candidatul trebuie să poată explica fluxul implementat.

## Pașii de implementare, în ordine

1. **Pregătirea proiectului (ziua 1) — finalizat.** Git, cele trei aplicații independente, contractul minim în `docs/API.md` și comenzile de dezvoltare în `README.md`. Verificat: fiecare aplicație pornește separat, iar React comunică cu API-ul prin proxy.
2. **PostgreSQL și schema SQL (ziua 1) — implementat și verificat.** PostgreSQL în Docker Compose, conexiune Spring JDBC și Flyway V1 pentru `users` și `contacts`. Verificate: migrarea pe o bază goală de test, regulile SQL și aplicarea în baza locală `netex`. Candidatul poate inspecta cele trei tabele în DBeaver.
3. **API-ul public al contactelor (zilele 1–2) — finalizat.** Listarea și căutarea după nume au controller, service și repository; parametrii sunt validați. Cererile HTTP publice și codurile de răspuns au fost verificate. Crearea, editarea și ștergerea vin în pasul 5, odată cu autentificarea.
4. **Prima interfață React (ziua 2) — finalizată.** Lista publică și căutarea după nume folosesc cereri HTTP asincrone. Lista și rezultatele se actualizează fără refresh complet; au fost verificate în browser cu baza goală și cu două contacte temporare șterse după test.
5. **Conturi, autorizare și modificarea contactelor (ziua 2).** Implementăm înregistrare, login, logout și sesiunea. Adăugăm creare, editare, ștergere și validarea datelor în backend; păstrăm regulile în service, protejăm modificările pe server și verificăm autorul la editare/ștergere. Legăm formularele React de API prin cereri asincrone. Verificare: utilizatorul autentificat poate crea și modifica propriile contacte, vizitatorul poate doar citi, iar modificarea contactelor altui autor este refuzată.
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

**Pasul 2 este implementat și verificat.** `compose.yaml` pornește `postgres:17.11` pe `127.0.0.1:5432`, cu volum persistent și healthcheck. Backendul folosește `spring-boot-starter-jdbc`, driverul PostgreSQL 42.7.11 și Flyway 11.7.2 (versiuni gestionate de Spring Boot). Profilul `local` importă `.env` de la rădăcină ca fișier properties; pornește din `backend/` cu `mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"` sau cu argumentul IntelliJ `--spring.profiles.active=local`. Formatul `.env` documentat este simplu KEY=value fără ghilimele sau expresii shell. Rularea fără profil local folosește variabilele de mediu.

V1 a fost aplicată în baza locală `netex`: `users` și `contacts` au 0 rânduri, `flyway_schema_history` are V1 cu succes. Health a răspuns HTTP 200 cu UP și include acum conexiunea SQL. Cele 8 teste ale backendului au trecut cu Testcontainers 1.21.4 și PostgreSQL temporar: migrare pe bază goală și fără reaplicare, relație cu autorul, ID-uri/date, unicitate email indiferent de litere, autor inexistent, ștergerea autorului cu contacte și nume gol, plus cele două verificări health. Testele cer Docker, dar nu folosesc `.env` sau baza locală. Backendul pornit pentru verificare a fost oprit; PostgreSQL a rămas pornit pentru DBeaver. Inspecția vizuală de către candidat nu este încă confirmată.

**Pasul 3 este implementat și verificat.** `contact/` conține `ContactController`, `ContactService` și `ContactRepository` cu Spring JDBC. `GET /api/contacts` listează public, ordonat după ID; parametrul opțional `name` caută un fragment fără diferență între litere mari și mici. Lipsa parametrului sau un text gol după eliminarea spațiilor listează toate contactele, iar o căutare de peste 255 de caractere primește 400. `GET /api/contacts/{id}` întoarce un contact sau 404. Căutarea folosește parametri SQL și tratează `%` și `_` ca text obișnuit. `Contact` reprezintă rândul intern, iar `ContactResponse` expune momentan doar `id`, `name` și `address`; calea fișierului și autorul rămân interne. Șase teste HTTP cu MockMvc și PostgreSQL temporar acoperă lista goală, ordinea și câmpurile publice, căutarea, caracterele speciale, lungimea invalidă și ID-ul absent. Împreună cu testele precedente, backendul are 14 teste verificate. Backendul a fost pornit temporar cu profilul `local`: lista și căutarea au întors HTTP 200 cu `[]`, iar un ID inexistent a întors 404. Backendul de probă a fost oprit; PostgreSQL local a rămas pornit.

**Pasul 4 este implementat și verificat.** `frontend/src/api/contacts.ts` trimite `GET /api/contacts` cu parametrul opțional `name` prin proxy-ul Vite. `App.tsx` păstrează textul căutării, contactele și starea cererii; întârzie cu 250 ms căutarea în timpul tastării și anulează cererile vechi. `ContactCard.tsx` afișează numele și adresa fiecărui rezultat. Interfața are mesaje pentru încărcare, listă goală, zero rezultate și eroare cu reîncercare. Buildul TypeScript și lintul trec. În browser s-au verificat lista goală, două contacte temporare, filtrarea fără diferență între litere mari și mici, golirea căutării și recuperarea după oprirea/repornirea backendului. Cele două contacte și utilizatorul temporar au fost șterse; baza locală a rămas fără date demo. Nu există încă formular de creare, editare sau ștergere.

**Nu sunt implementate încă** crearea/editarea/ștergerea contactelor, autentificarea, fotografiile, CSV, Kafka, comunicarea HTTP de business sau rularea întregului sistem prin Compose. Nu folosim JPA; repository-ul folosește Spring JDBC. Health-ul microserviciului nu îndeplinește singur cerința de interacțiune HTTP dintre servicii. Nu marca aceste cerințe ca finalizate înainte de implementare și verificare.

## Următorul pas

**Punctul de învățare curent:** explică pasul 4 candidatului, fișier cu fișier: `contacts.ts`, `ContactCard.tsx`, `App.tsx` și CSS; urmărește cererea din inputul de căutare prin React, `fetch`, proxy-ul Vite și API, apoi răspunsul în listă. Explică de ce folosim `AbortController` și întârzierea de 250 ms. În baza locală lista rămâne `[]` până la implementarea creării conturilor și contactelor. **Următoarea implementare: pasul 5, autentificarea și modificarea contactelor.** Explică pasul 4 înainte de a începe pasul 5 dacă utilizatorul dorește să înțeleagă codul. Nu aplica din nou manual CREATE TABLE și nu modifica V1 după aplicare.

## Întrebări încă deschise

- Numărul de ore disponibile zilnic și data exactă a predării nu au fost precizate.
- CV-ul a fost menționat, dar nu a fost trimis încă în această conversație.
- Trebuie decis la implementare cum persistă `activity-service` istoricul evenimentelor; comportamentul trebuie să poată fi demonstrat ușor.
- Configurarea variabilelor de mediu pentru SQL, Kafka și Compose se va documenta odată cu introducerea lor. Porturile locale și opțiunea `API_PROXY_TARGET` sunt deja documentate în README.

## Jurnal de progres

- 2026-09-23: cerințele și deciziile discutate au fost centralizate în acest fișier; planul a fost detaliat în pași de implementare. S-a decis folosirea unui singur repository Git cu `backend/`, `frontend/` și `microservice/`, precum și documentarea structurii SQL prin migrații și diagramă.
- 2026-09-23: repository-ul Git a fost conectat la GitHub. Au fost create cele trei directoare majore și `.gitignore`. Nu a început implementarea aplicației.
- 2026-09-23: pasul 1 a fost implementat: două aplicații Spring Boot pe Java 25, React + TypeScript + Vite, Maven Wrapper, health prin Actuator, proxy și pagină de verificare a conexiunii. Au fost adăugate README și contractul HTTP; buildurile, testele Java, lintul și pornirea separată au fost verificate.
- 2026-09-23: a început pasul 2 cu PostgreSQL în Docker Compose, volum persistent, configurație locală exclusă din Git și instrucțiuni DBeaver. Containerul a pornit și a trecut verificarea de disponibilitate. Etapa de învățare curentă: conectarea vizuală la baza goală, înainte de Flyway și schema aplicației.
- 2026-09-23: la cererea candidatului, `docs/database.md` a devenit referința vizuală pentru structura de date a întregului proiect. Au fost documentate diagrama users–contacts, coloanele, regulile și exemplele; sunt diferențiate tabelele propuse, istoricul Flyway și stocarea încă nestabilită a microserviciului. Nu au fost create tabele prin această actualizare de documentație.
- 2026-09-23: diagrama a fost salvată și în fișiere independente: `docs/database.svg` și `docs/database.png` pentru vizualizare și `docs/database.drawio` pentru editare în draw.io / diagrams.net. XML-ul a fost verificat, iar imaginea PNG a fost randată local din SVG și inspectată vizual. Păstrează aceste fișiere sincronizate cu documentația și cu viitoarele migrări. Starea rămâne schemă propusă.
- 2026-09-23: pasul 2 a fost implementat: Spring JDBC, driver PostgreSQL, Flyway și migrarea V1, profil local pentru citirea configurației, 8 teste cu PostgreSQL temporar. Migrarea a fost aplicată și verificată în netex. Documentația și diagramele au fost aliniate cu schema implementată; API-ul contactelor rămâne pasul următor.
- 2026-09-23: pasul 3 a adăugat API-ul public de listare, căutare și citire după ID, cu controller, service, repository și DTO public. Cele 14 teste backend au trecut cu PostgreSQL temporar; nu au fost create contacte demo în baza locală. Documentația API și README au fost actualizate. Urmează explicarea codului și apoi React.
- 2026-09-24: pasul 4 a înlocuit pagina de health din React cu lista publică și căutarea asincronă. Buildul, lintul și verificarea în browser au trecut, inclusiv două contacte temporare, eroarea când backendul e oprit și reîncercarea după repornire. Datele temporare au fost șterse. Urmează explicarea frontendului și pasul 5.

## Instrucțiune pentru un alt chat AI

Înainte de modificări, citește acest fișier și inspectează starea reală a repository-ului. Tratează secțiunea **Stare curentă** ca istoric care poate deveni depășit: verifică fișierele și testele. Continuă cu **Următorul pas**, lucrează în pași mici, explică deciziile candidatului și actualizează progresul doar după verificare. Păstrează proiectul suficient de simplu pentru a fi înțeles la un interviu junior, dar implementează complet cerințele Netex.
