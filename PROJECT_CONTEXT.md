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

**Extensie cerută ulterior de candidat:** o pagină pentru un utilizator admin, unde se vede istoricul evenimentelor de înregistrare procesate din Kafka și activitatea despre contacte. Aceasta completează cerința de a avea un consumer observabil; nu a fost formulată explicit în mesajul inițial Netex.

## Decizii de implementare convenite

### Arhitectură

- Frontend: React + TypeScript, cu cereri HTTP asincrone către API. Interfața nu se reîncarcă pentru căutare sau modificări.
- API principal: Spring Boot pe Java 25, construit cu Maven.
- Persistență: PostgreSQL. MongoDB nu satisface cerința explicită de SQL.
- Autentificare: Spring Security cu sesiune și cookie; regulile de acces se verifică pe server. Configurăm corect CSRF pentru cererile de modificare venite din React.
- Al doilea serviciu: `activity-service`, o aplicație Spring Boot mică. Procesează evenimentul de înregistrare din Kafka și primește prin HTTP evenimente despre modificarea contactelor. Rezultatul procesării trebuie să fie demonstrabil, nu doar un apel HTTP fără efect.
- Frontendul folosește React Router pentru pagini distincte. `src/features/contacts` păstrează lista, căutarea și cererile de date; `src/navigation` păstrează rutele și antetul. `App.tsx` rămâne scurt. Paginile `/login`, `/signup` și `/admin/activity` sunt momentan doar pagini de pregătire, fără formulare sau date private.
- Pagina admin va afișa istoricul procesat de `activity-service`, nu va citi direct topicul Kafka din browser. Microserviciul va persista evenimentele procesate în SQL și le va oferi backendului printr-un endpoint intern HTTP. Browserul va apela numai API-ul principal, care verifică pe server sesiunea și rolul `ADMIN`. Linkul admin va apărea doar pentru admin după autentificare; ascunderea linkului nu este considerată protecție.
- Înregistrarea publică creează numai conturi `USER`. Un cont `ADMIN` se configurează separat prin `ADMIN_EMAIL` și `ADMIN_PASSWORD` pe server; V2 a adăugat rolul fără să schimbe V1.
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

- `users`: `id`, `email` unic, `password_hash`, `role` (`USER`/`ADMIN`), `created_at`.
- `contacts`: `id`, `name`, `address`, `picture_path`, `created_by_user_id` (cheie externă către `users.id`), `created_at`, `updated_at`.

Adresa este un singur câmp text, limitat prin CHECK la 1000 de caractere. Emailul are index unic pe `lower(email)` și trebuie să fie fără spații la extremități. ID-urile sunt `BIGINT GENERATED ALWAYS AS IDENTITY`. FK-ul autorului folosește `ON DELETE RESTRICT` și are index. `picture_path` permite temporar NULL până la upload (pasul 6). Momentele de creare/modificare au DEFAULT CURRENT_TIMESTAMP; viitorul service Java trebuie să actualizeze `updated_at` la editare. Structura exactă a istoricului din `activity-service` se stabilește la implementarea serviciului; nu presupunem că este deja făcută.

Definițiile executabile sunt `backend/src/main/resources/db/migration/V1__create_users_and_contacts.sql` și `V2__add_user_role.sql`, aplicate prin Flyway. Pentru schimbări ulterioare adăugăm o migrație nouă în loc să rescriem V1 sau V2 deja aplicate. `docs/database.md` este documentul central pentru toate tabelele proiectului: inventar, coloane, reguli, exemple și istoricul Flyway. Diagrama este și în `docs/database.drawio`, `.svg` și `.png`. Actualizează documentația și diagramele la fiecare schimbare de schemă, inclusiv pentru viitorul microserviciu.

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
5. **Conturi, autorizare și modificarea contactelor (ziua 2) — împărțit în două subetape, cu explicație între ele.**
   - **5A.1 — Backendul de înregistrare și autentificare, primul.** Adăugăm rolurile `USER` și `ADMIN` printr-o migrare nouă. Înregistrarea publică creează numai `USER`; contul admin se configurează separat pe server. Implementăm signup, login, logout, citirea utilizatorului curent, sesiunea cu cookie, parole hash și protecția CSRF. Verificăm prin teste și cereri HTTP că un cont se poate crea și autentifica, sesiunea persistă între cereri, logout o încheie, iar datele de acces/rolurile nu pot fi falsificate din browser. **Punct de învățare obligatoriu:** explicăm fișierele și fluxurile backend candidatului după verificare, înainte de a începe frontendul.
   - **5A.2 — Frontendul de conturi, după backend.** Înlocuim paginile React de pregătire cu formulare asincrone de înregistrare și login, adăugăm logout și afișăm starea sesiunii în navigare. Folosim contractul backend deja verificat și explicat. Verificăm în browser fluxul complet, inclusiv erori, cookie și CSRF. Explicăm frontendul înainte de a trece la 5B.
   - **5B — Creare, editare și ștergere contacte.** Adăugăm validarea datelor și operațiile în backend, cu reguli în service și verificarea autorului pe server la editare/ștergere. Legăm formularele React de API prin cereri asincrone. Verificare: vizitatorul poate doar citi, utilizatorul autentificat poate crea și modifica propriile contacte, iar modificarea contactelor altui autor este refuzată. Fotografia reală rămâne pasul 6.
6. **Fotografii încărcate de utilizator (ziua 3).** Implementăm upload `multipart/form-data`, validare de fișier, stocare într-un volum Docker și afișare publică. Verificare: imaginea rămâne disponibilă după repornire și poate fi înlocuită la editare.
7. **Export CSV (ziua 3).** Generăm CSV corect, inclusiv pentru texte cu virgule, ghilimele sau linii noi; includem referința fotografiei. Verificare: fișierul descărcat se deschide corect.
8. **Evenimentul de înregistrare în Kafka (zilele 3–4).** La crearea contului, API-ul publică un mesaj; `activity-service` îl consumă și salvează în SQL un rezultat observabil. Verificare: o înregistrare nouă poate fi urmărită de la API până la istoricul procesat.
9. **Comunicarea HTTP și pagina admin (ziua 4).** API-ul principal trimite către `activity-service` evenimente despre creare, editare și ștergere. Un endpoint intern de citire oferă istoricul către API-ul principal, care îl expune numai contului admin în `/api/admin/activities`; pagina `/admin/activity` îl afișează. Verificare: adminul vede evenimentele procesate, utilizatorul obișnuit și vizitatorul primesc refuz pe server.
10. **Pornirea completă și predarea (zilele 4–5).** Finalizăm Dockerfile-urile și Compose, verificările de pornire, testele țintite și README-ul. Clonăm proiectul într-un director nou și urmăm exact instrucțiunile; apoi urcăm codul pe GitHub și trimitem URL-ul.

La fiecare pas: implementăm, rulăm, verificăm, explicăm fluxul și actualizăm **Stare curentă** și **Următorul pas**. Un pas este încheiat doar după verificare.

## Criterii de finalizare

- Un evaluator poate clona repository-ul și porni aplicația urmând exclusiv README-ul și comenzile Docker Compose.
- Vizitatorul poate vedea și căuta contacte fără login.
- Utilizatorul își poate crea cont, se poate autentifica, poate adăuga un contact cu fotografie încărcată, își poate edita/șterge contactul și poate exporta CSV.
- Serverul refuză modificările neautorizate și modificarea contactelor altui autor.
- Înregistrarea publică un mesaj Kafka; consumerul îl procesează într-un mod observabil.
- Contul admin vede într-o pagină istoricul înregistrărilor și al modificărilor contactelor; accesul la date este refuzat pe server pentru ceilalți utilizatori.
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

**Pasul 4 este implementat și verificat.** `frontend/src/features/contacts/contactsApi.ts` trimite `GET /api/contacts` cu parametrul opțional `name` prin proxy-ul Vite. La cererea candidatului, frontendul a fost reorganizat pe funcționalități: `useContacts.ts` ține starea, întârzierea de 250 ms și anularea cererilor; `ContactsPage.tsx` alcătuiește pagina; `ContactSearch`, `ContactList` și `ContactCard` au responsabilități de afișare separate. `App.tsx` păstrează doar cadrul general. React Router și `navigation/` oferă rute pentru contacte, login, înregistrare și viitoarea pagină admin. Ultimele trei sunt numai pagini de pregătire; ruta admin nu expune date și nu apare în antetul public. Buildul TypeScript și lintul trec. În browser s-au verificat lista goală, două contacte temporare și filtrarea după reorganizare, navigarea și deschiderea directă a rutei admin. S-au verificat și eroarea de conexiune și reîncercarea înainte de reorganizare. Datele temporare au fost șterse, astfel baza locală nu are date demo.

**Pasul 5A.1 este implementat și verificat în backend.** Flyway V2 adaugă `users.role` cu valoarea implicită `USER` și constrângere pentru `USER`/`ADMIN`; V1 nu a fost modificată. Pachetul `auth/` separă înregistrarea (`signup/`), datele utilizatorului, configurarea Spring Security și citirea sesiunii (`session/`). `POST /api/auth/signup` validează email/parolă, normalizează emailul, salvează hash BCrypt și creează numai `USER`; răspunde 201/400/409. **Simplificare ulterioară pentru învățare:** loginul și logoutul folosesc acum filtrele integrate Spring Security, în locul unui `LoginController` și al salvării manuale a contextului. Loginul primește câmpurile de formular `email`/`password` la `POST /api/auth/login` și răspunde 204; identitatea se citește separat prin `GET /api/auth/me`. Spring gestionează schimbarea ID-ului sesiunii și înlocuirea tokenului CSRF. `POST /api/auth/logout` răspunde 204 și invalidează sesiunea, iar `GET /api/auth/csrf` dă numele headerului și tokenul necesar pentru cereri de modificare. `ADMIN_EMAIL` și `ADMIN_PASSWORD` sunt opționale în `.env`; inițializatorul creează un admin dacă nu există, fără să promoveze conturi `USER` existente. `/api/admin/**` este protejat prin rol pe server, dar încă nu există endpointul de activitate. Backendul are 20 de teste trecute cu Testcontainers; pe baza locală s-a aplicat V2, iar un smoke test real pe portul temporar 18080 a verificat health, cookie/token CSRF, lista publică și răspunsul 401 fără login. Procesul de test a fost oprit. Nu s-au adăugat conturi demo în baza locală.

**Nu sunt implementate încă** formularele React pentru conturi, crearea/editarea/ștergerea contactelor, fotografiile, CSV, Kafka (inclusiv mesajul la signup), istoricul de activitate, comunicarea HTTP de business sau rularea întregului sistem prin Compose. Nu folosim JPA; repository-ul folosește Spring JDBC. Health-ul microserviciului nu îndeplinește singur cerința de interacțiune HTTP dintre servicii. Nu marca aceste cerințe ca finalizate înainte de implementare și verificare.

## Următorul pas

**Punctul de învățare curent:** explică backendul 5A.1 candidatului în ordinea implementării și cu exemple din codul real: V2 și `users.role`, dependențele Security/Validation, `AppUser`/`UserRepository`, DTO-urile, `SignupService`, `UserDetailsService`/`PasswordEncoder`, loginul/logoutul configurate în `SecurityConfig`, sesiunea și cookie-ul, `SessionController` pentru `/me` și `/csrf`, rolul admin și testele. Explică faptul că loginul primește formular URL encoded, răspunde 204, iar `/me` întoarce JSON. Arată în DBeaver coloana `users.role` și V2 în `flyway_schema_history` dacă cere. **Următoarea implementare, numai după explicația backendului: 5A.2, formularele React.** Abia apoi 5B (operațiile de creare, editare și ștergere). Ruta admin cu date reale va fi finalizată după Kafka și istoricul microserviciului. Nu aplica din nou manual CREATE TABLE și nu modifica V1/V2 după aplicare.

## Plan de învățare pentru backendul 5A.1 (cinci pași)

Acești pași sunt ordinea de **învățare**, nu pașii de implementare ai întregului proiect. Candidatul vrea să înțeleagă codul real în sesiuni scurte, fără a trece la React până când poate explica backendul.

1. **Baza de date și înregistrarea:** `users`, Flyway V1/V2, cererea JSON, `SignupRequest`, `SignupController`, `SignupService`, BCrypt, `UserRepository`, `AppUser` și `UserResponse`. Ghidul de lucru verificat este `output/pdf/pasul-1-baza-de-date-si-inregistrare.pdf` (10 pagini, bazat pe commitul `44618e2`). Include comenzi PowerShell, inspecție vizuală în DBeaver și întrebări de recapitulare. **Acesta este pasul curent de studiu; existența PDF-ului nu înseamnă că utilizatorul l-a parcurs deja.**
2. **Loginul:** ce citește codul nostru din SQL și ce verifică mecanismul integrat Spring Security.
3. **Sesiunea, cookie-ul și CSRF:** ce păstrează serverul, ce trimite browserul și de ce cererile de modificare au nevoie de token.
4. **Permisiunile:** acces public, utilizator autentificat și rolul `ADMIN`, verificate pe server.
5. **Testele:** urmărirea unei verificări complete și explicarea fluxului cu propriile cuvinte, ca la interviu.

Pentru fiecare pas: citește fișierele existente în ordinea fluxului, urmărește o cerere concretă, verifică efectul practic și apoi explică fără ghid. Nu crea exemple fictive de cod în locul explicației codului implementat.

## Întrebări încă deschise

- Numărul de ore disponibile zilnic și data exactă a predării nu au fost precizate.
- CV-ul a fost menționat, dar nu a fost trimis încă în această conversație.
- Structura exactă și migrarea tabelului SQL pentru istoricul `activity-service` se stabilesc la pasul 8. Microserviciul deține tabelul și nu se bazează pe un istoric recuperat direct din topic de browser.
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
- 2026-09-24: la cererea candidatului, frontendul a fost modularizat pe `features/` și `navigation/`, iar React Router a introdus rute pentru contacte, login, înregistrare și un viitor istoric admin. Login, înregistrarea și istoricul admin sunt încă pagini de pregătire. S-a adăugat în plan un cont `ADMIN` creat separat și istoricul procesat din Kafka/HTTP, citit prin API-ul principal cu autorizare pe server.
- 2026-09-24: candidatul a cerut împărțirea pasului 5. Planul este acum 5A (signup/login/logout/sesiune, cu explicație și verificare completă) urmat de 5B (creare/editare/ștergere contacte). Nu au fost implementate funcții de autentificare prin această schimbare de plan.
- 2026-09-24: candidatul a stabilit ordinea din 5A: mai întâi backendul complet și verificat (5A.1), apoi explicația backendului, apoi frontendul (5A.2). Nu se încep formularele React în aceeași etapă cu implementarea backendului.
- 2026-09-24: pasul 5A.1 a implementat și verificat autentificarea numai în backend. V2 a adăugat rolurile, iar Spring Security oferă signup, login, logout, `/me`, CSRF și sesiune HTTP. Adminul opțional se creează din configurarea serverului. 20 de teste au trecut; migrarea V2 și endpointurile publice au fost verificate și pe baza locală. Diagramele, API-ul, README și `.env.example` au fost actualizate. Urmează explicația backendului candidatului, apoi 5A.2.
- 2026-09-24: la cererea candidatului de a reduce codul dificil de învățat, loginul și logoutul au fost mutate la mecanismele integrate Spring Security. Au fost eliminate controllerul și DTO-ul de login, precum și salvarea manuală a sesiunii; loginul folosește acum form fields și returnează 204. Cele 20 de teste backend continuă să treacă. Nu a început frontendul 5A.2.
- 2026-09-24: candidatul a fixat cinci pași de învățare pentru backendul 5A.1. A fost creat și verificat vizual un PDF de 10 pagini pentru pasul 1 (baza de date și înregistrarea), cu extrase din codul real, fluxul HTTP→SQL, exercițiu PowerShell/DBeaver și întrebări de recapitulare. Pasul 1 este pregătit pentru studiu, nu marcat ca deja înțeles.

## Instrucțiune pentru un alt chat AI

Înainte de modificări, citește acest fișier și inspectează starea reală a repository-ului. Tratează secțiunea **Stare curentă** ca istoric care poate deveni depășit: verifică fișierele și testele. Continuă cu **Următorul pas**, lucrează în pași mici, explică deciziile candidatului și actualizează progresul doar după verificare. Păstrează proiectul suficient de simplu pentru a fi înțeles la un interviu junior, dar implementează complet cerințele Netex.
